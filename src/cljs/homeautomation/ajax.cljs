(ns homeautomation.ajax
  (:require [ajax.core :as ajax]))

(defn default-headers [request]
  (-> request
      (update :uri #(str js/context %))
      (update
        :headers
        #(merge
          %
          {"Accept" "application/transit+json"
           "x-csrf-token" js/csrfToken}))))

(defn load-interceptors! []
  (swap! ajax/default-interceptors
         conj
         (ajax/to-interceptor {:name "default headers"
                               :request default-headers})))


(defn fetch [url params handler & [error-handler]]
  (ajax/GET url
              {:params        params
               :handler       handler
               :error-handler error-handler}))

(defn send [url params handler & [error-handler]]
  (ajax/POST url
               {:params        params
                :handler       handler
                :error-handler error-handler}))