(ns homeautomation.misc
  (:require [cljs-time.core :as t]
            [cljs-time.coerce :as c]
            [cljs-time.format :as f]
            ))

(def my-formatter (f/formatter "yyyy-MM-dd HH:mm:ss"))
(defn fmt-time [t]
  (->> t (c/from-date) (t/to-default-time-zone) (f/unparse my-formatter)))


(defn fmt-date [d]
  (when d (->> d (c/from-date) (t/to-default-time-zone) (f/unparse (f/formatters :date)))))

(def time-only (f/formatters :hour-minute))
(def day-with-time (f/formatter "EEE HH:mm"))
(def month-day-time (f/formatter "MMM-dd HH:mm"))
(def full-date-time (f/formatters :date-hour-minute))

(defn fmt-date-recent [d]
  (let [datetime (-> d (c/from-date) (t/to-default-time-zone))
        i (t/interval datetime (t/now))]
    (cond (< (t/in-days i) 1) (f/unparse time-only datetime)
          (< (t/in-days i) 7) (f/unparse day-with-time datetime)
          (< (t/in-years i) 1) (f/unparse month-day-time datetime)
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

(defn render-table
  ([items]
   (let [items-no-id (->> items (map #(dissoc % :id)))
         columns (keys (first items-no-id))]
     (render-table items columns)))

  ([items columns]
   [:div.table-responsive
    [:table.table.table-striped.table-condensed
     [:thead
      [:tr
       (for [column columns] ^{:key (name column)} [:th (name column)])]]
     (into [:tbody]
           (for [row items]
             (into ^{:key (:id row)} [:tr]
                   (for [column columns]
                     [:td
                      (render-cell (get row column))]))))]]))


