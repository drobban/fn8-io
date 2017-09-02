(ns fn8-io.handlers
  (:require [re-frame.core :refer [register-handler path trim-v after] :as re-frame]
            [fn8-io.io.sound :refer [start-sound! stop-sound!]]))

(register-handler
 :initialize
 (fn [_ [_ state]]
   state))

(register-handler
 :filename
 trim-v
 (fn [db [filename]]
   (assoc db :filename filename)))

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
