(ns homeautomation.routes.home
  (:require [homeautomation.layout :as layout]
            [homeautomation.db.core :as db]
            [compojure.core :refer [defroutes GET]]
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

#_(response-handler get-users []
                  (db/get-users))

(response-handler get-devices []
                  (db/get-devices))

(defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/devices" [] (get-devices))
  (GET "/docs" [] (ok (-> "docs/docs.md" io/resource slurp))))

