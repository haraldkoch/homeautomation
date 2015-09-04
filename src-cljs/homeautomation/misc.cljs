(ns homeautomation.misc
  (:require [cljs-time.core :as t]
            [cljs-time.coerce :as c]
            [cljs-time.format :as f]
            ))

; MSFT is very strange
(def TICKS_AT_EPOCH_LDAP 116444736000000000)
(def TICKS_PER_SECOND 10000)

(defn cvt-ticks-to-date [t]
  (-> t (- TICKS_AT_EPOCH_LDAP) (/ TICKS_PER_SECOND) (c/from-long) (c/to-date)))

(def my-formatter (f/formatter "yyyy-MM-dd HH:mm:ss"))
(defn fmt-time [t]
  (->> t (c/from-date) (t/to-default-time-zone) (f/unparse my-formatter)))


(defn fmt-date [d]
  (->> d (c/from-date) (t/to-default-time-zone) (f/unparse (f/formatters :date))))

(def time-only (f/formatters :hour-minute))
(def day-with-time (f/formatter "EEE HH:mm"))
(def month-day-time (f/formatter "MMM-dd HH:mm"))
(def full-date-time (f/formatters :date-hour-minute))

(defn fmt-date-recent [d]
  (let [datetime (-> d (c/from-date) (t/to-default-time-zone))
        i (t/interval datetime (t/now))]
    (cond (< (t/in-days i) 0) (f/unparse time-only datetime)
          (< (t/in-days i) 7) (f/unparse day-with-time datetime)
          (< (t/in-years i) 0) (f/unparse month-day-time datetime)
          :else (f/unparse full-date-time datetime))))

(defn escape-html [s]
  (clojure.string/escape s
                         {"&"  "&amp;"
                          ">"  "&gt;"
                          "<"  "&lt;"
                          "\"" "&quot;"}))

(defn render-keyword [k]
  (->> k ((juxt namespace name)) (remove nil?) (clojure.string/join "/")))

(defn render-cell [v]
  (let [t (type v)]
    (cond
      (= t Keyword) [:span.jh-type-string (render-keyword v)]
      (= t js/String) [:span.jh-type-string (escape-html v)]
      (= t js/Date) [:span.jh-type-date (fmt-time v)]
      (= t js/Boolean) [:span.jh-type-bool (str v)]
      (= t js/Number) [:span.jh-type-number v]
      nil [:span.jh-empty nil]
      :else (str v))))

(defn render-table [items]
  (let [columns (keys (first items))]
    [:div.row
     [:div.col-sm-12
      [:table.table.table-striped
       [:thead
        [:tr
         (for [column columns] ^{:key (name column)} [:th (name column)])]]
       (into [:tbody]
             (for [row items]
               (into [:tr]
                     (for [column columns]
                       [:td
                        (render-cell (get row column))]))))]]]))

(defn spinner []
  [:div.spinner
   [:div.bounce1]
   [:div.bounce2]
   [:div.bounce3]])

