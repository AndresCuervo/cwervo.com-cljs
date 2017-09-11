(ns vr-test.prod
  (:require [vr-test.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
