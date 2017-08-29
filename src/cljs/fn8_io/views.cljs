(ns fn8-io.views
  (:require [re-frame.core :as re-frame]
            [re-com.core :as re-com]
            [fn8-io.io.files :as files]
            [fn8-io.io.gfx :as gfx]
            [fn8-io.machine.machine :refer [sim-com loaded]]
            [fn8-io.views.screen :as screen]
            [goog.string :as gstring]
            [goog.string.format]
            [cljs.core.async :as async]
            ))

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

;; Button
(defn zmdi-button
  []
  (fn[zmdi-type onclick-fn]
    [re-com/box
     :justify :center
     :align :center
     :width "40px"
     :height "40px"
     :class (str "btn btn-default zmdi " zmdi-type)
     :attr {:on-click onclick-fn}
     :child [:div]]
    ))

(defn zmdi-file-button
  []
  (fn[zmdi-type onclick-fn onchange-fn]
    [:label {:for "files"
             :style {:margin "0px"}}
     [re-com/box
      :justify :center
      :align :center
      :width "40px"
      :height "40px"
      :class (str "btn btn-default zmdi " zmdi-type)
      ;; :attr {:for "files"}
      :child [:input
              {:type "file"
               :id "files"
               :style {:display "none"}
               :on-click onclick-fn
               :on-change onchange-fn}]]]))

(defn screen-panel []
  (let []
    (fn []
      [re-com/v-box
       :gap "5px"
       :width "100%"
       :align :center
       :children [[re-com/box
                   :child [screen/canvas {:id "Screen" :width 640 :height 320}]]
                  [re-com/h-box
                   :align :center
                   :gap "2px"
                   :width "640px"
                   :height "50px"
                   :style {:background "#f5f5f5"
                           :border "1px solid #cfcfcf"}
                   :children [[zmdi-button "zmdi-play" #(async/put! sim-com :start)]
                              [zmdi-button "zmdi-pause" #(async/put! sim-com :pause)]
                              [zmdi-button "zmdi-stop" #(async/put! sim-com :stop)]
                              [zmdi-file-button "zmdi-file" #(async/put! sim-com :load) (fn[e](show-files e))]
                              [re-com/label :label (:filename @loaded)]]]]])))

;; main
(defn main-panel []
  (let []
    (fn []
      [re-com/v-box
       :gap "1em"
       :width "100%"
       :height "100%"
       :style {:padding-top "20px"}
       :children [[screen-panel]]])))
