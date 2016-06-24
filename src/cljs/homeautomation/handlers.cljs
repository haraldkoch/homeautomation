(ns homeautomation.handlers
  (:require
    #_[homeautomation.db :refer [default-value schema]]
    [homeautomation.ajax :refer [send]]
    [re-frame.core :refer [register-handler dispatch path trim-v after debug]]
    #_[schema.core :as s]))

(defn reload-users []
  (ajax.core/GET
    "/users"
    {:handler       #(dispatch [:process-users-response %1])
     :error-handler #(dispatch [:bad-response %1])}))

(defn reload-devices []
  (ajax.core/GET
    "/devices"
    {:handler       #(dispatch [:process-devices-response %1])
     :error-handler #(dispatch [:bad-response %1])}))

(defn clear-indicators [] (dispatch [:set-error nil]) (dispatch [:set-status nil]))

(register-handler
  :initialize
  (fn
    [db _]
    (-> db
        (assoc :users-loaded? false)
        (assoc :devices-loaded false))
    (reload-users)
    (reload-devices)
    db))                                                    ;; pure handlers must return a db

(register-handler
  :fetch-users
  (fn
    [db _]
    (reload-users)
    db))

(register-handler
  :fetch-devices
  (fn
    [db _]
    (reload-devices)
    db))

(register-handler                                           ;; when the GET succeeds
  :process-users-response
  (fn
    [db [_ response]]
    (clear-indicators)
    (-> db
        (assoc :users-loaded? true)                         ;; set flag saying we got it
        (assoc :users response))))

(register-handler                                           ;; when the GET succeeds
  :process-devices-response
  (fn
    [db [_ response]]
    (clear-indicators)
    (-> db
        (assoc :devices-loaded? true)                       ;; set flag saying we got it
        (assoc :devices response))))

(register-handler
  :set-status
  (fn [db [_ status]]
    (-> db (assoc :status status))))

(register-handler
  :set-error
  (fn [db [_ error]]
    (-> db (assoc :error error))))

