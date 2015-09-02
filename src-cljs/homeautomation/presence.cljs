(ns homeautomation.presence
  (:require [homeautomation.ajax :refer [fetch]]
            [homeautomation.misc :refer [render-table]]
            [reagent.core :refer [atom]]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [ajax.core :refer [GET POST]]))

(def error (atom nil))

(defn fetch-table [url query result error]
  (fetch url query
         #(do
           (println url query %)
           (reset! result %))
         #(reset! error (get-in % [:resonse :error]))))

(defn show-users []
  [:div.row
   [:div.col-sm-12
    [:h2 "Users"]
    [:div.row
     [:div.col-sm-4 "chk"]
     [:div.col-sm-4 "present"]
     [:div.col-sm-4 "2015-05-05 19:21:!2"]]]])

(defn show-devices []
  (let [devices (atom nil)]
    (fetch-table "/devices" [] devices error)
    (fn []
      [:div.row
       [:div.col-sm-12
        [:h2 "Devices"]
        [render-table @devices]]])))

(defn macaddrs []
  [:div.row
   [:div.col-sm-12
    [:h2 "Unknown MACs"]
    [:div.row
     [:div.col-sm-4 "00:00:00:00:00:00"]
     [:div.col-sm-4 "present"]
     [:div.col-sm-4 "2015-05-05 19:21:!2"]]]])

(defn presence-page []
  [:div.container
   [show-users]
   [show-devices]
   [macaddrs]])
