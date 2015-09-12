(ns homeautomation.core
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [markdown.core :refer [md->html]]
            [ajax.core :refer [GET POST]]
            [homeautomation.presence :refer [presence-page fetch-users users]]
            [homeautomation.misc :refer [render-table]])
  (:import goog.History))

(defn navbar []
  [:nav.navbar.navbar-inverse.navbar-fixed-top
   [:div.container-fluid
    [:div.navbar-header
     [:button.navbar-toggle.collapsed {:data-toggle "collapse" :data-target "#navbar-collapse-1" :aria-expanded "false"}
      [:span.sr-only "Toggle Navigation"]
      [:span.icon-bar]
      [:span.icon-bar]
      [:span.icon-bar]]
     [:a.navbar-brand {:href "#/"} "Home Automation"]]
    [:div#navbar-collapse-1.navbar-collapse.collapse
     [:ul.nav.navbar-nav
      [:li {:class (when (= :home (session/get :page)) "active")}
       [:a {:href "#/"} "Home"]]
      [:li {:class (when (= :presence (session/get :page)) "active")}
       [:a {:href "#/presence"} "Presence"]]
      [:li {:class (when (= :about (session/get :page)) "active")}
       [:a {:href "#/about"} "About"]]]]]])

(defn about-page []
  [:div.container
   [:div.row
    [:div.col-md-12
     "Harald's home automation clojure playground. Beware of dragon."]]])

(defn show-users []
  (fetch-users)
  (fn []
    [:div.row
     [:div.col-sm-12
      [:h2 "Users"]
      [render-table (->> @users (map #(dissoc % :id)))]]]))

(defn home-page []
  [:div.container
   [:div.jumbotron
    [:h1 "Harald's House"]
    [show-users]]])

(def pages
  {:home     #'home-page
   :presence #'presence-page
   :about    #'about-page})

(defn page []
  [(pages (session/get :page))])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" [] (session/put! :page :home))
(secretary/defroute "/presence" [] (session/put! :page :presence))
(secretary/defroute "/about" [] (session/put! :page :about))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
      EventType/NAVIGATE
      (fn [event]
        (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn fetch-docs! []
  (GET (str js/context "/docs") {:handler #(session/put! :docs %)}))

(defn mount-components []
  (reagent/render [#'navbar] (.getElementById js/document "navbar"))
  (reagent/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (fetch-docs!)
  (hook-browser-navigation!)
  (mount-components))
