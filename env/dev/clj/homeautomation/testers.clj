(ns homeautomation.testers
  (:require [homeautomation.presence :refer [do-message]]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clj-time.coerce :as c])
  (:import (java.util Calendar Date)))

(defn to-default-timezone [d] (t/to-time-zone d (t/default-time-zone)))
(defn fmt-time [t]
  (->> t (c/from-date) (to-default-timezone) (f/unparse (f/formatters :date-time))))

(defn join-message [date]
  {:read_time          (fmt-time date)
   :hostapd_clientname "IPHONE-CHK"
   :hostapd_mac        "40:b3:95:71:eb:83"
   :hostapd_action     "authenticated"})

(defn leave-message [date]
  {:read_time          (fmt-time date)
   :hostapd_clientname "IPHONE-CHK"
   :hostapd_mac        "40:b3:95:71:eb:83"
   :hostapd_action     "deauthenticated"})

(defn present []
  (let [now (Calendar/getInstance)
        _ (.set now Calendar/MILLISECOND 0)
        date (Date. (.getTimeInMillis now))]
    (do-message (join-message date))))

(defn absent []
  (let [now (Calendar/getInstance)
        _ (.set now Calendar/MILLISECOND 0)
        date (Date. (.getTimeInMillis now))]
    (do-message (leave-message date))))

(defn new-device []
  (let [now (Calendar/getInstance)
        _ (.set now Calendar/MILLISECOND 0)
        date (Date. (.getTimeInMillis now))]
    (do-message {:read_time          (fmt-time date)
                 :hostapd_clientname nil
                 :hostapd_mac        "02:04:06:08:0A:0C"
                 :hostapd_action     "authenticated"})))