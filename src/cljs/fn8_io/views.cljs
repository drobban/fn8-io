(ns fn8-io.views
  (:require [re-frame.core :as re-frame]
            [re-com.core :as re-com]
            [fn8-io.io.files :as files :refer [file-data]]
            [fn8-io.io.gfx :as gfx :refer [test-buffer]]
            [fn8-io.views.screen :as screen]
            [goog.string :as gstring]
            [goog.string.format]))

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
      ;; (println (take (- 0xf00 0x200) (concat @file-data (repeat 0))))
      (println (str "Rom loaded: " (take (- 0xf00 0x200) (concat @file-data (repeat 0)))))
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
                                 [re-com/label :label (str (aget file "size"))]
                                 ]]])]])))

(defn file-box []
  (fn []
    [re-com/v-box
     :gap "1em"
     :children [
                [:label {:for "files"}
                [re-com/md-circle-icon-button
                 :md-icon-name "zmdi-upload"]
                 ]
                [:input
                 {:type "file"
                  :style {:display "none"}
                  :id "files"
                  ;; :multiple {}
                  :on-change (fn [e] (show-files e))}]
                [file-row]]]))

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

;; Screen-panel
(defn home-title []
  (let [name (re-frame/subscribe [:name])]
    (fn []
      [re-com/title
       :label (str "Hello from " @name )
       :level :level1])))

;; (defn screen-panel []
;;   (let [toggle-button (re-frame/subscribe [:button-state])]
;;     (fn []
;;       (println (screen/test-print))
;;       [re-com/v-box
;;        :gap "1em"
;;        :children [[links]
;;                   [home-title]
;;                   [re-com/p
;;                    "This is the future I/O of the fn8 emulator"
;;                    [:br]
;;                    "Outputs; Sound & Graphics. "
;;                    [:br]
;;                    "Inputs; 0x0 - 0xF keyboard"]
;;                   [re-com/button
;;                    :label (if (:state @toggle-button)
;;                             "Stop sound"
;;                             "Start sound")
;;                    :on-click #(re-frame/dispatch [:toggle-button-state ""])]]])))

(defn screen-panel []
  (let [toggle-button (re-frame/subscribe [:button-state])]
    ;; (.log js/console test-image)
    (fn []
      [re-com/v-box
       :gap "1em"
       :children [[links]
                  [re-com/box
                   :child [screen/canvas {:id "Screen" :width 640 :height 320}]]
                  [re-com/button
                   :label "refresh"
                   :on-click #(gfx/display gfx/test-buffer "Screen" 10)]]])))

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
    :screen-panel [screen-panel]
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
