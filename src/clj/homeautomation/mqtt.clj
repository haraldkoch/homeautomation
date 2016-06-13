(ns homeautomation.mqtt
  (:require [homeautomation.config :refer [env]]
            [clojurewerkz.machine-head.client :as mh]
            [clojurewerkz.machine-head.durability :refer [new-memory-persister new-file-persister]]
            [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            [mount.core :refer [defstate]])
  (:import [java.util Date]
           [java.io PrintWriter]
           [org.eclipse.paho.client.mqttv3 MqttException]))

(defn- write-date [x ^PrintWriter out]
  (.print out (str x)))
(extend Date json/JSONWriter {:-write write-date})

(defonce conn (atom nil))

(defn get-client-id []
  (if (env :dev) (mh/generate-id) (env :mqtt-clientid)))

(defn get-persister []
  (if (env :dev) (new-memory-persister) (new-file-persister)))

(defn do-connect []
  (mh/connect (env :mqtt-url)
              (get-client-id)
              (get-persister)
              {:username            (env :mqtt-user)
               :password            (env :mqtt-pass)
               :keep-alive-interval 60
               ; the machine-head client checks this field for nil. NIL IS FALSE, SILLY PEOPLE!
               :clean-session       (boolean (env :dev))}))

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

(declare connection-lost)

(defn to-map [s]
  (json/read-str s :key-fn keyword))

(defonce callbacks (atom {}))

(declare handle-delivery)

(defn add-callback [topic f]
  (do
    (log/info "setting subcriber for topic " topic)
    (swap! callbacks assoc topic f)
    (mh/subscribe @conn {topic 1} handle-delivery {:on-connection-lost connection-lost})))

(defn del-callback [t]
  (do
    (swap! callbacks dissoc t)
    (mh/unsubscribe @conn t)))

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
    (let [payload (json/write-str message)]
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
      (mh/subscribe @conn {(key entry) 1} handle-delivery {:on-connection-lost connection-lost}))
    conn))

(defn stop-subscribers
  []
  (do
    (mh/disconnect-and-close @conn)
    (reset! conn nil)))

(defn connection-lost [reason]
  (log/info reason "connection lost - reconnecting...")
  (start-subscribers))

(defstate conn
          :start (start-subscribers)
          :stop (stop-subscribers))