(ns loupe.checks.concordance-test
  (:require [clojure.test :refer [deftest is testing]]
            [loupe.checks.glossary :refer [find-concordances]]))

(deftest find-concordances-test
  (testing "basic keyword match"
    (let [rows [{:row-index 0 :text "the big red fox"}
                {:row-index 1 :text "a small red car"}]
          results (find-concordances rows "red" 2 false)]
      (is (= 2 (count results)))
      (is (= "red" (:keyword (first results))))
      (is (= 0 (:row (first results))))))

  (testing "context window"
    (let [rows [{:row-index 0 :text "one two three four five"}]
          results (find-concordances rows "three" 2 false)]
      (is (= "one two" (:left (first results))))
      (is (= "four five" (:right (first results))))))

  (testing "keyword at start of text"
    (let [rows [{:row-index 0 :text "hello world today"}]
          results (find-concordances rows "hello" 2 false)]
      (is (= "" (:left (first results))))
      (is (= "world today" (:right (first results))))))

  (testing "keyword at end of text"
    (let [rows [{:row-index 0 :text "one two three"}]
          results (find-concordances rows "three" 2 false)]
      (is (= "one two" (:left (first results))))
      (is (= "" (:right (first results))))))

  (testing "case-insensitive"
    (let [rows [{:row-index 0 :text "Hello World"}]
          results (find-concordances rows "hello" 2 false)]
      (is (= 1 (count results)))))

  (testing "case-sensitive"
    (let [rows [{:row-index 0 :text "Hello World"}]
          results (find-concordances rows "hello" 2 true)]
      (is (= 0 (count results)))))

  (testing "no matches"
    (let [rows [{:row-index 0 :text "nothing here"}]
          results (find-concordances rows "missing" 2 false)]
      (is (= 0 (count results)))))

  (testing "multiple matches in one cell"
    (let [rows [{:row-index 0 :text "the cat and the dog"}]
          results (find-concordances rows "the" 1 false)]
      (is (= 2 (count results)))))

  (testing "result limit"
    (let [rows (vec (for [i (range 600)]
                      {:row-index i :text "word here now"}))
          results (find-concordances rows "word" 2 false)]
      (is (<= (count results) 500))))

  (testing "context window of 1"
    (let [rows [{:row-index 0 :text "a b c d e"}]
          results (find-concordances rows "c" 1 false)]
      (is (= "b" (:left (first results))))
      (is (= "d" (:right (first results)))))))
