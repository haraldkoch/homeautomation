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

(response-handler add-user [{:keys [params]}]
  (do (db/create-user! params)
      (let [message (str "user " (:username params) " created successfully")]
        (log/info message)
        message)))

(response-handler set-device-owner [{{:keys [device_id owner] :as params} :params}]
  (let [current-owner (:owner (db/get-device {:id device_id}))
        message (str "changed owner from " current-owner " to " owner)]
    (db/set-device-owner! params)
    (presence/update-user-presence owner)                   ;; new owner
    (presence/update-user-presence current-owner)
    (log/info message)
    message))

(response-handler set-device-ignore [{{:keys [ignore device_id] :as params} :params}]
  (do (db/set-device-ignore! params)
      (let [device (db/get-device {:id device_id})
            message (str "device " {:name device} " is now " (if ignore "ignored" "NOT ignored"))]
        (log/info message)
        (presence/update-user-presence (:owner device))
        message)))

;; FIXME: use a validator instead - see Dmitri's selme-guestbook
(response-handler set-device-name [{{:keys [device_id name] :as params} :params}]
  (let [device (db/get-device {:id device_id})
        old-name (:name device)
        message (str "device name changed from '" old-name "' to '" name "'")]
    (if-not (clojure.string/blank? name)
      (do (db/set-device-name! params)
          (log/info message)
          message)
      (do (log/error "set-device-name: new name cannot be empty")
          (response/internal-server-error {:error "set-device-name: new name cannot be empty"})))))

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
