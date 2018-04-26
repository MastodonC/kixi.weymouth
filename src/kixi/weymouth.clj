(ns kixi.weymouth
  (:require [kixi.weymouth.witanhttp :as whttp]
            [kixi.weymouth.classification :as classification]
            [kixi.weymouth.addressbase :as addressbase]
            [kixi.weymouth.geo :as geo]
            [clojure.data.csv :as csv]
            [clojure.core.async :as a]
            [clojure.java.io :as io]))

(defn classification-percentage-report->csv [out xs]
  (csv/write-csv out [["Classification"
                       "Percentage"]])
  (csv/write-csv out xs))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Checker 1
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


(defn checker1-reports [classification-id adprc-id prefix]
  (let [classifications (classification/classification-codes classification-id)
        adprc (addressbase/load-data adprc-id)
        classification-pc-report (addressbase/classification-percentage-report adprc)
        no-classification-report (addressbase/no-classification-report classifications adprc)]
    ;; checker 1
    (with-open [writer (clojure.java.io/writer (str prefix "no-classification-report.csv"))]
      (no-classification-report->csv writer no-classification-report))
    (with-open [writer (clojure.java.io/writer (str prefix "classification-percentage-report.csv"))]
      (classification-percentage-report->csv writer classification-pc-report))
    ;; checker 2
    (with-open [writer (clojure.java.io/writer (str prefix "urpn-not-in-toid-percentage-report.csv"))]
      (uprn-not-in-toid-%->csv writer classification-pc-report))
    (with-open [writer (clojure.java.io/writer (str prefix "urpn-not-in-toid-percentage-report.csv"))]
      (uprns-not-in-toid->csv writer classification-pc-report))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Checker 2
;; "UPRN not within building percentage"
;; "UPRN not within building list"
(defn uprn-not-in-toid-%->csv [out report-map]
  (let [pct (double
             (* 100 (/ (:not-in-topo report-map)
                       (:count report-map))))]
    (csv/write-csv out [["Percentage of UPRNs not within Building"]])
    (csv/write-csv out [[(str pct "%")]])))

(defn uprns-not-in-toid->csv [out report-map]
  (csv/write-csv out [["UPRN not within building list"]])
  (csv/write-csv out (mapv #(vector %) (:uprns-not-in-topo report-map))))

(defn checker2-reports [prefix topography xref addresspoint]
  (let [report-map (geo/uprns-not-in-topography topography
                                                xref
                                                addresspoint)]
    (with-open [w (io/writer (str prefix "-uprn-not-within-building-percentage.csv"))]
      (uprn-not-in-toid-%->csv w report-map))
    (with-open [w (io/writer (str prefix "-uprn-not-within-building-list.csv"))]
      (uprns-not-in-toid->csv w report-map))))

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

  (def bury-report (checker2-reports
                    "bury"
                    "data/bury_topography_esri/Topography.shp"
                    "data/bury_building_esri/Bury_ADPRAXR.dbf"
                    "data/bury_building_esri/AddressPoint.shp"))

  (def bcbc-report (checker2-reports
                    "bcbc"
                    "data/bcbc_topography_esri/Topography.shp"
                    "data/bcbc_building_esri/BCBCADPRAXR.dbf"
                    "data/bcbc_building_esri/AddressPoint.shp"))



  )
