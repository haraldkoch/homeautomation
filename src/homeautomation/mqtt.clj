(ns homeautomation.mqtt
  (:require [environ.core :refer [env]]
            [clojurewerkz.machine-head.client :as mh]
            [owntracks-receiver.db.core :as db]
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

(defn get-user-from-topic [t] (get (clojure.string/split t #"/") 1))

(defn find-or-add-device [device]
  (let [entry (db/find-device {:device device})])
  (if (count entry)
    (-> entry (first) (:id))
    (db/create-device<! {:device device})))

(defn handle-delivery
  [^String topic _ ^bytes payload]

  (let
    [message (String. payload "UTF-8")]
    (try
      (do
        (timbre/info "RCV topic: " topic "message: " message)
        (let [content (to-map message)]
          ; find / add device
          ; find user for device
          ; if not found, add to unknown devices list
          ; otherwise update user status
          )
        )
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
