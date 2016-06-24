(ns homeautomation.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [register-sub]]))

(defn device-sort [devicea deviceb]
  (let [ignore (compare (:ignore devicea) (:ignore deviceb))
        seen (compare (:last_seen deviceb) (:last_seen devicea))
        name (compare (:name devicea) (:name deviceb))]
    (cond (not= 0 ignore) ignore
          (not= 0 seen) seen
          :else name)))

(def presences {"HOME" 0, "AWAY" 1, "UNKNOWN" 2})

(defn compare-presence [usera userb]
  (compare (presences (:presence usera)) (presences (:presence userb))))

(defn users-by-presence [usera userb]
  (let  [presence (compare-presence usera userb)
         name (compare (:username usera) (:username userb))]
    (cond (not= 0 presence) presence
          :else name)))

(register-sub
  :users
  (fn [db _]
    (let [users (reaction (:users @db))]
      (reaction (sort users-by-presence @users)))))

(register-sub
  :usernames
  (fn [db _]
    (let [users (reaction (:users @db))
          usernames (reaction (map #(:username %) @users))]
      (reaction (sort @usernames)))))

(register-sub
  :devices
  (fn [db _]
    (let [devices (reaction (:devices @db))]
      (reaction (sort device-sort @devices)))))

(register-sub
  :named-devices
  (fn [db _]
    (let [devices (reaction (:devices @db))
          named (reaction (filter #(:name %) @devices))]
               (reaction (sort device-sort @named)))))

(register-sub
  :unnamed-devices
  (fn [db _] (let [devices (reaction (:devices @db))
                   unnamed (reaction (filter #(nil? (:name %)) @devices))]
               (reaction (sort-by :macaddr @unnamed)))))

(register-sub :status
              (fn [db _] (reaction (:status @db))))

(register-sub :error
              (fn [db _] (reaction (:error @db))))

(register-sub :devices-loaded?
              (fn [db _] (reaction (:devices-loaded? @db))))

(register-sub :users-loaded?
              (fn [db _] (reaction (:users-loaded? @db))))