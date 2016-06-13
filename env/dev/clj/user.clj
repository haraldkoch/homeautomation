(ns user
  (:require [mount.core :as mount]
            [homeautomation.figwheel :refer [start-fw stop-fw cljs]]
            homeautomation.core))

(defn start []
  (mount/start-without #'homeautomation.core/repl-server))

(defn stop []
  (mount/stop-except #'homeautomation.core/repl-server))

(defn restart []
  (stop)
  (start))


