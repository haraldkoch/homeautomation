(ns homeautomation.db.core
  (:require
    [clj-time.jdbc]
    [conman.core :as conman]
    [homeautomation.config :refer [env]]
    [mount.core :refer [defstate]]))

(defstate ^:dynamic *db*
           :start (conman/connect! {:jdbc-url (env :database-url)})
           :stop (conman/disconnect! *db*))

(conman/bind-connection *db* "sql/queries.sql")


(declare create-user! update-user! get-user get-user-by-name delete-user! get-users set-user-presence!
         get-devices get-device find-user-for-device find-device get-devices-for-user
         create-device! delete-device! update-device-name! update-device-seen! update-device-status! set-device-owner! set-device-name! set-device-ignore!)