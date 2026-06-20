(ns loupe.checks.quality
  (:require [clojure.string :as str]
            [loupe.checks.encoding :as encoding]
            [loupe.checks.whitespace :as whitespace]
            [loupe.checks.normalization :as normalization]))

(defn all-issues
  "Runs 27+ checks, returns pipe-separated labels or CLEAN.
  Composes from individual check functions."
  [value]
  (if (or (nil? value) (str/blank? (str value)))
    "EMPTY"
    (let [s (str value)
          dominated-by-letters (> (count (re-seq #"\p{L}" s)) (/ (count s) 2))
          nfc (java.text.Normalizer/normalize s java.text.Normalizer$Form/NFC)

          issues (filter identity [
            (when (re-find #"\uFFFD" s) "REPLACEMENT_CHAR")
            (when (re-find #"\u00C3[\u0080-\u00BF]" s) "MOJIBAKE_DETECTED")
            (when (re-find #"[\u0000-\u0008\u000B\u000C\u000E-\u001F]" s) "CONTROL_CHARS")
            (when (re-find #"\uFEFF" s) "BOM_PRESENT")
            (when (re-find #"\u200B" s) "ZERO_WIDTH_SPACE")
            (when (re-find #"[\u200C\u200D]" s) "ZERO_WIDTH_JOINER")
            (when (re-find #"\u00A0" s) "NON_BREAKING_SPACE")
            (when (re-find #"[\u2000-\u200A]" s) "FANCY_WHITESPACE")
            (when (re-find #"  +" s) "MULTIPLE_SPACES")
            (when (re-find #"^\s|\s$" s) "LEADING_TRAILING_SPACE")
            (when (re-find #"\t" s) "HAS_TABS")
            (when (not= s nfc) "NOT_NFC_NORMALIZED")
            (when (and (re-find #"['\"]" s) (re-find #"[\u201C\u201D\u2018\u2019]" s)) "MIXED_QUOTE_STYLES")
            (when (re-find #"''|\"\"" s) "DOUBLED_QUOTES")
            (when (and (re-find #"-" s) (re-find #"[\u2013\u2014]" s)) "MIXED_DASH_STYLES")
            (when (re-find #"--+" s) "MULTIPLE_HYPHENS")
            (when (and dominated-by-letters (= s (str/upper-case s)) (> (count s) 3)) "ALL_CAPS")
            (when (re-find #"[a-z][A-Z]" s) "MID_WORD_CAPS")
            (when (and (re-find #"[a-zA-Z]" s) (re-find #"\p{IsCyrillic}" s)) "LATIN_CYRILLIC_MIX")
            (when (and (re-find #"[a-zA-Z]" s) (re-find #"\p{IsGreek}" s)) "LATIN_GREEK_MIX")
            (when (re-find #"[<>]" s) "HTML_ANGLE_BRACKETS")
            (when (re-find #"&[a-z]+;" s) "HTML_ENTITIES")
            (when (re-find #"\\[nrt]" s) "ESCAPED_CHARS_LITERAL")
            (when (re-find #"(?i)null|undefined|NaN|#N/A" s) "SPREADSHEET_ERROR_VALUE")
            (when (re-find #"^\s*$" s) "WHITESPACE_ONLY")
            (when (re-find #"[!?]{2,}" s) "EXCESSIVE_PUNCTUATION")
            (when (re-find #"\.\.\.\." s) "EXCESSIVE_ELLIPSIS")
            (when (re-find #"[,;:]{2,}" s) "DOUBLED_PUNCTUATION")
            (when (re-find #"[\u200E\u200F\u202A-\u202E]" s) "BIDI_CONTROL_CHARS")])]

      (if (empty? issues)
        "CLEAN"
        (str/join " | " issues)))))

(defn quality-score
  "Numeric 0-100 with weighted penalties. Returned as string."
  [value]
  (if (or (nil? value) (str/blank? (str value)))
    "0"
    (let [s (str value)
          nfc (java.text.Normalizer/normalize s java.text.Normalizer$Form/NFC)

          critical (count (filter identity [
            (re-find #"\uFFFD" s)
            (re-find #"\u00C3[\u0080-\u00BF]" s)
            (re-find #"[\u0000-\u0008\u000B\u000C\u000E-\u001F]" s)
            (re-find #"(?i)^(null|undefined|NaN|#N/A|#REF!|#VALUE!)$" s)
            (re-find #"^\s*$" s)]))

          major (count (filter identity [
            (re-find #"[\u200B-\u200F]" s)
            (not= s nfc)
            (and (re-find #"[a-zA-Z]" s) (re-find #"\p{IsCyrillic}" s))
            (re-find #"&[a-z]+;" s)
            (re-find #"[\u202A-\u202E]" s)]))

          minor (count (filter identity [
            (re-find #"\u00A0" s)
            (re-find #"  +" s)
            (re-find #"^\s|\s$" s)
            (and (re-find #"['\"]" s) (re-find #"[\u201C\u201D\u2018\u2019]" s))
            (and (re-find #"-" s) (re-find #"[\u2013\u2014]" s))
            (re-find #"[!?]{2,}" s)]))

          score (- 100 (* 10 critical) (* 5 major) (* 2 minor))]

      (str (max 0 score)))))

(defn quality-grade
  "A-F letter grade."
  [value]
  (if (or (nil? value) (str/blank? (str value)))
    "F - Critical Issues"
    (let [s (str value)
          nfc (java.text.Normalizer/normalize s java.text.Normalizer$Form/NFC)

          critical (some identity [
            (re-find #"\uFFFD" s)
            (re-find #"\u00C3[\u0080-\u00BF]" s)
            (re-find #"[\u0000-\u0008\u000B\u000C\u000E-\u001F]" s)
            (re-find #"(?i)^(null|undefined|NaN)$" s)])

          major (some identity [
            (re-find #"[\u200B-\u200F\u202A-\u202E]" s)
            (and (re-find #"[a-zA-Z]" s) (re-find #"\p{IsCyrillic}" s))
            (not= s nfc)])

          minor (some identity [
            (re-find #"\u00A0" s)
            (re-find #"  +" s)
            (re-find #"^\s|\s$" s)
            (and (re-find #"['\"]" s) (re-find #"[\u201C\u201D\u2018\u2019]" s))])

          cosmetic (some identity [
            (re-find #"[!?]{2,}" s)
            (re-find #"--+" s)])]

      (cond
        critical "F - Critical Issues"
        major "D - Major Issues"
        minor "C - Minor Issues"
        cosmetic "B - Cosmetic Issues"
        :else "A - Clean"))))

(defn fresh-hell
  "Humorous anomaly detector."
  [value]
  (if (or (nil? value) (str/blank? (str value)))
    "GHOST DATA"
    (let [s (str value)
          reactions (filter identity
            [(when (str/blank? s) "GHOST DATA")
             (when (re-find #"\uFFFD{3,}" s) "ENCODING MASSACRE")
             (when (re-find #"\u00C3.\u00C3.\u00C3." s) "MOJIBAKE EXPLOSION")
             (when (= s (apply str (repeat (count s) (first s)))) "GROUNDHOG DAY")
             (when (re-find #"[\u0300-\u036f]{5,}" s) "ZALGO TEXT")
             (when (> (count (re-seq #"[\u200B-\u200F]" s)) 5) "INVISIBLE INK")
             (when (and (> (count s) 50) (not (re-find #"\s" s))) "WALL OF TEXT")
             (when (re-find #"(.)\1{9,}" s) "STUTTER MODE")
             (when (> (count (re-seq #"[!?]" s)) 10) "CALM DOWN")
             (when (re-find #"(?i)(test|asdf|qwerty|xxx)" s) "TEST DATA LEAK")])]
      (if (seq reactions)
        (first reactions)
        "Looks fine"))))

(defn reconciliation-ready
  "Pre-reconciliation validation."
  [value]
  (if (or (nil? value) (str/blank? (str value)))
    "WARNING: empty"
    (let [s (str value)
          s-trimmed (str/trim s)
          s-normalized (java.text.Normalizer/normalize s-trimmed java.text.Normalizer$Form/NFC)
          s-cleaned (-> s-normalized
                        (str/replace #"[\u00A0\u2000-\u200A]" " ")
                        (str/replace #"  +" " "))

          issues (filter identity [
            (when (not= s s-trimmed) "needs-trim")
            (when (not= s-trimmed s-normalized) "needs-normalization")
            (when (not= s-normalized s-cleaned) "needs-whitespace-fix")
            (when (re-find #"[\u200B-\u200F]" s) "has-invisible-chars")
            (when (re-find #"(?i)\(.*\)$" s) "has-parenthetical")
            (when (re-find #"," s) "has-comma-maybe-multiple")
            (when (re-find #"^\d" s) "starts-with-digit")
            (when (re-find #"[<>&]" s) "has-special-chars")
            (when (< (count s-trimmed) 2) "suspiciously-short")
            (when (> (count s-trimmed) 200) "suspiciously-long")])]

      (if (empty? issues)
        "RECONCILIATION READY"
        (str "WARNING: " (str/join ", " issues))))))

(defn autopsy-report
  "Full character breakdown."
  [value]
  (if (nil? value)
    "len:0 bytes:0"
    (let [s (str value)
          len (count s)
          bytes (count (.getBytes s "UTF-8"))
          nfc (java.text.Normalizer/normalize s java.text.Normalizer$Form/NFC)
          letters (count (re-seq #"\p{L}" s))
          digits (count (re-seq #"\p{N}" s))
          spaces (count (re-seq #"\s" s))
          punct (count (re-seq #"\p{P}" s))
          other (- len letters digits spaces punct)]
      (str "len:" len
           " bytes:" bytes
           (when (not= len bytes) "*")
           " L:" letters
           " N:" digits
           " S:" spaces
           " P:" punct
           (when (> other 0) (str " ?:" other))
           (when (not= s nfc) " [!NFC]")
           (when (re-find #"[\u0080-\uFFFF]" s) " [UTF]")))))
