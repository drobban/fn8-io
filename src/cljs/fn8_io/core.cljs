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
            [fn8-io.io.keyboard :as keyboard]))


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
  (mount-root)
  (js/setInterval #(gfx/display @gfx/gfx-atom "Screen" 10) 17))
