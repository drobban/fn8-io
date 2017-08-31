(ns fn8-io.machine.machine
  (:require [clojure.core.match :refer [match]]
            [fn8-io.io.gfx :as gfx]
            [cljs.core.async :as async])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

;; Supported input is a hexkeybord range 0 to F
;; Programs start at 0x200
;; Display buffer at 0xF00
;; Stack at 0xEA0

(def internals {:filename ""
                :pc 0x200
                :input nil
                :delay-timer 0
                :sound-timer 0
                :memory (into [] (repeat 0x1000 0))
                :vreg (into [] (repeat 0x10 0))
                :ireg 0
                :stack []})

;;
;; Helper functions
;;

(defn pack-to-byte
  "
  Packs a vector of 8 bits into a byte
  "
  [bit-vector]
  (reduce-kv (fn[m k v] (+ m (bit-shift-left v k))) 0 (into [] (reverse bit-vector))))


(defn unpack-to-vector
  "
  Unpacks a byte into a vector of bits
  "
  [word]
  (loop [value word
         x 128
         bits []]
    (if (= 8 (count bits))
      bits
      (recur (rem value x) (/ x 2) (conj bits (quot value x))))))


;; Some old code, probably needs a rewrite.
;;
;; will be used to draw sprite into vector of bit pixels.

(defn trim_into [board_selection brick_selection pos]
  "Trim into returns a new brick data that will fit into board at pos"
  (if (< (count board_selection) (+ pos (count brick_selection)))
    (subvec brick_selection
            0
            (max 0 (- (count brick_selection)
                      (- (+ pos (count brick_selection))
                         (count board_selection)))))
    (subvec brick_selection
            (max 0 (min (count brick_selection)
                        (- (count brick_selection)
                           (+ (count brick_selection) pos))))
            (count brick_selection))))

(defn mask_in [board_row brick_row pos_col]
  "Merges a brick row into board row at a given column"
  (let [beginning (subvec board_row
                          0
                          (max 0 pos_col))
        selected (subvec board_row
                         (max 0 pos_col)
                         (+ (count (trim_into board_row brick_row pos_col)) (max 0 pos_col)))
        end (subvec board_row
                    (+ (max 0 pos_col) (count (trim_into board_row brick_row pos_col)))
                    (count board_row))]
    (vec (concat beginning (map bit-xor selected (trim_into board_row brick_row pos_col)) end))))

(defn brick_position [gameboard brick pos_col pos_row]
  "Merges brick into gameboard at position row & col"
  (let [game_row (min (count gameboard) (max 0 pos_row))
        trimmed_brick (trim_into gameboard brick (min (count gameboard) pos_row))]
    (loop [processed_rows (subvec gameboard 0 game_row)
           selected_row (nth gameboard game_row)
           rest_rows (subvec gameboard (+ game_row 1) (count gameboard))
           row_idx 0]
      (if (= row_idx (count trimmed_brick))
        (if (nil? selected_row)
          (into processed_rows rest_rows)
          (into (conj processed_rows selected_row) rest_rows))
        (recur (conj processed_rows (mask_in selected_row (nth trimmed_brick row_idx) pos_col))
               (first rest_rows)
               (rest rest_rows)
               (inc row_idx))))))

(defn clear-screen
  "
  Clears gfx bytes.
  Graphics refresh is stored at 0xF00-0xFFF
  "
  [m]
  (assoc-in m [:memory]
            (into [] (map-indexed (fn [idx e]
                                    (if (<= 0xF00 idx 0xFFF)
                                      0
                                      e))
                                  (:memory m)))))

(defn call-return
  "
  00EE
  Jumps back to where the stack tells it to.
  Related to the call function.
  "
  [m]
  (assoc m :pc (last (:stack m)) :stack (pop (:stack m))))

(defn jump
  "
  1NNN
  Jumps to NNN - 1, takes the emulator work flow into account.
  "
  [m location]
  (assoc-in m [:pc] (- location 2)))
;; The program counter will increment in every iteration
;; So the jump will take that into account.


(defn call
  "
  2NNN
  Calls subroutine at NNN.
  Keeps notes in the stack on how to return
  "
  [m location]
  (-> m
      (update :stack #(conj % (:pc m)))
      (assoc :pc (- location 2))))

(defn skip-if-val
  "
  3XNN
  4XNN
  Inc PC if (f Vreg(x) value) -> True
  To be used with eq and neq ops
  "
  [m f x value]
  (if (f (nth (:vreg m) x) value)
    (update-in m [:pc] + 2)
    m))

(defn skip-if-vreg
  "
  5XY0
  9XY0
  Inc PC if (f Vreg(x) Vreg(y)) -> True
  To be used with eq and neq ops
  "
  [m f x y]
  (if (f (nth (:vreg m) x) (nth (:vreg m) y))
    (update-in m [:pc] + 2)
    m))

(defn set-vreg
  "
  6XNN
  Sets Vreg(x) to value NN
  "
  [m x value]
  (assoc-in m [:vreg x] value))

(defn add-to-vreg
  "
  7XNN
  Adds NN to Vreg(x)
  "
  [m x value]
  (update-in m [:vreg x] #(mod (+ % value) 0x100)))

(defn set-vreg-xy
  "
  8XY0
  Sets Vreg(x) to value of Vreg(y)
  "
  [m x y]
  (assoc-in m [:vreg x] (nth (:vreg m) y)))

(defn set-vreg-with
  "
  8XY1 - or
  8XY2 - and
  8XY3 - xor
  "
  [m f x y]
  (update-in m [:vreg x] f (nth (:vreg m) y)))

(defn set-vreg-add
  "
  8XY4 - adds
  Emulates carry flag in Vreg(0xF)
  "
  [m x y]
  (let [carry (< 0xFF (+ (nth (:vreg m) x) (nth (:vreg m) y)))
        sum (assoc-in m [:vreg x] (bit-and 0xFF (+ (nth (:vreg m) x) (nth (:vreg m) y))))]
    (if carry
      (assoc-in sum [:vreg 0xF] 1)
      (assoc-in sum [:vreg 0xF] 0))))

(defn set-vreg-suby
  "
  8XY5
  Vreg(x) = Vreg(x) - Vreg(y)
  Vreg(0xF) == 1 when no borrow
  "
  [m x y]
  (let [sum (bit-and 0xFF (+ (nth (:vreg m) x) (bit-xor (nth (:vreg m) y) 0xFF) 1))
        stored (assoc-in m [:vreg x] sum)
        borrow (< (get-in m [:vreg x]) (get-in m [:vreg y]))]
    (if borrow
      (assoc-in stored [:vreg 0xF] 0)
      (assoc-in stored [:vreg 0xF] 1))))

(defn vreg-shift-right
  "
  8XY6
  Bit shifts Vreg(x) right 1 bit.
  "
  [m x]
  (let [vf (assoc-in m [:vreg 0xF] (bit-and 0x1 (nth (:vreg m) x)))]
    (update-in vf [:vreg x] #(bit-shift-right % 1))))

(defn set-vreg-subx
  "
  8XY7
  Vreg(x) = Vreg(y) - Vreg(x)
  Vreg(0xF) == 1 when no borrow
  "
  [m x y]
  (let [sum (bit-and 0xFF (+ (nth (:vreg m) y) (bit-xor (nth (:vreg m) x) 0xFF) 1))
        stored (assoc-in m [:vreg x] sum)
        borrow (< (get-in m [:vreg y]) (get-in m [:vreg x]))]
    (assoc-in stored [:vreg 0xF] (if borrow
                                   0
                                   1))))

(defn vreg-shift-left
  "
  8XYE
  Bit shift Vreg(x) left 1 bit.
  "
  [m x]
  (let [vf (assoc-in m [:vreg 0xF] (bit-shift-right (bit-and 0x80 (nth (:vreg m) x)) 7))]
    (update-in vf [:vreg x] #(bit-and 0xFF (bit-shift-left % 1)))))

(defn set-ireg
  "
  ANNN
  Sets Ireg with address
  "
  [m location]
  (assoc-in m [:ireg] location))

(defn jump-reg
  "
  BNNN
  "
  [m location]
  (jump m (+ location (get-in m [:vreg 0]))))

(defn set-vreg-rand
  "
  CXNN
  Sets Vreg(x) with random value 0 - ulimit
  "
  [m x ulimit]
  (assoc-in m [:vreg x] (rand-int (+ ulimit 1))))

(defn draw
  "
  DXYN
  Draw sprite in location Ireg to coordinate Vreg(x) and Vreg(y)
  Draws N lines of the sprite.
  "
  [m x y n]
  (let [sprite (mapv vec (map unpack-to-vector (subvec (:memory m) (:ireg m) (+ (:ireg m) n))))
        graphics (mapv vec (partition 64 (apply concat (mapv unpack-to-vector (subvec (:memory m) 0xF00 0x1000)))))
        xpos (get-in m [:vreg x])
        ypos (get-in m [:vreg y])
        new_gfx_bytes (mapv pack-to-byte (mapv vec (partition 8 (apply concat (brick_position graphics sprite xpos ypos)))))]
    (update m :memory #(vec (concat (subvec % 0x0 0xf00) new_gfx_bytes)))))

(defn read-key
  "
  EX9E
  EXA1
  Read key, if (f read-key Vreg(x)) -> True skip next
  "
  [m f x]
  (if (f (:input m) (get-in m [:vreg x]))
    (update m :pc + 2)
    m))

(defn get-delay
  "
  FX07
  Get delay and store in Vreg(x)
  "
  [m x]
  (assoc-in m [:vreg x] (:delay-timer m)))

(defn blocking-read-key
  "
  FX0A
  if let to get input, stores it in Vreg(x)
  else reruns op-code by decrementing PC and act blocking
  "
  [m x]
  (if-let [input (:input m)]
    (-> m
        (assoc :input nil)
        (assoc-in [:vreg 0] input))
    (assoc m :pc (- (:pc m) 2))))

(defn set-delay
  "
  FX15
  Set delay timer to value stored in Vreg(x)
  "
  [m x]
  (assoc m :delay-timer (get-in m [:vreg x])))

(defn set-sound-timer
  "
  FX18
  Sets sound timer. Plays tone for duration of Vreg(x)/60 seconds
  "
  [m x]
  (assoc m :sound-timer (get-in m [:vreg x])))

(defn add-to-ireg
  "
  FX1E
  Adds Vreg(x) to Ireg
  "
  [m x]
  (update m :ireg + (get-in m [:vreg x])))

(defn set-font
  "
  FX29
  Store location to font 0x0 - 0xF from Vreg(x) to Ireg
  Every font-sprite is 5byte in size and is found at location 0x000 0x04B
  "
  [m x]
  (let [selected-font (get-in m [:vreg x])
        font-size 5]
    (assoc m :ireg (* selected-font font-size))))

(defn vreg-to-bcd
  "
  FX33
  Converts Vreg(x) value to Binary coded decimal.
  It stores 100's in I+0, 10's in I+1 & 1's in I+2
  "
  [m x]
  (let [value (get-in m [:vreg x])
        i (:ireg m)]
    (-> m
        (assoc-in [:memory i] (int (/ value 100)))
        (assoc-in [:memory (+ i 1)] (int (mod (/ value 10) 10)))
        (assoc-in [:memory (+ i 2)] (int (mod value 10))))))

(defn vreg-dump
  "
  FX55
  Dump Vreg(0) - Vreg(x) including x, to Memory(I)
  "
  [m x]
  (let [vregload (subvec (:vreg m) 0x0 (inc x))
        mem-beg (subvec (:memory m) 0x0 (:ireg m))
        mem-end (subvec (:memory m) (+ (:ireg m) (inc x)) (count (:memory m)))]
    (assoc m :memory (vec (concat mem-beg vregload mem-end)))))

(defn vreg-load
  "
  FX65
  Load Vreg(0) - Vreg(x) including x, from Memory(I)
  "
  [m x]
  (let [ending (subvec (:vreg m) (inc x) (count (:vreg m)))
        memload (subvec (:memory m) (:ireg m) (+ (:ireg m) (inc x)))]
    (assoc m :vreg (apply conj memload ending))))

(defn chip-cpu
  "
  NNN: address
  NN: 8-bit constant
  N: 4-bit constant
  X and Y: 4-bit register identifier
  PC : Program Counter
  I : 16bit register (For memory address) (Similar to void pointer)

  Note to self;
  MFB - MS First byte
  LFB - LS First byte
  MSB - MS Second byte
  LSB - LS Second byte
  fb - First byte
  sb - Second byte
  "
  [m]
  (let [[fb sb] (subvec (:memory m) (:pc m) (+ (:pc m) 2))
        mfb (bit-shift-right fb 4)
        lfb (bit-and 0x0f fb)
        addr (bit-or (bit-shift-left lfb 8) sb)
        msb (bit-shift-right sb 4)
        lsb (bit-and 0x0f sb)]
    (match [mfb sb lsb]
           [0x0 0x00 0x0] m
           [0x0 0xe0 _] (clear-screen m)
           [0x0 0xee _] (call-return m)
           [0x0 _ _] m ;; 0NNN Call RCA prog not implemented
           [0x1 _ _] (jump m addr)
           [0x2 _ _] (call m addr)
           [0x3 _ _] (skip-if-val m = lfb sb)
           [0x4 _ _] (skip-if-val m not= lfb sb)
           [0x5 _ _] (skip-if-vreg m = lfb msb)
           [0x6 _ _] (set-vreg m lfb sb)
           [0x7 _ _] (add-to-vreg m lfb sb)
           [0x8 _ 0x0] (set-vreg-xy m lfb msb)
           [0x8 _ 0x1] (set-vreg-with m bit-or lfb msb)
           [0x8 _ 0x2] (set-vreg-with m bit-and lfb msb)
           [0x8 _ 0x3] (set-vreg-with m bit-xor lfb msb)
           [0x8 _ 0x4] (set-vreg-add m lfb msb)
           [0x8 _ 0x5] (set-vreg-suby m lfb msb)
           [0x8 _ 0x6] (vreg-shift-right m lfb)
           [0x8 _ 0x7] (set-vreg-subx m lfb msb)
           [0x8 _ 0xe] (vreg-shift-left m lfb)
           [0x9 _ 0x0] (skip-if-vreg m not= lfb msb)
           [0xa _ _] (set-ireg m addr)
           [0xb _ _] (jump-reg m addr)
           [0xc _ _] (set-vreg-rand m lfb sb)
           [0xd _ _] (draw m lfb msb lsb)
           [0xe 0x9e _] (read-key m = lfb)
           [0xe 0xa1 _] (read-key m not= lfb)
           [0xf 0x07 _] (get-delay m lfb)
           [0xf 0x0a _] (blocking-read-key m lfb)
           [0xf 0x15 _] (set-delay m lfb)
           [0xf 0x18 _] (set-sound-timer m lfb)
           [0xf 0x1e _] (add-to-ireg m lfb)
           [0xf 0x29 _] (set-font m lfb)
           [0xf 0x33 _] (vreg-to-bcd m lfb)
           [0xf 0x55 _] (vreg-dump m lfb)
           [0xf 0x65 _] (vreg-load m lfb)
           :else m)))

(defn step-machine
  [m]
  (-> m chip-cpu (update :pc + 2)))

(defn read-rom
  "
  Filename loaded into 0x200
  Set PC to 0x200
  "
  [rom m]
  (let [mem-rom (take (- 0xf00 0x200) (concat rom (repeat 0)))
        beginning (subvec (:memory m) 0x0 0x200)
        ending (subvec (:memory m) 0xf00 0x1000)]
    (assoc m :memory (vec (concat beginning mem-rom ending)))))

(defn show-gfx-buff
  [m]
  (mapv vec (partition 64 (apply concat (mapv unpack-to-vector (subvec (:memory m) 0xF00 0x1000))))))

(defonce loaded (atom internals))
(def sim-com (async/chan (async/buffer 1)))
(def sim-state (atom nil))
