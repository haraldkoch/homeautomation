(ns homeautomation.events
  (:require [homeautomation.db :as db]
            [re-frame.core :refer [dispatch reg-event-db reg-sub]]))

;;dispatchers

(reg-event-db
  :initialize-db
  (fn [_ _]
    db/default-db))

(reg-event-db
  :set-active-page
  (fn [db [_ page]]
    (assoc db :page page)))

;;subscriptions

(reg-sub
  :page
  (fn [db _]
    (:page db)))

