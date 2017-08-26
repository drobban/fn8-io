(ns fn8-io.views.screen
  (:require [re-frame.core :as re-frame]
            [re-com.core :as re-com]
            [fn8-io.io.files :as files :refer [file-data]]
            [fn8-io.io.gfx :as gfx :refer [test-buffer]]
            [goog.string :as gstring]
            [goog.string.format]))

(defn test-print
  []
  (println test-buffer))
