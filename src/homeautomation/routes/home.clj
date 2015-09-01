(ns homeautomation.routes.home
  (:require [homeautomation.layout :as layout]
            [compojure.core :refer [defroutes GET]]
            [ring.util.http-response :refer [ok]]
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
(defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/docs" [] (ok (-> "docs/docs.md" io/resource slurp))))

