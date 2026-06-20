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
