(ns loupe.checks.glossary-test
  (:require [clojure.test :refer [deftest is testing]]
            [loupe.checks.glossary :refer [tokenize word-index]]))

(deftest tokenize-test
  (testing "Latin text"
    (is (= ["Hello" "world"] (tokenize "Hello world"))))

  (testing "punctuation stripped"
    (is (= ["Hello" "world"] (tokenize "Hello, world!"))))

  (testing "multiple spaces"
    (is (= ["one" "two" "three"] (tokenize "one   two   three"))))

  (testing "empty string"
    (is (= [] (tokenize ""))))

  (testing "nil input"
    (is (= [] (tokenize nil))))

  (testing "single word"
    (is (= ["Hello"] (tokenize "Hello"))))

  (testing "Arabic text tokenizes"
    (is (pos? (count (tokenize "مرحبا بالعالم")))))

  (testing "mixed scripts"
    (is (pos? (count (tokenize "Hello مرحبا world"))))))

(deftest word-index-test
  (testing "single letter prefix, case-sensitive"
    (is (= #{"H" "w"} (set (word-index "Hello world" 1 true)))))

  (testing "single letter prefix, case-insensitive"
    (is (= #{"h" "w"} (set (word-index "Hello world" 1 false)))))

  (testing "two letter prefix"
    (is (= #{"He" "wo"} (set (word-index "Hello world" 2 true)))))

  (testing "two letter prefix, case-insensitive"
    (is (= #{"he" "wo"} (set (word-index "Hello world" 2 false)))))

  (testing "prefix longer than word returns full word"
    (is (= #{"Hi" "a"} (set (word-index "Hi a" 3 true)))))

  (testing "empty input"
    (is (= [] (word-index "" 1 true))))

  (testing "nil input"
    (is (= [] (word-index nil 1 true))))

  (testing "deduplicates prefixes"
    (is (= #{"h"} (set (word-index "hello happy" 1 false))))))
