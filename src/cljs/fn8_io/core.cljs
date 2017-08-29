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
            [fn8-io.io.files :as files])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

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


(go-loop []
  (async/<! (async/timeout 17))
  (gfx/display @gfx/gfx-atom "Screen" 10)
  (recur))
;; (js/setInterval #(gfx/display gfx/gfx-atom "Screen" 10) 17)

(go-loop []
  (let [command (async/<! machine/sim-com)]
    (reset! machine/sim-state command))
  (recur))

(go-loop []
  (let []
    (cond
      (= :start @machine/sim-state) (doseq [i (range 50)]
                                      (swap! machine/loaded #(-> % machine/chip-cpu (update :pc + 2)))
                                      (reset! gfx/gfx-atom (machine/show-gfx-buff @machine/loaded))
                                      (if (= i 49)
                                        (async/<! (async/timeout 20))))
      (= :load @machine/sim-state) (let [file (async/<! files/done-file)
                                         file-name (async/<! files/name-chan)]
                                     (do
                                       (reset! machine/loaded machine/internals)
                                       (swap!  machine/loaded assoc :filename file-name)
                                       (reset! machine/sim-state nil)
                                       (reset! machine/loaded (machine/read-rom file @machine/loaded))))
      (= :stop @machine/sim-state) (do
                                     (reset! machine/sim-state nil)
                                     (reset! machine/loaded machine/internals)
                                     (reset! gfx/gfx-atom (machine/show-gfx-buff @machine/loaded)))
      (= :pause @machine/sim-state) (reset! machine/sim-state nil)
      :else (async/<! (async/timeout 1000)))
    (recur)))
