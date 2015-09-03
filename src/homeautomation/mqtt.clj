(ns homeautomation.mqtt
  (:require [environ.core :refer [env]]
            [clojurewerkz.machine-head.client :as mh]
            [homeautomation.db.core :as db]
            [clojure.data.json :as json]
            [taoensso.timbre :as timbre]))

(defonce conn (atom nil))

(defn connect
  []
  (let [id (mh/generate-id)
        c (mh/connect (env :mqtt-url) id
                      {:username (env :mqtt-user)
                       :password (env :mqtt-pass)})]
    (reset! conn c)
    (timbre/info "MQTT connected to " (env :mqtt-url))))

(defn to-map [s] (json/read-str s :key-fn keyword))

(defn add-device
  [{:keys [:hostapd_mac :hostapd_clientname :hostapd_action :syslog_timestamp]}]
  (timbre/info "add new device mac:" hostapd_mac "name:" hostapd_clientname)
  (db/create-device! {:macaddr            hostapd_mac
                      :name               hostapd_clientname
                      :status             hostapd_action
                      :last_status_change syslog_timestamp
                      :last_seen          syslog_timestamp}))

(defn update-device-status
  [{:keys [:hostapd_mac :hostapd_clientname :hostapd_action :syslog_timestamp] :as message}]
  (let [device (db/find-device {:macaddr hostapd_mac})]

    (if (= 0 (count device))
      (add-device message)
      (do
        (if (not= hostapd_clientname (:name device))
          (do
            (timbre/info "update name for mac" hostapd_mac "from" (:name device) "to" hostapd_clientname)
            (db/update-device-name! {:macaddr hostapd_mac
                                   :name    hostapd_clientname})))

        (if (and hostapd_action (not= hostapd_action (:status device)))
          (do
            (timbre/info "update status for mac" hostapd_mac "from" (:status device) "to" hostapd_action)
            (db/update-device-status! {:macaddr            hostapd_mac
                                     :status             hostapd_action
                                     :last_status_change syslog_timestamp})))

        (timbre/info "update seen for mac" hostapd_mac)
        (db/update-device-seen! {:macaddr   hostapd_mac
                                 :last_seen syslog_timestamp})))))

(defn handle-delivery
  [^String topic _ ^bytes payload]

  (let
    [message (String. payload "UTF-8")]
    (try
      (do
        (timbre/info "RCV topic: " topic "message: " message)
        (update-device-status (to-map message)))
      (catch Throwable t (timbre/error "received message" message "on topic" topic "with error" t)))))

(defn start-subscriber
  []
  (do
    (connect)
    (mh/subscribe @conn {"hostapd" 1} handle-delivery {:on-connection-lost connect})))

(defn stop-subscriber
  []
  (do
    (mh/disconnect-and-close @conn)
    (reset! conn nil)))
