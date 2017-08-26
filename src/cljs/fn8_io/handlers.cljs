(ns fn8-io.handlers
  (:require [re-frame.core :refer [register-handler path trim-v after] :as re-frame]
            [fn8-io.io.sound :refer [start-sound! stop-sound!]]))

(register-handler
 :initialize
 (fn [_ [_ state]]
   state))

(register-handler
 :set-file-list
 trim-v
 (fn [db [files]]
   (assoc db :files files)))

(register-handler
 :set-rom
 trim-v
 (fn [db [rom]]
   (assoc db :rom rom)))

(register-handler
 :toggle-button-state
 trim-v
 (fn [db _]
   (do
     (if (get-in db [:toggle-button :state])
       (stop-sound!)
       (start-sound!))
     (update-in db [:toggle-button :state] not))))
