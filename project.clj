(defproject com.loupe/loupe "0.1.0"
  :description "OpenRefine extension for one-click text data quality checks"
  :license {:name "CC-BY"}
  :dependencies [[org.clojure/clojure "1.12.3"]]
  :source-paths ["src"]
  :test-paths ["test"]
  :target-path "target"
  :aot [loupe.checks.encoding
        loupe.checks.whitespace
        loupe.checks.normalization
        loupe.checks.script
        loupe.checks.diacritics
        loupe.checks.casing
        loupe.checks.punctuation
        loupe.checks.content
        loupe.checks.quality
        loupe.checks.glossary]
  :jar-name "loupe.jar")
