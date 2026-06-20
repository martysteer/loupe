(ns loupe.checks.encoding-test
  (:require [clojure.test :refer [deftest is testing]]
            [loupe.checks.encoding :refer [encoding-issues]]))

(deftest encoding-issues-test
  (testing "nil and blank input"
    (is (= "EMPTY" (encoding-issues nil)))
    (is (= "EMPTY" (encoding-issues "")))
    (is (= "EMPTY" (encoding-issues "   "))))

  (testing "clean string"
    (is (= "CLEAN" (encoding-issues "Hello world")))
    (is (= "CLEAN" (encoding-issues "Biblioth\u00e8que nationale")))
    (is (= "CLEAN" (encoding-issues "\u0645\u0643\u062a\u0628\u0629"))))

  (testing "replacement character U+FFFD"
    (is (= "REPLACEMENT_CHAR" (encoding-issues "Hello \uFFFD world")))
    (is (= "REPLACEMENT_CHAR" (encoding-issues "\uFFFD"))))

  (testing "control characters (excluding tab, newline, CR)"
    (is (= "CONTROL_CHAR" (encoding-issues "Hello\u0001world")))
    (is (= "CONTROL_CHAR" (encoding-issues "\u0000")))
    (is (= "CONTROL_CHAR" (encoding-issues "text\u007Fmore")))
    (is (= "CONTROL_CHAR" (encoding-issues "text\u0085more"))))

  (testing "tab, newline, CR are NOT control char issues"
    (is (= "CLEAN" (encoding-issues "Hello\tworld")))
    (is (= "CLEAN" (encoding-issues "Hello\nworld")))
    (is (= "CLEAN" (encoding-issues "Hello\rworld"))))

  (testing "BOM U+FEFF"
    (is (= "BOM" (encoding-issues "\uFEFFHello"))))

  (testing "invisible formatters"
    (is (= "INVISIBLE_FORMATTER" (encoding-issues "Hello\u200Bworld")))
    (is (= "INVISIBLE_FORMATTER" (encoding-issues "Hello\u200Cworld")))
    (is (= "INVISIBLE_FORMATTER" (encoding-issues "Hello\u200Dworld")))
    (is (= "INVISIBLE_FORMATTER" (encoding-issues "Hello\u200Eworld")))
    (is (= "INVISIBLE_FORMATTER" (encoding-issues "Hello\u200Fworld")))
    (is (= "INVISIBLE_FORMATTER" (encoding-issues "text\u2028more")))
    (is (= "INVISIBLE_FORMATTER" (encoding-issues "text\u2029more")))
    (is (= "INVISIBLE_FORMATTER" (encoding-issues "text\u202Amore")))
    (is (= "INVISIBLE_FORMATTER" (encoding-issues "text\u202Emore")))
    (is (= "INVISIBLE_FORMATTER" (encoding-issues "text\u2060more")))
    (is (= "INVISIBLE_FORMATTER" (encoding-issues "text\u2061more")))
    (is (= "INVISIBLE_FORMATTER" (encoding-issues "text\u2069more"))))

  (testing "mojibake — UTF-8 decoded as Latin-1"
    (is (= "MOJIBAKE" (encoding-issues "\u00C3\u00A0")))
    (is (= "MOJIBAKE" (encoding-issues "\u00C3\u00BF")))
    (is (= "MOJIBAKE" (encoding-issues "Biblioth\u00C3\u00A8que"))))

  (testing "mojibake in C1 range triggers both control char and mojibake"
    (is (= "CONTROL_CHAR|MOJIBAKE" (encoding-issues "\u00C3\u0080"))))

  (testing "multiple issues — pipe-separated, in check order"
    (is (= "REPLACEMENT_CHAR|BOM"
           (encoding-issues "\uFEFF\uFFFD")))
    (is (= "REPLACEMENT_CHAR|CONTROL_CHAR"
           (encoding-issues "\uFFFD\u0001")))))
