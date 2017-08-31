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
  (swap! machine/loaded update :delay-timer dec)
  (recur))

(go-loop []
  (let [command (async/<! machine/sim-com)]
    (reset! machine/sim-state command))
  (recur))

(go-loop []
  (let []
    (async/<! (async/timeout 1))
    (condp = @machine/sim-state
      :start (do
               (swap! machine/loaded #(nth 10 (iterate machine/step-machine %)))
               (reset! gfx/gfx-atom (machine/show-gfx-buff @machine/loaded)))
      :load (let [file (async/<! files/done-file)
                  file-name (async/<! files/name-chan)]
              (reset! machine/loaded machine/internals)
              (swap!  machine/loaded assoc :filename file-name)
              (reset! machine/sim-state nil)
              (reset! machine/loaded (machine/read-rom file @machine/loaded))
              (re-frame/dispatch [:filename file-name]))
      :stop  (do
               (reset! machine/sim-state nil)
               (reset! machine/loaded machine/internals)
               (reset! gfx/gfx-atom (machine/show-gfx-buff @machine/loaded))
               (re-frame/dispatch [:filename ""]))
      :pause  (reset! machine/sim-state nil)
      nil)
    (recur)))

;; (swap! machine/loaded #((nth (iterate (fn[m](-> m machine/chip-cpu (update :pc + 2))) %) 2000)))
