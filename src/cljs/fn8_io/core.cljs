(ns fn8-io.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [fn8-io.events]
            [fn8-io.subs]
            [fn8-io.routes :as routes]
            [fn8-io.views :as views]
            [fn8-io.config :as config]))


(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (routes/app-routes)
  (re-frame/dispatch-sync [:initialize-db])
  (dev-setup)
  (mount-root))
