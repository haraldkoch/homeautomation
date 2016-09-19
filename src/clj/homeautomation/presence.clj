(ns homeautomation.presence
  (:require [homeautomation.config :refer [env]]
            [homeautomation.db.core :as db]
            [homeautomation.routes.ws :as ws]
            [clojure.tools.logging :as log]
            [clojure.string :refer [blank?]]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [clj-time.format :as f]
            [homeautomation.mqtt :as mqtt]
            [homeautomation.telegram :as telegram]
            [mount.core :refer [defstate]]))

(defn convert-timestamp [s] (->> s (f/parse (:date-time f/formatters)) (c/to-date)))

; need a fn because ->> puts the date at the end and we need it in the middle
(defn to-default-timezone [d] (t/to-time-zone d (t/default-time-zone)))
(defn hour-minute [s] (->> s (c/from-date) (to-default-timezone) (f/unparse (f/formatter-local "HH:mm"))))

(defn notify-event [event]
  (mqtt/send-message "presence/event" event))

(defn add-device
  [{:keys [:hostapd_mac :hostapd_clientname :status :read_time]}]
  (let [logmessage (str "new device " hostapd_clientname " at " (hour-minute read_time))]
    (log/info logmessage)
    (db/create-device! {:macaddr            hostapd_mac
                        :name               hostapd_clientname
                        :status             (or status "present")
                        :last_status_change read_time
                        :last_seen          read_time})
    (notify-event {:event   "NEW"
                   :device  (db/find-device {:macaddr hostapd_mac})
                   :message logmessage})))

(defn update-user-presence [username]
  (log/debug "update-user-presence" username "called")
  (when-not (blank? username)                               ;; blank usernames are 'true'
    (let [user (db/get-user-by-name {:username username})
          presence (:presence user)
          devices (db/get-devices-for-user {:owner username})
          present (for [device devices :when (and (not (:ignore device)) (= (:status device) "present"))] true)
          new-presence (cond
                         (zero? (count devices)) "UNKNOWN"
                         (pos? (count present)) "HOME"
                         :else "AWAY")]
      (when (not= presence new-presence)
        (log/info "updating presence for" username "from" presence "to" new-presence)
        (db/set-user-presence! {:id (:id user) :presence new-presence})
        (notify-event {:event   "PRESENCE" :user user :presence new-presence
                       :message (str (:first_name user) " is now " new-presence)})
        (ws/update-user (db/get-user-by-name {:username username}))))))

(defn update-device-status
  [{:keys [:hostapd_mac :hostapd_clientname :status :read_time] :as message}]
  (log/debug "update-device-status mac:" hostapd_mac "client" hostapd_clientname "status" status "read_time" read_time)
  (let [device (db/find-device {:macaddr hostapd_mac})]

    (if-not device
      (do
        (add-device message)
        (ws/add-device (db/find-device {:macaddr hostapd_mac})))
      (do
        (when (and (not (blank? hostapd_clientname)) (blank? (:name device)))
          (log/info "update name for mac" hostapd_mac "from" (:name device) "to" hostapd_clientname)
          (db/update-device-name! {:macaddr hostapd_mac
                                   :name    hostapd_clientname}))

        (when (and status (not= status (:status device)))
          (log/info "update status for mac" hostapd_mac "from" (:status device) "to" status)
          (db/update-device-status! {:macaddr            hostapd_mac
                                     :status             status
                                     :last_status_change read_time})

          (when-not (:ignore device)
            (update-user-presence (:owner device))
            (notify-event {:event   "STATUS" :device device :status status
                           :message (str hostapd_clientname " is now " status " at " (hour-minute read_time))})))

        (log/info "update seen for mac" hostapd_mac)
        (db/update-device-seen! {:macaddr   hostapd_mac
                                 :last_seen read_time})
        (ws/update-device (db/find-device {:macaddr hostapd_mac}))))))

(defn set-status [m]
  (let [action (:hostapd_action m)
        status (case action
                 ("authenticated" "associated") "present"
                 ("deauthenticated" "disassociated") "absent"
                 "present")]                                ; we may want to change this to 'unknown' later
    (assoc m :status status)))

(defn set-read-time [m]
  (update m :read_time convert-timestamp))

(defn do-message [m]
  (log/debug "do-message called for" m)
  (-> m
      (set-read-time)
      (set-status)
      (update-device-status)))

(defn init []
  (mqtt/add-callback "hostapd" do-message))

(defstate presence
          :start (mqtt/add-callback "hostapd" do-message)
          :stop (mqtt/del-callback "hostapd"))
