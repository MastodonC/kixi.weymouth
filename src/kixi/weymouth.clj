(ns kixi.weymouth
  (:require [kixi.weymouth.witanhttp :as whttp]
            [kixi.weymouth.classification :as classification]
            [kixi.weymouth.addressbase :as addressbase]
            [kixi.weymouth.geo :as geo]
            [dk.ative.docjure.spreadsheet :as excel]
            [clojure.data.csv :as csv]
            [clojure.core.async :as a]
            [clojure.java.io :as io]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Checker 1
(defn classification-percentage-report [xs]
  (let [classifications [["C" "Commercial"]
                         ["L" "Land"]
                         ["M" "Military"]
                         ["O" "Other (Ordnance Survey Only)"]
                         ["P" "Parent Shell"]
                         ["R" "Residential"]
                         ["U" "Unclassified"]
                         ["X" "Dual Use"]
                         ["Z" "Object of Interest"]
                         ["Anything else, e.g. '09'" "Misclassified"]
                         ["Null/empty" "No classification"]]]
    (into [["Concatenated"
            "Class_Desc"
            "Percentage"]]
          (map (fn [[c desc]]
                 [c desc (get xs c 0)])
               classifications))))

(defn no-classification-row [m]
  ((juxt :uprn :classifica) m))

(defn no-classification-report [xs]
  (into [["UPRN"
          "Incorrect or Missing Classification Code"]]
        (->> (map no-classification-row xs)
             (map (fn [[u c]] (if (= "Missing classification code" c)
                                [u (str "~" c)]
                                [u c])))
             (sort-by second)
             (map (fn [[u c]] (if (= "~Missing classification code" c)
                                [u "Missing classification code"]
                                [u c]))))))

(defn checker-1-xls [out-file-name ncr cpr]
  (let [wb (excel/create-workbook
            "No Classification"
            ncr
            "Classification Percentages"
            cpr)
        h-style (excel/create-cell-style! wb {:font {:size 10 :bold true}})
        pct-fmt (excel/create-cell-style! wb {:data-format "0.00%"})
        txt-fmt (excel/create-cell-style! wb {:data-format "@"})]
    ;; tweak headers
    (excel/set-row-style! (first (excel/row-seq (excel/select-sheet "No Classification" wb))) h-style)
    (excel/set-row-style! (first (excel/row-seq (excel/select-sheet "Classification Percentages" wb))) h-style)

    ;; tweak percentages
    (run! #(excel/set-row-styles! % [txt-fmt txt-fmt pct-fmt]) (drop 1 (excel/select-sheet "Classification Percentages" wb)))

    (excel/save-workbook-into-file! out-file-name wb)))

(defn ->checker1-report [classification-id adprc-id prefix]
  (let [classifications (classification/classification-codes classification-id)
        adprc (addressbase/load-data adprc-id)
        ncr (no-classification-report (addressbase/no-classification-report classifications adprc))
        cpr(classification-percentage-report (addressbase/classification-percentage-report classifications adprc))]
    (checker-1-xls (str prefix "-checker-1.xlsx") ncr cpr)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Checker 2
;; "UPRN not within building percentage"
;; "UPRN not within building list"
(defn uprn-not-in-toid-% [report-map]
  (let [pct (double
             (/ (:not-in-topo report-map)
                (:count report-map)))]
    [["Percentage of UPRNs not within Building"]
     [pct]]))

(defn uprns-not-in-toid [report-map]
  (into [["UPRN not within building list"]]
        (map #(vector %) (:uprns-not-in-topo report-map))))

(defn ->checker-2-report [prefix topography xref addresspoint]
  (let [report-map (geo/uprns-not-in-topography topography
                                                xref
                                                addresspoint)
        wb (excel/create-workbook
            "UPRN not within building pct"
            (uprn-not-in-toid-% report-map)
            "UPRN not within building list"
            (uprns-not-in-toid report-map))
        h-style (excel/create-cell-style! wb {:font {:size 10 :bold true}})
        pct-fmt (excel/create-cell-style! wb {:data-format "0.00%"})
        txt-fmt (excel/create-cell-style! wb {:data-format "@"})]
    ;; tweak headers
    (excel/set-row-style! (first (excel/row-seq (excel/select-sheet "UPRN not within building pct" wb))) h-style)
    (excel/set-row-style! (first (excel/row-seq (excel/select-sheet "UPRN not within building list" wb))) h-style)

    ;; tweak percentages
    (run! #(excel/set-row-styles! % [pct-fmt]) (drop 1 (excel/select-sheet "UPRN not within building pct" wb)))

    (excel/save-workbook-into-file! (str prefix "-checker-2.xlsx") wb)))
