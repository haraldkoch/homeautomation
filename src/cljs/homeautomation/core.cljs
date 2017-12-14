(ns homeautomation.core
  (:require [day8.re-frame.http-fx]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [secretary.core :as secretary]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [homeautomation.ajax :refer [load-interceptors!]] 
            [homeautomation.events]
            [homeautomation.handlers]                       ;; load these namespaces so that registers trigger
            [homeautomation.subs]
            [homeautomation.views :refer [main-page]]
            [homeautomation.ws :as ws])
  (:import goog.History))

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (rf/dispatch [:set-active-page :home]))

(secretary/defroute "/about" []
  (rf/dispatch [:set-active-page :about]))

(secretary/defroute "/presence" []
  (rf/dispatch [:set-active-page :presence]))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
        (events/listen
          HistoryEventType/NAVIGATE
          (fn [event]
              (secretary/dispatch! (.-token event))))
        (.setEnabled true)))

;; -------------------------
;; Initialize app


(defn mount-components []
  (rf/clear-subscription-cache!)
  (r/render [#'main-page] (.getElementById js/document "app"))
    (ws/start-router!))                                       ; call this here so figwheel reloads trigger it


(defn init! []
  (load-interceptors!)
  (hook-browser-navigation!)
  (rf/dispatch-sync [:initialize-db])
  (rf/dispatch [:initialize])
  (mount-components))
