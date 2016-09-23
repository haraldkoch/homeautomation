(ns homeautomation.presence
  (:require [homeautomation.ajax :refer [fetch send]]
            [homeautomation.misc :refer [render-table fmt-date-recent fmt-time edit-field]]
            [ajax.core :refer [GET POST]]
            [clojure.string :refer [blank?]]
            [reagent.core :refer [atom]]
            [re-frame.core :refer [dispatch dispatch-sync subscribe]]))

(defn clear-indicators [] (dispatch [:set-error nil]) (dispatch [:set-status nil]))

;; FIXME - should these be event handlers instead of local functions?
(defn add-user! [query]
  (send "/add-user" query
        #(do
          (dispatch [:set-status %])
          (dispatch [:fetch-users]))
        #(dispatch [:set-error (get-in % [:response :error])])))

(defn set-owner! [device-id owner]
  (send "/set-device-owner" {:device_id device-id :owner owner}
        #(do
          (dispatch [:fetch-devices])
          (dispatch [:fetch-users]))                        ;; changing an owner can update user presence
        #(dispatch [:set-error (get-in % [:response :error])])))

(defn set-device-name! [device-id name]
  (send "/set-device-name" {:device_id device-id :name name}
        #(dispatch [:fetch-devices])
        #(dispatch [:set-error (get-in % [:response :error])])))

(defn set-ignore! [device-id ignore]
  (send "/set-device-ignore" {:device_id device-id :ignore ignore}
        #(do
          (dispatch [:fetch-devices])
          (dispatch [:fetch-users]))                        ;; toggling ignore can update user presence
        #(dispatch [:set-error (get-in % [:response :error])])))

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
                          (add-user! form-data))}
          "add user"]]]])))

(defn show-users []
  (let [show-user-form (atom nil)
        form-data (atom {:username nil :first_name nil :last_name nil})
        users (subscribe [:users])
        users-loaded? (subscribe [:users-loaded?])]
    (fn []
      [:div
       [add-user-form show-user-form form-data]
       [:div.row
        [:div.col-sm-10
         [:h2 "Users"]
         (if-not @users-loaded?
           [:div "Loading Users..."]
           [render-table @users [:username :presence :first_name :last_name]])]
        [:div.col-sm-2
         [:button.btn.btn-primary
          {:on-click #(do (clear-indicators)
                          (if @show-user-form
                            (reset! show-user-form nil)
                            (reset! show-user-form true)))}
          (if @show-user-form "hide" "new user")]]]])))

(defn device-name-field [_ _]
  (let [editing (atom false)]
    (fn [id name]
      [:div.editable {:class (str (if @editing "editing"))}
       [:div.view
        (if (blank? name)
          [:button {:on-click #(reset! editing true)} "Add Name"]
          [:label {:on-double-click #(reset! editing true)} name])
        ]
       (when @editing
         ; FIXME - allow deleting the name also
         [edit-field {:class   "edit"
                                   :initial name
                                   :on-save #(set-device-name! id %)
                                   :on-stop #(reset! editing false)}])])))

(defn username-selection-list [_ _]
  (let [usernames (subscribe [:usernames])]
    (fn [device-id user]
      [:div.form-group
       (into [:select.form-control
              {:default-value user
               :on-change     #(set-owner! device-id (.-value (.-target %)))}]
             (for [name (conj @usernames "")]
               [:option {:value name} name]))])))

(defn ignore-checkbox [device-id ignore]
  [:div.form-group
   [:input.form-control
    {:type      :checkbox :checked ignore
     :on-change #(set-ignore! device-id (.-checked (.-target %)))}]])

(defn devices-table [items]
  [:table.table.table-striped.table-condensed
   [:thead [:tr [:th "Device"] [:th "Owner"] [:th "Ignore"] [:th "Status"] [:th [:div "Last Change"] [:div "Last Seen"]]]]
   (into [:tbody]
         (for [device items]
           ^{:key (:id device)}
           [:tr
            [:td
             [device-name-field (:id device) (:name device)]
             [:div.small (:macaddr device)]]
            [:td [username-selection-list (:id device) (:owner device)]]
            [:td [ignore-checkbox (:id device) (:ignore device)]]
            [:td (:status device)]
            [:td
             [:div {:title (fmt-time (:last_status_change device))}
              (fmt-date-recent (:last_status_change device))]
             [:div {:title (fmt-time (:last_seen device))}
              (fmt-date-recent (:last_seen device))]]]))])

(defn show-devices []
  (let [devices (subscribe [:named-devices])
        devices-loaded? (subscribe [:devices-loaded?])]
    (fn []
      (if-not @devices-loaded?
        [:div "Loading Devices..."]
        [:div.row
         [:div.col-sm-12
          [:h2 "Devices"]
          [devices-table @devices]]]))))

(defn macaddr-table [items]
  [:table.table.table-striped.table-condensed
   [:thead [:tr [:th "Device"] [:th "Name"] [:th "Ignore"] [:th "Status"] [:th [:div "Last Change"] [:div "Last Seen"]]]]
   (into [:tbody]
         (for [device items]
           ^{:key (:id device)}
           [:tr
            [:td (:macaddr device)]
            [:td [device-name-field (:id device) (:name device)]]
            [:td (:ignore device)]
            [:td (:status device)]
            [:td
             [:div {:title (fmt-time (:last-status_change device))}
              (fmt-date-recent (:last_status_change device))]
             [:div {:title (fmt-time (:last-seen device))}
              (fmt-date-recent (:last_seen device))]]]))])

(defn macaddrs []
  (let [devices (subscribe [:unnamed-devices])
        devices-loaded? (subscribe [:devices-loaded?])]
    (fn []
      (if @devices-loaded?
        [:div.row
         [:div.col-sm-12
          [:h2 "New MAC Addresses"]
          [macaddr-table @devices]]]))))

(defn status-bar []
  (let [status (subscribe [:status])]
    (fn []
      (if @status
        [:div.row
         [:div.col-sm-12
          [:div.alert.alert-success "Status:" @status]]]))))

(defn error-bar []
  (let [error (subscribe [:error])]
    (fn []
      (if @error
        [:div.row
         [:div.col-sm-12
          [:div.alert.alert-danger "Error:" @error]]]))))

(defn presence-page []
  [:div.container
   [status-bar]
   [error-bar]
   [show-users]
   [show-devices]
   [macaddrs]])
