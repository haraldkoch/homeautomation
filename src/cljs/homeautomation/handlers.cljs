(ns homeautomation.handlers
  (:require
    #_[homeautomation.db :refer [default-value schema]]
    [homeautomation.ajax :refer [send]]
    [re-frame.core :as rf]
    #_[schema.core :as s]
    [ajax.core :as ajax]))

(rf/reg-event-db
  :set-status
  (fn [db [_ status]]
    (assoc db :status status)))

(rf/reg-event-db
  :clear-status
  (fn [db _]
    (dissoc db :status)))

(rf/reg-event-db
  :set-error
  (fn [db [_ error]]
    (assoc db :error error)))

(rf/reg-event-db
  :clear-error
  (fn [db _]
    (dissoc db :error)))

(rf/reg-event-fx
  :fetch-users
  (fn [{:keys [db]} _]
    {:http-xhrio {:method          :get
                  :uri             "/users"
                  :format          (ajax/transit-request-format)
                  :response-format (ajax/transit-response-format)
                  :on-success      [:process-users-response]
                  :on-failure      [:bad-response]}}))

(rf/reg-event-fx
  :fetch-devices
  (fn [{:keys [db]} _]
    {:http-xhrio {:method          :get
                  :uri             "/devices"
                  :format          (ajax/transit-request-format)
                  :response-format (ajax/transit-response-format)
                  :on-success      [:process-devices-response]
                  :on-failure      [:bad-response]}}))

(rf/reg-event-fx
  :initialize
  (fn
    [{:keys [db]} _]
    {:db         (-> db
                     (assoc :users-loaded? false)
                     (assoc :devices-loaded? false))
     :dispatch-n (list
                   [:fetch-users]
                   [:fetch-devices])}))

(rf/reg-event-db
  :process-users-response
  (fn
    [db [_ response]]
    (-> db
        (assoc :users-loaded? true)
        (assoc :users response))))

(rf/reg-event-db
  :process-devices-response
  (fn
    [db [_ response]]
    (-> db
        (assoc :devices-loaded? true)
        (assoc :devices response))))

(defn replace-by-id [sequence entry]
  (map #(if (= (:id %) (:id entry)) entry %) sequence))

(rf/reg-event-db
  :presence/device-updated
  (fn [db [_ device]]
    (update db :devices replace-by-id device)))

(rf/reg-event-db
  :presence/user-updated
  (fn [db [_ user]]
    (update db :users replace-by-id user)))

(rf/reg-event-db
  :presence/device-added
  (fn [db [_ device]]
    (update db :devices conj device)))