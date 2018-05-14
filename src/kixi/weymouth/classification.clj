(ns kixi.weymouth.classification
  (:require [clojure.data.csv :as csv]
            [kixi.weymouth.witanhttp :as whttp]))

(defn classification-codes
  "Downloads the classification codes file from Witan and returns the distinct first letter of the classification codes."
  [file-id]
  (distinct
   (map #(str (first (first %))) ;; Convert to string the first letter of the value of the first column.
        (-> file-id
            (whttp/download-file-from-witan)
            :payload
            (csv/read-csv)))))
