# Loupe Phase 1 Design: Port v2 Facet Library

**Date:** 2026-06-20
**Scope:** Port all 16 categories (all variants) from the v2 facet library into tested Clojure namespaces. Wire up submenu groups in OpenRefine. No report.clj.
**Exit criterion:** `make test` green with full coverage of all 30 functions. OpenRefine column menu shows 3 submenu groups with all checks accessible. Behavioural parity with `OpenRefine Data Quality Facets.md`.

## Context

Phase 0 validated the Clojure binding mechanism. One function (`encoding-issues`) works end-to-end. Phase 1 ports the entire v2 facet library — 16 categories, ~30 functions — into tested, reusable Clojure namespaces, and wires them into the OpenRefine column menu as organized submenu groups.

## Namespace Structure

```
src/loupe/checks/
  encoding.clj        # encoding issues (moved from checks.clj)
  whitespace.clj       # whitespace analysis
  normalization.clj    # unicode normalization
  script.clj           # script detection, directionality
  diacritics.clj       # diacritics analysis
  casing.clj           # case patterns
  punctuation.clj      # punctuation analysis, quote styles
  content.clj          # numerics, emoji, words, ligatures, length, char categories
  quality.clj          # composite: all-in-one, grade, score, fresh-hell, recon, autopsy
```

Each namespace depends only on `clojure.string` and `java.*` interop. `quality.clj` additionally depends on other `loupe.checks.*` namespaces for composition.

The old `src/loupe/checks.clj` is deleted. Its `encoding-issues` function moves to `src/loupe/checks/encoding.clj` with namespace `loupe.checks.encoding`.

## Functions by Namespace

### encoding.clj
- `encoding-issues` — detect replacement chars, control chars, BOM, invisible formatters, mojibake. Returns pipe-separated issue labels or "CLEAN". (Moved from Phase 0, unchanged.)

### whitespace.clj
- `whitespace-types` — categorize whitespace: normal spaces only, non-breaking space, thin space, zero-width space, special Unicode spaces. Returns descriptive string.

### normalization.clj
- `normalization-status` — check NFC/NFD status. Returns "ASCII/Simple", "NFC normalized", "NFD normalized", or "Non-normalized".

### script.clj
- `primary-script` — detect dominant script: Chinese/CJK, Hiragana, Katakana, Korean, Cyrillic, Greek, Arabic, Hebrew, Thai, Devanagari, Georgian, Armenian, Ethiopic, Latin, Other/Unknown.
- `mixed-scripts` — detect multi-script content. Returns "Mixed: Latin+Cyrillic" or single script name.
- `directionality` — LTR, RTL (Arabic), RTL (Hebrew), Bidirectional (mixed).

### diacritics.clj
- `unique-marks` — extract unique combining marks (NFD decomposed). Returns sorted marks or "none".
- `diacritic-letters` — extract letters with diacritics (U+00C0–U+00FF range). Returns sorted or "none".
- `non-ascii-chars` — all non-ASCII characters. Returns sorted unique or "ASCII only".
- `diacritic-type` — categorize: "no diacritics", "combining diacritics", "other marks".
- `diacritic-count` — count of combining marks as string (for facet grouping).

### casing.clj
- `case-pattern` — ALL UPPERCASE, all lowercase, Title case, CamelCase, mixedCase, Mixed/Other.
- `case-complexity` — percentage upper as string, e.g., "45% upper" or "no letters".
- `word-case-pattern` — per-word pattern, e.g., "Title-lower-UPPER".

### punctuation.clj
- `unique-punctuation` — sorted unique punctuation chars or "no punctuation".
- `quote-style` — Smart/typographic quotes, Straight quotes, CJK quotes, No quotes.

### content.clj
- `numeric-system` — Arabic-Indic, Extended Arabic-Indic, Devanagari, Thai, CJK, Roman, Western/ASCII, No numerals.
- `emoji-summary` — count + first 5 emoji, or "no emoji".
- `word-stats` — "words:N avg:X.X".
- `ligatures` — detected ligature chars or "no ligatures".
- `char-count` — character count as string.
- `byte-count` — UTF-8 byte count as string.
- `string-shape` — abstract pattern: A for uppercase, a for lowercase, 0 for digit, _ for whitespace.
- `char-category-breakdown` — Unicode category counts, e.g., "Lu:3 Ll:12 Zs:2".
- `dominant-char-type` — Letter-dominant, Digit-dominant, Punctuation-dominant, Symbol-dominant, Mixed.

### quality.clj (composite — calls other namespaces)
- `all-issues` — runs 27+ individual checks, returns pipe-separated issue labels or "CLEAN". Composes from individual check functions rather than duplicating logic.
- `quality-score` — numeric 0-100 with weighted penalties (critical ×10, major ×5, minor ×2). Returned as string.
- `quality-grade` — A (Clean), B (Cosmetic), C (Minor), D (Major), F (Critical).
- `fresh-hell` — humorous anomaly detector: GHOST DATA, ENCODING MASSACRE, MOJIBAKE EXPLOSION, GROUNDHOG DAY, ZALGO TEXT, INVISIBLE INK, WALL OF TEXT, STUTTER MODE, CALM DOWN, TEST DATA LEAK.
- `reconciliation-ready` — pre-reconciliation validation: needs-trim, needs-normalization, needs-whitespace-fix, has-invisible-chars, etc. Returns "RECONCILIATION READY" or "WARNING: ...".
- `autopsy-report` — full character breakdown: len, bytes, letter/digit/space/punct counts, NFC status, UTF flag.

## Function Signatures

All functions follow the same convention:

- **Input:** single cell value (String). Nil/blank returns "EMPTY" (or function-appropriate default like "none", "no punctuation").
- **Output:** String. Always string because OpenRefine text facets group by string values.
- **Dependencies:** `clojure.string` and `java.*` interop only. No OpenRefine imports. `quality.clj` calls functions from sibling namespaces.

## Menu Structure

Three submenu groups under the column header Facet menu:

```
Facet →
  ├── [existing facets...]
  ├── ─────────
  ├── Loupe: Quality →
  │     ├── All Issues
  │     ├── Quality Grade (A-F)
  │     ├── Quality Score (0-100)
  │     ├── Encoding Issues
  │     ├── Whitespace Types
  │     ├── Normalization Status
  │     ├── Reconciliation Ready
  │     ├── Fresh Hell Detector
  │     └── Autopsy Report
  ├── Loupe: Scripts & Characters →
  │     ├── Primary Script
  │     ├── Mixed Scripts
  │     ├── Directionality
  │     ├── Diacritics: Unique Marks
  │     ├── Diacritics: Letters
  │     ├── Diacritics: Non-ASCII
  │     ├── Diacritics: Type
  │     ├── Diacritics: Count
  │     ├── Case Pattern
  │     ├── Case Complexity
  │     └── Word Case Pattern
  └── Loupe: Content Analysis →
        ├── Punctuation
        ├── Quote Style
        ├── Numeric System
        ├── Emoji
        ├── Word Stats
        ├── Ligatures
        ├── Character Count
        ├── Byte Count
        ├── String Shape
        ├── Character Categories
        └── Dominant Character Type
```

Each menu item creates a text facet with expression:
```
clojure:((resolve 'loupe.checks.<namespace>/<function>) value)
```

## AOT and Classloader Wiring

**project.clj** AOT-compiles all 9 namespaces:
```clojure
:aot [loupe.checks.encoding
      loupe.checks.whitespace
      loupe.checks.normalization
      loupe.checks.script
      loupe.checks.diacritics
      loupe.checks.casing
      loupe.checks.punctuation
      loupe.checks.content
      loupe.checks.quality]
```

**controller.js** loads all init classes through the same URLClassLoader pattern from Phase 0:
```javascript
var namespaces = [
  "loupe.checks.encoding", "loupe.checks.whitespace",
  "loupe.checks.normalization", "loupe.checks.script",
  "loupe.checks.diacritics", "loupe.checks.casing",
  "loupe.checks.punctuation", "loupe.checks.content",
  "loupe.checks.quality"
];
for (var i = 0; i < namespaces.length; i++) {
  var initClass = namespaces[i].replace(/\./g, "/") + "__init";
  java.lang.Class.forName(initClass, true, jarCL);
}
```

## Testing

One test file per namespace:
```
test/loupe/checks/
  encoding_test.clj      # moved from test/loupe/checks_test.clj
  whitespace_test.clj
  normalization_test.clj
  script_test.clj
  diacritics_test.clj
  casing_test.clj
  punctuation_test.clj
  content_test.clj
  quality_test.clj
```

**Per function:** nil/blank input, clean/normal input, each detection path with Unicode fixtures, edge cases.

**Composite checks (quality.clj):** test that composite output is correct for known inputs. Don't re-test individual check logic — covered in their own test files.

**No mocking.** All pure String → String functions with string literal test fixtures.

## Migration from Phase 0

- Delete `src/loupe/checks.clj`
- Delete `test/loupe/checks_test.clj`
- Move `encoding-issues` fn to `src/loupe/checks/encoding.clj` (namespace `loupe.checks.encoding`)
- Move tests to `test/loupe/checks/encoding_test.clj` (namespace `loupe.checks.encoding-test`)
- Update `project.clj` AOT list
- Update `controller.js` to load new init classes
- Update `loupe.js` with submenu structure

## What Phase 1 does NOT include

- No `loupe.report` (per-column/dataset aggregation)
- No custom UI/dialogs
- No CLI functionality
- No GitHub Release or distribution
- No new build targets

## Success Criteria

1. `make test` passes — all ~30 functions tested across 9 test files
2. `make install` produces working drop-in extension
3. Column menu shows 3 submenu groups (Quality, Scripts & Characters, Content Analysis)
4. Each menu item creates a working text facet
5. Behavioural parity with v2 facet library for all ported checks
