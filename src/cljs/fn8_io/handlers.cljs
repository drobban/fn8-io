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
 :set-tab
 trim-v
 (fn [db [tab]]
   (assoc db :tab tab)))

(register-handler
 :set-key
 trim-v
 (fn [db [[action key]]]
   (cond
     (= action :set) (assoc db :active-key key)
     (= action :unset) (if (= (:active-key db) key)
                         (assoc db :active-key nil)
                         db)
     :else (assoc db :active-key "undefined action"))))

(register-handler
 :toggle-button-state
 trim-v
 (fn [db _]
   (do
     (if (get-in db [:toggle-button :state])
       (stop-sound!)
       (start-sound!))
     (update-in db [:toggle-button :state] not))))
