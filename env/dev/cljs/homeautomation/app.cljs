(ns ^:figwheel-no-load homeautomation.app
  (:require [homeautomation.core :as core]
            [devtools.core :as devtools]))

(enable-console-print!)

(devtools/install!)

(core/init!)
