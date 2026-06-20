(ns loupe.checks.script
  (:require [clojure.string :as str]))

(defn primary-script
  "Detect dominant script.
  Returns script name or Other/Unknown."
  [value]
  (if (or (nil? value) (str/blank? (str value)))
    "EMPTY"
    (let [s (str value)]
      (cond
        (re-find #"\p{IsHan}" s) "Chinese/CJK"
        (re-find #"\p{IsHiragana}" s) "Japanese Hiragana"
        (re-find #"\p{IsKatakana}" s) "Japanese Katakana"
        (re-find #"\p{IsHangul}" s) "Korean"
        (re-find #"\p{IsCyrillic}" s) "Cyrillic"
        (re-find #"\p{IsGreek}" s) "Greek"
        (re-find #"\p{IsArabic}" s) "Arabic"
        (re-find #"\p{IsHebrew}" s) "Hebrew"
        (re-find #"\p{IsThai}" s) "Thai"
        (re-find #"\p{IsDevanagari}" s) "Devanagari"
        (re-find #"\p{IsGeorgian}" s) "Georgian"
        (re-find #"\p{IsArmenian}" s) "Armenian"
        (re-find #"\p{IsEthiopic}" s) "Ethiopic"
        (re-find #"[a-zA-Z]" s) "Latin"
        :else "Other/Unknown"))))

(defn mixed-scripts
  "Detect multi-script content.
  Returns Mixed: Script1+Script2 or single script name."
  [value]
  (if (or (nil? value) (str/blank? (str value)))
    "EMPTY"
    (let [s (str value)
          scripts (filter identity
                    [(when (re-find #"\p{IsLatin}" s) "Latin")
                     (when (re-find #"\p{IsCyrillic}" s) "Cyrillic")
                     (when (re-find #"\p{IsGreek}" s) "Greek")
                     (when (re-find #"\p{IsArabic}" s) "Arabic")
                     (when (re-find #"\p{IsHan}" s) "CJK")])]
      (if (> (count scripts) 1)
        (str "Mixed: " (str/join "+" scripts))
        (first scripts)))))

(defn directionality
  "Detect text directionality.
  Returns LTR, RTL (Arabic), RTL (Hebrew), or Bidirectional (mixed)."
  [value]
  (if (or (nil? value) (str/blank? (str value)))
    "EMPTY"
    (let [s (str value)]
      (cond
        (and (re-find #"[\u0600-\u06FF\u0590-\u05FF]" s)
             (re-find #"[a-zA-Z]" s)) "Bidirectional (mixed)"
        (re-find #"[\u0600-\u06FF]" s) "RTL (Arabic)"
        (re-find #"[\u0590-\u05FF]" s) "RTL (Hebrew)"
        :else "LTR"))))
