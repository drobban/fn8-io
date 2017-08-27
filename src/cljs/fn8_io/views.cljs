(ns fn8-io.views
  (:require [re-frame.core :as re-frame]
            [re-com.core :as re-com]
            [fn8-io.io.files :as files :refer [file-data]]
            [fn8-io.io.gfx :as gfx :refer [test-buffer]]
            [fn8-io.views.screen :as screen]
            [goog.string :as gstring]
            [goog.string.format]))

;; (defn links []
;;   [re-com/h-box
;;    :gap "1em"
;;    :children [[re-com/hyperlink-href
;;                :label "Screen"
;;                :href "#/"]
;;               [re-com/hyperlink-href
;;                :label "Upload file"
;;                :href "#/files"]
;;               [re-com/hyperlink-href
;;                :label "About"
;;                :href "#/about"]]])

;; File view
(defn obj->vec [obj]
  "Put object properties into a vector"
  (vec (map (fn [k] (aget obj k)) (.keys js/Object obj))))

(defn show-files
  [e]
  (let [files (obj->vec (.-files (.-target e)))]
    (files/put-upload e)
    (re-frame/dispatch [:set-file-list files])))

(defn file-row []
  (let [files (re-frame/subscribe [:file-list])]
    (fn []
      ;; (println (str "Rom loaded: " (vec (take (- 0xf00 0x200) (concat @file-data (repeat 0))))))
      [re-com/v-box
       :children [(for [file @files]
                    [:div
                     {:key (str (aget file "name"))}
                     [re-com/h-box
                      :gap "1em"
                      :children [
                                 [re-com/box
                                  :min-width "150px"
                                  :child [re-com/label :label (str (aget file "name"))]]
                                 [re-com/label :label (str (aget file "size") " bytes")]]]])]])))

(defn file-box []
  (fn []
    [re-com/v-box
     :gap "1em"
     :children [[:input
                 {:type "file"
                  :id "files"
                  :on-change (fn [e] (show-files e))}]
                [file-row]]]))

(defn file-panel []
  (let [toggle-button (re-frame/subscribe [:button-state])]
    (fn []
      [re-com/v-box
       :gap "1em"
       :children [[re-com/title
                   :label "File selection"
                   :level :level1]
                  [re-com/p "Select Chip-8 rom file"]
                  [file-box]]])))

;; Screen-panel
(defn home-title []
  (let [name (re-frame/subscribe [:name])]
    (fn []
      [re-com/title
       :label (str "Hello from " @name )
       :level :level1])))

(defn screen-panel []
  (let [toggle-button (re-frame/subscribe [:button-state])]
    (fn []
      [re-com/v-box
       :gap "1em"
       :width "100%"
       :align :center
       :children [[re-com/box
                   :child [screen/canvas {:id "Screen" :width 640 :height 320}]]]])))

;; about
(defn about-title []
  [re-com/title
   :label "Work in progress...."
   :level :level1])

(defn about-panel []
  [re-com/v-box
   :gap "1em"
   :children [[about-title]]])

;; main
(defn- panels [panel-name]
  (case panel-name
    :screen-panel [screen-panel]
    :about-panel [about-panel]
    :file-panel [file-panel]
    [:div]))

(defn show-panel [panel-name]
  [panels panel-name])

(def tab-icons
  [{:id :screen-panel    :label [:i {:class "zmdi zmdi-home"}]}
   {:id :file-panel      :label [:i {:class "zmdi zmdi-upload"}]}
   {:id :about-panel     :label [:i {:class "zmdi zmdi-info"}]}])

(defn tabs
  [tab]
  [re-com/h-box
   :align :center
   :justify :center
   :width "100%"
   :gap "8px"
   :children [[re-com/horizontal-bar-tabs
               :model     tab
               :tabs      tab-icons
               :on-change #(re-frame/dispatch [:set-tab %])]]])

(defn main-panel []
  (let [active-panel (re-frame/subscribe [:active-panel])
        active-tab (re-frame/subscribe [:tab])]
    (fn []
      [re-com/v-box
       :gap "1em"
       :width "100%"
       :height "100%"
       :children [[tabs active-tab]
                  [panels @active-tab]]])))
