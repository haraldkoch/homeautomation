(ns homeautomation.config
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [homeautomation.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[homeautomation started successfully using the development profile]=-"))
   :middleware wrap-dev})
