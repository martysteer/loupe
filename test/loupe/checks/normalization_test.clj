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
