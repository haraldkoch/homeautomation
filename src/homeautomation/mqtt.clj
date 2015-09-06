(ns homeautomation.mqtt
  (:require [environ.core :refer [env]]
            [clojurewerkz.machine-head.client :as mh]
            [homeautomation.db.core :as db]
            [clojure.data.json :as json]
            [taoensso.timbre :as timbre]
            [clj-time.coerce :as c]
            [clj-time.format :as f])
  (:import [java.util Date]
           [java.io PrintWriter]
           [org.eclipse.paho.client.mqttv3 MqttException]))

; TODO - separate the MQTT generic logic with the presence processing logic

(defn- write-date [x ^PrintWriter out]
  (.print out (str x)))

(extend Date json/JSONWriter {:-write write-date})

(def custom-formatter (:date-time f/formatters))
(defn convert-timestamp [s] (->> s (f/parse custom-formatter) (c/to-date)))
(defn hour-minute [s] (->> s (c/from-date) (f/unparse (:hour-minute f/formatters))))

(defonce conn (atom nil))

(defn do-connect []
  (mh/connect (env :mqtt-url)
              (env :mqtt-clientid)
              {:username            (env :mqtt-user)
               :password            (env :mqtt-pass)
               :keep-alive-interval 60
               :clean-session       (env :dev)}))

; FIXME: might be better to re-use existing conn instead of creating a new on on connection faiulre?
(defn connect
  []
  (while (or (nil? @conn) (not (mh/connected? @conn)))
    (try
      (do
        (reset! conn (do-connect))
        (timbre/info "MQTT connected to " (env :mqtt-url)))
      (catch MqttException e
        (do
          (timbre/error e "cannot connect to" (env :mqtt-url))
          (Thread/sleep 15000))))))

(declare connection-lost)

; all of this event processing code runs from the MQTT library callback. We can't *send* a message while we're still
; receiving one, so use a future to send the message from a separate thread. This is MQTT, so we don't really care
; if the message gets sent or not ;)
(defn notify-event [event]
  (future
    (let [payload (json/write-str event)]
      (timbre/debug "sending presence event" payload)
      (mh/publish @conn "presence/event" payload 0)
      (timbre/debug "presence event sent!"))))

(defn add-device
  [{:keys [:hostapd_mac :hostapd_clientname :status :read_time]}]
  (let [logmessage (str "new device " hostapd_clientname " at " (hour-minute read_time))]
    (timbre/info logmessage)
    (db/create-device! {:macaddr            hostapd_mac
                        :name               hostapd_clientname
                        :status             (if (nil? status) "present" status)
                        :last_status_change read_time
                        :last_seen          read_time})
    (notify-event {:event "NEW" :macaddr hostapd_mac :name hostapd_clientname :message logmessage})))

(defn update-device-status
  [{:keys [:hostapd_mac :hostapd_clientname :status :read_time] :as message}]
  (timbre/debug "update-device-status mac:" hostapd_mac "client" hostapd_clientname "status" status "read_time" read_time)
  (let [devices (db/find-device {:macaddr hostapd_mac})
        device (first devices)]

    (if (= 0 (count device))
      (add-device message)
      (do
        (if (not= hostapd_clientname (:name device))
          (do
            (timbre/info "update name for mac" hostapd_mac "from" (:name device) "to" hostapd_clientname)
            (db/update-device-name! {:macaddr hostapd_mac
                                     :name    hostapd_clientname})))

        (if (and status (not= status (:status device)))
          (do
            (timbre/info "update status for mac" hostapd_mac "from" (:status device) "to" status)
            (db/update-device-status! {:macaddr            hostapd_mac
                                       :status             status
                                       :last_status_change read_time})
            (notify-event {:event   "PRESENCE" :macaddr hostapd_mac :name hostapd_clientname :status status
                           :message (str hostapd_clientname " is now " status " at " (hour-minute read_time))})))

        (timbre/info "update seen for mac" hostapd_mac)
        (db/update-device-seen! {:macaddr   hostapd_mac
                                 :last_seen read_time})))))

(defn set-status [m]
  (let [action (:hostapd_action m)
        status (case action
                 ("authenticated" "associated") "present"
                 ("deauthenticated" "disassociated") "absent"
                 "present")]                                ; we may want to change this to 'unknown' later
    (merge m {:status status})))

(defn set-read-time [m]
  (merge m {:read_time (convert-timestamp (:read_time m))}))

(defn to-map [s]
  (json/read-str s :key-fn keyword))

(defn handle-delivery
  [^String topic _ ^bytes payload]

  (let
    [message (String. payload "UTF-8")]
    (try
      (do
        (timbre/info "RCV topic: " topic "message: " message)
        (-> message
            (to-map)
            (set-read-time)
            (set-status)
            (update-device-status)))
      (catch Throwable t (timbre/error "received message" message "on topic" topic "with error" t)))))

(defn start-subscriber
  []
  (do
    (connect)
    (mh/subscribe @conn {"hostapd" 1} handle-delivery {:on-connection-lost connection-lost})))

(defn stop-subscriber
  []
  (do
    (mh/disconnect-and-close @conn)
    (reset! conn nil)))

(defn connection-lost [reason]
  (timbre/info reason "connection lost - reconnecting...")
  (start-subscriber))
