# Loupe Phase 0 Design: De-risk the Clojure Binding

**Date:** 2026-06-20
**Scope:** Phase 0 only — spike the binding mechanism, exit with "hello-loupe" drop-in extension
**Exit criterion:** Clicking a column menu item in OpenRefine runs one real encoding-issues check from a bundled Clojure jar, installed by dropping the extension folder into `extensions/` and restarting.

## Context

Loupe is an OpenRefine extension that gives non-technical users a one-click quality report on text columns. The core logic is Clojure. A mature library of facet expressions already exists (`OpenRefine Data Quality Facets.md`).

Phase 0 answers the central open question: can OpenRefine's Clojure evaluator call a function from a namespace in an AOT-compiled jar bundled with the extension?

## Environment

- OpenRefine 3.10.1, JDK 11.0.30 (macOS)
- OpenRefine master bundles Clojure 1.12.5
- Extensions path: `~/Library/Application Support/OpenRefine/extensions`

## Binding Strategy: B First, Fallback Ladder

**Primary (Binding B):** Bundled namespace + injected Clojure expression.
- Ship AOT-compiled `loupe.jar` in `MOD-INF/lib/`
- Menu command creates a text facet with pre-filled Clojure expression:
  ```clojure
  (do (require 'loupe.checks) (loupe.checks/encoding-issues value))
  ```
- OpenRefine's Clojure evaluator runs it per-cell

**Fallback ladder if B fails:**
1. Debug classloader — maybe fixable
2. Binding A — GREL `gen-class` implementing `Function` interface
3. Java shim class that calls Clojure runtime
4. Binding C — pure JS, paste expression strings, no jar

## Project Structure

```
loupe/
  deps.edn
  build.clj
  Makefile
  src/
    loupe/
      checks.clj              # ONE fn: encoding-issues. Pure String -> String.
  test/
    loupe/
      checks_test.clj
  extension/
    module/
      MOD-INF/
        module.properties
        controller.js
        lib/                   # loupe.jar (gitignored)
      scripts/
        loupe.js
      styles/                  # empty for Phase 0
```

## The Check Function

`loupe.checks/encoding-issues` — single pure function.

**Signature:** `String -> String`
- Input: single cell value. Null/blank returns `"EMPTY"`.
- Output: pipe-separated issue labels (e.g. `"REPLACEMENT_CHAR|MOJIBAKE|BOM"`) or `"CLEAN"`.

String output because OpenRefine text facets group by string values — users see a clickable list of issue types in the facet sidebar.

**Checks performed (ported from v2 facet library):**
- `REPLACEMENT_CHAR` — U+FFFD present
- `CONTROL_CHAR` — U+0000-U+001F, U+007F-U+009F (excluding tab U+0009, newline U+000A, carriage return U+000D)
- `BOM` — U+FEFF present
- `INVISIBLE_FORMATTER` — zero-width spaces (U+200B), zero-width non-joiner/joiner (U+200C-U+200D), directional marks (U+200E-U+200F), line/paragraph separators (U+2028-U+2029), directional embeddings (U+202A-U+202E), word joiner (U+2060), invisible formatters (U+2061-U+2069)
- `MOJIBAKE` — common UTF-8-decoded-as-Latin-1 patterns: Ã followed by specific byte ranges

**Dependencies:** `clojure.core` and `java.lang.Character` only. No OpenRefine imports.

## Spikes

### Spike 1: Can the evaluator `require` from a classpath jar?
- Build `loupe.jar` with AOT-compiled `loupe.checks`
- Place in `MOD-INF/lib/`
- Create facet with `(do (require 'loupe.checks) (loupe.checks/encoding-issues value))`
- Observe: does it work, error, or silently fail?
- Document exact error if it fails (classloader, security, namespace resolution)

### Spike 2: Clojure version compatibility
- Check actual Clojure jar version in OpenRefine 3.10.1's `lib/` folder
- Compile `loupe.jar` against that version (provided dependency, not bundled)
- Confirm no version clash at runtime

### Spike 3: Menu command creates facet with zero user typing
- `loupe.js` adds item to column header menu
- Click triggers facet creation with pre-filled Clojure expression
- User sees text facet appear in sidebar immediately

## Build System

### `deps.edn`
- `:paths` — `["src"]`
- `:deps` — `org.clojure/clojure` pinned to OpenRefine's bundled version (1.12.x). Provided dependency (not included in jar).
- `:aliases` — `:test` (test runner), `:build` (tools.build for AOT + jar)

### `build.clj`
- AOT-compiles `loupe.checks`
- Produces `loupe.jar` (classes only, no Clojure runtime)

### Makefile targets
- `make jar` — AOT compile `loupe.checks` and produce `loupe.jar`
- `make test` — run `checks_test.clj`
- `make extension` — copy `loupe.jar` into `extension/module/MOD-INF/lib/`
- `make install` — copy extension tree into `~/Library/Application Support/OpenRefine/extensions/loupe/`
- `make clean` — remove `target/`, `.cpcache/`, built jars
- `make zip` — zip extension tree for distribution

### Install flow
1. `make test` — verify check fn works standalone
2. `make extension` — build jar, assemble extension tree
3. `make install` — drop into OpenRefine extensions folder
4. Restart OpenRefine
5. Open project, column menu, "Loupe: Encoding Issues", facet appears

## Extension Client Side

### `module.properties`
```
name=loupe
requires=core
```

### `controller.js`
- Registers client-side scripts (`loupe.js`)
- No server commands for Phase 0

### `loupe.js`
- Hooks into column header menu via `DataTableColumnHeaderUI.extendMenu`
- Adds one item: "Loupe: Encoding Issues"
- On click: creates Clojure text facet on that column with pre-filled expression
- Uses OpenRefine's facet creation API

### No custom UI for Phase 0
- Standard OpenRefine text facet handles display
- Users see facet sidebar with clickable groups: "CLEAN", "REPLACEMENT_CHAR", etc.

## .gitignore additions
```
target/
.cpcache/
extension/module/MOD-INF/lib/*.jar
dist/
```

## What Phase 0 does NOT include
- No `report.clj` (aggregation)
- No `openrefine.clj` (dedicated binding namespace)
- No `cli.clj`
- No custom dialog/panel/CSS
- No multi-check selector
- No structured return types (maps)
- No distribution zip on GitHub Releases

## Success criteria
1. `make test` passes — `encoding-issues` fn works standalone with all check types
2. `make install` produces a working drop-in extension
3. Column menu shows "Loupe: Encoding Issues"
4. Clicking it produces a text facet grouping cells by encoding issue type
5. Spike results documented: which binding mechanism works, what Clojure version, any classloader issues

## Risk: Binding B fails
If `require` doesn't work in the evaluator, document:
- Exact error message
- Root cause (classloader isolation? sandbox? namespace resolution?)
- Whether fixable or fundamental

Then proceed down fallback ladder. Phase 0 exit criterion remains the same — one check accessible via menu — but binding mechanism changes.
