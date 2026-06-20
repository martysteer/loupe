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
