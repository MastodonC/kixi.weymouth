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

(defn dbase-field-names [db]
  )


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
      .getFeatures
      ;;.features
      ))

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






(comment

  ;; Addresspoint
  (def abfilepath "data/bury_building_esri/AddressPoint.shp")
  (def ab (features-coll abfilepath))
  (def ab-seq (feature-coll->seq ab))

  ;; Cross ref file
  (def crf (dbf-to-vec "data/bury_building_esri/Bury_ADPRAXR.dbf"))
  (def cr (into [] uprn-toid-xref-xf crf))

  ;; Topography (shp)
  (def topog "data/bury_topography_esri/Topography.shp")
  (def top (features-coll topog))
  (def top-seq (feature-coll->seq top))

  ;; create uprn/toid lookup
  (def crlookup (reduce (fn [acc x]
                          (assoc acc (:uprn x) (:toid x))) {} cr))

  (def ab-seq (feature-coll->seq ab))

  (.getAttribute (first ab-seq) "Uprn")

  (get crlookup (.getAttribute (first ab-seq) "Uprn"))

  (.getAttribute (first top-seq) "Toid")
  (.getDefaultGeometry (first top-seq))


  (def c (cql-filter "Toid = 1000002031633908" top))
  (first (feature-coll->seq c))
  (def topogeom (.getDefaultGeometry (first (feature-coll->seq c))))

  (def abgeom (.getDefaultGeometry (first ab-seq)))
  (.within topogeom abgeom)



  )
