(ns homeautomation.test.db.core
  (:require [homeautomation.db.core :as db]
            [homeautomation.db.migrations :as migrations]
            [clojure.test :refer :all]
            [clojure.java.jdbc :as jdbc]
            [conman.core :refer [with-transaction]]
            [environ.core :refer [env]])
  (:import (java.util Date Calendar)))

(use-fixtures
  :once
  (fn [f]
    (db/connect!)
    (migrations/migrate ["migrate"])
    (f)))

; find-device when device doesn't exist
; find device when device does exist
; find-device-for-user when user doesn't exist
(deftest new-device
  (with-transaction [t-conn db/conn]
                    (jdbc/db-set-rollback-only! t-conn)
                    (is (= 0 (count (db/find-device {:macaddr "00:00:00:00:00:00"}))))))

(deftest existing-device
  (let [now (Calendar/getInstance)
        _ (.set now Calendar/MILLISECOND 0)
        date (Date. (.getTimeInMillis now))]
    (with-transaction [t-conn db/conn]
                      (jdbc/db-set-rollback-only! t-conn)
                      (db/create-device! {:macaddr            "00:00:00:00:00:00"
                                          :name               "FNORD"
                                          :status             "present"
                                          :last_status_change date
                                          :last_seen          date})
                      (is (= [{:macaddr            "00:00:00:00:00:00"
                               :name               "FNORD"
                               :status             "present"
                               :last_status_change date
                               :last_seen          date
                               :owner              nil}]
                             (map #(dissoc % :id)
                                  (db/find-device {:macaddr "00:00:00:00:00:00"}))))
                      ; update status
                      (let [newdate (Date. (+ 86400000 (.getTimeInMillis now)))]
                        (db/update-device-status! {:macaddr            "00:00:00:00:00:00"
                                                   :status             "absent"
                                                   :last_status_change newdate})
                        (is (= [{:macaddr            "00:00:00:00:00:00"
                                 :name               "FNORD"
                                 :status             "absent"
                                 :last_status_change newdate
                                 :last_seen          date
                                 :owner              nil}]
                               (map #(dissoc % :id)
                                    (db/find-device {:macaddr "00:00:00:00:00:00"}))))
                        (db/update-device-seen! {:macaddr   "00:00:00:00:00:00"
                                                 :last_seen newdate})
                        (is (= [{:macaddr            "00:00:00:00:00:00"
                                 :name               "FNORD"
                                 :status             "present"
                                 :last_status_change newdate
                                 :last_seen          newdate
                                 :owner              nil}]
                               (map #(dissoc % :id)
                                    (db/find-device {:macaddr "00:00:00:00:00:00"}))))))))

#_(deftest test-users
    (with-transaction [t-conn db/conn]
                      (jdbc/db-set-rollback-only! t-conn)
                      (is (= 1 (db/create-user!
                                 {:id         "1"
                                  :first_name "Sam"
                                  :last_name  "Smith"
                                  :email      "sam.smith@example.com"
                                  :pass       "pass"})))
                      (is (= [{:id         "1"
                               :first_name "Sam"
                               :last_name  "Smith"
                               :email      "sam.smith@example.com"
                               :pass       "pass"
                               :admin      nil
                               :last_login nil
                               :is_active  nil}]
                             (db/get-user {:id "1"})))))
