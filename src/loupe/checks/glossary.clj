(ns loupe.checks.glossary
  (:require [clojure.string :as str])
  (:import [java.text BreakIterator]
           [java.util Locale]))

(defn tokenize
  "Split text into words using Java BreakIterator.
  Handles multilingual text (Latin, Arabic, CJK, etc.).
  Returns vector of word strings, excluding punctuation and whitespace."
  [text]
  (if (or (nil? text) (str/blank? (str text)))
    []
    (let [s (str text)
          bi (doto (BreakIterator/getWordInstance Locale/ROOT)
               (.setText s))
          start (.first bi)]
      (loop [prev start
             boundary (.next bi)
             words (transient [])]
        (if (= boundary BreakIterator/DONE)
          (persistent! words)
          (let [word (subs s prev boundary)
                is-word (and (not (str/blank? word))
                             (some #(Character/isLetterOrDigit (int %)) word))]
            (recur boundary
                   (.next bi)
                   (if is-word
                     (conj! words word)
                     words))))))))

(defn- normalize-case
  [word case-sensitive?]
  (if case-sensitive?
    word
    (str/lower-case word)))

(defn word-index
  "Extract first N characters of each word as index prefixes.
  Returns a collection of unique prefixes (for multi-valued facets).
  If a word is shorter than prefix-length, returns the full word."
  [value prefix-length case-sensitive?]
  (let [words (tokenize value)]
    (if (empty? words)
      []
      (->> words
           (map #(subs % 0 (min prefix-length (count %))))
           (map #(normalize-case % case-sensitive?))
           distinct
           vec))))

(defn word-glossary
  "Generate n-grams from tokenized text.
  Returns a collection of n-gram strings (for multi-valued facets).
  Unigrams (n=1) returns individual words.
  Bigrams (n=2) returns consecutive word pairs.
  If fewer words than n-gram-size, returns empty."
  [value n-gram-size case-sensitive?]
  (let [words (tokenize value)]
    (if (< (count words) n-gram-size)
      []
      (->> words
           (map #(normalize-case % case-sensitive?))
           (partition n-gram-size 1)
           (mapv #(str/join " " %))))))

(def ^:private concordance-limit 500)

(defn find-concordances
  "Search for keyword in rows and return KWIC concordance results.
  Input: rows is a sequence of {:row-index N :text \"...\"}
  Returns vector of maps {:row N :left \"...\" :keyword \"word\" :right \"...\"}
  Capped at 500 results."
  [rows keyword context-size case-sensitive?]
  (let [match-fn (if case-sensitive?
                   (fn [word] (= word keyword))
                   (let [kw-lower (str/lower-case keyword)]
                     (fn [word] (= (str/lower-case word) kw-lower))))]
    (loop [remaining rows
           results []
           total 0]
      (if (or (empty? remaining) (>= total concordance-limit))
        results
        (let [{:keys [row-index text]} (first remaining)
              words (vec (tokenize text))
              word-count (count words)
              matches (loop [i 0 acc []]
                        (if (or (>= i word-count) (>= (+ total (count acc)) concordance-limit))
                          acc
                          (if (match-fn (nth words i))
                            (recur (inc i)
                                   (conj acc
                                         {:row row-index
                                          :left (str/join " " (subvec words
                                                                       (max 0 (- i context-size))
                                                                       i))
                                          :keyword (nth words i)
                                          :right (str/join " " (subvec words
                                                                        (min (inc i) word-count)
                                                                        (min (+ i context-size 1) word-count)))}))
                            (recur (inc i) acc))))]
          (recur (rest remaining)
                 (into results matches)
                 (+ total (count matches))))))))
