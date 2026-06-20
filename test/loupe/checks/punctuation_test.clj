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
