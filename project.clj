(defproject o-tariff-comparison "0.1.0-SNAPSHOT"
  :description "O Tariff Comparison"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.json "0.2.6"]]
  :main ^:skip-aot o-tariff-comparison.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[org.clojure/test.check "0.9.0"]]}})
