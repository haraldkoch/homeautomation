(ns homeautomation.presence
  (:require [homeautomation.ajax :refer [fetch send]]
            [homeautomation.misc :refer [render-table render-cell fmt-date-recent fmt-date]]
            [reagent.core :refer [atom]]
            [ajax.core :refer [GET POST]]))

(def error (atom nil))
(def status (atom nil))
(def users (atom nil))
(def devices (atom nil))

(defn clear-indicators [] (reset! error nil) (reset! status nil))

(defn fetch-table [url query result error]
  (fetch url query
         #(do
           (reset! result %))
         #(reset! error (get-in % [:resonse :error]))))

(defn fetch-users [] (fetch-table "/users" [] users error))
(defn fetch-devices [] (fetch-table "/devices" [] devices error))

(defn add-user! [query result]
  (send "/add-user" @query
        #(do
          (clear-indicators)
          (reset! result %)
          (fetch-users))
        #(reset! error (get-in % [:response :error]))))

(defn set-owner! [device-id owner]
  (send "/set-device-owner" {:device_id device-id :owner owner}
        #(fetch-devices)
        #(reset! error (get-in % [:response :error]))))

(defn set-device-name! [device-id name]
  (send "/set-device-name" {:device_id device-id :name name}
        #(fetch-devices)
        #(reset! error (get-in % [:response :error]))))

(defn set-ignore! [device-id ignore]
  (send "/set-device-ignore" {:device_id device-id :ignore ignore}
        #(fetch-devices)
        #(reset! error (get-in % [:response :error]))))

(defn input-field [param data-atom]
  [:input.form-control
   {:type        :text :value (get @data-atom param)
    :placeholder (name param)
    :on-change   #(swap! data-atom assoc param (.-value (.-target %)))}])

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
         [render-table (->> @users (map #(dissoc % :id)))]]
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
  [:div.form-group
   (into [:select.form-control {:default-value user
                                :on-change     #(set-owner! device-id (.-value (.-target %)))}]
         (for [name (usernames)]
           [:option {:value name} name]))])

(defn ignore-checkbox [device-id ignore]
  [:div.form-group
   [:input.form-control
    {:type      :checkbox :checked ignore :defaultChecked ignore
     :on-change #(set-ignore! device-id (.-checked (.-target %)))}]])

(defn device-sort [devicea deviceb]
  (let [ignore (compare (:ignore devicea) (:ignore deviceb))
        seen (compare (:last_seen deviceb) (:last_seen devicea))
        name (compare (:name devicea) (:name deviceb))]
    (cond (not= 0 ignore) ignore
          (not= 0 seen) seen
          :else name)))

(defn devices-table [items]
  [:table.table.table-striped.table-condensed
   [:thead [:tr [:th "Device"] [:th "Owner"] [:th "Ignore"] [:th "Status"] [:th "Seen"]]]
   (into [:tbody]
         (for [device (sort device-sort items)]
           [:tr
            [:td [:div (:name device)] [:div.small (:macaddr device)]]
            [:td [username-selection-list (:id device) (:owner device)]]
            [:td [ignore-checkbox (:id device) (:ignore device)]]
            [:td (:status device)]
            [:td
             [:div {:title (fmt-date (:last_status_change device))}
              (fmt-date-recent (:last_status_change device))]
             [:div {:title (fmt-date (:last_seen device))}
              (fmt-date-recent (:last_seen device))]]]))])

(defn show-devices []
  (let []
    (fetch-table "/devices" [] devices error)
    (fn []
      [:div.row
       [:div.col-sm-12
        [:h2 "Devices"]
        (let [items (filter #(:name %) @devices)]
          [devices-table items])]])))

(defn device-name-field [id name]
  [:input.form-control
   {:type        :text :value name
    :placeholder "device name"
    :on-key-down #(case (.-which %)
                   13 (set-device-name! id (.-value (.-target %)))
                   "default")
    :on-blur     #(set-device-name! id (.-value (.-target %)))
    }])

(defn macaddr-table [items]
  [:table.table.table-striped.table-condensed
   [:thead [:tr [:th "Device"] [:th "Name"] [:th "Ignore"] [:th "Status"] [:th "Seen"]]]
   (into [:tbody]
         (for [device items]
           [:tr
            [:td (:macaddr device)]
            [:td [device-name-field (:id device) (:name device)]]
            [:td (:ignore device)]
            [:td (:status device)]
            [:td
             [:div {:title (fmt-date (:last-status_change device))}
              (fmt-date-recent (:last_status_change device))]
             [:div {:title (fmt-date (:last-seen device))}
              (fmt-date-recent (:last_seen device))]]]))])

(defn macaddrs []
  (fn []
    [:div.row
     [:div.col-sm-12
      [:h2 "New MAC Addresses"]
      (let [items (filter #(nil? (:name %)) @devices)]
        [macaddr-table items])]]))

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
