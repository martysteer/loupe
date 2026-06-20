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
