(ns kixi.weymouth
  (:require [kixi.weymouth.witanhttp :as whttp]
            [kixi.weymouth.classification :as classification]
            [kixi.weymouth.addressbase :as addressbase]
            [clojure.data.csv :as csv]
            [clojure.core.async :as a]))

(defn classification-percentage-report->csv [out xs]
  (csv/write-csv out [["Classification"
                       "Percentage"]])
  (csv/write-csv out xs))


(defn no-classification-row [m]
  ((juxt :uprn :startdate :enddate :lastupdate :entrydate :classkey :classifica :classschem :schemevers) m))

(defn no-classification-report->csv [out xs]
  (csv/write-csv out [["UPRN"
                       "Start Date"
                       "End Date"
                       "Last Update"
                       "Entry Date"
                       "Class Key"
                       "Classification"
                       "Class Scheme"
                       "Scheme Version"]])
  (csv/write-csv out (map no-classification-row xs)))

(defn produce-reports [classification-id adprc-id prefix]
  (let [classifications (classification/classification-codes classification-id)
        adprc (addressbase/load-data adprc-id)
        classification-pc-report (addressbase/classification-percentage-report adprc)
        no-classification-report (addressbase/no-classification-report classifications adprc)]
    (with-open [writer (clojure.java.io/writer (str prefix "no-classification-report.csv"))]
      (no-classification-report->csv writer no-classification-report))
    (with-open [writer (clojure.java.io/writer (str prefix "classification-percentage-report.csv"))]
      (classification-percentage-report->csv writer classification-pc-report))))





(comment
  ;; Some useful REPL

  ;; Requirements:
  ;; Environment variables:
  ;; WITAN_ENDPOINT=https://api.witanforcities.com
  ;; WITAN_USERNAME=your_username
  ;; WITAN_PASSWORD=your_password

  ;; 1. Define your address base file id and your classification file id.
  ;; 2. Load the classification file, will return the distinct business classifications.
  ;; 3. Convert the address base csv into a sequence and put that onto the core.async channel.
  ;; 4. Mult the channel.
  ;; 5. The two report types will take off the tap from the events-to-check-mult.

  ;; FOR CHECKER 1.
  ;; 6. Create the counts for each classification type.
  ;; 7. Create a list of the address base lines that don't return any classification.

  ;; FOR CHECKER 2.
  ;;

  )
