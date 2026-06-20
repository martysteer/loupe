(ns loupe.checks.content
  (:require [clojure.string :as str]))

(defn numeric-system
  "Detect numeral system.
  Returns system name or No numerals."
  [value]
  (if (or (nil? value) (str/blank? (str value)))
    "EMPTY"
    (let [s (str value)]
      (cond
        (re-find #"[\u0660-\u0669]" s) "Arabic-Indic numerals"
        (re-find #"[\u06F0-\u06F9]" s) "Extended Arabic-Indic"
        (re-find #"[\u0966-\u096F]" s) "Devanagari numerals"
        (re-find #"[\u0E50-\u0E59]" s) "Thai numerals"
        (re-find #"[\u3007\u4E00\u4E8C\u4E09\u56DB\u4E94\u516D\u4E03\u516B\u4E5D\u5341]" s) "CJK numerals"
        (re-find #"[\u2160-\u216B]" s) "Roman numerals"
        (re-find #"[0-9]" s) "Western/ASCII numerals"
        :else "No numerals"))))

(defn emoji-summary
  "Count + first 5 emoji, or no emoji."
  [value]
  (if (or (nil? value) (str/blank? (str value)))
    "EMPTY"
    (let [emoji (re-seq #"[\u2600-\u26FF\u2700-\u27BF]" (str value))]
      (if emoji
        (str (count emoji) " emoji: " (apply str (take 5 emoji)))
        "no emoji"))))

(defn word-stats
  "Word count and average length.
  Returns words:N avg:X.X"
  [value]
  (if (or (nil? value) (str/blank? (str value)))
    "EMPTY"
    (let [words (str/split (str value) #"\s+")
          lengths (map count words)]
      (str "words:" (count words)
           " avg:" (if (seq lengths)
                     (format "%.1f" (double (/ (reduce + lengths) (count lengths))))
                     "0")))))

(defn ligatures
  "Detect ligature characters.
  Returns ligatures: ... or no ligatures."
  [value]
  (if (or (nil? value) (str/blank? (str value)))
    "EMPTY"
    (let [ligs (re-seq #"[\uFB00-\uFB06\u0133\u0152\u0153\u00C6\u00E6]" (str value))]
      (if ligs
        (str "ligatures: " (apply str (set ligs)))
        "no ligatures"))))

(defn char-count
  "Character count as string."
  [value]
  (if (nil? value)
    "0"
    (str (count (str value)))))

(defn byte-count
  "UTF-8 byte count as string."
  [value]
  (if (nil? value)
    "0"
    (str (count (.getBytes (str value) "UTF-8")))))

(defn string-shape
  "Abstract pattern: A for uppercase, a for lowercase, 0 for digit, _ for whitespace."
  [value]
  (if (or (nil? value) (str/blank? (str value)))
    "EMPTY"
    (-> (str value)
        (str/replace #"[A-Z]" "A")
        (str/replace #"[a-z]" "a")
        (str/replace #"[0-9]" "0")
        (str/replace #"\s+" "_"))))

(defn char-category-breakdown
  "Unicode category counts.
  Returns format like Lu:3 Ll:12 Zs:2"
  [value]
  (if (or (nil? value) (str/blank? (str value)))
    "EMPTY"
    (let [s (str value)
          cats (group-by #(Character/getType (int %)) s)]
      (->> cats
           (map (fn [[k v]]
                  (str (case k
                         1 "Lu" 2 "Ll" 3 "Lt" 4 "Lm" 5 "Lo"
                         6 "Mn" 7 "Me" 8 "Mc"
                         9 "Nd" 10 "Nl" 11 "No"
                         12 "Zs" 13 "Zl" 14 "Zp"
                         20 "Pd" 21 "Ps" 22 "Pe" 23 "Pc" 24 "Po" 29 "Pi" 30 "Pf"
                         k) ":" (count v))))
           (str/join " ")))))

(defn dominant-char-type
  "Dominant character type.
  Returns Letter-dominant, Digit-dominant, etc."
  [value]
  (if (or (nil? value) (str/blank? (str value)))
    "EMPTY"
    (let [s (str value)
          letters (count (re-seq #"\p{L}" s))
          digits (count (re-seq #"\p{N}" s))
          punct (count (re-seq #"\p{P}" s))
          spaces (count (re-seq #"\p{Z}" s))
          symbols (count (re-seq #"\p{S}" s))]
      (cond
        (> letters (max digits punct symbols)) "Letter-dominant"
        (> digits (max letters punct symbols)) "Digit-dominant"
        (> punct (max letters digits symbols)) "Punctuation-dominant"
        (> symbols (max letters digits punct)) "Symbol-dominant"
        :else "Mixed"))))
