(ns homeautomation.routes.home
  (:require [homeautomation.layout :as layout]
            [homeautomation.db.core :as db]
            [compojure.core :refer [defroutes GET POST]]
            [ring.util.http-response :refer :all]
            [taoensso.timbre :as timbre]
            [clojure.java.io :as io]))

(defn home-page []
  (layout/render "home.html"))

(defmacro response-handler [fn-name args & body]
  `(defn ~fn-name ~args
     (try
       (ok (do ~@body))
       (catch Exception e#
         (timbre/error "error handling request" e#)
         (internal-server-error {:error (.getMessage e#)})))))

(response-handler get-users []
                  (db/get-users))

(defn add-user [request]
  (try
    (do (db/create-user! (:params request))
        (let [message (str "user " (get-in request [:params :username]) " created successfully")]
          (timbre/info message)
          message))
    (catch Exception e#
      (timbre/error "error handling request" e#)
      (internal-server-error {:error (.getMessage e#)}))))

(defn set-device-owner [request]
  (try
    (do (db/set-device-owner! (:params request))
        (let [message (str "device now owned by " (get-in request [:params :owner]))]
          (timbre/info message)
          message))
    (catch Exception e#
      (timbre/error "error handling request" e#)
      (internal-server-error {:error (.getMessage e#)}))))

(response-handler get-devices []
                  (db/get-devices))

(defroutes home-routes
           (GET "/" [] (home-page))
           (GET "/users" [] (get-users))
           (GET "/devices" [] (get-devices))
           (POST "/add-user" request (add-user request))
           (POST "/set-device-owner" request (set-device-owner request))
           (GET "/docs" [] (ok (-> "docs/docs.md" io/resource slurp))))

