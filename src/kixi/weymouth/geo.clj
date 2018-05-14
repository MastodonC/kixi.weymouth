(ns kixi.weymouth.geo
  (:require [clojure.java.io :as io]
            [clojure.string :as st])
  (:import [java.nio.charset Charset])
  (:import [org.geotools.data.shapefile ShapefileDataStore])
  (:import [org.geotools.data.shapefile.dbf DbaseFileReader DbaseFileHeader])
  (:import [org.geotools.data.store ContentFeatureSource ContentFeatureCollection])
  (:import [org.geotools.data.simple SimpleFeatureIterator])
  (:import [org.geotools.factory CommonFactoryFinder])
  (:import [org.geotools.filter.text.cql2 CQL])
  (:import [org.opengis.geometry.primitive Point])
  (:import [org.opengis.feature.simple SimpleFeature])
  (:import [org.opengis.filter FilterFactory FilterFactory2])
  (:import [com.vividsolutions.jts.geom MultiPolygon GeometryFactory Geometry Coordinate]))

(defn get-geom [feature]
  (.getDefaultGeometry feature))



(defn dbf-file-channel [filepath]
  (-> filepath
      java.io.FileInputStream.
      .getChannel))

(defn dbase-loader [filepath]
  (DbaseFileReader. (dbf-file-channel filepath) false (Charset/forName "ISO-8859-1")) )


(defn extract-toid [refstring]
  (st/replace refstring #"osgb" ""))


(defn iteration-seq [iteration]
  (iterator-seq
   (reify java.util.Iterator
     (hasNext [this] (.hasNext iteration))
     (next [this] (.next iteration))
     (remove [this] (.remove iteration)))))

(defn get-file [filepath]
  (io/file filepath))

(defn shapefile-datastore [f]
  (ShapefileDataStore. (.toURL f)))

(defn features-coll [path]
  (-> path
      io/file
      shapefile-datastore
      .getFeatureSource
      .getFeatures))

(defn geo-filter-factory []
  (CommonFactoryFinder/getFilterFactory2))

(def header [:uprn :start-date :end-date :last-update :entry-date :xref-key :cross-refer :version :source])

(defn dbf-to-vec [dbfile]
  (let [dbf (dbase-loader dbfile)]
    (loop [d (.readEntry dbf)
           records []]
      (if (.hasNext dbf)
        (recur (.readEntry dbf)
               (conj records (vec d)))
        records))))

(defn toid-rec? [row]
  (= (last row) "7666MT"))

(defn select-fields [row]
  (select-keys row [:uprn :start-date :end-date :cross-refer]))

(defn fix-toid [record]
  (assoc record :toid
         (st/replace (:cross-refer record) #"^osgb" "")))

(def uprn-toid-xref-xf
  (comp
   (filter toid-rec?)
   (map #(zipmap header %))
   (map select-fields)
   (map fix-toid)))

(defn cql-filter [cql feature-coll]
  (.subCollection feature-coll (CQL/toFilter cql)))

(defn feature-coll->seq [fc]
  (iteration-seq (.features fc)))

(defn feature-by-toid [toid topo-features]
  (-> (cql-filter (str "Toid = " toid) topo-features)
      feature-coll->seq
      first))

(defn addressbase-within-topo? [topo-hashmap xref-lookup ab-feature]
  (let [uprn (.getAttribute ab-feature "Uprn")
        toid (get xref-lookup uprn)
        ab-point (.getDefaultGeometry ab-feature)
        topo-poly (get topo-hashmap toid)]
    (when topo-poly
      (.within ab-point topo-poly))))

(defn toid-map [topography-path]
  (reduce
   (fn [a x]
     (assoc a (.getAttribute x "Toid") (.getDefaultGeometry x)))
   {}
   (-> topography-path features-coll feature-coll->seq)))

(defn xref-map [xref-path]
  (transduce uprn-toid-xref-xf
             (fn
               ([acc x]
                (assoc acc (:uprn x) (:toid x)))
               ([a] a))
             {}
             (dbf-to-vec xref-path)))

(defn uprn-report-counter [toids xref a x]
  (-> a
      (assoc :count (inc (get a :count 0)))
      ((fn [a] (if-not (addressbase-within-topo? toids xref x)
                 (assoc a
                        :not-in-topo (inc (get a :not-in-topo 0))
                        :uprns-not-in-topo (conj (get a :uprns-not-in-topo []) (.getAttribute x "Uprn")))
                 a)))))

(defn uprns-not-in-topography [topography-path xref-path addressbase-path]
  (let [toids (toid-map topography-path)
        xref (xref-map xref-path)
        ab-seq (-> addressbase-path features-coll feature-coll->seq)]
    (reduce
     (partial uprn-report-counter toids xref)
     {}
     ab-seq)))

(comment

  (def bury-report (uprns-not-in-topography
                    "data/bury_topography_esri/Topography.shp"
                    "data/bury_building_esri/Bury_ADPRAXR.dbf"
                    "data/bury_building_esri/AddressPoint.shp"))

  (def bcbc-report (uprns-not-in-topography
                    "data/bcbc_topography_esri/Topography.shp"
                    "data/bcbc_building_esri/BCBCADPRAXR.dbf"
                    "data/bcbc_building_esri/AddressPoint.shp"))

  ;; Addresspoint
  (def abfilepath "data/bury_building_esri/AddressPoint.shp")
  (def ab (features-coll abfilepath))
  (def ab-seq (feature-coll->seq ab))

  ;; Cross ref file
  (def crf (dbf-to-vec "data/bury_building_esri/Bury_ADPRAXR.dbf"))
  (def cr (into [] uprn-toid-xref-xf crf))
  (def crlookup (reduce (fn [acc x]
                          (assoc acc (:uprn x) (:toid x))) {} cr))

  ;; Topography (shp)
  (def topog "data/bury_topography_esri/Topography.shp")
  (def top (features-coll topog))
  (def top-seq (feature-coll->seq top))
  (def toid-poly-map (reduce (fn [a x]
                               (assoc a (.getAttribute x "Toid") (.getDefaultGeometry x)))
                             {}
                             top-seq))


  (.getAttribute (first ab-seq) "Uprn")

  (get crlookup (.getAttribute (first ab-seq) "Uprn"))

  (.getAttribute (first top-seq) "Toid")
  (.getDefaultGeometry (first top-seq))


  (def c (cql-filter "Toid = 1000002031633908" top))
  (first (feature-coll->seq c))
  (def topogeom (.getDefaultGeometry (first (feature-coll->seq c))))

  (def abgeom (.getDefaultGeometry (first ab-seq)))
  (.within topogeom abgeom)


  (addressbase-within-topo? toid-poly-map crlookup (first ab-seq))

  )
