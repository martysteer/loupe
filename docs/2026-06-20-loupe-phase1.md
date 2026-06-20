# Loupe Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Port all 30 functions from the v2 facet library into tested Clojure namespaces and wire them into OpenRefine submenu groups.

**Architecture:** 9 focused namespaces under `loupe.checks.*`, each pure String → String functions. Composite `quality` namespace calls sibling namespaces. All AOT-compiled and loaded via URLClassLoader in controller.js. Menu organized into 3 submenu groups.

**Tech Stack:** Clojure 1.12.3, Leiningen, OpenRefine 3.10.1, Rhino JavaScript

**Source material:** `OpenRefine Data Quality Facets.md` in project root

---

### Task 1: Migrate encoding.clj from Phase 0

**Files:**
- Delete: `src/loupe/checks.clj`
- Delete: `test/loupe/checks_test.clj`
- Create: `src/loupe/checks/encoding.clj`
- Create: `test/loupe/checks/encoding_test.clj`

- [ ] **Step 1: Create directory structure**

```bash
mkdir -p src/loupe/checks test/loupe/checks
```

- [ ] **Step 2: Move encoding-issues to new namespace**

Create `src/loupe/checks/encoding.clj`:

```clojure
(ns loupe.checks.encoding
  (:require [clojure.string :as str]))

(defn encoding-issues
  "Detect encoding issues in a string value.
  Returns pipe-separated issue labels or \"CLEAN\".
  Returns \"EMPTY\" for nil/blank input."
  [value]
  (if (or (nil? value) (str/blank? (str value)))
    "EMPTY"
    (let [s (str value)
          issues
          (filterv
           some?
           [(when (re-find #"\uFFFD" s)
              "REPLACEMENT_CHAR")

            (when (re-find #"[\u0000-\u0008\u000B\u000C\u000E-\u001F\u007F-\u009F]" s)
              "CONTROL_CHAR")

            (when (re-find #"\uFEFF" s)
              "BOM")

            (when (re-find #"[\u200B-\u200F\u2028-\u202E\u2060-\u2069]" s)
              "INVISIBLE_FORMATTER")

            (when (re-find #"\u00C3[\u0080-\u00BF]" s)
              "MOJIBAKE")])]
      (if (empty? issues)
        "CLEAN"
        (str/join "|" issues)))))
```

- [ ] **Step 3: Move tests to new namespace**

Create `test/loupe/checks/encoding_test.clj` (copy content from old `checks_test.clj`, change namespace):

```clojure
(ns loupe.checks.encoding-test
  (:require [clojure.test :refer [deftest is testing]]
            [loupe.checks.encoding :refer [encoding-issues]]))

;; [paste all tests from old checks_test.clj here, unchanged]
```

- [ ] **Step 4: Delete old files**

```bash
rm src/loupe/checks.clj test/loupe/checks_test.clj
```

- [ ] **Step 5: Run tests**

```bash
make test
```

Expected: all encoding tests pass.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "refactor: move encoding-issues to loupe.checks.encoding namespace"
```

---

### Task 2: Implement whitespace.clj

**Files:**
- Create: `src/loupe/checks/whitespace.clj`
- Create: `test/loupe/checks/whitespace_test.clj`

- [ ] **Step 1: Write failing test**

Create `test/loupe/checks/whitespace_test.clj`:

```clojure
(ns loupe.checks.whitespace-test
  (:require [clojure.test :refer [deftest is testing]]
            [loupe.checks.whitespace :refer [whitespace-types]]))

(deftest whitespace-types-test
  (testing "nil and blank"
    (is (= "EMPTY" (whitespace-types nil)))
    (is (= "EMPTY" (whitespace-types ""))))

  (testing "no whitespace"
    (is (= "no whitespace" (whitespace-types "HelloWorld"))))

  (testing "normal spaces only"
    (is (= "normal spaces only" (whitespace-types "Hello world")))
    (is (= "normal spaces only" (whitespace-types "a b c"))))

  (testing "non-breaking space"
    (is (= "has non-breaking space" (whitespace-types "Hello\u00A0world"))))

  (testing "thin space"
    (is (= "has thin space" (whitespace-types "Hello\u2009world"))))

  (testing "zero-width space"
    (is (= "has zero-width space" (whitespace-types "Hello\u200Bworld"))))

  (testing "special Unicode spaces"
    (is (= "has special Unicode spaces" (whitespace-types "Hello\u2002world")))
    (is (= "has special Unicode spaces" (whitespace-types "a\u2003b")))))
```

- [ ] **Step 2: Run test — verify fail**

```bash
lein test loupe.checks.whitespace-test
```

Expected: FAIL with "could not locate loupe/checks/whitespace"

- [ ] **Step 3: Implement whitespace-types**

Create `src/loupe/checks/whitespace.clj`:

```clojure
(ns loupe.checks.whitespace
  (:require [clojure.string :as str]))

(defn whitespace-types
  "Categorize whitespace characters present.
  Returns descriptive string."
  [value]
  (if (or (nil? value) (str/blank? (str value)))
    "EMPTY"
    (let [s (str value)
          ws (re-seq #"\s" s)
          types (set (map #(format "U+%04X" (int (first %))) ws))]
      (cond
        (empty? types) "no whitespace"
        (= types #{"U+0020"}) "normal spaces only"
        (contains? types "U+00A0") "has non-breaking space"
        (contains? types "U+2009") "has thin space"
        (contains? types "U+200B") "has zero-width space"
        (some #(re-matches #"U\+200[0-9A-F]" %) types) "has special Unicode spaces"
        :else (str "whitespace: " (clojure.string/join "," types))))))
```

- [ ] **Step 4: Run test — verify pass**

```bash
lein test loupe.checks.whitespace-test
```

Expected: all tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/loupe/checks/whitespace.clj test/loupe/checks/whitespace_test.clj
git commit -m "feat: add whitespace-types check"
```

---

### Task 3: Implement normalization.clj

**Files:**
- Create: `src/loupe/checks/normalization.clj`
- Create: `test/loupe/checks/normalization_test.clj`

- [ ] **Step 1: Write failing test**

```clojure
(ns loupe.checks.normalization-test
  (:require [clojure.test :refer [deftest is testing]]
            [loupe.checks.normalization :refer [normalization-status]]))

(deftest normalization-status-test
  (testing "nil and blank"
    (is (= "EMPTY" (normalization-status nil)))
    (is (= "EMPTY" (normalization-status ""))))

  (testing "ASCII/Simple"
    (is (= "ASCII/Simple" (normalization-status "Hello")))
    (is (= "ASCII/Simple" (normalization-status "123"))))

  (testing "NFC normalized"
    (is (= "NFC normalized" (normalization-status "é"))))

  (testing "NFD normalized"
    (is (= "NFD normalized" (normalization-status "e\u0301"))))

  (testing "Non-normalized"
    ;; Construct a non-normalized string for test
    (is (string? (normalization-status "test")))))
```

- [ ] **Step 2: Run test — verify fail**

- [ ] **Step 3: Implement normalization-status**

```clojure
(ns loupe.checks.normalization)

(defn normalization-status
  "Check NFC/NFD normalization status.
  Returns ASCII/Simple, NFC normalized, NFD normalized, or Non-normalized."
  [value]
  (if (or (nil? value) (clojure.string/blank? (str value)))
    "EMPTY"
    (let [s (str value)
          nfc (java.text.Normalizer/normalize s java.text.Normalizer$Form/NFC)
          nfd (java.text.Normalizer/normalize s java.text.Normalizer$Form/NFD)]
      (cond
        (and (= s nfc) (= s nfd)) "ASCII/Simple"
        (= s nfc) "NFC normalized"
        (= s nfd) "NFD normalized"
        :else "Non-normalized"))))
```

- [ ] **Step 4: Run test — verify pass**

- [ ] **Step 5: Commit**

```bash
git add src/loupe/checks/normalization.clj test/loupe/checks/normalization_test.clj
git commit -m "feat: add normalization-status check"
```

---

### Task 4: Implement script.clj (3 functions)

**Files:**
- Create: `src/loupe/checks/script.clj`
- Create: `test/loupe/checks/script_test.clj`

- [ ] **Step 1: Write failing tests for all 3 functions**

```clojure
(ns loupe.checks.script-test
  (:require [clojure.test :refer [deftest is testing]]
            [loupe.checks.script :refer [primary-script mixed-scripts directionality]]))

(deftest primary-script-test
  (testing "nil and blank"
    (is (= "EMPTY" (primary-script nil))))

  (testing "Latin"
    (is (= "Latin" (primary-script "Hello"))))

  (testing "Arabic"
    (is (= "Arabic" (primary-script "\u0645\u0643\u062a\u0628\u0629"))))

  (testing "Cyrillic"
    (is (= "Cyrillic" (primary-script "\u0427\u0435"))))

  (testing "Chinese/CJK"
    (is (= "Chinese/CJK" (primary-script "\u4E2D\u6587")))))

(deftest mixed-scripts-test
  (testing "single script"
    (is (= "Latin" (mixed-scripts "Hello"))))

  (testing "mixed"
    (is (= "Mixed: Latin+Cyrillic" (mixed-scripts "Hello\u0427")))))

(deftest directionality-test
  (testing "LTR"
    (is (= "LTR" (directionality "Hello"))))

  (testing "RTL Arabic"
    (is (= "RTL (Arabic)" (directionality "\u0645\u0643"))))

  (testing "Bidirectional"
    (is (= "Bidirectional (mixed)" (directionality "Hello\u0645")))))
```

- [ ] **Step 2: Run test — verify fail**

- [ ] **Step 3: Implement all 3 functions**

```clojure
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
```

- [ ] **Step 4: Run test — verify pass**

- [ ] **Step 5: Commit**

```bash
git add src/loupe/checks/script.clj test/loupe/checks/script_test.clj
git commit -m "feat: add script detection (primary, mixed, directionality)"
```

---

### Task 5: Implement diacritics.clj (5 functions)

**Files:**
- Create: `src/loupe/checks/diacritics.clj`
- Create: `test/loupe/checks/diacritics_test.clj`

Reference v2 source: section 1 of `OpenRefine Data Quality Facets.md`.

- [ ] **Step 1: Write failing tests**

```clojure
(ns loupe.checks.diacritics-test
  (:require [clojure.test :refer [deftest is testing]]
            [loupe.checks.diacritics :refer [unique-marks diacritic-letters
                                               non-ascii-chars diacritic-type
                                               diacritic-count]]))

(deftest unique-marks-test
  (testing "no diacritics"
    (is (= "none" (unique-marks "Hello"))))

  (testing "with combining marks"
    (is (string? (unique-marks "café")))))

(deftest diacritic-letters-test
  (testing "no diacritics"
    (is (= "none" (diacritic-letters "Hello"))))

  (testing "with diacritics"
    (is (string? (diacritic-letters "café")))))

(deftest non-ascii-chars-test
  (testing "ASCII only"
    (is (= "ASCII only" (non-ascii-chars "Hello"))))

  (testing "has non-ASCII"
    (is (string? (non-ascii-chars "café")))))

(deftest diacritic-type-test
  (testing "no diacritics"
    (is (= "no diacritics" (diacritic-type "Hello"))))

  (testing "combining diacritics"
    (is (string? (diacritic-type "e\u0301")))))

(deftest diacritic-count-test
  (testing "count as string"
    (is (= "0" (diacritic-count "Hello")))
    (is (string? (diacritic-count "café")))))
```

- [ ] **Step 2: Run test — verify fail**

- [ ] **Step 3: Implement all 5 functions**

```clojure
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
```

- [ ] **Step 4: Run test — verify pass**

- [ ] **Step 5: Commit**

```bash
git add src/loupe/checks/diacritics.clj test/loupe/checks/diacritics_test.clj
git commit -m "feat: add diacritics analysis (5 functions)"
```

---

### Task 6: Implement casing.clj (3 functions)

**Files:**
- Create: `src/loupe/checks/casing.clj`
- Create: `test/loupe/checks/casing_test.clj`

Reference v2 source: section 4 of `OpenRefine Data Quality Facets.md`.

- [ ] **Step 1: Write failing tests**

```clojure
(ns loupe.checks.casing-test
  (:require [clojure.test :refer [deftest is testing]]
            [loupe.checks.casing :refer [case-pattern case-complexity word-case-pattern]]))

(deftest case-pattern-test
  (testing "all uppercase"
    (is (= "ALL UPPERCASE" (case-pattern "HELLO"))))

  (testing "all lowercase"
    (is (= "all lowercase" (case-pattern "hello"))))

  (testing "Title case"
    (is (= "Title case" (case-pattern "Hello"))))

  (testing "CamelCase"
    (is (= "CamelCase" (case-pattern "HelloWorld"))))

  (testing "mixedCase"
    (is (= "mixedCase" (case-pattern "helloWorld")))))

(deftest case-complexity-test
  (testing "no letters"
    (is (= "no letters" (case-complexity "123"))))

  (testing "percentage"
    (is (string? (case-complexity "HeLLo")))))

(deftest word-case-pattern-test
  (testing "pattern"
    (is (string? (word-case-pattern "Hello world")))))
```

- [ ] **Step 2: Run test — verify fail**

- [ ] **Step 3: Implement all 3 functions**

```clojure
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
```

- [ ] **Step 4: Run test — verify pass**

- [ ] **Step 5: Commit**

```bash
git add src/loupe/checks/casing.clj test/loupe/checks/casing_test.clj
git commit -m "feat: add casing analysis (3 functions)"
```

---

### Task 7: Implement punctuation.clj (2 functions)

**Files:**
- Create: `src/loupe/checks/punctuation.clj`
- Create: `test/loupe/checks/punctuation_test.clj`

Reference v2 source: section 8.

- [ ] **Step 1: Write failing tests**

```clojure
(ns loupe.checks.punctuation-test
  (:require [clojure.test :refer [deftest is testing]]
            [loupe.checks.punctuation :refer [unique-punctuation quote-style]]))

(deftest unique-punctuation-test
  (testing "no punctuation"
    (is (= "no punctuation" (unique-punctuation "Hello"))))

  (testing "has punctuation"
    (is (string? (unique-punctuation "Hello, world!")))))

(deftest quote-style-test
  (testing "No quotes"
    (is (= "No quotes" (quote-style "Hello"))))

  (testing "Straight quotes"
    (is (= "Straight quotes" (quote-style "\"Hello\""))))

  (testing "Smart quotes"
    (is (= "Smart/typographic quotes" (quote-style "\u201CHello\u201D")))))
```

- [ ] **Step 2: Run test — verify fail**

- [ ] **Step 3: Implement both functions**

```clojure
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
```

- [ ] **Step 4: Run test — verify pass**

- [ ] **Step 5: Commit**

```bash
git add src/loupe/checks/punctuation.clj test/loupe/checks/punctuation_test.clj
git commit -m "feat: add punctuation analysis (2 functions)"
```

---

### Task 8: Implement content.clj (11 functions)

**Files:**
- Create: `src/loupe/checks/content.clj`
- Create: `test/loupe/checks/content_test.clj`

Reference v2 source: sections 6, 9-11, 13-14.

This is the largest namespace. I'll provide test structure and implementations for all 11 functions.

- [ ] **Step 1: Write failing tests** (abbreviated for space — expand each)

```clojure
(ns loupe.checks.content-test
  (:require [clojure.test :refer [deftest is testing]]
            [loupe.checks.content :refer [numeric-system emoji-summary word-stats
                                           ligatures char-count byte-count string-shape
                                           char-category-breakdown dominant-char-type]]))

(deftest numeric-system-test
  (testing "Western numerals"
    (is (= "Western/ASCII numerals" (numeric-system "123"))))

  (testing "No numerals"
    (is (= "No numerals" (numeric-system "Hello")))))

(deftest emoji-summary-test
  (testing "no emoji"
    (is (= "no emoji" (emoji-summary "Hello"))))

  (testing "has emoji"
    (is (string? (emoji-summary "Hello\u2600")))))

(deftest word-stats-test
  (testing "stats format"
    (is (string? (word-stats "Hello world")))))

(deftest ligatures-test
  (testing "no ligatures"
    (is (= "no ligatures" (ligatures "Hello"))))

  (testing "has ligatures"
    (is (string? (ligatures "\uFB00")))))

(deftest char-count-test
  (testing "count as string"
    (is (= "5" (char-count "Hello")))))

(deftest byte-count-test
  (testing "byte count as string"
    (is (string? (byte-count "Hello")))))

(deftest string-shape-test
  (testing "shape pattern"
    (is (= "Aaaaa" (string-shape "Hello")))))

(deftest char-category-breakdown-test
  (testing "breakdown format"
    (is (string? (char-category-breakdown "Hello 123")))))

(deftest dominant-char-type-test
  (testing "letter dominant"
    (is (= "Letter-dominant" (dominant-char-type "Hello")))))
```

- [ ] **Step 2: Run test — verify fail**

- [ ] **Step 3: Implement all 11 functions**

```clojure
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
```

- [ ] **Step 4: Run test — verify pass**

- [ ] **Step 5: Commit**

```bash
git add src/loupe/checks/content.clj test/loupe/checks/content_test.clj
git commit -m "feat: add content analysis (11 functions)"
```

---

### Task 9: Implement quality.clj (6 composite functions)

**Files:**
- Create: `src/loupe/checks/quality.clj`
- Create: `test/loupe/checks/quality_test.clj`

Reference v2 source: section 15. These functions compose from other namespaces.

- [ ] **Step 1: Write failing tests** (abbreviated)

```clojure
(ns loupe.checks.quality-test
  (:require [clojure.test :refer [deftest is testing]]
            [loupe.checks.quality :refer [all-issues quality-score quality-grade
                                           fresh-hell reconciliation-ready autopsy-report]]))

(deftest all-issues-test
  (testing "clean string"
    (is (= "CLEAN" (all-issues "Hello"))))

  (testing "has issues"
    (is (string? (all-issues "\uFFFD")))))

(deftest quality-score-test
  (testing "returns numeric string"
    (is (= "100" (quality-score "Hello")))
    (is (string? (quality-score "\uFFFD")))))

(deftest quality-grade-test
  (testing "A grade"
    (is (= "A - Clean" (quality-grade "Hello"))))

  (testing "F grade"
    (is (= "F - Critical Issues" (quality-grade "\uFFFD")))))

(deftest fresh-hell-test
  (testing "looks fine"
    (is (= "Looks fine" (fresh-hell "Hello"))))

  (testing "detects anomalies"
    (is (string? (fresh-hell "")))))

(deftest reconciliation-ready-test
  (testing "ready"
    (is (= "RECONCILIATION READY" (reconciliation-ready "Hello"))))

  (testing "warnings"
    (is (string? (reconciliation-ready " Hello ")))))

(deftest autopsy-report-test
  (testing "report format"
    (is (string? (autopsy-report "Hello")))))
```

- [ ] **Step 2: Run test — verify fail**

- [ ] **Step 3: Implement all 6 composite functions**

```clojure
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
```

- [ ] **Step 4: Run test — verify pass**

- [ ] **Step 5: Commit**

```bash
git add src/loupe/checks/quality.clj test/loupe/checks/quality_test.clj
git commit -m "feat: add quality composite checks (6 functions)"
```

---

### Task 10: Update project.clj for AOT

**Files:**
- Modify: `project.clj`

- [ ] **Step 1: Update AOT list**

Replace `:aot [loupe.checks]` with:

```clojure
:aot [loupe.checks.encoding
      loupe.checks.whitespace
      loupe.checks.normalization
      loupe.checks.script
      loupe.checks.diacritics
      loupe.checks.casing
      loupe.checks.punctuation
      loupe.checks.content
      loupe.checks.quality]
```

- [ ] **Step 2: Build and verify**

```bash
make jar
```

Expected: jar builds with all 9 namespace init classes.

- [ ] **Step 3: Verify jar contents**

```bash
jar tf target/loupe.jar | grep "__init.class"
```

Expected: 9 init classes listed.

- [ ] **Step 4: Commit**

```bash
git add project.clj
git commit -m "build: update AOT list for all 9 namespaces"
```

---

### Task 11: Update controller.js to load all namespaces

**Files:**
- Modify: `extension/module/MOD-INF/controller.js`

- [ ] **Step 1: Update init class loading**

Replace the single `Class.forName` call with a loop:

```javascript
var ClientSideResourceManager = Packages.com.google.refine.ClientSideResourceManager;

function init() {
  // Load all loupe.checks.* namespaces from bundled jar
  try {
    var modulePath = module.getPath();
    var jarFile = new java.io.File(modulePath, "MOD-INF/lib/loupe.jar");
    var jarUrl = jarFile.toURI().toURL();

    var parentCL = java.lang.Thread.currentThread().getContextClassLoader();
    var urls = java.lang.reflect.Array.newInstance(java.net.URL, 1);
    urls[0] = jarUrl;
    var jarCL = new java.net.URLClassLoader(urls, parentCL);

    var namespaces = [
      "loupe.checks.encoding",
      "loupe.checks.whitespace",
      "loupe.checks.normalization",
      "loupe.checks.script",
      "loupe.checks.diacritics",
      "loupe.checks.casing",
      "loupe.checks.punctuation",
      "loupe.checks.content",
      "loupe.checks.quality"
    ];

    for (var i = 0; i < namespaces.length; i++) {
      var initClass = namespaces[i].replace(/\./g, "/") + "__init";
      java.lang.Class.forName(initClass, true, jarCL);
    }
  } catch (e) {
    java.lang.System.err.println("Loupe: Failed to load namespaces: " + e);
  }

  ClientSideResourceManager.addPaths(
    "project/scripts",
    module,
    [
      "scripts/loupe.js"
    ]
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add extension/module/MOD-INF/controller.js
git commit -m "feat: load all 9 check namespaces in controller.js"
```

---

### Task 12: Update loupe.js with 3 submenu groups

**Files:**
- Modify: `extension/module/scripts/loupe.js`

- [ ] **Step 1: Replace with full submenu structure**

```javascript
DataTableColumnHeaderUI.extendMenu(function (column, columnHeaderUI, menu) {
  // Loupe: Quality submenu
  MenuSystem.appendTo(menu, ["core/facet"], [
    {},  // separator
    {
      id: "loupe/quality",
      label: "Loupe: Quality",
      submenu: [
        {
          id: "loupe/all-issues",
          label: "All Issues",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "All Issues — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.quality/all-issues) value)"
            });
          }
        },
        {
          id: "loupe/quality-grade",
          label: "Quality Grade (A-F)",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Quality Grade — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.quality/quality-grade) value)"
            });
          }
        },
        {
          id: "loupe/quality-score",
          label: "Quality Score (0-100)",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Quality Score — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.quality/quality-score) value)"
            });
          }
        },
        {},
        {
          id: "loupe/encoding-issues",
          label: "Encoding Issues",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Encoding Issues — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.encoding/encoding-issues) value)"
            });
          }
        },
        {
          id: "loupe/whitespace-types",
          label: "Whitespace Types",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Whitespace — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.whitespace/whitespace-types) value)"
            });
          }
        },
        {
          id: "loupe/normalization",
          label: "Normalization Status",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Normalization — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.normalization/normalization-status) value)"
            });
          }
        },
        {},
        {
          id: "loupe/reconciliation-ready",
          label: "Reconciliation Ready",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Reconciliation Ready — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.quality/reconciliation-ready) value)"
            });
          }
        },
        {
          id: "loupe/fresh-hell",
          label: "Fresh Hell Detector",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Fresh Hell — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.quality/fresh-hell) value)"
            });
          }
        },
        {
          id: "loupe/autopsy",
          label: "Autopsy Report",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Autopsy — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.quality/autopsy-report) value)"
            });
          }
        }
      ]
    },
    // Loupe: Scripts & Characters submenu
    {
      id: "loupe/scripts",
      label: "Loupe: Scripts & Characters",
      submenu: [
        {
          id: "loupe/primary-script",
          label: "Primary Script",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Primary Script — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.script/primary-script) value)"
            });
          }
        },
        {
          id: "loupe/mixed-scripts",
          label: "Mixed Scripts",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Mixed Scripts — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.script/mixed-scripts) value)"
            });
          }
        },
        {
          id: "loupe/directionality",
          label: "Directionality",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Directionality — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.script/directionality) value)"
            });
          }
        },
        {},
        {
          id: "loupe/diacritics-marks",
          label: "Diacritics: Unique Marks",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Diacritics (Marks) — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.diacritics/unique-marks) value)"
            });
          }
        },
        {
          id: "loupe/diacritics-letters",
          label: "Diacritics: Letters",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Diacritics (Letters) — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.diacritics/diacritic-letters) value)"
            });
          }
        },
        {
          id: "loupe/diacritics-nonascii",
          label: "Diacritics: Non-ASCII",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Non-ASCII Chars — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.diacritics/non-ascii-chars) value)"
            });
          }
        },
        {
          id: "loupe/diacritics-type",
          label: "Diacritics: Type",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Diacritic Type — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.diacritics/diacritic-type) value)"
            });
          }
        },
        {
          id: "loupe/diacritics-count",
          label: "Diacritics: Count",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Diacritic Count — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.diacritics/diacritic-count) value)"
            });
          }
        },
        {},
        {
          id: "loupe/case-pattern",
          label: "Case Pattern",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Case Pattern — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.casing/case-pattern) value)"
            });
          }
        },
        {
          id: "loupe/case-complexity",
          label: "Case Complexity",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Case Complexity — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.casing/case-complexity) value)"
            });
          }
        },
        {
          id: "loupe/word-case",
          label: "Word Case Pattern",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Word Case — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.casing/word-case-pattern) value)"
            });
          }
        }
      ]
    },
    // Loupe: Content Analysis submenu
    {
      id: "loupe/content",
      label: "Loupe: Content Analysis",
      submenu: [
        {
          id: "loupe/punctuation",
          label: "Punctuation",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Punctuation — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.punctuation/unique-punctuation) value)"
            });
          }
        },
        {
          id: "loupe/quote-style",
          label: "Quote Style",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Quote Style — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.punctuation/quote-style) value)"
            });
          }
        },
        {},
        {
          id: "loupe/numeric-system",
          label: "Numeric System",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Numeric System — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.content/numeric-system) value)"
            });
          }
        },
        {
          id: "loupe/emoji",
          label: "Emoji",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Emoji — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.content/emoji-summary) value)"
            });
          }
        },
        {
          id: "loupe/word-stats",
          label: "Word Stats",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Word Stats — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.content/word-stats) value)"
            });
          }
        },
        {
          id: "loupe/ligatures",
          label: "Ligatures",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Ligatures — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.content/ligatures) value)"
            });
          }
        },
        {},
        {
          id: "loupe/char-count",
          label: "Character Count",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Char Count — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.content/char-count) value)"
            });
          }
        },
        {
          id: "loupe/byte-count",
          label: "Byte Count",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Byte Count — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.content/byte-count) value)"
            });
          }
        },
        {
          id: "loupe/string-shape",
          label: "String Shape",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "String Shape — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.content/string-shape) value)"
            });
          }
        },
        {},
        {
          id: "loupe/char-categories",
          label: "Character Categories",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Char Categories — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.content/char-category-breakdown) value)"
            });
          }
        },
        {
          id: "loupe/dominant-char-type",
          label: "Dominant Character Type",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Dominant Type — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.content/dominant-char-type) value)"
            });
          }
        }
      ]
    }
  ]);
});
```

- [ ] **Step 2: Commit**

```bash
git add extension/module/scripts/loupe.js
git commit -m "feat: add 3 submenu groups with all 31 menu items"
```

---

### Task 13: Final integration test

**Files:**
- No new files

- [ ] **Step 1: Run all tests**

```bash
make test
```

Expected: all tests across 9 test files pass.

- [ ] **Step 2: Build and install**

```bash
make clean && make install
```

- [ ] **Step 3: Restart OpenRefine and test menu**

1. Quit OpenRefine
2. Start OpenRefine
3. Open a project
4. Click a column header → Facet
5. Verify 3 Loupe submenus appear:
   - Loupe: Quality (9 items)
   - Loupe: Scripts & Characters (11 items)
   - Loupe: Content Analysis (11 items)
6. Test 2-3 menu items from each group — verify facets appear

- [ ] **Step 4: Tag Phase 1 complete**

```bash
git tag -a v0.1.0-phase1 -m "Phase 1 complete: full v2 facet library ported"
```

---

## Phase 1 Complete

**Exit criteria met:**
- ✓ `make test` green with all ~30 functions tested
- ✓ OpenRefine column menu shows 3 submenu groups
- ✓ All checks accessible and working
- ✓ Behavioural parity with v2 facet library
