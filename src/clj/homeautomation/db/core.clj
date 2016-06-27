(ns homeautomation.db.core
  (:require
    [clojure.java.jdbc :as jdbc]
    [conman.core :as conman]
    [homeautomation.config :refer [env]]
    [mount.core :refer [defstate]])
  (:import [java.sql
            BatchUpdateException
            PreparedStatement]))

(defstate ^:dynamic *db*
          :start (conman/connect!
                   {:init-size  1
                    :min-idle   1
                    :max-idle   4
                    :max-active 32
                    :jdbc-url   (env :database-url)})
          :stop (conman/disconnect! *db*))

(conman/bind-connection *db* "sql/queries.sql")

(defn to-date [sql-date]
  (-> sql-date (.getTime) (java.util.Date.)))

(extend-protocol jdbc/IResultSetReadColumn
  java.sql.Date
  (result-set-read-column [v _ _] (to-date v))

  java.sql.Timestamp
  (result-set-read-column [v _ _] (to-date v)))

(extend-type java.util.Date
  jdbc/ISQLParameter
  (set-parameter [v ^PreparedStatement stmt idx]
    (.setTimestamp stmt idx (java.sql.Timestamp. (.getTime v)))))

(declare create-user! update-user! get-user get-user-by-name delete-user! get-users set-user-presence!
         get-devices get-device find-user-for-device find-device get-devices-for-user
         create-device! delete-device! update-device-name! update-device-seen! update-device-status! set-device-owner! set-device-name! set-device-ignore!)