(ns fn8-io.views
  (:require [re-frame.core :as re-frame]
            [re-com.core :as re-com]))


;; home

(defn home-title []
  (let [name (re-frame/subscribe [:name])]
    (fn []
      [re-com/title
       :label (str "Hello from " @name )
       :level :level1])))

(defn link-to-about-page []
  [re-com/hyperlink-href
   :label "go to About Page"
   :href "#/about"])

(defn home-panel []
  (let [toggle-button (re-frame/subscribe [:button-state])]
    (fn []
      [re-com/v-box
       :gap "1em"
       :children [[home-title]
                  [re-com/p
                   "This is the future I/O of the fn8 emulator"
                   [:br]
                   "Outputs; Sound & Graphics. "
                   [:br]
                   "Inputs; 0x0 - 0xF keyboard"]
                  [re-com/button
                   :label (if (:state @toggle-button)
                            "Stop sound"
                            "Start sound")
                   :on-click #(re-frame/dispatch [:toggle-button-state ""])]
                  [link-to-about-page]]])))


;; about

(defn about-title []
  [re-com/title
   :label "Work in progress...."
   :level :level1])

(defn link-to-home-page []
  [re-com/hyperlink-href
   :label "go to Home Page"
   :href "#/"])

(defn about-panel []
  [re-com/v-box
   :gap "1em"
   :children [[about-title]  [link-to-home-page]]])


;; main

(defn- panels [panel-name]
  (case panel-name
    :home-panel [home-panel]
    :about-panel [about-panel]
    [:div]))

(defn show-panel [panel-name]
  [panels panel-name])

(defn main-panel []
  (let [active-panel (re-frame/subscribe [:active-panel])]
    (fn []
      [re-com/v-box
       :height "100%"
       :children [[panels @active-panel]]])))
