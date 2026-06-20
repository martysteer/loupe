# OpenRefine Data Quality Facets

*"Because your data deserves better"*

A comprehensive collection of Clojure expressions for OpenRefine custom text facets to analyze linguistic and data quality characteristics.



## How to Use

1. In OpenRefine, click the column dropdown → **Facet** → **Custom text facet**
2. Change the language dropdown to **Clojure**
3. Paste one of the expressions below
4. Click OK



---

## Table of Contents

1. [Diacritics Analysis](#diacritics-analysis)
2. [Script/Writing System Detection](#scriptwriting-system-detection)
3. [Text Directionality](#text-directionality)
4. [Case Patterns](#case-patterns)
5. [Unicode Normalization](#unicode-normalization)
6. [Character Category Analysis](#character-category-analysis)
7. [Whitespace Analysis](#whitespace-analysis)
8. [Punctuation Analysis](#punctuation-analysis)
9. [Numeric Systems](#numeric-systems)
10. [Emoji Detection](#emoji-detection)
11. [Word Pattern Analysis](#word-pattern-analysis)
12. [Encoding Issues](#encoding-issues)
13. [Ligature Detection](#ligature-detection)
14. [Length Metrics](#length-metrics)
15. [Data Quality Facets](#data-quality-facets)
16. [Quick Reference: Unicode Escape Codes](#quick-reference-unicode-escape-codes)



---

## Diacritics Analysis

### Extract unique diacritical marks (combining characters)

```clojure
(let [normalized (java.text.Normalizer/normalize value java.text.Normalizer$Form/NFD)
      marks (re-seq #"\p{M}" normalized)]
  (if marks (apply str (sort (set marks))) "none"))
```

### Extract letters that have diacritics (the full characters)

```clojure
(let [chars (re-seq #"[\u00C0-\u00FF]" value)]
  (if chars (apply str (sort (set chars))) "none"))
```

### Broader: all non-ASCII characters

```clojure
(let [chars (re-seq #"[^\u0000-\u007F]" value)]
  (if chars (apply str (sort (set chars))) "ASCII only"))
```

### Categorize by diacritic type

```clojure
(let [normalized (java.text.Normalizer/normalize value java.text.Normalizer$Form/NFD)
      marks (set (re-seq #"\p{M}" normalized))]
  (cond
    (empty? marks) "no diacritics"
    (some #(re-matches #"[\u0300-\u036f]" %) marks) "combining diacritics"
    :else "other marks"))
```

### Count of diacritics (for numeric facet)

```clojure
(let [normalized (java.text.Normalizer/normalize value java.text.Normalizer$Form/NFD)]
  (count (re-seq #"\p{M}" normalized)))
```



---

## Script/Writing System Detection

### Detect primary script

```clojure
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
    :else "Other/Unknown"))
```

### Multi-script detection (mixed scripts)

```clojure
(let [s (str value)
      scripts (filter identity
                [(when (re-find #"\p{IsLatin}" s) "Latin")
                 (when (re-find #"\p{IsCyrillic}" s) "Cyrillic")
                 (when (re-find #"\p{IsGreek}" s) "Greek")
                 (when (re-find #"\p{IsArabic}" s) "Arabic")
                 (when (re-find #"\p{IsHan}" s) "CJK")])]
  (if (> (count scripts) 1)
    (str "Mixed: " (clojure.string/join "+" scripts))
    (first scripts)))
```

---

## Text Directionality

```clojure
(let [s (str value)]
  (cond
    (and (re-find #"[\u0600-\u06FF\u0590-\u05FF]" s)
         (re-find #"[a-zA-Z]" s)) "Bidirectional (mixed)"
    (re-find #"[\u0600-\u06FF]" s) "RTL (Arabic)"
    (re-find #"[\u0590-\u05FF]" s) "RTL (Hebrew)"
    :else "LTR"))
```



---

## Case Patterns

### Detect case pattern

```clojure
(let [s (str value)]
  (cond
    (= s (clojure.string/upper-case s)) "ALL UPPERCASE"
    (= s (clojure.string/lower-case s)) "all lowercase"
    (= s (clojure.string/capitalize s)) "Title case"
    (re-matches #"[A-Z][a-z]+([A-Z][a-z]+)+" s) "CamelCase"
    (re-find #"[a-z][A-Z]" s) "mixedCase"
    :else "Mixed/Other"))
```

### Case complexity score

```clojure
(let [upper (count (re-seq #"\p{Lu}" value))
      lower (count (re-seq #"\p{Ll}" value))
      total (+ upper lower)]
  (if (zero? total) "no letters"
    (str (int (* 100 (/ upper total))) "% upper")))
```

### Capitalization pattern per word

```clojure
(let [words (clojure.string/split value #"\s+")
      patterns (map (fn [w]
                      (cond
                        (re-matches #"[A-Z]+" w) "UPPER"
                        (re-matches #"[a-z]+" w) "lower"
                        (re-matches #"[A-Z][a-z]*" w) "Title"
                        :else "mixed")) words)]
  (clojure.string/join "-" patterns))
```



---

## Unicode Normalization

### Check normalization status

```clojure
(let [nfc (java.text.Normalizer/normalize value java.text.Normalizer$Form/NFC)
      nfd (java.text.Normalizer/normalize value java.text.Normalizer$Form/NFD)]
  (cond
    (and (= value nfc) (= value nfd)) "ASCII/Simple"
    (= value nfc) "NFC normalized"
    (= value nfd) "NFD normalized"
    :else "Non-normalized"))
```



---

## Character Category Analysis

### Character category breakdown

```clojure
(let [cats (group-by #(Character/getType (int %)) value)]
  (->> cats
       (map (fn [[k v]] 
              (str (case k
                     1 "Lu" 2 "Ll" 3 "Lt" 4 "Lm" 5 "Lo"
                     6 "Mn" 7 "Me" 8 "Mc"
                     9 "Nd" 10 "Nl" 11 "No"
                     12 "Zs" 13 "Zl" 14 "Zp"
                     20 "Pd" 21 "Ps" 22 "Pe" 23 "Pc" 24 "Po" 29 "Pi" 30 "Pf"
                     k) ":" (count v))))
       (clojure.string/join " ")))
```

### Dominant character type

```clojure
(let [letters (count (re-seq #"\p{L}" value))
      digits (count (re-seq #"\p{N}" value))
      punct (count (re-seq #"\p{P}" value))
      spaces (count (re-seq #"\p{Z}" value))
      symbols (count (re-seq #"\p{S}" value))]
  (cond
    (> letters (max digits punct symbols)) "Letter-dominant"
    (> digits (max letters punct symbols)) "Digit-dominant"
    (> punct (max letters digits symbols)) "Punctuation-dominant"
    (> symbols (max letters digits punct)) "Symbol-dominant"
    :else "Mixed"))
```



---

## Whitespace Analysis

### Whitespace types

```clojure
(let [ws (re-seq #"\s" value)
      types (set (map #(format "U+%04X" (int (first %))) ws))]
  (cond
    (empty? types) "no whitespace"
    (= types #{"U+0020"}) "normal spaces only"
    (contains? types "U+00A0") "has non-breaking space"
    (contains? types "U+2009") "has thin space"
    (contains? types "U+200B") "has zero-width space"
    (some #(re-matches #"U\+200[0-9A-F]" %) types) "has special Unicode spaces"
    :else (str "whitespace: " (clojure.string/join "," types))))
```



---

## Punctuation Analysis

### Extract unique punctuation

```clojure
(let [puncts (re-seq #"\p{P}" value)]
  (if puncts
    (apply str (sort (set puncts)))
    "no punctuation"))
```

### Quote style detection

```clojure
(cond
  (re-find #"[\u201C\u201D\u201E\u00AB\u00BB\u2039\u203A]" value) "Smart/typographic quotes"
  (re-find #"['\"]" value) "Straight quotes"
  (re-find #"[\u300C\u300D\u300E\u300F]" value) "CJK quotes"
  :else "No quotes")
```



---

## Numeric Systems

```clojure
(let [s (str value)]
  (cond
    (re-find #"[\u0660-\u0669]" s) "Arabic-Indic numerals"
    (re-find #"[\u06F0-\u06F9]" s) "Extended Arabic-Indic"
    (re-find #"[\u0966-\u096F]" s) "Devanagari numerals"
    (re-find #"[\u0E50-\u0E59]" s) "Thai numerals"
    (re-find #"[\u3007\u4E00\u4E8C\u4E09\u56DB\u4E94\u516D\u4E03\u516B\u4E5D\u5341]" s) "CJK numerals"
    (re-find #"[\u2160-\u216B]" s) "Roman numerals"
    (re-find #"[0-9]" s) "Western/ASCII numerals"
    :else "No numerals"))
```



---

## Emoji Detection

```clojure
(let [emoji (re-seq #"[\u2600-\u26FF\u2700-\u27BF]" value)]
  (if emoji
    (str (count emoji) " emoji: " (apply str (take 5 emoji)))
    "no emoji"))
```



---

## Word Pattern Analysis

### Word statistics

```clojure
(let [words (clojure.string/split value #"\s+")
      lengths (map count words)]
  (str "words:" (count words) 
       " avg:" (if (seq lengths) 
                 (format "%.1f" (double (/ (reduce + lengths) (count lengths))))
                 "0")))
```



---

## Encoding Issues

### Potential encoding issue detection

```clojure
(cond
  (re-find #"\uFFFD" value) "Has replacement char (encoding error)"
  (re-find #"[\u0000-\u0008\u000B\u000C\u000E-\u001F]" value) "Has control characters"
  (re-find #"\uFEFF" value) "Has BOM"
  (re-find #"[\u200B-\u200F\u2028-\u202F]" value) "Has invisible formatters"
  (re-find #"\u00C3[\u0080-\u00BF]" value) "Possible UTF-8 as Latin-1 (mojibake)"
  :else "OK")
```



---

## Ligature Detection

```clojure
(let [ligatures (re-seq #"[\uFB00-\uFB06\u0133\u0152\u0153\u00C6\u00E6]" value)]
  (if ligatures
    (str "ligatures: " (apply str (set ligatures)))
    "no ligatures"))
```



---

## Length Metrics

### Character count

```clojure
(count value)
```

### Byte count (UTF-8)

```clojure
(count (.getBytes value "UTF-8"))
```

### String "Shape" (abstract pattern)

```clojure
(-> value
    (clojure.string/replace #"[A-Z]" "A")
    (clojure.string/replace #"[a-z]" "a")
    (clojure.string/replace #"[0-9]" "0")
    (clojure.string/replace #"\s+" "_"))
```



---

## Data Quality Facets

### The Ultimate All-in-One Data Quality Facet

```clojure
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
        (when (and dominated-by-letters (= s (clojure.string/upper-case s)) (> (count s) 3)) "ALL_CAPS")
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
    (clojure.string/join " | " issues)))
```

### Tiered Quality Score (Numeric Facet)

```clojure
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
  
  (max 0 score))
```

### Quality Grade (A-F)

```clojure
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
    :else "A - Clean"))
```

### The "What Fresh Hell Is This?" Detector

```clojure
(let [s (str value)
      reactions (filter identity
        [(when (clojure.string/blank? s) "GHOST DATA")
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
    "Looks fine"))
```

### Reconciliation-Ready Check

*Specifically for pre-reconciliation data prep:*

```clojure
(let [s (str value)
      s-trimmed (clojure.string/trim s)
      s-normalized (java.text.Normalizer/normalize s-trimmed java.text.Normalizer$Form/NFC)
      s-cleaned (-> s-normalized
                    (clojure.string/replace #"[\u00A0\u2000-\u200A]" " ")
                    (clojure.string/replace #"  +" " "))
      
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
    (str "WARNING: " (clojure.string/join ", " issues))))
```

### The Full Autopsy Report

```clojure
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
       (when (re-find #"[\u0080-\uFFFF]" s) " [UTF]")))
```



---

## Quick Reference: Unicode Escape Codes

| Character | Escape | Description |
|-----------|--------|-------------|
| ` ` | `\u00A0` | Non-breaking space |
| `–` | `\u2013` | En dash |
| `—` | `\u2014` | Em dash |
| `"` | `\u201C` | Left double quote |
| `"` | `\u201D` | Right double quote |
| `'` | `\u2018` | Left single quote |
| `'` | `\u2019` | Right single quote |
| `�` | `\uFFFD` | Replacement character |
| ` ` | `\u200B` | Zero-width space |
| `Ã` | `\u00C3` | Common mojibake indicator |



---

*claudie.com™ - "We judge your data so you don't have to"*

*Last updated: January 2025*