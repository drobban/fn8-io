(ns fn8-io.core
  (:require [reagent.core  :as reagent :refer [atom]]
            [re-frame.core :as re-frame]
            [fn8-io.db :as db]
            [fn8-io.events]
            [fn8-io.subs]
            [fn8-io.handlers]
            [fn8-io.routes :as routes]
            [fn8-io.views :as views]
            [fn8-io.config :as config]
            [fn8-io.io.gfx :as gfx]
            [fn8-io.io.keyboard :as keyboard]
            [fn8-io.machine.machine :as machine]
            [cljs.core.async :as async]
            [fn8-io.io.files :as files]
            )

  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  )

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app"))
  (keyboard/keyboard-init! :set-key))

(defn ^:export init []
  (routes/app-routes)
  (re-frame/dispatch-sync [:initialize-db db/default-db])
  (dev-setup)
  (mount-root))


(defonce loaded (atom machine/internals))
(def sim-com (async/chan (async/buffer 1)))
(def sim-state (atom nil))


(go-loop []
  (async/<! (async/timeout 17))
  (gfx/display @gfx/gfx-atom "Screen" 10)
  (recur))
;; (js/setInterval #(gfx/display gfx/gfx-atom "Screen" 10) 17)

(go-loop []
  (let [command (async/<! sim-com)]
    (reset! sim-state command))
  (recur))

(go-loop []
  (async/<! (async/timeout 5))
  (cond
    (= :start @sim-state) (do
                            (swap! loaded #(-> % machine/chip-cpu (update :pc + 2)))
                            (reset! gfx/gfx-atom (machine/show-gfx-buff @loaded)))
    (= :load @sim-state) (do
                           (reset! sim-state nil)
                           (reset! loaded (machine/read-rom @files/file-data @loaded))
                           )
    (= :stop @sim-state) (do
                           (reset! sim-state nil)
                           (reset! loaded machine/internals)
                           (reset! gfx/gfx-atom (machine/show-gfx-buff @loaded)))
    :else nil)
  (recur))
