(ns fn8-io.io.keyboard
  (:require [reagent.core :refer [atom]]
            [re-frame.core :as re-frame]
            [cljs.core.async :refer [put! chan <! >!]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(def keymap {\1 0x1 \2 0x2 \3 0x3 \4 0xC
             \q 0x4 \w 0x5 \e 0x6 \r 0xD
             \a 0x7 \s 0x8 \d 0x9 \f 0xE
             \z 0xA \x 0x0 \c 0xB \v 0xF})

(defn key-action
  "
  change key val in atom.
  "
  [m [action key]]
  (condp = action
    :set (assoc m :input (keymap key))
    :unset (if (= (:input m) (keymap key))
                        (assoc m :input nil)
                        m)
    :else m))

(defn keyboard-init!
  "
  Sends key event to dispatch
  This approach seems to be problematic & might be replaced
  "
  [a]
  (do
    (.addEventListener js/document "keyup" (fn[e](swap! a #(key-action % [:unset (.-key e)]))) false)
    (.addEventListener js/document "keydown" (fn[e](swap! a #(key-action % [:set (.-key e)]))) false)))
