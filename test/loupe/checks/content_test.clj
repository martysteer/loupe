(ns loupe.checks.content-test
  (:require [clojure.test :refer [deftest is testing]]
            [loupe.checks.content :refer [numeric-system emoji-summary word-stats
                                           ligatures char-count byte-count string-shape
                                           char-category-breakdown dominant-char-type
                                           unicode-variants]]))

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

(deftest unicode-variants-test
  (testing "empty input"
    (is (= "EMPTY" (unicode-variants nil)))
    (is (= "EMPTY" (unicode-variants ""))))

  (testing "plain ASCII"
    (is (= "none" (unicode-variants "Hello world"))))

  (testing "non-breaking space"
    (is (= "non-breaking space" (unicode-variants "Hello\u00A0world"))))

  (testing "en dash"
    (is (= "en dash" (unicode-variants "pages 1\u20132"))))

  (testing "em dash"
    (is (= "em dash" (unicode-variants "word\u2014word"))))

  (testing "smart quotes"
    (is (= "double smart quotes" (unicode-variants "\u201CHello\u201D"))))

  (testing "single smart quotes"
    (is (= "single smart quotes" (unicode-variants "\u2018word\u2019"))))

  (testing "guillemets"
    (is (= "guillemets" (unicode-variants "\u00ABtext\u00BB"))))

  (testing "zero-width space"
    (is (= "zero-width space" (unicode-variants "word\u200Bword"))))

  (testing "multiple variants"
    (is (= "non-breaking space | en dash | double smart quotes"
           (unicode-variants "\u201CHello\u00A0world\u201D \u2013 test"))))

  (testing "CJK brackets"
    (is (= "CJK brackets" (unicode-variants "\u3008text\u3009"))))

  (testing "fullwidth parens"
    (is (= "fullwidth parens" (unicode-variants "\uFF08text\uFF09"))))

  (testing "bidi control"
    (is (= "bidi control" (unicode-variants "text\u202Amore"))))

  (testing "minus sign vs hyphen"
    (is (= "minus sign" (unicode-variants "5\u2212 3")))))
