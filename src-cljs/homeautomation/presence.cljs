(ns homeautomation.presence
  (:require [homeautomation.ajax :refer [fetch send]]
            [homeautomation.misc :refer [render-table render-cell]]
            [reagent.core :refer [atom]]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [ajax.core :refer [GET POST]]))

(def error (atom nil))
(def status (atom nil))
(def users (atom nil))
(def devices (atom nil))

(defn clear-indicators [] (reset! error nil) (reset! status nil))

(defn fetch-table [url query result error]
  (fetch url query
         #(do
           (println url query %)
           (reset! result %))
         #(reset! error (get-in % [:resonse :error]))))

(defn fetch-users [] (fetch-table "/users" [] users error))
(defn fetch-devices [] (fetch-table "/devices" [] devices error))

(defn add-user! [query result]
  (send "/add-user" @query
        #(do
          (clear-indicators)
          (println "add-user" @query %)
          (reset! result %)
          (fetch-users))
        #(reset! error (get-in % [:response :error]))))

(defn set-owner! [device-id owner]
  (println "set-owner!" device-id owner)
  (send "/set-device-owner" {:device_id device-id :owner owner}
        #(fetch-devices)
        #(reset! error (get-in % [:response :error]))))

(defn input-field [param data-atom]
  [:input.form-control
   {:type        :text :value (get @data-atom param)
    :placeholder (name param)
    :on-change   #(swap! data-atom assoc param (.-target.value %))}])

(defn add-user-form [show-user-form form-data]
  (fn []
    (if @show-user-form
      [:div.row
       [:div.col-sm-12
        [:div.input-group
         [input-field :username form-data]
         [input-field :first_name form-data]
         [input-field :last_name form-data]
         [:button.btn.btn-primary
          {:on-click #(do (reset! show-user-form nil)
                          (add-user! form-data status))}
          "add user"]]]])))

(defn show-users []
  (let [show-user-form (atom nil)
        form-data (atom {:username nil :first_name nil :last_name nil})]
    (fetch-users)
    (fn []
      [:div
       [add-user-form show-user-form form-data]
       [:div.row
        [:div.col-sm-10
         [:h2 "Users"]
         [render-table @users]]
        [:div.col-sm-2
         [:button.btn.btn-primary
          {:on-click #(do (clear-indicators)
                          (if @show-user-form
                            (reset! show-user-form nil)
                            (reset! show-user-form true)))}
          (if @show-user-form "hide" "new user")]]]])))

(defn usernames []
  (conj (map #(:username %) @users) ""))

(defn username-selection-list [device-id user]
  (println "device-id " device-id " user " user)
  [:div.form-group
   (into [:select.form-control {:default-value user
                                :on-change #(set-owner! device-id (.. % -target -value))}]
         (for [name (usernames)] [:option {:value name} name]))])

(defn show-devices []
  (let []
    (fetch-table "/devices" [] devices error)
    (fn []
      [:div.row
       [:div.col-sm-12
        [:h2 "Devices"]
        (let [items @devices
              columns (keys (first items))]
          [:div.row
           [:div.col-sm-12
            [:table.table.table-striped
             [:thead
              [:tr
               (for [column columns] ^{:key (name column)} [:th (name column)])]]
             (into [:tbody]
                   (for [row items]
                     (into [:tr]
                           (for [column columns]
                             [:td 
                              (if (= column :owner)
                                (username-selection-list (:id row) (:owner row))
                                (render-cell (get row column)))]))))]]])]])))

(defn macaddrs []
  [:div.row
   [:div.col-sm-12
    [:h2 "Unknown MACs"]
    [:div.row
     [:div.col-sm-4 "00:00:00:00:00:00"]
     [:div.col-sm-4 "present"]
     [:div.col-sm-4 "2015-05-05 19:21:!2"]]]])

(defn status-bar []
  (fn []
    (if @status
      [:div.row
       [:div.col-sm-12
        [:div.alert.alert-success "Status:" @status]]])))

(defn error-bar []
  (if @error
    [:div.row
     [:div.col-sm-12
      [:div.alert.alert-danger "Error:" @error]]]))

(defn presence-page []
  [:div.container
   [status-bar]
   [error-bar]
   [show-users]
   [show-devices]
   [macaddrs]])
