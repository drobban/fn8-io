(ns fn8-io.views
  (:require [re-frame.core :as re-frame]
            [re-com.core :as re-com]
            [fn8-io.io.files :as files]
            [goog.string :as gstring]
            [goog.string.format]
            ))

(defn links []
  [re-com/h-box
   :gap "1em"
   :children [[re-com/hyperlink-href
               :label "Screen"
               :href "#/"]
              [re-com/hyperlink-href
               :label "Upload file"
               :href "#/files"]
              [re-com/hyperlink-href
               :label "About"
               :href "#/about"]]])

;; File view
(defn file-row [[name size]]
  [re-com/h-box
   :gap "1em"
   :children [[re-com/label :label name] [re-com/label :label size]]])

(defn file-box []
  (let [files (re-frame/subscribe [:file-list])]
    (fn []
      [re-com/v-box
       :gap "1em"
       :children [[:input {:type "file"
                           :id "files"
                           :multiple {}
                           :on-change (fn [e] (files/show-files e))}]
                  (for [file @files]
                    (file-row file)
                    )]])))


  ;; [re-com/v-box
  ;;  :gap "1em"
  ;;  :children [(for [file @files]
  ;;               [:div {:key file} (str file)])]]
  ;; ]

(defn file-panel []
  (let [toggle-button (re-frame/subscribe [:button-state])]
    (fn []
      [re-com/v-box
       :gap "1em"
       :children [[links]
                  [re-com/title
                   :label "File"
                   :level :level1]
                  [file-box]]])))

;; home

(defn home-title []
  (let [name (re-frame/subscribe [:name])]
    (fn []
      [re-com/title
       :label (str "Hello from " @name )
       :level :level1])))

(defn home-panel []
  (let [toggle-button (re-frame/subscribe [:button-state])]
    (fn []
      [re-com/v-box
       :gap "1em"
       :children [[links]
                  [home-title]
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
                   :on-click #(re-frame/dispatch [:toggle-button-state ""])]]])))


;; about

(defn about-title []
  [re-com/title
   :label "Work in progress...."
   :level :level1])

(defn about-panel []
  [re-com/v-box
   :gap "1em"
   :children [[links]
              [about-title]]])

;; main

(defn- panels [panel-name]
  (case panel-name
    :home-panel [home-panel]
    :about-panel [about-panel]
    :file-panel [file-panel]
    [:div]))

(defn show-panel [panel-name]
  [panels panel-name])

(defn main-panel []
  (let [active-panel (re-frame/subscribe [:active-panel])]
    (fn []
      [re-com/v-box
       :height "100%"
       :children [[panels @active-panel]]])))
