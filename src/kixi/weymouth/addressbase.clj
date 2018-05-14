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
      str
      clojure.string/trim))

(defn good-classification? [classifications m]
  ((set classifications) (classification-code m)))

(defn class-frequencies
  [classifications counts x]
  (let [raw-code (classification-code x)
        classification (cond
                         ((set classifications) raw-code) raw-code
                         (= "" raw-code) "Null/Empty"
                         :default "Anything else, e.g. '09'")]
    (-> counts
        (assoc classification (inc (get counts classification 0)))
        (assoc :total (inc (get counts :total 0))))))

(defn class-percentage [total [k v]]
  [k (double (/ v total))])

(defn classification-percentage-report
  [classifications address-base-seq]
  (let [result-map (->> address-base-seq
                        ;;(map classification-code)
                        (reduce (partial class-frequencies classifications) {}))
        total (:total result-map)]
    (into {} (map (partial class-percentage total) (dissoc result-map :total)))))

(defn fix-missing-classification [x]
  (if (= "" (get x :classifica))
    (assoc x :classifica "Missing classification code")
    x))

(defn no-classification-report [classifications address-base-seq]
  (->> address-base-seq
       (remove (partial good-classification? classifications))
       (map fix-missing-classification)))
