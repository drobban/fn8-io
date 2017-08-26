(ns fn8-io.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
 :file-list
 (fn [db]
   (:files db)))

(re-frame/reg-sub
 :rom-data
 (fn [db]
   (:rom db)))

(re-frame/reg-sub
 :name
 (fn [db]
   (:name db)))

(re-frame/reg-sub
 :button-state
 (fn [db]
   (:toggle-button db)))

(re-frame/reg-sub
 :active-panel
 (fn [db _]
   (:active-panel db)))
