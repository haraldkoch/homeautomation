(ns homeautomation.telegram
  (:require [homeautomation.config :refer [env]]
            [morse.api :as api]
            [morse.handlers :refer :all]
            [morse.polling :as p]
            [mount.core :refer [defstate]]))

(defn start-command [{{chat-id :id} :chat text :text user :from}]
  (println "user" user "with chat-id" chat-id "joined")
  (api/send-text (:token env) chat-id (str "Welcome to the presence bot, " (:first_name user))))

(defhandler bot-api
  (command "start" update
           (println "received start command" update)
           (start-command update))
  #_(command "presence" (dump-presence))
  
  (message message (println "intercepted message: " message))) 

(defstate channel
          :start (p/start (:token env) bot-api)
          :stop (p/stop channel))
