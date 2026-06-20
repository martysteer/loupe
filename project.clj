(defproject com.loupe/loupe "0.1.0-SNAPSHOT"
  :description "OpenRefine extension for one-click text data quality checks"
  :license {:name "CC-BY"}
  :dependencies [[org.clojure/clojure "1.12.3"]]
  :source-paths ["src"]
  :test-paths ["test"]
  :target-path "target"
  :aot [loupe.checks]
  :jar-name "loupe.jar")
