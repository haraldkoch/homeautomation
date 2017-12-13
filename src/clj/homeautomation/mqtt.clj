(ns homeautomation.mqtt
  (:require [homeautomation.config :refer [env]]
            [clojurewerkz.machine-head.client :as mh]
            [clojurewerkz.machine-head.durability :refer [new-memory-persister new-file-persister]]
            [cheshire.core :refer :all]
            [clojure.tools.logging :as log]
            [mount.core :refer [defstate]])
  (:import [org.eclipse.paho.client.mqttv3 MqttException]))

(defonce conn (atom nil))

(defn get-client-id []
  (if (env :dev) (mh/generate-id) (env :mqtt-clientid)))

(defn get-persister []
  (if (env :dev) (new-memory-persister) (new-file-persister)))

(declare connection-lost)

(defn do-connect []
  (mh/connect (env :mqtt-url)
              {:client-id          (get-client-id)
               :persister          (get-persister)
               :on-connection-lost connection-lost
               :opts               {:username            (env :mqtt-user)
                                    :password            (env :mqtt-pass)
                                    :keep-alive-interval 60
                                    ; the machine-head client checks this field for nil. NIL IS FALSE, SILLY PEOPLE!
                                    :clean-session      (boolean (env :dev))}}))

; FIXME: might be better to re-use existing conn instead of creating a new on on connection failure?
(defn connect
  []
  (while (or (nil? @conn) (not (mh/connected? @conn)))
    (try
      (do
        (reset! conn (do-connect))
        (log/info "MQTT connected to " (env :mqtt-url)))
      (catch MqttException e
        (do
          (log/error e "cannot connect to" (env :mqtt-url))
          (Thread/sleep 15000))))))

(defn to-map [s]
  (parse-string s true))

(defonce callbacks (atom {}))

(declare handle-delivery)

(defn add-callback [topic f]
  (do
    (log/info "setting subcriber for topic " topic)
    (swap! callbacks assoc topic f)
    (when @conn
      (mh/subscribe @conn {topic 1} handle-delivery))))

(defn del-callback [t]
  (do
    (swap! callbacks dissoc t)
    (when @conn
      (mh/unsubscribe @conn t))))

(defn get-callback
  [t]
  ; fixme: need to handle wildcards
  (get @callbacks t))

(defn handle-delivery
  [^String topic _ ^bytes payload]

  (let
    [message (String. payload "UTF-8")]
    (try
      (do
        (log/info "RCV topic: " topic "message: " message)
        (-> message
            (to-map)
            ((get-callback topic))))
      (catch Throwable t (log/error t "while processing message" message "on topic" topic)))))

; this fn can be called from the MQTT library callback.
; We can't *send* a message while we're still
; receiving one, so use a future to send the message from a separate thread.
; This is MQTT, so we don't really care
; if the message is delivered.

(defn send-message [topic message]
  (future
    (let [payload (generate-string message)]
      (log/debug "sending: topic" topic "message" payload)
      (mh/publish @conn topic payload 0)
      (log/debug "message sent!"))))

; FIXME - this should happen in a thread so that it doesn't block the caller when the MQTT server is down
(defn start-subscribers
  []
  (do
    (connect)
    (doseq [entry @callbacks]
      (log/info "starting subscriber for topic" (key entry))
      (mh/subscribe @conn {(key entry) 1} handle-delivery))
    conn))

(defn stop-subscribers
  []
  (do
    (mh/disconnect-and-close @conn)
    (reset! conn nil)))

(defn connection-lost [reason]
  (log/info reason "connection lost - reconnecting...")
  (start-subscribers))

(defstate mqtt
  :start (start-subscribers)
  :stop (stop-subscribers))