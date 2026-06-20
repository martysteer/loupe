# Loupe Phase 0 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prove that an AOT-compiled Clojure namespace in a jar can be called from OpenRefine's Clojure expression evaluator, delivered as a drop-in extension with a column menu item.

**Architecture:** Single `deps.edn` Clojure project. `loupe.checks` namespace contains one pure function (`encoding-issues`). AOT-compiled to jar, placed in extension's `MOD-INF/lib/`. Client-side JS adds a column menu item that creates a text facet with a pre-filled Clojure expression calling the function.

**Tech Stack:** Clojure 1.12.x (matching OpenRefine's bundled version), tools.build for AOT/jar, Make for orchestration, OpenRefine Butterfly module system (JS/CSS).

**Spec:** `docs/superpowers/specs/2026-06-20-loupe-phase0-design.md`

---

### Task 1: Spike 2 — Determine bundled Clojure version

**Files:**
- Read: OpenRefine's bundled Clojure jar in its `lib/` or `webapp/WEB-INF/lib/` folder
- Modify: none yet

This must happen first because the Clojure version determines what we pin in `deps.edn`.

- [ ] **Step 1: Find the Clojure jar in OpenRefine's installation**

Locate OpenRefine's application bundle and find the Clojure jar:

```bash
# macOS — OpenRefine is typically in /Applications
find /Applications/OpenRefine.app -name "clojure-*.jar" 2>/dev/null
# If not found, try common alternatives:
find /Applications -name "clojure-*.jar" 2>/dev/null
# Or check if installed via Homebrew:
find /opt/homebrew -name "clojure-*.jar" 2>/dev/null
```

Record the exact version from the jar filename (e.g., `clojure-1.12.5.jar`).

- [ ] **Step 2: Document the version**

Add a note to the top of the spec or create a short spike log. The version number feeds into Task 2. If the jar is NOT Clojure 1.12.x, adjust the `deps.edn` version in Task 2 accordingly.

- [ ] **Step 3: Commit spike result**

```bash
git add -A
git commit -m "spike: document OpenRefine's bundled Clojure version"
```

---

### Task 2: Project scaffold — deps.edn, build.clj, Makefile, .gitignore

**Files:**
- Create: `deps.edn`
- Create: `build.clj`
- Create: `Makefile`
- Modify: `.gitignore`

- [ ] **Step 1: Create `deps.edn`**

```clojure
{:paths ["src"]

 :deps {org.clojure/clojure {:mvn/version "CLOJURE_VERSION_FROM_SPIKE"}}

 :aliases
 {:test
  {:extra-paths ["test"]
   :extra-deps {io.github.cognitect-labs/test-runner
                {:git/tag "v0.5.1"
                 :git/sha "dfb30dd"}}
   :main-opts ["-m" "cognitect.test-runner"]
   :exec-fn cognitect.test-runner.api/test}

  :build
  {:deps {io.github.clojure/tools.build
          {:git/tag "v0.10.6"
           :git/sha "9ea4a47"}}
   :ns-default build}}}
```

Replace `CLOJURE_VERSION_FROM_SPIKE` with the actual version found in Task 1 (e.g., `"1.12.5"`).

- [ ] **Step 2: Create `build.clj`**

```clojure
(ns build
  (:require [clojure.tools.build.api :as b]))

(def lib 'com.loupe/loupe)
(def version "0.1.0-SNAPSHOT")
(def class-dir "target/classes")
(def jar-file "target/loupe.jar")
(def basis (delay (b/create-basis {:project "deps.edn"})))

(defn clean [_]
  (b/delete {:path "target"}))

(defn jar [_]
  (clean nil)
  (b/compile-clj {:basis @basis
                   :ns-compile ['loupe.checks]
                   :class-dir class-dir})
  (b/jar {:class-dir class-dir
           :jar-file jar-file}))
```

- [ ] **Step 3: Create `Makefile`**

```makefile
EXTENSION_DIR = extension/module/MOD-INF/lib
INSTALL_DIR = $(HOME)/Library/Application Support/OpenRefine/extensions/loupe

.PHONY: jar test extension install clean zip

jar:
	clojure -T:build jar

test:
	clojure -X:test

extension: jar
	mkdir -p $(EXTENSION_DIR)
	cp target/loupe.jar $(EXTENSION_DIR)/loupe.jar

install: extension
	rm -rf "$(INSTALL_DIR)"
	mkdir -p "$(INSTALL_DIR)"
	cp -R extension/module/ "$(INSTALL_DIR)/"

clean:
	rm -rf target .cpcache
	rm -f $(EXTENSION_DIR)/loupe.jar

zip: extension
	mkdir -p dist
	cd extension && zip -r ../dist/loupe.zip module/
```

- [ ] **Step 4: Update `.gitignore`**

Append to the existing `.gitignore`:

```
target/
.cpcache/
extension/module/MOD-INF/lib/*.jar
dist/
```

- [ ] **Step 5: Verify the scaffold compiles (will fail — no source yet, that's OK)**

```bash
clojure -Sdeps '{:deps {}}' -e '(println "Clojure CLI works")'
```

Expected: prints `Clojure CLI works`. This verifies the Clojure CLI is installed. If it's not installed:

```bash
brew install clojure/tools/clojure
```

- [ ] **Step 6: Commit**

```bash
git add deps.edn build.clj Makefile .gitignore
git commit -m "scaffold: deps.edn, build.clj, Makefile, .gitignore"
```

---

### Task 3: Write failing tests for `encoding-issues`

**Files:**
- Create: `test/loupe/checks_test.clj`

- [ ] **Step 1: Create test file**

```clojure
(ns loupe.checks-test
  (:require [clojure.test :refer [deftest is testing]]
            [loupe.checks :refer [encoding-issues]]))

(deftest encoding-issues-test
  (testing "nil and blank input"
    (is (= "EMPTY" (encoding-issues nil)))
    (is (= "EMPTY" (encoding-issues "")))
    (is (= "EMPTY" (encoding-issues "   "))))

  (testing "clean string"
    (is (= "CLEAN" (encoding-issues "Hello world")))
    (is (= "CLEAN" (encoding-issues "Bibliothèque nationale")))
    (is (= "CLEAN" (encoding-issues "مكتبة"))))

  (testing "replacement character U+FFFD"
    (is (= "REPLACEMENT_CHAR" (encoding-issues "Hello \uFFFD world")))
    (is (= "REPLACEMENT_CHAR" (encoding-issues "\uFFFD"))))

  (testing "control characters (excluding tab, newline, CR)"
    (is (= "CONTROL_CHAR" (encoding-issues "Hello\u0001world")))
    (is (= "CONTROL_CHAR" (encoding-issues "\u0000")))
    (is (= "CONTROL_CHAR" (encoding-issues "text\u007Fmore")))
    (is (= "CONTROL_CHAR" (encoding-issues "text\u0085more"))))

  (testing "tab, newline, CR are NOT control char issues"
    (is (= "CLEAN" (encoding-issues "Hello\tworld")))
    (is (= "CLEAN" (encoding-issues "Hello\nworld")))
    (is (= "CLEAN" (encoding-issues "Hello\rworld"))))

  (testing "BOM U+FEFF"
    (is (= "BOM" (encoding-issues "\uFEFFHello"))))

  (testing "invisible formatters"
    (is (= "INVISIBLE_FORMATTER" (encoding-issues "Hello\u200Bworld")))
    (is (= "INVISIBLE_FORMATTER" (encoding-issues "Hello\u200Cworld")))
    (is (= "INVISIBLE_FORMATTER" (encoding-issues "Hello\u200Dworld")))
    (is (= "INVISIBLE_FORMATTER" (encoding-issues "Hello\u200Eworld")))
    (is (= "INVISIBLE_FORMATTER" (encoding-issues "Hello\u200Fworld")))
    (is (= "INVISIBLE_FORMATTER" (encoding-issues "text\u2028more")))
    (is (= "INVISIBLE_FORMATTER" (encoding-issues "text\u2029more")))
    (is (= "INVISIBLE_FORMATTER" (encoding-issues "text\u202Amore")))
    (is (= "INVISIBLE_FORMATTER" (encoding-issues "text\u202Emore")))
    (is (= "INVISIBLE_FORMATTER" (encoding-issues "text\u2060more")))
    (is (= "INVISIBLE_FORMATTER" (encoding-issues "text\u2061more")))
    (is (= "INVISIBLE_FORMATTER" (encoding-issues "text\u2069more"))))

  (testing "mojibake — UTF-8 decoded as Latin-1"
    (is (= "MOJIBAKE" (encoding-issues "Ã\u0080")))
    (is (= "MOJIBAKE" (encoding-issues "Ã\u00BF")))
    (is (= "MOJIBAKE" (encoding-issues "BibliothÃ\u00A8que"))))

  (testing "multiple issues — pipe-separated"
    (is (= "REPLACEMENT_CHAR|BOM"
           (encoding-issues "\uFEFF\uFFFD")))
    (is (= "REPLACEMENT_CHAR|CONTROL_CHAR"
           (encoding-issues "\uFFFD\u0001")))))
```

- [ ] **Step 2: Create minimal stub so tests can compile but fail**

Create `src/loupe/checks.clj`:

```clojure
(ns loupe.checks)

(defn encoding-issues
  "Detect encoding issues in a string value.
  Returns pipe-separated issue labels or \"CLEAN\".
  Returns \"EMPTY\" for nil/blank input."
  [value]
  ;; stub — tests should fail
  nil)
```

- [ ] **Step 3: Run tests — verify they fail**

```bash
cd /Users/marty/Devel/loupe && make test
```

Expected: tests compile and run, but most fail (return `nil` instead of expected strings).

- [ ] **Step 4: Commit**

```bash
git add src/loupe/checks.clj test/loupe/checks_test.clj
git commit -m "test: add failing tests for encoding-issues"
```

---

### Task 4: Implement `encoding-issues`

**Files:**
- Modify: `src/loupe/checks.clj`

- [ ] **Step 1: Implement the function**

Replace the stub in `src/loupe/checks.clj` with:

```clojure
(ns loupe.checks
  (:require [clojure.string :as str]))

(defn encoding-issues
  "Detect encoding issues in a string value.
  Returns pipe-separated issue labels or \"CLEAN\".
  Returns \"EMPTY\" for nil/blank input."
  [value]
  (if (or (nil? value) (str/blank? (str value)))
    "EMPTY"
    (let [s (str value)
          issues
          (filterv
           some?
           [(when (re-find #"\uFFFD" s)
              "REPLACEMENT_CHAR")

            (when (re-find #"[\u0000-\u0008\u000B\u000C\u000E-\u001F\u007F-\u009F]" s)
              "CONTROL_CHAR")

            (when (re-find #"\uFEFF" s)
              "BOM")

            (when (re-find #"[\u200B-\u200F\u2028-\u202E\u2060-\u2069]" s)
              "INVISIBLE_FORMATTER")

            (when (re-find #"\u00C3[\u0080-\u00BF]" s)
              "MOJIBAKE")])]
      (if (empty? issues)
        "CLEAN"
        (str/join "|" issues)))))
```

Control char regex breakdown:
- `\u0000-\u0008` — C0 controls before tab
- `\u000B\u000C` — VT and FF (between LF and CR)
- `\u000E-\u001F` — C0 controls after CR
- `\u007F-\u009F` — DEL + all C1 controls

Exclusions (tab `\u0009`, newline `\u000A`, CR `\u000D`) are gaps in the first range. The C1 range `\u007F-\u009F` has no exclusions per spec.

- [ ] **Step 2: Run tests**

```bash
cd /Users/marty/Devel/loupe && make test
```

Expected: all tests pass.

- [ ] **Step 3: If any tests fail, fix and re-run**

Common issues:
- Regex escaping differences between Clojure and Java regex
- Unicode character ranges not matching expected patterns
- Order of checks affects pipe-separated output (tests expect specific order)

The order of issues in the output matches the order of checks in the `filterv` vector: `REPLACEMENT_CHAR`, `CONTROL_CHAR`, `BOM`, `INVISIBLE_FORMATTER`, `MOJIBAKE`. The test for multiple issues (`"\uFEFF\uFFFD"` expecting `"REPLACEMENT_CHAR|BOM"`) depends on this order.

Note: `\uFEFF` (BOM) will also match the `INVISIBLE_FORMATTER` check's range `[\u200B-\u200F\u2028-\u202E\u2060-\u2069]` — no, it won't, because `\uFEFF` is outside that range. But `\uFEFF` IS checked separately as BOM. Good — no overlap issue.

- [ ] **Step 4: Commit**

```bash
git add src/loupe/checks.clj
git commit -m "feat: implement encoding-issues check function"
```

---

### Task 5: Verify AOT compilation and jar build

**Files:**
- No new files — uses existing `build.clj` and `Makefile`

- [ ] **Step 1: Build the jar**

```bash
cd /Users/marty/Devel/loupe && make jar
```

Expected: produces `target/loupe.jar` containing AOT-compiled `.class` files for `loupe.checks`.

- [ ] **Step 2: Verify jar contents**

```bash
jar tf target/loupe.jar | head -20
```

Expected output should include:
```
loupe/checks.clj
loupe/checks__init.class
loupe/checks$encoding_issues.class
```

(Exact class file names may vary based on Clojure AOT compilation output.)

- [ ] **Step 3: Verify the jar does NOT contain Clojure runtime classes**

```bash
jar tf target/loupe.jar | grep "^clojure/" | head -5
```

Expected: no output (empty). The jar should only contain `loupe/` classes.

- [ ] **Step 4: Commit (nothing to commit — just a verification step)**

No files changed. Move on.

---

### Task 6: Extension skeleton — module.properties, controller.js, loupe.js

**Files:**
- Create: `extension/module/MOD-INF/module.properties`
- Create: `extension/module/MOD-INF/controller.js`
- Create: `extension/module/scripts/loupe.js`
- Create: `extension/module/styles/` (empty directory, tracked via `.gitkeep`)

- [ ] **Step 1: Create directory structure**

```bash
mkdir -p /Users/marty/Devel/loupe/extension/module/MOD-INF/lib
mkdir -p /Users/marty/Devel/loupe/extension/module/scripts
mkdir -p /Users/marty/Devel/loupe/extension/module/styles
```

- [ ] **Step 2: Create `module.properties`**

File: `extension/module/MOD-INF/module.properties`

```properties
name = loupe
module-impl = com.google.butterfly.BasicModuleImpl
requires = core
```

- [ ] **Step 3: Create `controller.js`**

File: `extension/module/MOD-INF/controller.js`

```javascript
var ClientSideResourceManager = Packages.com.google.refine.ClientSideResourceManager;

function init() {
  ClientSideResourceManager.addPaths(
    "project/scripts",
    module,
    [
      "scripts/loupe.js"
    ]
  );
}
```

- [ ] **Step 4: Create `loupe.js`**

File: `extension/module/scripts/loupe.js`

```javascript
DataTableColumnHeaderUI.extendMenu(function (column, columnHeaderUI, menu) {
  MenuSystem.appendTo(menu, ["core/facet"], [
    {},  // separator
    {
      id: "loupe/encoding-issues",
      label: "Loupe: Encoding Issues",
      click: function () {
        ui.browsingEngine.addFacet("list", {
          "name": "Loupe: Encoding Issues \u2014 " + column.name,
          "columnName": column.name,
          "expression": "clojure:(do (require 'loupe.checks) (loupe.checks/encoding-issues value))"
        });
      }
    }
  ]);
});
```

Key details:
- `{}` before the menu item creates a visual separator in the menu
- `"clojure:"` prefix tells OpenRefine to use the Clojure expression evaluator
- `\u2014` is an em dash for the facet display name
- The `require` call loads the namespace from the classpath jar at eval time

- [ ] **Step 5: Create `.gitkeep` for empty styles directory**

```bash
touch /Users/marty/Devel/loupe/extension/module/styles/.gitkeep
```

- [ ] **Step 6: Commit**

```bash
git add extension/module/MOD-INF/module.properties \
       extension/module/MOD-INF/controller.js \
       extension/module/scripts/loupe.js \
       extension/module/styles/.gitkeep
git commit -m "feat: add OpenRefine extension skeleton with menu item"
```

---

### Task 7: Spike 1 — Build, install, test in OpenRefine

**Files:**
- No new files — uses `Makefile` targets

This is the critical spike. We find out if Binding B works.

- [ ] **Step 1: Build the extension**

```bash
cd /Users/marty/Devel/loupe && make extension
```

Expected: `target/loupe.jar` is copied to `extension/module/MOD-INF/lib/loupe.jar`.

- [ ] **Step 2: Install into OpenRefine**

```bash
cd /Users/marty/Devel/loupe && make install
```

Expected: extension tree copied to `~/Library/Application Support/OpenRefine/extensions/loupe/`.

- [ ] **Step 3: Verify installation**

```bash
ls -la "$HOME/Library/Application Support/OpenRefine/extensions/loupe/MOD-INF/lib/"
```

Expected: `loupe.jar` is present.

```bash
ls -la "$HOME/Library/Application Support/OpenRefine/extensions/loupe/MOD-INF/"
```

Expected: `module.properties`, `controller.js`, `lib/` directory all present.

```bash
ls -la "$HOME/Library/Application Support/OpenRefine/extensions/loupe/scripts/"
```

Expected: `loupe.js` is present.

- [ ] **Step 4: Restart OpenRefine and test**

1. Quit OpenRefine completely (check Activity Monitor — kill any lingering Java process)
2. Start OpenRefine
3. Open (or create) a project with some text data
4. Click a column header dropdown
5. Look for **Facet** → **Loupe: Encoding Issues**
6. Click it

**If it works:** A text facet appears in the left sidebar grouping cells by encoding issue. Celebrate. Binding B is confirmed.

**If the menu item doesn't appear:**
- Check browser console (Cmd+Option+J in Chrome) for JS errors
- Verify `controller.js` is loading — look for errors mentioning `loupe`
- Check OpenRefine's terminal output for module loading errors

**If the menu item appears but the facet errors:**
- The error message is the most important output of this spike
- Common failures:
  - `FileNotFoundException` or `ClassNotFoundException` → classloader can't see the jar
  - `Could not locate loupe/checks__init.class` → namespace not found on classpath
  - `SecurityException` → sandbox blocks `require`
- Document the exact error for the fallback decision

- [ ] **Step 5: Document spike results**

Create or update a spike log. If Binding B works, document:
- Confirmation that `require` works in the evaluator
- Clojure version confirmed compatible
- Any quirks or warnings observed

If Binding B fails, document:
- Exact error message
- Root cause analysis
- Recommendation for next step on fallback ladder

- [ ] **Step 6: Commit spike results**

```bash
git add -A
git commit -m "spike: test Binding B — Clojure namespace require from extension jar"
```

---

### Task 8: (If Binding B fails) Fallback to Binding A — GREL gen-class

**Files:**
- Create: `src/loupe/grel.clj`
- Modify: `build.clj` (add `loupe.grel` to AOT)
- Modify: `extension/module/MOD-INF/controller.js` (register GREL function)
- Modify: `extension/module/scripts/loupe.js` (change expression to GREL)

**Skip this task entirely if Binding B succeeded in Task 7.**

- [ ] **Step 1: Create GREL function wrapper**

File: `src/loupe/grel.clj`

```clojure
(ns loupe.grel
  (:require [loupe.checks :as checks])
  (:import [com.google.refine.grel Function])
  (:gen-class
   :name loupe.grel.EncodingIssues
   :implements [com.google.refine.grel.Function]))

(defn -call [this bindings args]
  (let [value (first args)]
    (checks/encoding-issues (when value (str value)))))

(defn -getDescription [this]
  "Detects encoding issues in text. Returns pipe-separated issue labels or CLEAN.")

(defn -getParams [this]
  "string value")

(defn -getReturns [this]
  "string")
```

- [ ] **Step 2: Add `loupe.grel` to AOT in `build.clj`**

In `build.clj`, change the `:ns-compile` vector:

```clojure
:ns-compile ['loupe.checks 'loupe.grel]
```

- [ ] **Step 3: Update `controller.js` to register GREL function**

```javascript
var ClientSideResourceManager = Packages.com.google.refine.ClientSideResourceManager;
var ControlFunctionRegistry = Packages.com.google.refine.grel.ControlFunctionRegistry;

function init() {
  ControlFunctionRegistry.registerFunction(
    "loupeEncodingIssues",
    new Packages.loupe.grel.EncodingIssues()
  );

  ClientSideResourceManager.addPaths(
    "project/scripts",
    module,
    [
      "scripts/loupe.js"
    ]
  );
}
```

- [ ] **Step 4: Update `loupe.js` to use GREL expression**

```javascript
DataTableColumnHeaderUI.extendMenu(function (column, columnHeaderUI, menu) {
  MenuSystem.appendTo(menu, ["core/facet"], [
    {},
    {
      id: "loupe/encoding-issues",
      label: "Loupe: Encoding Issues",
      click: function () {
        ui.browsingEngine.addFacet("list", {
          "name": "Loupe: Encoding Issues \u2014 " + column.name,
          "columnName": column.name,
          "expression": "loupeEncodingIssues(value)"
        });
      }
    }
  ]);
});
```

- [ ] **Step 5: Rebuild, reinstall, test**

```bash
cd /Users/marty/Devel/loupe && make clean && make install
```

Restart OpenRefine and test as in Task 7, Step 4.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat: fallback to Binding A — GREL gen-class for encoding-issues"
```

---

### Task 9: Manual end-to-end test with real data

**Files:**
- No new code files

- [ ] **Step 1: Create test CSV with known encoding issues**

Create a file `test-data/encoding-test.csv` (not committed — just for manual testing):

```csv
id,text
1,Clean text
2,Hello world
3,Bibliothèque nationale
4,مكتبة الإسكندرية
5,Has replacement: �
6,"Has control char: text\u0001more"
7,﻿BOM at start
8,"Zero-width: Hello​world"
9,"BibliothÃ¨que (mojibake)"
10,
```

Note: Some characters can't be represented in this plan. Create the file manually with:
- Row 5: paste a U+FFFD character
- Row 6: insert a U+0001 character
- Row 7: insert U+FEFF at the start
- Row 8: insert U+200B between Hello and world
- Row 9: type `BibliothÃ¨que` (the Ã¨ is the mojibake pattern)
- Row 10: empty cell

- [ ] **Step 2: Import into OpenRefine and run the facet**

1. Open OpenRefine
2. Create Project → choose `encoding-test.csv`
3. Import with default settings
4. On the `text` column, click dropdown → Facet → Loupe: Encoding Issues
5. Verify facet sidebar shows expected groupings

Expected facet groups:
- `CLEAN` — rows 1, 2, 3, 4
- `REPLACEMENT_CHAR` — row 5
- `CONTROL_CHAR` — row 6
- `BOM` — row 7
- `INVISIBLE_FORMATTER` — row 8
- `MOJIBAKE` — row 9
- `EMPTY` — row 10

- [ ] **Step 3: Click facet groups to verify filtering works**

Click each group in the facet sidebar. Verify the data table filters to show only matching rows.

- [ ] **Step 4: Document results**

Note any discrepancies between expected and actual results. If the facet works correctly, Phase 0 is complete.

---

### Task 10: Final cleanup and Phase 0 completion

**Files:**
- Modify: `README.md` (update status)

- [ ] **Step 1: Update README**

Add a "Current Status" section to `README.md`:

```markdown
## Current Status

**Phase 0 complete.** The Clojure binding mechanism has been validated. The extension installs as a drop-in and provides one-click encoding issue detection via column menu.

### Install (for testing)

1. Download or build `loupe.zip`
2. Unzip into `~/Library/Application Support/OpenRefine/extensions/`
3. Restart OpenRefine
4. Column dropdown → Facet → Loupe: Encoding Issues
```

- [ ] **Step 2: Run `make clean && make test && make extension` one final time**

```bash
cd /Users/marty/Devel/loupe && make clean && make test && make extension
```

Expected: all tests pass, jar builds, extension assembles.

- [ ] **Step 3: Commit**

```bash
git add README.md
git commit -m "docs: update README with Phase 0 status and install instructions"
```

- [ ] **Step 4: Tag Phase 0**

```bash
git tag -a v0.0.1-spike -m "Phase 0 complete: Clojure binding validated"
```
