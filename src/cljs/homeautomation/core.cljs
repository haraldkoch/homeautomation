(ns homeautomation.core
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [secretary.core :as secretary]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [markdown.core :refer [md->html]]
            [ajax.core :refer [GET POST]]
            [homeautomation.ajax :refer [load-interceptors!]] 
            [homeautomation.events]
            [homeautomation.presence :refer [presence-page]]
            [homeautomation.handlers]                       ;; load these namespaces so that registers trigger
            [homeautomation.subs]
            [homeautomation.ws :as ws])
  (:import goog.History))

(defn nav-link [uri title page collapsed?]
  (let [selected-page (rf/subscribe [:page])]
    [:li.nav-item
     {:class (when (= page @selected-page) "active")}
     [:a.nav-link
      {:href uri
       :on-click #(reset! collapsed? true)} title]]))

(defn navbar []
  (r/with-let [collapsed? (r/atom true)]
    [:nav.navbar.navbar-dark.bg-primary
     [:button.navbar-toggler.hidden-sm-up
      {:on-click #(swap! collapsed? not)} "☰"]
     [:div.collapse.navbar-toggleable-xs
      (when-not @collapsed? {:class "in"})
      [:a.navbar-brand {:href "#/"} "homeautomation"]
      [:ul.nav.navbar-nav
       [nav-link "#/" "Home" :home collapsed?]
       [nav-link "#/presence" "Presence" :presence collapsed?]
       [nav-link "#/about" "About" :about collapsed?]]]]))

(defn about-page []
  [:div.container
   [:div.row
    [:div.col-md-12
     [:img {:src (str js/context "/img/warning_clojure.png")}]
     [:p "Harald's home automation clojure playground. Beware of dragon."]]]])

(defn show-users []
  (let [users (rf/subscribe [:users])]
    (fn []
      [:div.row
       [:div.col-sm-12
        [:table.table.table-striped
         (into [:tbody]
               (for [user @users]
                 [:tr
                  [:td (:first_name user) [:span.small " (" (:username user) ")"]]
                  [:td (:presence user)]]))]]])))

(defn home-page []
  [:div.container
   [:div.row
    [:div.col-xs-12
     [:h1 "Harald's House"]
     [show-users]]]
   [:footer
    [:p.small "Powered by " [:a {:href "http://www.luminusweb.net/"} "Luminus"]]]])

(defmulti pages (fn [page] page))
(defmethod pages :home [_] [home-page])
(defmethod pages :about [_] [about-page])
(defmethod pages :presence [_] [presence-page])


(defn page []
  (r/with-let [active-page (rf/subscribe [:page])]
    [:div
     [navbar]
     (pages @active-page)]))

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
(defn fetch-docs! []
  (GET "/docs" {:handler #(rf/dispatch [:set-docs %])}))

(defn mount-components []
  (rf/clear-subscription-cache!)
  (r/render [#'page] (.getElementById js/document "app"))
    (ws/start-router!))                                       ; call this here so figwheel reloads trigger it


(defn init! []
  (rf/dispatch-sync [:initialize-db])
  (rf/dispatch [:initialize])
  (load-interceptors!)
  (fetch-docs!)
  (hook-browser-navigation!)
  (mount-components))
