(ns homeautomation.mqtt
  (:require [environ.core :refer [env]]
            [clojurewerkz.machine-head.client :as mh]
            [clojurewerkz.machine-head.durability :refer [new-memory-persister new-file-persister]]
            [clojure.data.json :as json]
            [taoensso.timbre :as timbre])
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

(defn to-map [s]
  (json/read-str s :key-fn keyword))

(defonce callbacks (atom {}))

(defn add-callback [t f]
  (swap! callbacks assoc t f))

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
        (timbre/info "RCV topic: " topic "message: " message)
        (-> message
            (to-map)
            ((get-callback topic))))
      (catch Throwable t (timbre/error t "while processing message" message "on topic" topic)))))

; this fn can be called from the MQTT library callback.
; We can't *send* a message while we're still
; receiving one, so use a future to send the message from a separate thread.
; This is MQTT, so we don't really care
; if the message is delivered.

(defn send-message [topic message]
  (future
    (let [payload (json/write-str message)]
      (timbre/debug "sending: topic" topic "message" payload)
      (mh/publish @conn topic payload 0)
      (timbre/debug "message sent!"))))

(defn start-subscribers
  []
  (do
    (connect)
    (doseq [entry @callbacks]
      (timbre/info "starting subscriber for topic" (key entry))
      (mh/subscribe @conn {(key entry) 1} handle-delivery {:on-connection-lost connection-lost}))
    ))

(defn stop-subscribers
  []
  (do
    (mh/disconnect-and-close @conn)
    (reset! conn nil)))

(defn connection-lost [reason]
  (timbre/info reason "connection lost - reconnecting...")
  (start-subscribers))
