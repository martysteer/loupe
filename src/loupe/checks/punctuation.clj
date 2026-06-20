(ns loupe.checks.punctuation
  (:require [clojure.string :as str]))

(defn unique-punctuation
  "Sorted unique punctuation chars or no punctuation."
  [value]
  (if (or (nil? value) (str/blank? (str value)))
    "EMPTY"
    (let [puncts (re-seq #"\p{P}" (str value))]
      (if puncts
        (apply str (sort (set puncts)))
        "no punctuation"))))

(defn quote-style
  "Detect quote style.
  Returns Smart/typographic quotes, Straight quotes, CJK quotes, or No quotes."
  [value]
  (if (or (nil? value) (str/blank? (str value)))
    "EMPTY"
    (let [s (str value)]
      (cond
        (re-find #"[\u201C\u201D\u201E\u00AB\u00BB\u2039\u203A]" s) "Smart/typographic quotes"
        (re-find #"['\"]" s) "Straight quotes"
        (re-find #"[\u300C\u300D\u300E\u300F]" s) "CJK quotes"
        :else "No quotes"))))
