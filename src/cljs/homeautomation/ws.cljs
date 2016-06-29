;START:ns
(ns homeautomation.ws
  (:require [taoensso.sente :as sente]
            [re-frame.core :refer [dispatch]]))
;END:ns

;START:connection
(let [connection (sente/make-channel-socket! "/ws" {:type :auto})
      {:keys [chsk ch-recv send-fn state]} connection]
  (def chsk chsk)
  (def ch-chsk ch-recv)    ; ChannelSocket's receive channel
  (def chsk-send! send-fn)                                  ; ChannelSocket's send API fn
  (def chsk-state state))                                   ; Watchable, read only atom
;END:connection

;START:event-handlers
(defn state-handler [{:keys [?data]}]
  (.log js/console (str "state changed: " ?data)))

(defn handshake-handler [{:keys [?data]}]
  (.log js/console (str "connection established: " ?data)))

(defn default-event-handler [ev-msg]
  (.log js/console (str "Unhandled event: " (:event ev-msg))))

;; this is the magic. dispatch incoming events from the server to re-frame.
(defn message-handler [{[event data] :?data}]
   (.log js/console (str "push event type:" event " device: " data))
  (dispatch [event data]))

(defn event-msg-handler [& [{:keys [message state handshake]
                             :or {state state-handler
                                  handshake handshake-handler}}]]
  (fn [ev-msg]
    (case (:id ev-msg)
      :chsk/handshake (handshake ev-msg)
      :chsk/state (state ev-msg)
      :chsk/recv (message ev-msg)
      (default-event-handler ev-msg))))
;END:event-handlers

;START:router
(def router (atom nil))

(defn stop-router! []
  (when-let [stop-f @router] (stop-f)))

(defn start-router! []
  (stop-router!)
  (println "starting the router...  ")
  (reset! router (sente/start-client-chsk-router!
                   ch-chsk
                   (event-msg-handler
                     {:message   message-handler
                      :state     state-handler
                      :handshake handshake-handler}))))
;END:router
