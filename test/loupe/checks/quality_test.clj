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
