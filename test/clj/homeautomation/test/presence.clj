(ns homeautomation.test.presence
  (:require [homeautomation.db.core :refer [*db*] :as db]
            [luminus-migrations.core :as migrations]
            [clojure.test :refer :all]
            [clojure.java.jdbc :as jdbc]
            [homeautomation.config :refer [env]]
            [homeautomation.presence :refer [presence]]
            [mount.core :as mount]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clj-time.coerce :as c]
            [homeautomation.presence :as presence]
            [clojure.tools.logging :as log])
  (:import (java.util Date Calendar)))

(use-fixtures
  :once
  (fn [f]
    (mount/start
      #'homeautomation.config/env
      #'homeautomation.db.core/*db*
      #'homeautomation.presence/presence)
    (migrations/migrate ["migrate"] (select-keys env [:database-url]))
    (f)))

(defn to-default-timezone [d] (t/to-time-zone d (t/default-time-zone)))
(defn fmt-time [t]
  (->> t (c/from-date) (to-default-timezone) (f/unparse (f/formatters :date-time))))

(defn join-message [date]
  {:read_time          (fmt-time date)
   :hostapd_clientname "TEST-DEVICE"
   :hostapd_mac        "01:02:03:04:05:06"
   :hostapd_action     "authenticated"})

(defn leave-message [date]
  {:read_time          (fmt-time date)
   :hostapd_clientname "TEST-DEVICE"
   :hostapd_mac        "01:02:03:04:05:06"
   :hostapd_action     "deauthenticated"})

(defn add-second-device [date]
  (db/create-device! {:macaddr            "02:02:02:02:02:02"
                      :name               "ANOTHER-DEVICE"
                      :status             "absent"
                      :last_status_change date
                      :last_seen          date}))

(defn device-id [mac]
  (:id (db/find-device {:macaddr mac})))

(defn update-device-owner [owner mac]
  (do
    (db/set-device-owner! {:owner owner :device_id (device-id mac)})
    (is (= owner (:owner (db/find-device {:macaddr mac}))))
    (presence/update-user-presence "snake")))

(defn user-presence [user]
  (:presence (db/get-user-by-name {:username user})))

; find-device when device doesn't exist
; find device when device does exist
; find-device-for-user when user doesn't exist
(deftest new-device
  (let [now (Calendar/getInstance)
        _ (.set now Calendar/MILLISECOND 0)
        date (Date. (.getTimeInMillis now))]
    (jdbc/with-db-transaction [t-conn *db*]
                              (jdbc/db-set-rollback-only! t-conn)
                              (log/info "Autocommit is" (.getAutoCommit (jdbc/db-find-connection t-conn)))
                              (.setAutoCommit (jdbc/db-find-connection t-conn) false)
                              (presence/do-message (join-message date))
                              (is (= {:macaddr            "01:02:03:04:05:06"
                                      :name               "TEST-DEVICE"
                                      :status             "present"
                                      :ignore             false
                                      :last_status_change date
                                      :last_seen          date
                                      :owner              nil}
                                     (dissoc (db/find-device {:macaddr "01:02:03:04:05:06"}) :id)))


                              (db/create-user! {:username "snake" :first_name "Snake" :last_name "Plisskin"})
                              (log/info "user is" (db/get-user-by-name {:username "snake"}))
                              (update-device-owner "snake" "01:02:03:04:05:06")
                              
                              (is (= {:macaddr            "01:02:03:04:05:06"
                                      :name               "TEST-DEVICE"
                                      :status             "present"
                                      :ignore             false
                                      :last_status_change date
                                      :last_seen          date
                                      :owner              "snake"}
                                     (dissoc (db/find-device {:macaddr "01:02:03:04:05:06"}) :id)))
                              (is (= 1 (count (db/get-devices-for-user {:owner "snake"}))))

                              ;; user with one device that is present should be home
                              (is (= "HOME" (user-presence "snake")))

                              ;; user with zero devices should be unknown
                              (update-device-owner nil "01:02:03:04:05:06")
                              (is (= "UNKNOWN" (user-presence "snake")))

                              ;; user with many devices, one that is present, is home
                              (add-second-device date)
                              (update-device-owner "snake" "01:02:03:04:05:06")
                              (update-device-owner "snake" "02:02:02:02:02:02")
                              (is (= "HOME" (user-presence "snake")))
                              (is (= 2 (count (db/get-devices-for-user {:owner "snake"}))))

                              ;; removing present device should toggle to away
                              (update-device-owner nil "01:02:03:04:05:06")
                              (is (= "AWAY" (user-presence "snake")))

                              ;; removing second device should go back to unknown
                              (update-device-owner nil "02:02:02:02:02:02")
                              (is (= "UNKNOWN" (user-presence "snake")))
                              
                              ;; toggle device away should mark device away and user away
                              (update-device-owner "snake" "01:02:03:04:05:06")
                              (presence/do-message (leave-message date))
                              (is (= {:macaddr            "01:02:03:04:05:06"
                                      :name               "TEST-DEVICE"
                                      :status             "absent"
                                      :ignore             false
                                      :last_status_change date
                                      :last_seen          date
                                      :owner              "snake"}
                                     (dissoc (db/find-device {:macaddr "01:02:03:04:05:06"}) :id)))
                              (presence/update-user-presence "snake")
                              (is (= "AWAY" (user-presence "snake")))

                              ;; device name updates in message should not update database
                              (presence/do-message (assoc (leave-message date) :hostapd_clientname "A-NEW-NAME"))
                              (is (= {:macaddr            "01:02:03:04:05:06"
                                      :name               "TEST-DEVICE"
                                      :status             "absent"
                                      :ignore             false
                                      :last_status_change date
                                      :last_seen          date
                                      :owner              "snake"}
                                     (dissoc (db/find-device {:macaddr "01:02:03:04:05:06"}) :id))))))

(deftest clean-database
  (jdbc/with-db-transaction [t-conn *db*]
                            (db/delete-user! {:id (:id (db/get-user-by-name {:username "snake"}))})
                            (db/delete-device! {:id (device-id "01:02:03:04:05:06")})
                            (db/delete-device! {:id (device-id "02:02:02:02:02:02")})))