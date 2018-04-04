(defproject kixi.weymouth "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [environ "1.1.0"]
                 [org.clojure/core.async "0.4.474"]
                 [org.clojure/data.csv "0.1.4"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/spec.alpha "0.1.143"]
                 [org.clojure/test.check "0.10.0-alpha2"]
                 [clj-http "3.8.0"]
                 [cheshire "5.8.0"] ;; for clj-http form-params deps
                 [clojure.java-time "0.3.1"]]
  :main ^:skip-aot kixi.weymouth
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
