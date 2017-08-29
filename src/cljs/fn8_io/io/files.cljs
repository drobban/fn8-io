(ns fn8-io.io.files
  (:require [reagent.core :refer [atom]]
            [re-frame.core :as re-frame]
            [fn8-io.machine.machine :refer [sim-com]]
            [cljs.core.async :as async :refer [put! chan <! >!]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(def first-file
  (map (fn [e]
         (let [target (.-currentTarget e)
               file (-> target .-files (aget 0))]
           (set! (.-value target) "")
           file))))

(def extract-result
  (map #(-> % .-target .-result js->clj)))

(def extract-name
  (map #(-> % .-name js->clj)))

;; two core.async channels to take file array and then file and apply above transducers to them.
(def upload-reqs (chan 1 first-file))
(def file-reads (chan 1 extract-result))
(def done-file (chan (async/buffer 1)))
(def name-chan (chan 1 extract-name))
;; (def name-chan (chan (async/buffer 1)))

; When file-event put the request on the channel
(defn put-upload [e]
  (put! upload-reqs e))

; Sends file over channel
(go-loop []
  (let [reader (js/FileReader.)
        file (<! upload-reqs)]
    (set! (.-onload reader) #(put! file-reads %))
    (.readAsBinaryString reader file)
    (>! name-chan file)
    (recur)))

; Sends done file over channel.
(go-loop []
  (let [file (vec (map #(.charCodeAt %) (<! file-reads)))]
    (>! done-file file))
  (recur))

