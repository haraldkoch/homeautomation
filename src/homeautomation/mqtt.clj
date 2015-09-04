(ns homeautomation.mqtt
  (:require [environ.core :refer [env]]
            [clojurewerkz.machine-head.client :as mh]
            [homeautomation.db.core :as db]
            [clojure.data.json :as json]
            [taoensso.timbre :as timbre]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [clj-time.format :as f]))

(defonce conn (atom nil))

(def custom-formatter (:date-time f/formatters))
(defn convert-timestamp [s] (->> s (f/parse custom-formatter) (c/to-date)))

(defn connect
  []
  (let [id (mh/generate-id)
        c (mh/connect (env :mqtt-url) id
                      {:username (env :mqtt-user)
                       :password (env :mqtt-pass)})]
    (reset! conn c)
    (timbre/info "MQTT connected to " (env :mqtt-url))))

(defn notify-event [event]
  (mh/publish @conn "presence/event" (json/write-str event)))

(defn add-device
  [{:keys [:hostapd_mac :hostapd_clientname :status :read_time]}]
  (let [logmessage (str "new device " hostapd_mac "/" hostapd_clientname " at " read_time)]
    (timbre/info logmessage)
    (db/create-device! {:macaddr            hostapd_mac
                        :name               hostapd_clientname
                        :status             (if (nil? status) "present" status)
                        :last_status_change read_time
                        :last_seen          read_time})
    (notify-event {:event "NEW" :macaddr hostapd_mac :name hostapd_clientname :timestamp read_time :message logmessage})))

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
            (notify-event {:event "PRESENCE" :macaddr hostapd_mac :name hostapd_clientname :status status
                          :message (str hostapd_clientname " is now " status " at " read_time)})))

        (timbre/info "update seen for mac" hostapd_mac)
        (db/update-device-seen! {:macaddr   hostapd_mac
                                 :last_seen read_time})))))

(defn set-status [m]
  (let [action (:hostapd_action m)]
    (if action
      (merge m {:status (cond (= action "authenticated") "present"
                              (= action "assosciated") "present"
                              (= action "deauthenticated") "absent"
                              (= action "disassosciated") "absent"
                              :else "unknown")})
      m)))

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
    (mh/subscribe @conn {"hostapd" 1} handle-delivery {:on-connection-lost connect})))

(defn stop-subscriber
  []
  (do
    (mh/disconnect-and-close @conn)
    (reset! conn nil)))
