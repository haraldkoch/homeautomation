(ns homeautomation.core
  (:require [reagent.core :as r]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [markdown.core :refer [md->html]]
            [homeautomation.ajax :refer [load-interceptors!]]
            [ajax.core :refer [GET POST]]
            [homeautomation.presence :refer [presence-page fetch-users users]]
            [homeautomation.misc :refer [render-table]])
  (:import goog.History))

(defn nav-link [uri title page collapsed?]
  [:li.nav-item
   {:class (when (= page (session/get :page)) "active")}
   [:a.nav-link
    {:href uri
     :on-click #(reset! collapsed? true)} title]])

(defn navbar []
  (let [collapsed? (r/atom true)]
    (fn []
      [:nav.navbar.navbar-light.bg-faded
       [:button.navbar-toggler.hidden-sm-up
        {:on-click #(swap! collapsed? not)} "☰"]
       [:div.collapse.navbar-toggleable-xs
        (when-not @collapsed? {:class "in"})
        [:a.navbar-brand {:href "#/"} "homeautomation"]
        [:ul.nav.navbar-nav
         [nav-link "#/" "Home" :home collapsed?]
	 [nav-link "#/presence" "Presence" :presence collapsed?]
         [nav-link "#/about" "About" :about collapsed?]]]])))

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
      [:table.table.table-striped
       (into [:tbody]
             (for [user @users]
               [:tr
                [:td (:first_name user) [:span.small " (" (:username user) ")"]]
                [:td (:presence user)]]))]]]))

(defn home-page []
  [:div.container
   [:div.row
    [:div.col-xs-12
     [:h1 "Harald's House"]
     [show-users]]]
   [:footer
    [:p.small "Powered by " [:a {:href "http://www.luminusweb.net/"} "Luminus"]]]])

(def pages
  {:home     #'home-page
   :presence #'presence-page
   :about    #'about-page})

(defn page []
  [(pages (session/get :page))])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :page :home))

(secretary/defroute "/presence" []
  (session/put! :page :presence))

(secretary/defroute "/about" []
  (session/put! :page :about))

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
  (GET (str js/context "/docs") {:handler #(session/put! :docs %)}))

(defn mount-components []
  (r/render [#'navbar] (.getElementById js/document "navbar"))
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (load-interceptors!)
  (fetch-docs!)
  (hook-browser-navigation!)
  (mount-components))
