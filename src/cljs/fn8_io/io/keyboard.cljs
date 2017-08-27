(ns fn8-io.io.keyboard
  (:require [reagent.core :refer [atom]]
            [re-frame.core :as re-frame]
            [cljs.core.async :refer [put! chan <! >!]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defn keyboard-init!
  "
  Sends key event to dispatch
  This approach seems to be problematic & might be replaced
  "
  [dispatch-key]
  (do
    (.addEventListener js/document "keyup" (fn[e](re-frame/dispatch [dispatch-key [:unset (.-key e)]])) false)
    (.addEventListener js/document "keydown" (fn[e](re-frame/dispatch [dispatch-key [:set (.-key e)]])) false)))
