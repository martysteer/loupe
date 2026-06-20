(ns loupe.checks.casing
  (:require [clojure.string :as str]))

(defn case-pattern
  "Detect case pattern.
  Returns ALL UPPERCASE, all lowercase, Title case, CamelCase, mixedCase, or Mixed/Other."
  [value]
  (if (or (nil? value) (str/blank? (str value)))
    "EMPTY"
    (let [s (str value)]
      (cond
        (= s (str/upper-case s)) "ALL UPPERCASE"
        (= s (str/lower-case s)) "all lowercase"
        (= s (str/capitalize s)) "Title case"
        (re-matches #"[A-Z][a-z]+([A-Z][a-z]+)+" s) "CamelCase"
        (re-find #"[a-z][A-Z]" s) "mixedCase"
        :else "Mixed/Other"))))

(defn case-complexity
  "Percentage upper as string.
  Returns X% upper or no letters."
  [value]
  (if (or (nil? value) (str/blank? (str value)))
    "EMPTY"
    (let [s (str value)
          upper (count (re-seq #"\p{Lu}" s))
          lower (count (re-seq #"\p{Ll}" s))
          total (+ upper lower)]
      (if (zero? total)
        "no letters"
        (str (int (* 100 (/ upper total))) "% upper")))))

(defn word-case-pattern
  "Per-word pattern.
  Returns pattern like Title-lower-UPPER."
  [value]
  (if (or (nil? value) (str/blank? (str value)))
    "EMPTY"
    (let [words (str/split (str value) #"\s+")
          patterns (map (fn [w]
                          (cond
                            (re-matches #"[A-Z]+" w) "UPPER"
                            (re-matches #"[a-z]+" w) "lower"
                            (re-matches #"[A-Z][a-z]*" w) "Title"
                            :else "mixed")) words)]
      (str/join "-" patterns))))
