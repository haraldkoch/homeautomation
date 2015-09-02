-(ns homeautomation.ajax
  (:require [ajax.core :as client]))

(defn fetch [url params handler & [error-handler]]
  (client/GET url
              {:headers       {"Accept" "application/transit+json"}
               :params        params
               :handler       handler
               :error-handler error-handler}))