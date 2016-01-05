(ns homeautomation.config
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[homeautomation started successfully]=-"))
   :middleware identity})
