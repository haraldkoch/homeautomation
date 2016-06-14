(ns homeautomation.test.db.core
  (:require [homeautomation.db.core :refer [*db*] :as db]
            [luminus-migrations.core :as migrations]
            [clojure.test :refer :all]
            [clojure.java.jdbc :as jdbc]
            [homeautomation.config :refer [env]]
            [mount.core :as mount])
  (:import (java.util Date Calendar)))

(use-fixtures
  :once
  (fn [f]
    (mount/start
      #'homeautomation.config/env
      #'homeautomation.db.core/*db*)
    (migrations/migrate ["migrate"] (select-keys env [:database-url]))
    (f)))

; find-device when device doesn't exist
; find device when device does exist
; find-device-for-user when user doesn't exist
(deftest new-device
  (jdbc/with-db-transaction [t-conn *db*]
                            (jdbc/db-set-rollback-only! t-conn)
                            (is (= 0 (count (db/find-device {:macaddr "00:00:00:00:00:00"}))))))

(deftest existing-device
  (let [now (Calendar/getInstance)
        _ (.set now Calendar/MILLISECOND 0)
        date (Date. (.getTimeInMillis now))]
    (jdbc/with-db-transaction [t-conn db/*db*]
                              (jdbc/db-set-rollback-only! t-conn)
                              (is (= 1 (db/create-device! {:macaddr            "00:00:00:00:00:00"
                                                           :name               "FNORD"
                                                           :status             "present"
                                                           :last_status_change date
                                                           :last_seen          date})))
                              (is (= {:macaddr            "00:00:00:00:00:00"
                                      :name               "FNORD"
                                      :status             "present"
                                      :ignore             false
                                      :last_status_change date
                                      :last_seen          date
                                      :owner              nil}
                                     (dissoc (db/find-device {:macaddr "00:00:00:00:00:00"}) :id)))
                              ; update status
                              (let [newdate (Date. (+ 86400000 (.getTimeInMillis now)))]
                                (db/update-device-status! {:macaddr            "00:00:00:00:00:00"
                                                           :status             "absent"
                                                           :last_status_change newdate})
                                (is (= {:macaddr            "00:00:00:00:00:00"
                                        :name               "FNORD"
                                        :status             "absent"
                                        :ignore             false
                                        :last_status_change newdate
                                        :last_seen          date
                                        :owner              nil}
                                       (dissoc (db/find-device {:macaddr "00:00:00:00:00:00"}) :id)))
                                (db/update-device-seen! {:macaddr   "00:00:00:00:00:00"
                                                         :last_seen newdate})
                                (is (= {:macaddr            "00:00:00:00:00:00"
                                        :name               "FNORD"
                                        :status             "absent"
                                        :ignore             false
                                        :last_status_change newdate
                                        :last_seen          newdate
                                        :owner              nil}
                                       (dissoc (db/find-device {:macaddr "00:00:00:00:00:00"}) :id)))))))

#_(deftest test-users
    (jdbc/with-db-transaction [t-conn *db*]
                              (jdbc/db-set-rollback-only! t-conn)
                              (is (= 1 (db/create-user!
                                         t-conn
                                         {:id         "1"
                                          :first_name "Sam"
                                          :last_name  "Smith"
                                          :email      "sam.smith@example.com"
                                          :pass       "pass"})))
                              (is (= {:id         "1"
                                      :first_name "Sam"
                                      :last_name  "Smith"
                                      :email      "sam.smith@example.com"
                                      :pass       "pass"
                                      :admin      nil
                                      :last_login nil
                                      :is_active  nil}
                                     (db/get-user t-conn {:id "1"})))))
