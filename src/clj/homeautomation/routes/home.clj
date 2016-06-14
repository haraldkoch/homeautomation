(ns homeautomation.routes.home
  (:require [homeautomation.layout :as layout]
            [homeautomation.db.core :as db]
            [homeautomation.presence :as presence]
            [compojure.core :refer [defroutes GET POST]]
            [ring.util.http-response :as response]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]))

(defn home-page []
  (layout/render "home.html"))

; FIXME move all of these routes to a separate namespace

(defmacro response-handler [fn-name args & body]
  `(defn ~fn-name ~args
     (try
       (response/ok (do ~@body))
       (catch Exception e#
         (log/error e# "error handling request")
         (response/internal-server-error {:error (.getMessage e#)})))))

(response-handler get-users []
                  (db/get-users))

(defn add-user [request]
  (try
    (do (db/create-user! (:params request))
        (let [message (str "user " (get-in request [:params :username]) " created successfully")]
          (log/info message)
          message))
    (catch Exception e#
      (log/error e# "error handling request")
      (response/internal-server-error {:error (.getMessage e#)}))))

(defn set-device-owner [request]
  (try
    (do (db/set-device-owner! (:params request))
        (presence/update-user-presence (get-in request [:params :owner]))
        (let [message (str "device now owned by " (get-in request [:params :owner]))]
          (log/info message)
          message))
    (catch Exception e#
      (log/error e# "error handling request")
      (response/internal-server-error {:error (.getMessage e#)}))))

(defn set-device-ignore [request]
  (try
    (do (db/set-device-ignore! (:params request))
        (let [ignored? (get-in request [:params :ignore])
              device-id (get-in request [:params :device_id])
              device (db/get-device {:id device-id})
              user (db/get-user {:id (:owner device)})
              username (:username user)
              message (str "device " {:name device} " is now "  (if ignored? "ignored" "NOT ignored"))]
          (log/info message)
          (presence/update-user-presence username)
          message))
    (catch Exception e#
      (log/error e# "error handling request")
      (response/internal-server-error {:error (.getMessage e#)}))))

(defn set-device-name [request]
  (try
    (do (db/set-device-name! (:params request))
        (let [message (str "device name changed to " (get-in request [:params :name]))]
          (log/info message)
          message))
    (catch Exception e#
      (log/error e# "error handling request")
      (response/internal-server-error {:error (.getMessage e#)}))))

(response-handler get-devices []
                  (db/get-devices))

(defroutes home-routes
           (GET "/" [] (home-page))
           (GET "/users" [] (get-users))
           (GET "/devices" [] (get-devices))
           (POST "/add-user" request (add-user request))
           (POST "/set-device-owner" request (set-device-owner request))
           (POST "/set-device-name" request (set-device-name request))
           (POST "/set-device-ignore" request (set-device-ignore request))
           (GET "/docs" [] (response/ok (-> "docs/docs.md" io/resource slurp))))

