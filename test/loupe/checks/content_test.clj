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
