(ns kixi.weymouth.addressbase
  (:require [kixi.weymouth.witanhttp :as whttp]
            [clojure.data.csv :as csv]))

(defn format-key
  "Converts the CSV header to a friend Clojure keyword."
  [str-key]
  (when (string? str-key)
    (-> str-key
        clojure.string/lower-case
        (clojure.string/replace #" " "-")
        keyword)))

(defn load-data
  "Loads the CSV file from Witan and converts the data to a Clojure friendly map using the CSV header as the keywords."
  [file-id]
  (let [csv (-> file-id
                (whttp/download-file-from-witan)
                :payload
                (csv/read-csv))
        headers (map format-key (first csv))]
    (map #(zipmap headers %) (rest csv))))

(defn classification-code [m]
  (-> m
      :classifica
      first
      str))

(defn class-frequencies
  [counts x]
  (-> counts
      (assoc x (inc (get counts x 0)))
      (assoc :total (inc (get counts :total 0)))))

(defn class-percentage [total [k v]]
  [k (double (/ v total))])

(defn good-classification? [classifications m]
  ((set classifications) (classification-code m)))

(defn classification-percentage-report
  [address-base-seq]
  (let [result-map (->> address-base-seq
                        (map classification-code)
                        (reduce class-frequencies {}))
        total (:total result-map)]
    (map (partial class-percentage total) (dissoc result-map :total))))

(defn no-classification-report [classifications address-base-seq]
  (remove (partial good-classification? classifications) address-base-seq))
