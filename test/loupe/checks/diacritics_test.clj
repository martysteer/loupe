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
