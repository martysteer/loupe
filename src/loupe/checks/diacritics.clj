(ns loupe.checks.diacritics
  (:require [clojure.string :as str]))

(defn unique-marks
  "Extract unique combining marks (NFD decomposed).
  Returns sorted marks or none."
  [value]
  (if (or (nil? value) (str/blank? (str value)))
    "EMPTY"
    (let [normalized (java.text.Normalizer/normalize
                       (str value)
                       java.text.Normalizer$Form/NFD)
          marks (re-seq #"\p{M}" normalized)]
      (if marks
        (apply str (sort (set marks)))
        "none"))))

(defn diacritic-letters
  "Extract letters with diacritics (U+00C0-U+00FF range).
  Returns sorted or none."
  [value]
  (if (or (nil? value) (str/blank? (str value)))
    "EMPTY"
    (let [chars (re-seq #"[\u00C0-\u00FF]" (str value))]
      (if chars
        (apply str (sort (set chars)))
        "none"))))

(defn non-ascii-chars
  "All non-ASCII characters.
  Returns sorted unique or ASCII only."
  [value]
  (if (or (nil? value) (str/blank? (str value)))
    "EMPTY"
    (let [chars (re-seq #"[^\u0000-\u007F]" (str value))]
      (if chars
        (apply str (sort (set chars)))
        "ASCII only"))))

(defn diacritic-type
  "Categorize diacritic type.
  Returns no diacritics, combining diacritics, or other marks."
  [value]
  (if (or (nil? value) (str/blank? (str value)))
    "EMPTY"
    (let [normalized (java.text.Normalizer/normalize
                       (str value)
                       java.text.Normalizer$Form/NFD)
          marks (set (re-seq #"\p{M}" normalized))]
      (cond
        (empty? marks) "no diacritics"
        (some #(re-matches #"[\u0300-\u036f]" %) marks) "combining diacritics"
        :else "other marks"))))

(defn diacritic-count
  "Count of combining marks as string."
  [value]
  (if (or (nil? value) (str/blank? (str value)))
    "0"
    (let [normalized (java.text.Normalizer/normalize
                       (str value)
                       java.text.Normalizer$Form/NFD)]
      (str (count (re-seq #"\p{M}" normalized))))))
