# Glossary, Index & Concordance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a fourth menu group "Loupe: Glossary & Index" with parameterized index/glossary facets and a KWIC concordance dialog to the loupe OpenRefine extension.

**Architecture:** One new Clojure namespace (`loupe.checks.glossary`) handles tokenization via Java BreakIterator, index prefix extraction, n-gram glossary generation, and concordance search. Facets use existing Clojure expression pattern. Concordance uses a gen-class Command to access project rows server-side, with a custom JS dialog for KWIC display.

**Tech Stack:** Clojure 1.12.3, Java BreakIterator, OpenRefine extension API (gen-class Command for concordance), JavaScript (Butterfly UI)

**Spec:** `docs/superpowers/specs/2026-06-21-glossary-index-concordance-design.md`

---

## File Structure

| File | Purpose |
|------|---------|
| `src/loupe/checks/glossary.clj` | Core tokenization, word-index, word-glossary functions (facet functions) |
| `src/loupe/commands/concordance.clj` | gen-class Command for concordance endpoint, calls glossary functions |
| `test/loupe/checks/glossary_test.clj` | Unit tests for tokenization, index, glossary |
| `test/loupe/commands/concordance_test.clj` | Unit tests for concordance search logic (pure function tests, no OpenRefine) |
| `extension/module/scripts/loupe.js` | Modify: add 4th menu group with 9 items |
| `extension/module/scripts/concordance-dialog.js` | Concordance dialog UI + KWIC table rendering |
| `extension/module/styles/concordance-dialog.css` | KWIC table styling |
| `extension/module/MOD-INF/controller.js` | Modify: load new namespace, register concordance command, load dialog JS/CSS |
| `project.clj` | Modify: add AOT namespace, add provided OpenRefine dependency |

---

## Task 1: Spike — Multi-Valued Facet Returns from Clojure

Verify that returning a Clojure vector from a facet expression creates multiple facet entries (not a single stringified entry). This is critical for index/glossary — selecting "a" must show all cells containing words starting with "a".

**Files:**
- None (manual testing in running OpenRefine)

- [ ] **Step 1: Build and install current extension**

Run:
```bash
make install
```
Restart OpenRefine. Open any project with text data.

- [ ] **Step 2: Test Clojure vector return in custom facet**

In OpenRefine: column dropdown → Facet → Custom text facet → Language: Clojure

Test expression:
```clojure
(vec (map str (seq (str value))))
```

This returns a vector of individual characters. If multi-valued facets work, the facet panel shows one entry per unique character across all cells, with counts. If it doesn't work, entries will be stringified vectors like `["H" "e" "l" "l" "o"]`.

- [ ] **Step 3: Test alternative if vector doesn't work**

If vectors don't create multi-valued facets, try returning a Java ArrayList:
```clojure
(java.util.ArrayList. (map str (seq (str value))))
```

- [ ] **Step 4: Document result**

Record which return type works (Clojure vector, Java ArrayList, or neither). If neither works, the fallback is: facet functions return a pipe-delimited string, and users see combined entries per cell rather than individual words/prefixes. This changes the UX but still provides value.

- [ ] **Step 5: Commit spike notes**

```bash
git add -A
git commit -m "spike: test multi-valued Clojure facet returns"
```

---

## Task 2: Tokenization and Word-Index — Tests

Write failing tests for the `tokenize` helper and `word-index` function before implementation.

**Files:**
- Create: `test/loupe/checks/glossary_test.clj`

- [ ] **Step 1: Write tokenize tests**

```clojure
(ns loupe.checks.glossary-test
  (:require [clojure.test :refer [deftest is testing]]
            [loupe.checks.glossary :refer [tokenize word-index word-glossary]]))

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
```

- [ ] **Step 2: Write word-index tests**

Append to same file:

```clojure
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
```

- [ ] **Step 3: Run tests to verify they fail**

Run:
```bash
lein test loupe.checks.glossary-test
```
Expected: FAIL — `loupe.checks.glossary` namespace not found.

- [ ] **Step 4: Commit failing tests**

```bash
git add test/loupe/checks/glossary_test.clj
git commit -m "test: add failing tests for tokenize and word-index"
```

---

## Task 3: Tokenization and Word-Index — Implementation

Implement `tokenize` and `word-index` to make Task 2 tests pass.

**Files:**
- Create: `src/loupe/checks/glossary.clj`

- [ ] **Step 1: Implement tokenize and word-index**

```clojure
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
```

- [ ] **Step 2: Run tests**

Run:
```bash
lein test loupe.checks.glossary-test
```
Expected: All tokenize and word-index tests PASS. word-glossary tests will fail (not yet implemented — that's expected since we haven't written them yet).

- [ ] **Step 3: Commit**

```bash
git add src/loupe/checks/glossary.clj
git commit -m "feat: add tokenize and word-index functions"
```

---

## Task 4: Word-Glossary — Tests

Write failing tests for the `word-glossary` function.

**Files:**
- Modify: `test/loupe/checks/glossary_test.clj`

- [ ] **Step 1: Add word-glossary tests**

Append to `test/loupe/checks/glossary_test.clj`:

```clojure
(deftest word-glossary-test
  (testing "unigrams, case-sensitive"
    (is (= ["Hello" "beautiful" "world"]
           (word-glossary "Hello beautiful world" 1 true))))

  (testing "unigrams, case-insensitive"
    (is (= ["hello" "beautiful" "world"]
           (word-glossary "Hello beautiful world" 1 false))))

  (testing "bigrams"
    (is (= ["Hello beautiful" "beautiful world"]
           (word-glossary "Hello beautiful world" 2 true))))

  (testing "trigrams"
    (is (= ["Hello beautiful world"]
           (word-glossary "Hello beautiful world" 3 true))))

  (testing "n-gram size exceeds word count"
    (is (= [] (word-glossary "Hello" 2 true))))

  (testing "empty input"
    (is (= [] (word-glossary "" 1 true))))

  (testing "nil input"
    (is (= [] (word-glossary nil 1 true))))

  (testing "bigrams case-insensitive"
    (is (= ["hello world" "world test"]
           (word-glossary "Hello World Test" 2 false)))))
```

- [ ] **Step 2: Run tests to verify new tests fail**

Run:
```bash
lein test loupe.checks.glossary-test
```
Expected: word-glossary tests FAIL. tokenize and word-index tests still PASS.

- [ ] **Step 3: Commit failing tests**

```bash
git add test/loupe/checks/glossary_test.clj
git commit -m "test: add failing tests for word-glossary"
```

---

## Task 5: Word-Glossary — Implementation

Implement `word-glossary` to make Task 4 tests pass.

**Files:**
- Modify: `src/loupe/checks/glossary.clj`

- [ ] **Step 1: Add word-glossary function**

Add after the `word-index` function in `src/loupe/checks/glossary.clj`:

```clojure
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
```

- [ ] **Step 2: Run all tests**

Run:
```bash
lein test loupe.checks.glossary-test
```
Expected: All tests PASS.

- [ ] **Step 3: Commit**

```bash
git add src/loupe/checks/glossary.clj
git commit -m "feat: add word-glossary function with n-gram support"
```

---

## Task 6: Build Config — Add Glossary Namespace

Register the new namespace for AOT compilation and verify the build works.

**Files:**
- Modify: `project.clj`

- [ ] **Step 1: Add glossary to AOT namespaces**

In `project.clj`, add `loupe.checks.glossary` to the `:aot` vector. The updated `:aot` should be:

```clojure
  :aot [loupe.checks.encoding
        loupe.checks.whitespace
        loupe.checks.normalization
        loupe.checks.script
        loupe.checks.diacritics
        loupe.checks.casing
        loupe.checks.punctuation
        loupe.checks.content
        loupe.checks.quality
        loupe.checks.glossary]
```

- [ ] **Step 2: Build and verify**

Run:
```bash
lein clean && lein jar
```
Expected: Build succeeds, `target/loupe.jar` produced.

- [ ] **Step 3: Verify namespace loaded in jar**

Run:
```bash
jar tf target/loupe.jar | grep glossary
```
Expected: `loupe/checks/glossary__init.class` and related class files present.

- [ ] **Step 4: Commit**

```bash
git add project.clj
git commit -m "build: add loupe.checks.glossary to AOT compilation"
```

---

## Task 7: Menu Integration — Index & Glossary Facets

Add the 8 facet menu items to the extension UI.

**Files:**
- Modify: `extension/module/MOD-INF/controller.js`
- Modify: `extension/module/scripts/loupe.js`

- [ ] **Step 1: Add glossary namespace to controller.js**

In `extension/module/MOD-INF/controller.js`, add `"loupe.checks.glossary"` to the `namespaces` array:

```javascript
    var namespaces = [
      "loupe.checks.encoding",
      "loupe.checks.whitespace",
      "loupe.checks.normalization",
      "loupe.checks.script",
      "loupe.checks.diacritics",
      "loupe.checks.casing",
      "loupe.checks.punctuation",
      "loupe.checks.content",
      "loupe.checks.quality",
      "loupe.checks.glossary"
    ];
```

- [ ] **Step 2: Add 4th menu group to loupe.js**

In `extension/module/scripts/loupe.js`, add the following block after the "Loupe: Content Analysis" submenu closing brace (after the `}` on the line before `]);`):

```javascript
    // Loupe: Glossary & Index submenu
    {
      id: "loupe/glossary",
      label: "Loupe: Glossary & Index",
      submenu: [
        {
          id: "loupe/index-1",
          label: "Index: First Letter",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Index (1 letter) — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.glossary/word-index) value 1 true)"
            });
          }
        },
        {
          id: "loupe/index-1-nocase",
          label: "Index: First Letter (ignore case)",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Index (1 letter, ignore case) — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.glossary/word-index) value 1 false)"
            });
          }
        },
        {
          id: "loupe/index-2",
          label: "Index: First 2 Letters",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Index (2 letters) — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.glossary/word-index) value 2 true)"
            });
          }
        },
        {
          id: "loupe/index-2-nocase",
          label: "Index: First 2 Letters (ignore case)",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Index (2 letters, ignore case) — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.glossary/word-index) value 2 false)"
            });
          }
        },
        {},
        {
          id: "loupe/glossary-words",
          label: "Glossary: Words",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Glossary (words) — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.glossary/word-glossary) value 1 true)"
            });
          }
        },
        {
          id: "loupe/glossary-words-nocase",
          label: "Glossary: Words (ignore case)",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Glossary (words, ignore case) — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.glossary/word-glossary) value 1 false)"
            });
          }
        },
        {
          id: "loupe/glossary-bigrams",
          label: "Glossary: Bigrams",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Glossary (bigrams) — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.glossary/word-glossary) value 2 true)"
            });
          }
        },
        {
          id: "loupe/glossary-trigrams",
          label: "Glossary: Trigrams",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Glossary (trigrams) — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.glossary/word-glossary) value 3 true)"
            });
          }
        }
      ]
    }
```

- [ ] **Step 3: Build, install, and verify menus**

Run:
```bash
make install
```
Restart OpenRefine. Open a project. Click any column header → Facet. Verify "Loupe: Glossary & Index" submenu appears with 8 items (4 index, separator, 4 glossary).

- [ ] **Step 4: Test a facet**

Click "Glossary: Words (ignore case)" on a text column. Verify:
- Facet panel appears
- Shows individual words with counts (if multi-valued works from Task 1 spike)
- OR shows per-cell word lists (if multi-valued doesn't work — still functional, different UX)

- [ ] **Step 5: Commit**

```bash
git add extension/module/MOD-INF/controller.js extension/module/scripts/loupe.js
git commit -m "feat: add Glossary & Index menu group with 8 facet items"
```

---

## Task 8: Spike — Concordance Command Registration

Verify that a Clojure gen-class extending `com.google.refine.commands.Command` can be registered and called from the extension. This is the architectural spike for concordance.

**Rationale:** The current extension has no OpenRefine compile-time dependency. Concordance needs server-side access to project rows, requiring a Command subclass. This spike validates the approach.

**Files:**
- Modify: `project.clj` (add provided dependency)
- Create: `src/loupe/commands/concordance.clj` (spike version)
- Modify: `extension/module/MOD-INF/controller.js` (register command)

- [ ] **Step 1: Determine OpenRefine jar location**

Find OpenRefine's main jar on the local machine:
```bash
find /Applications/OpenRefine.app -name "openrefine*.jar" -o -name "refine*.jar" 2>/dev/null
ls "$HOME/Library/Application Support/OpenRefine/" 2>/dev/null
```

Note the path. If installed via homebrew or differently, adjust accordingly. The key jar is the one containing `com.google.refine.commands.Command`.

- [ ] **Step 2: Add OpenRefine as provided dependency**

In `project.clj`, add a `:profiles` section. Replace `OPENREFINE_JAR_PATH` with the actual path found in Step 1:

```clojure
(defproject com.loupe/loupe "0.1.0"
  :description "OpenRefine extension for one-click text data quality checks"
  :license {:name "CC-BY"}
  :dependencies [[org.clojure/clojure "1.12.3"]]
  :profiles {:provided {:dependencies []
                        :resource-paths ["OPENREFINE_JAR_PATH"]}
             :dev {:resource-paths ["OPENREFINE_JAR_PATH"]}}
  :source-paths ["src"]
  :test-paths ["test"]
  :target-path "target"
  :aot [loupe.checks.encoding
        loupe.checks.whitespace
        loupe.checks.normalization
        loupe.checks.script
        loupe.checks.diacritics
        loupe.checks.casing
        loupe.checks.punctuation
        loupe.checks.content
        loupe.checks.quality
        loupe.checks.glossary
        loupe.commands.concordance]
  :jar-name "loupe.jar")
```

If OpenRefine publishes to Maven Central (check `org.openrefine/main` or similar), use Maven coordinates in `:dependencies` under the `:provided` profile instead of `:resource-paths`.

- [ ] **Step 3: Create minimal concordance command**

Create `src/loupe/commands/concordance.clj`:

```clojure
(ns loupe.commands.concordance
  (:gen-class
    :name loupe.commands.ConcordanceCommand
    :extends com.google.refine.commands.Command)
  (:require [loupe.checks.glossary :as glossary]
            [clojure.string :as str])
  (:import [javax.servlet.http HttpServletRequest HttpServletResponse]
           [com.google.refine.commands Command]
           [java.io PrintWriter]))

(defn -doPost
  [this ^HttpServletRequest request ^HttpServletResponse response]
  (.setContentType response "application/json")
  (.setCharacterEncoding response "UTF-8")
  (let [writer (.getWriter response)]
    (.println writer "{\"status\": \"ok\", \"message\": \"concordance spike works\"}")))
```

- [ ] **Step 4: Register command in controller.js**

Add to the `init()` function in `controller.js`, after the namespace loading block:

```javascript
  // Register concordance command
  try {
    var ConcordanceCommand = java.lang.Class.forName(
      "loupe.commands.ConcordanceCommand", true, jarCL);
    var commandInstance = ConcordanceCommand.getDeclaredConstructor().newInstance();

    var RS = Packages.com.google.refine.RefineServlet;
    RS.registerCommand(module, "loupe-concordance", commandInstance);
  } catch (e) {
    java.lang.System.err.println("Loupe: Failed to register concordance command: " + e);
  }
```

- [ ] **Step 5: Build and test**

Run:
```bash
lein clean && lein jar && make install
```
Restart OpenRefine. Test the command:
```bash
curl -s http://localhost:3333/command/loupe-concordance -X POST
```
Expected: `{"status": "ok", "message": "concordance spike works"}`

- [ ] **Step 6: Record result and decide**

If spike succeeds: proceed with Tasks 9-12 using the gen-class Command approach.

If spike fails (classloader issues, missing servlet API, etc.): **fallback** — implement concordance entirely in client-side JavaScript. The dialog JS will fetch visible rows via OpenRefine's `/command/core/get-rows` API and do tokenization/search in JS. This means concordance tokenization differs from facet tokenization (JS regex vs BreakIterator) but still ships the feature.

- [ ] **Step 7: Commit spike**

```bash
git add project.clj src/loupe/commands/concordance.clj extension/module/MOD-INF/controller.js
git commit -m "spike: test gen-class Command registration for concordance"
```

---

## Task 9: Concordance Search Logic — Tests

Write tests for the concordance search function. These test the pure search logic without OpenRefine — input is a sequence of `{:row-index N :text "..."}` maps.

**Files:**
- Create: `test/loupe/commands/concordance_test.clj`

- [ ] **Step 1: Write concordance search tests**

```clojure
(ns loupe.commands.concordance-test
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
```

- [ ] **Step 2: Run tests to verify they fail**

Run:
```bash
lein test loupe.commands.concordance-test
```
Expected: FAIL — `find-concordances` not found.

- [ ] **Step 3: Commit**

```bash
git add test/loupe/commands/concordance_test.clj
git commit -m "test: add failing tests for concordance search"
```

---

## Task 10: Concordance Search Logic — Implementation

Implement the `find-concordances` function in the glossary namespace. This is a pure function — no OpenRefine dependency.

**Files:**
- Modify: `src/loupe/checks/glossary.clj`

- [ ] **Step 1: Add find-concordances function**

Add to `src/loupe/checks/glossary.clj`:

```clojure
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
           results (transient [])
           count 0]
      (if (or (empty? remaining) (>= count concordance-limit))
        (persistent! results)
        (let [{:keys [row-index text]} (first remaining)
              words (tokenize text)
              matches (for [i (range (clojure.core/count words))
                            :when (match-fn (nth words i))
                            :while (< (+ count 1) (inc concordance-limit))]
                        {:row row-index
                         :left (str/join " " (subvec (vec words)
                                                      (max 0 (- i context-size))
                                                      i))
                         :keyword (nth words i)
                         :right (str/join " " (subvec (vec words)
                                                       (min (inc i) (clojure.core/count words))
                                                       (min (+ i context-size 1) (clojure.core/count words))))})]
          (let [new-results (reduce conj! results (take (- concordance-limit count) matches))
                new-count (+ count (min (clojure.core/count matches) (- concordance-limit count)))]
            (recur (rest remaining) new-results new-count)))))))
```

- [ ] **Step 2: Run all tests**

Run:
```bash
lein test
```
Expected: All tests PASS (both glossary and concordance tests).

- [ ] **Step 3: Commit**

```bash
git add src/loupe/checks/glossary.clj
git commit -m "feat: add find-concordances function for KWIC search"
```

---

## Task 11: Concordance Command — Full Implementation

Replace the spike concordance command with the real implementation that accesses OpenRefine project data. Depends on Task 8 spike succeeding.

**Files:**
- Modify: `src/loupe/commands/concordance.clj`

- [ ] **Step 1: Implement full concordance command**

Replace `src/loupe/commands/concordance.clj` with:

```clojure
(ns loupe.commands.concordance
  (:gen-class
    :name loupe.commands.ConcordanceCommand
    :extends com.google.refine.commands.Command)
  (:require [loupe.checks.glossary :as glossary]
            [clojure.string :as str])
  (:import [javax.servlet.http HttpServletRequest HttpServletResponse]
           [com.google.refine.commands Command]
           [com.google.refine ProjectManager]
           [com.google.refine.model Project Row Cell]
           [com.google.refine.browsing Engine]
           [org.json JSONObject JSONArray JSONWriter]))

(defn- get-visible-rows
  "Get visible rows with text from the specified column, respecting filters."
  [^Project project ^String column-name ^HttpServletRequest request]
  (let [engine (Engine. project (JSONObject. (.getParameter request "engine")))
        col-index (.getCellIndex (.getColumnByName (.getColumnModel project) column-name))
        rows (transient [])]
    (.getAllFilteredRows engine
      (reify com.google.refine.browsing.RowVisitor
        (start [this project] false)
        (visit [this project row-index row]
          (let [cell (.getCell row col-index)]
            (when (and cell (.value cell))
              (conj! rows {:row-index row-index
                           :text (str (.value cell))})))
          false)
        (end [this project])))
    (persistent! rows)))

(defn -doPost
  [this ^HttpServletRequest request ^HttpServletResponse response]
  (.setContentType response "application/json")
  (.setCharacterEncoding response "UTF-8")
  (try
    (let [project-id (Long/parseLong (.getParameter request "project"))
          project (.getProject (ProjectManager/singleton) project-id)
          column-name (.getParameter request "columnName")
          keyword (.getParameter request "keyword")
          context-size (Integer/parseInt (or (.getParameter request "contextSize") "3"))
          case-sensitive (Boolean/parseBoolean (or (.getParameter request "caseSensitive") "false"))
          rows (get-visible-rows project column-name request)
          results (glossary/find-concordances rows keyword context-size case-sensitive)
          total-matches (count results)
          json-results (JSONArray.)]
      (doseq [{:keys [row left keyword right]} results]
        (let [obj (JSONObject.)]
          (.put obj "row" row)
          (.put obj "left" left)
          (.put obj "keyword" keyword)
          (.put obj "right" right)
          (.put json-results obj)))
      (let [response-obj (JSONObject.)]
        (.put response-obj "status" "ok")
        (.put response-obj "total" total-matches)
        (.put response-obj "results" json-results)
        (.println (.getWriter response) (.toString response-obj))))
    (catch Exception e
      (let [error-obj (JSONObject.)]
        (.put error-obj "status" "error")
        (.put error-obj "message" (.getMessage e))
        (.println (.getWriter response) (.toString error-obj))))))
```

**Note:** The exact OpenRefine API classes (`Engine`, `RowVisitor`, `ProjectManager`) and their method signatures may differ between OpenRefine versions. Verify against your OpenRefine 3.10.x source. The key interfaces are:
- `Engine` constructed with project + JSON config
- `getAllFilteredRows` with a `RowVisitor`
- `Project.getColumnModel().getColumnByName()`
- `Column.getCellIndex()`
- `Row.getCell(index)`

- [ ] **Step 2: Build and verify**

Run:
```bash
lein clean && lein jar
```
Expected: Build succeeds. If there are compile errors due to OpenRefine API differences, adjust import paths and method calls to match the actual 3.10.x API.

- [ ] **Step 3: Commit**

```bash
git add src/loupe/commands/concordance.clj
git commit -m "feat: implement concordance command with OpenRefine project access"
```

---

## Task 12: Concordance Dialog — JavaScript

Create the concordance dialog UI that opens from the menu, collects parameters, calls the command, and renders the KWIC table.

**Files:**
- Create: `extension/module/scripts/concordance-dialog.js`
- Create: `extension/module/styles/concordance-dialog.css`
- Modify: `extension/module/MOD-INF/controller.js` (load new resources)
- Modify: `extension/module/scripts/loupe.js` (add menu item)

- [ ] **Step 1: Create concordance dialog JavaScript**

Create `extension/module/scripts/concordance-dialog.js`:

```javascript
function LoupeConcordanceDialog(column) {
  this._column = column;
  this._createDialog();
}

LoupeConcordanceDialog.prototype._createDialog = function () {
  var self = this;
  var frame = $(DOM.loadHTML("loupe", "scripts/concordance-dialog.html"));

  this._elmts = DOM.bind(frame);
  this._elmts.columnName.text(this._column.name);
  this._elmts.contextSize.val("3");

  this._elmts.searchButton.click(function () {
    self._doSearch();
  });

  this._elmts.keywordInput.keypress(function (e) {
    if (e.which === 13) {
      self._doSearch();
    }
  });

  this._elmts.closeButton.click(function () {
    DialogSystem.dismissUntil(self._level - 1);
  });

  this._level = DialogSystem.showDialog(frame);
  this._elmts.keywordInput.focus();
};

LoupeConcordanceDialog.prototype._doSearch = function () {
  var self = this;
  var keyword = this._elmts.keywordInput.val().trim();
  if (!keyword) return;

  var contextSize = parseInt(this._elmts.contextSize.val()) || 3;
  var caseSensitive = this._elmts.caseSensitiveCheck.is(":checked");

  this._elmts.statusText.text("Searching...");
  this._elmts.resultsBody.empty();

  var engine = ui.browsingEngine.getJSON();

  $.post(
    "command/loupe-concordance",
    {
      project: theProject.id,
      columnName: this._column.name,
      keyword: keyword,
      contextSize: contextSize,
      caseSensitive: caseSensitive,
      engine: JSON.stringify(engine)
    },
    function (data) {
      if (data.status === "ok") {
        self._renderResults(data.results, data.total);
      } else {
        self._elmts.statusText.text("Error: " + (data.message || "Unknown error"));
      }
    },
    "json"
  ).fail(function () {
    self._elmts.statusText.text("Request failed.");
  });
};

LoupeConcordanceDialog.prototype._renderResults = function (results, total) {
  var tbody = this._elmts.resultsBody;
  tbody.empty();

  if (results.length === 0) {
    this._elmts.statusText.text("No matches found.");
    return;
  }

  var statusMsg = "Showing " + results.length + " of " + total + " matches";
  if (results.length < total) {
    statusMsg += " (limited)";
  }
  this._elmts.statusText.text(statusMsg);

  for (var i = 0; i < results.length; i++) {
    var r = results[i];
    var tr = $("<tr></tr>");
    tr.append($("<td></td>").addClass("concordance-row-num").text(r.row + 1));
    tr.append($("<td></td>").addClass("concordance-left").text(r.left));
    tr.append($("<td></td>").addClass("concordance-keyword").text(r.keyword));
    tr.append($("<td></td>").addClass("concordance-right").text(r.right));
    tbody.append(tr);
  }
};
```

- [ ] **Step 2: Create concordance dialog HTML template**

Create `extension/module/scripts/concordance-dialog.html`:

```html
<div class="dialog-frame concordance-dialog">
  <div class="dialog-header">Concordance: <span bind="columnName"></span></div>
  <div class="dialog-body">
    <div class="concordance-controls">
      <label>Keyword:
        <input type="text" bind="keywordInput" size="30" />
      </label>
      <label>Context:
        <select bind="contextSize">
          <option value="1">1</option>
          <option value="2">2</option>
          <option value="3" selected>3</option>
          <option value="4">4</option>
          <option value="5">5</option>
        </select>
        words each side
      </label>
      <label>
        <input type="checkbox" bind="caseSensitiveCheck" />
        Case sensitive
      </label>
    </div>
    <div class="concordance-actions">
      <button class="button" bind="searchButton">Search</button>
      <button class="button" bind="closeButton">Close</button>
    </div>
    <div class="concordance-status" bind="statusText"></div>
    <div class="concordance-results-container">
      <table class="concordance-table">
        <thead>
          <tr>
            <th>Row</th>
            <th class="concordance-left">Left Context</th>
            <th>Keyword</th>
            <th class="concordance-right">Right Context</th>
          </tr>
        </thead>
        <tbody bind="resultsBody"></tbody>
      </table>
    </div>
  </div>
</div>
```

- [ ] **Step 3: Create concordance CSS**

Create `extension/module/styles/concordance-dialog.css`:

```css
.concordance-dialog {
  min-width: 700px;
}

.concordance-controls {
  display: flex;
  gap: 16px;
  align-items: center;
  margin-bottom: 8px;
}

.concordance-controls label {
  display: flex;
  align-items: center;
  gap: 4px;
}

.concordance-actions {
  margin-bottom: 8px;
}

.concordance-actions .button {
  margin-right: 8px;
}

.concordance-status {
  margin-bottom: 8px;
  font-style: italic;
  color: #666;
}

.concordance-results-container {
  max-height: 400px;
  overflow-y: auto;
  border: 1px solid #ddd;
}

.concordance-table {
  width: 100%;
  border-collapse: collapse;
  font-family: monospace;
  font-size: 12px;
}

.concordance-table th,
.concordance-table td {
  padding: 4px 8px;
  border-bottom: 1px solid #eee;
}

.concordance-table th {
  background: #f5f5f5;
  position: sticky;
  top: 0;
}

.concordance-row-num {
  color: #999;
  text-align: right;
  white-space: nowrap;
}

td.concordance-left {
  text-align: right;
  color: #555;
}

td.concordance-keyword {
  font-weight: bold;
  text-align: center;
  white-space: nowrap;
}

td.concordance-right {
  text-align: left;
  color: #555;
}
```

- [ ] **Step 4: Register dialog resources in controller.js**

In `controller.js`, add the CSS and dialog JS to the resource loading. Add after the existing `ClientSideResourceManager.addPaths("project/scripts", ...)` block:

```javascript
  ClientSideResourceManager.addPaths(
    "project/scripts",
    module,
    [
      "scripts/loupe.js",
      "scripts/concordance-dialog.js"
    ]
  );

  ClientSideResourceManager.addPaths(
    "project/styles",
    module,
    [
      "styles/concordance-dialog.css"
    ]
  );
```

Note: this replaces the existing `addPaths` call for scripts (which currently only loads `loupe.js`).

- [ ] **Step 5: Add concordance menu item to loupe.js**

In `extension/module/scripts/loupe.js`, add the concordance menu item at the end of the "Loupe: Glossary & Index" submenu array, after the trigrams item:

```javascript
        {},
        {
          id: "loupe/concordance",
          label: "Concordance...",
          click: function() {
            new LoupeConcordanceDialog(column);
          }
        }
```

- [ ] **Step 6: Build, install, test**

Run:
```bash
make install
```
Restart OpenRefine. Open a project. Click column → Facet → Loupe: Glossary & Index → Concordance...

Verify:
- Dialog opens with column name in title
- Can type keyword and click Search
- KWIC table shows results with row numbers, left context, keyword (bold), right context
- Context size dropdown works
- Case-sensitive checkbox works
- Close button dismisses dialog

- [ ] **Step 7: Commit**

```bash
git add extension/module/scripts/concordance-dialog.js extension/module/scripts/concordance-dialog.html extension/module/styles/concordance-dialog.css extension/module/MOD-INF/controller.js extension/module/scripts/loupe.js
git commit -m "feat: add concordance dialog with KWIC table"
```

---

## Task 13: Final Integration and README Update

Final verification, update README, and clean up.

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Run all unit tests**

Run:
```bash
lein test
```
Expected: All tests PASS.

- [ ] **Step 2: Full build test**

Run:
```bash
lein clean && make extension
```
Expected: Build succeeds, `extension/module/MOD-INF/lib/loupe.jar` present with all glossary + concordance classes.

- [ ] **Step 3: Update README usage section**

In `README.md`, update the Usage section. Replace the existing menu group list with:

```markdown
Click any column header → Facet. Four Loupe submenu groups provide text quality checks and linguistic analysis:

- **Loupe: Quality** — composite checks, encoding issues, whitespace analysis, normalization status, reconciliation readiness
- **Loupe: Scripts & Characters** — script detection, directionality, diacritics analysis, case patterns
- **Loupe: Content Analysis** — punctuation, quotes, numeric systems, emoji, word stats, ligatures, character categories
- **Loupe: Glossary & Index** — word index by prefix, word/bigram/trigram glossary, KWIC concordance
```

Also update the status line in the README header from "31 text quality checks" to reflect the new count (31 + 9 = 40 items):

```markdown
> **Status:** v0.2 development. 40 text quality checks and linguistic analysis tools across encoding, scripts, normalization, diacritics, casing, punctuation, content, and glossary/index.
```

- [ ] **Step 4: Commit**

```bash
git add README.md
git commit -m "docs: update README with glossary, index, and concordance features"
```

---

## Dependency Summary

```
Task 1 (spike: multi-valued facets) → informs Task 3, 5 implementation details
Task 2 (index tests) → Task 3 (index impl)
Task 4 (glossary tests) → Task 5 (glossary impl)
Task 3, 5 → Task 6 (build config)
Task 6 → Task 7 (menu integration)
Task 8 (spike: command registration) → Tasks 9-12 (concordance)
Task 9 (concordance tests) → Task 10 (concordance impl)
Task 10 → Task 11 (command impl)
Task 11 → Task 12 (dialog JS/CSS)
Task 7, 12 → Task 13 (integration + README)
```

Parallel tracks possible:
- Tasks 2-7 (index/glossary) and Task 8 (concordance spike) can run in parallel
- Tasks 9-12 (concordance) depend on Task 8 spike result
