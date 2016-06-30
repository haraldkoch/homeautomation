;START:ns
(ns homeautomation.routes.ws
  (:require [compojure.core :refer [GET POST defroutes]]
            [homeautomation.db.core :as db]
            [mount.core :refer [defstate]]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.immutant
             :refer [sente-web-server-adapter]]))
;END:ns

;START:socket
(let [connection (sente/make-channel-socket!
                   sente-web-server-adapter
                   {:user-id-fn
                    (fn [ring-req] (get-in ring-req [:params :client-id]))
                    })
      {:keys [ch-recv send-fn connected-uids ajax-post-fn ajax-get-or-ws-handshake-fn]} connection]
  (def ring-ajax-post ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk ch-recv)
  (def chsk-send! send-fn)
  (def connected-uids connected-uids))
;END:socket

;START:save-message
#_(defn validate-message [params]
    (first
      (b/validate
        params
        :name v/required
        :message [v/required [v/min-count 10]])))

#_(defn save-message! [message]
    (if-let [errors (validate-message message)]
      {:errors errors}
      (do
        (db/save-message! message)
        message)))

#_(defn save-message! [message]
  (do
    (db/save-message! message)
    message))

(defn handle-message! [{:keys [id client-id ?data]}]
  (println "\n\n+++++++ GOT MESSAGE:" id (keys ?data))
  #_(when (= id :guestbook/add-message)
    (let [response (-> ?data
                       (assoc :timestamp (java.util.Date.))
                       save-message!)]
      (if (:errors response)
        (chsk-send! client-id [:guestbook/error response])
        (doseq [uid (:any @connected-uids)]
          (chsk-send! uid [:guestbook/add-message response]))))))
;END:save-message

(defn update-device
  "push an updated device to all clients"
  [device]
  (doseq [uid (:any @connected-uids)]
    (chsk-send! uid [:presence/device-updated device])))

(defn add-device
  "push an updated device to all clients"
  [device]
  (doseq [uid (:any @connected-uids)]
    (chsk-send! uid [:presence/device-added device])))

(defn update-user
  "push an updated user to all clients"
  [user]
  (doseq [uid (:any @connected-uids)]
    (chsk-send! uid [:presence/user-updated user])))

;START:router
(defn stop-router! [stop-fn]
  (when stop-fn (stop-fn)))

(defn start-router! []
  (sente/start-chsk-router! ch-chsk handle-message!))

(defstate router
          :start (start-router!)
          :stop (stop-router! router))
;END:router

;START:defroutes
(defroutes websocket-routes
           (GET "/ws" req (ring-ajax-get-or-ws-handshake req))
           (POST "/ws" req (ring-ajax-post req)))
;END:defroutes
