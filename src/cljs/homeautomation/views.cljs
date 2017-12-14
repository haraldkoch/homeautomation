(ns homeautomation.views
  (:require
    [reagent.core :as r]
    [re-frame.core :as rf]
    [homeautomation.presence :refer [presence-page]]))

(defn error-modal []
  (when-let [error @(rf/subscribe [:error])]
    [:div.modal-wrapper
     [:div.modal-backdrop
      {:on-click (fn [event]
                   (do
                     (rf/dispatch [:clear-error])
                     (.preventDefault event)
                     (.stopPropagation event)))}]
     [:div.modal-child {:style {:width "70%"}}
      [:div.modal-content.panel-danger
       [:div.modal-header.panel-heading
        [:button.close
         {:type                    "button" :title "Cancel"
          :on-click                #(rf/dispatch [:clear-error])
          :dangerouslySetInnerHTML {:__html "&times;"}}]
        [:h4.modal-title "An anomaly has been detected. Please remain calm."]]
       [:div.modal-body
        [:div [:b error]]]
       [:div.modal-footer
        [:button.btn.btn-default
         {:type     "button" :title "Ok"
          :on-click #(rf/dispatch [:clear-error])}
         "Ok"]]]]]))

(defn status-message []
  (let [status (rf/subscribe [:status])]
    (when-let [status-text @status]
      [:div.row
       [:div.col-sm-11
        [:div.alert.alert-success status-text]]
       [:div-col-sm-1
        [:button.btn.btn-success
         {:type                    "button"
          :title                   "clear"
          :on-click                #(rf/dispatch [:unset-status])
          :dangerouslySetInnerHTML {:__html "&times;"}}]]])))

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
     [:p "Harald's home automation clojure playground. Beware of dragon."]]]
   [:div.row
    [:div.col-md-12
     [:button.btn.btn-sm
      {:on-click #(rf/dispatch [:set-loading])}
      "Set Loading"]
     [:button.btn.btn-sm
      {:on-click #(rf/dispatch [:clear-loading])}
      "Clear Loading"]
     [:button.btn.btn-sm
      {:on-click #(rf/dispatch [:set-status "here is a lovely status message."])}
      "Set Status"]
     [:button.btn.btn-sm
      {:on-click #(rf/dispatch [:clear-status])}
      "Clear Status"]
     [:button.btn.btn-sm
      {:on-click #(rf/dispatch [:set-error "An Error Has Occurred. Panic!"])}
      "Set Error"]]]])

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


(defn main-page []
  (r/with-let [active-page (rf/subscribe [:page])]
    [:div
     [navbar]
     [error-modal]
     [:div.container.content
      [status-message]
      (pages @active-page)]]))

