- (ns homeautomation.ajax
    (:require [ajax.core :as client]))

(defn fetch [url params handler & [error-handler]]
  (client/GET (str js/context url)
              {:headers       {"Accept" "application/transit+json"}
               :params        params
               :handler       handler
               :error-handler error-handler}))

(defn send [url params handler & [error-handler]]
  (client/POST (str js/context url)
               {:headers       {"Accept" "application/transit+json"
                                "X-CSRF-Token" js/csrfToken}
                :params        params
                :handler       handler
                :error-handler error-handler}))