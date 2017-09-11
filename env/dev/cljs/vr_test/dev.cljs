(ns ^:figwheel-no-load vr-test.dev
  (:require
    [vr-test.core :as core]
    [devtools.core :as devtools]))

(devtools/install!)

(enable-console-print!)

(core/init!)
