(ns fn8-io.io.gfx)

(def gfx-atom (atom (vec (partition 64 (repeat 2048 0)))))

(defn draw-row
  " Draw row into canvas "
  [pix-size width row coll ctx]
  (loop [i 0]
    (if (= i 64)
      coll
      (do
        (if (= 1 (nth coll i))
          (do
            (set! (.-fillStyle ctx) "#33ff33")
            ;; (println (.-fillstyle ctx))
            (.fillRect ctx (* pix-size i) row pix-size pix-size))
          (do
            (set! (.-fillStyle ctx) "#000000")
            ;; (println (.-fillstyle ctx))
            (.fillRect ctx (* pix-size i) row pix-size pix-size)))
        (recur (inc i))))))

(defn clear-context!
  [ctx canvas]
  (.clearRect ctx 0 0 (.-width canvas) (.-height canvas)))


(defn display
  "
  Draw graphics buffer into canvas.
  Assumes canvas to be pix-size * width wide and pix-size * height high.
  "
  [coll screen-selection pix-size]
  (if (not= nil (js->clj (.getElementById js/document screen-selection)))
    (let [canvas (.getElementById js/document screen-selection)
          ctx (.getContext canvas "2d")]
      (clear-context! ctx canvas)
      (loop [i 0]
        (if (= i 32)
          coll
          (do
            (draw-row 10 64 (* i pix-size) (nth coll i) ctx)
            (recur (inc i))))))))
