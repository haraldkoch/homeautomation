(ns user
  (:require [luminus-migrations.core :as migrations]
            [homeautomation.config :refer [env]]
            [mount.core :as mount]
            [homeautomation.figwheel :refer [start-fw stop-fw cljs]]
            homeautomation.core))

(defn start []
  (mount/start-without #'homeautomation.core/repl-server))

(defn stop []
  (mount/stop-except #'homeautomation.core/repl-server))

(defn restart []
  (stop)
  (start))

(defn migrate []
  (migrations/migrate ["migrate"] (select-keys env [:database-url])))

(defn rollback []
  (migrations/migrate ["rollback"] (select-keys env [:database-url])))

(defn create-migration [name]
  (migrations/create name (select-keys env [:database-url])))


