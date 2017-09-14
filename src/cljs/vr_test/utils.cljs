(ns vr-test.utils
  (:require-macros [hiccups.core :as hiccups :refer [html]])
  (:require [cljsjs.aframe :as aframe]
            [secretary.core :as secretary :include-macros true]
            [clojure.string :as string]
            [hiccups.runtime :as hiccupsrt]))

(defn html-af [v]
  (html (map v #(conj % ""))))
