# Glossary, Index & Concordance Features

*Design spec for loupe OpenRefine extension — new linguistic analysis facets and concordance tool.*

---

## Summary

Add a fourth menu group "Loupe: Glossary & Index" with 8 facet menu items and 1 concordance dialog. Together, index and glossary facets let users navigate column data like a book's back matter. Concordance shows keywords in context (KWIC) for interpreting word usage.

---

## Architecture

### New files

- `src/loupe/checks/glossary.clj` — Core functions: tokenization, index, glossary, concordance
- `test/loupe/checks/glossary_test.clj` — Unit tests
- `extension/module/scripts/glossary-dialog.js` — Concordance dialog UI
- `extension/module/styles/glossary-dialog.css` — Concordance table styling

### Integration points

- `extension/module/MOD-INF/controller.js` — Register 8 facet items + 1 concordance command
- Menu group: "Loupe: Glossary & Index" (4th group after Quality, Scripts & Characters, Content Analysis)

### Tokenization

Java `BreakIterator` (`java.text.BreakIterator/getWordInstance`) handles multilingual word segmentation. Works for Latin, Arabic, Persian, CJK, mixed scripts. Default punctuation handling (strips most punctuation, keeps apostrophes in contractions). No external dependencies.

---

## Clojure Functions

### Namespace: `loupe.checks.glossary`

**Helper functions:**

- `(tokenize text)` — Uses Java BreakIterator. Returns vector of word strings. Handles multilingual text.
- `(normalize-case word case-sensitive?)` — Returns word as-is if case-sensitive, lowercase if not.

**Public facet functions (parameterized):**

- `(word-index value prefix-length case-sensitive?)` — Tokenizes value, extracts first N chars of each word. Returns a collection of unique prefixes (one facet entry per prefix). Values shorter than prefix-length return full word as prefix.
- `(word-glossary value n-gram-size case-sensitive?)` — Tokenizes value, generates n-grams. Returns a collection of n-gram strings (one facet entry per n-gram). Facet counts handle frequency. If word count < n-gram-size, returns nothing.

**Concordance function:**

- `(find-concordance rows column-name keyword context-size case-sensitive?)` — Searches visible rows for keyword. Returns vector of maps: `{:row N :left "..." :keyword "word" :right "..."}`. Capped at 500 results.

### Multi-valued facets

Facet functions return Java-compatible collections (Clojure vectors), not joined strings. OpenRefine splits collections into separate facet entries — selecting "a" in an index facet shows all cells containing a word starting with "a". GREL supports this; Clojure vectors implement `java.util.Collection`, so should work. **Spike needed to verify** Clojure collection returns work with OpenRefine's facet evaluator before full implementation.

### Design principle

Menu items are shortcuts calling parameterized functions. Users can edit the facet expression to customize parameters (e.g., change prefix-length from 1 to 3).

---

## Menu Items

New group: **Loupe: Glossary & Index** — 9 items total.

| Menu Item | Clojure Expression |
|-----------|-------------------|
| Index: First Letter | `(loupe.checks.glossary/word-index value 1 true)` |
| Index: First Letter (ignore case) | `(loupe.checks.glossary/word-index value 1 false)` |
| Index: First 2 Letters | `(loupe.checks.glossary/word-index value 2 true)` |
| Index: First 2 Letters (ignore case) | `(loupe.checks.glossary/word-index value 2 false)` |
| Glossary: Words | `(loupe.checks.glossary/word-glossary value 1 true)` |
| Glossary: Words (ignore case) | `(loupe.checks.glossary/word-glossary value 1 false)` |
| Glossary: Bigrams | `(loupe.checks.glossary/word-glossary value 2 true)` |
| Glossary: Trigrams | `(loupe.checks.glossary/word-glossary value 3 true)` |
| Concordance... | Opens concordance dialog |

---

## Concordance Dialog

### Trigger

Menu item "Concordance..." registers a command (not a facet). JS handler opens custom dialog. "..." suffix follows OpenRefine convention for dialog-opening items.

### Dialog layout

```
┌─────────────────────────────────────────┐
│ Concordance: [column name]              │
│                                         │
│ Keyword: [________________]             │
│ Context: [3 ▼] words each side          │
│ ☐ Case sensitive                        │
│                                         │
│ [Search]                [Close]         │
│                                         │
│ Showing 47 of 47 matches                │
│ ┌────┬──────────────┬───────┬─────────┐ │
│ │Row#│Left Context   │Keyword│Right    │ │
│ ├────┼──────────────┼───────┼─────────┤ │
│ │  5 │ of the great │ word  │ in this │ │
│ │ 12 │ another fine │ word  │ was used│ │
│ │ 88 │   the first  │ word  │ appears │ │
│ └────┴──────────────┴───────┴─────────┘ │
└─────────────────────────────────────────┘
```

### Data flow

1. User fills form, clicks Search
2. JS fetches visible rows in batches via OpenRefine's `get-rows` API
3. JS tokenizes each cell value using Unicode-aware word boundary regex
4. JS searches tokens for keyword match (case-sensitive or insensitive)
5. JS builds KWIC result objects with left/right context
6. JS renders KWIC table from results
7. If results >= 500, shows "limited" warning

**Note:** Concordance runs entirely client-side. Server-side gen-class Command was explored but rejected due to excessive transitive dependency requirements for AOT compilation. The Clojure `find-concordances` function exists in `loupe.checks.glossary` for future server-side migration if needed.

### KWIC table formatting

- Left context: right-aligned
- Keyword: centered, bold
- Right context: left-aligned
- Reads naturally as continuous text across columns

### Search scope

Respects active filters/facets — searches visible cells only. Aligns with what user is currently viewing.

### Case sensitivity

Checkbox in dialog. Unchecked by default (case-insensitive search).

---

## Error Handling & Edge Cases

### Tokenization

- Empty/blank cells: skip, return empty string for facets
- Single character values: index returns that character, glossary returns word
- Values shorter than prefix-length: return full word as prefix
- N-gram size > word count: return nothing (no partial n-grams)

### Concordance

- Keyword not found: show "No matches found" in dialog
- Keyword at start/end of cell: partial context (fewer words on that side)
- Empty keyword: Search button disabled
- 0 visible rows: show "No visible rows to search"

### Performance

- Concordance capped at 500 results
- BreakIterator reused per cell (not per word)
- Facets are per-cell, OpenRefine handles aggregation — no special optimization needed

### No stopword filtering

Users filter facet results manually. Keeps code simple and avoids language-specific stopword lists.

---

## Testing

### Unit tests: `test/loupe/checks/glossary_test.clj`

- `tokenize` — Latin text, Arabic text, mixed scripts, punctuation stripping, empty input
- `word-index` — prefix lengths 1/2/3, case sensitivity, values shorter than prefix
- `word-glossary` — unigrams, bigrams, trigrams, case sensitivity, fewer words than n-gram size
- `find-concordance` — keyword at start/middle/end of text, case sensitivity, context window sizes, result limit cap, no matches

No OpenRefine needed — follows existing pattern where `loupe.checks.*` tests run standalone via `make test`.

### Manual testing

- Concordance dialog in running OpenRefine with multilingual data
- Verify BreakIterator handles Arabic, Persian, CJK word boundaries
- Verify facet expressions are editable by user (change parameters, re-run)

---

## Future Work (out of scope)

- Right-click glossary facet → "Show concordance" (pre-fill keyword)
- **Tokenization cache via overlay model** — Store pre-tokenized column data as an OpenRefine overlay model (like reconciliation data). Would avoid re-tokenizing on repeated concordance searches. Include "Clear cache" menu option and auto-invalidation on cell edits. Add if performance becomes a problem on large datasets.
- Stopword filtering
- Export concordance results
- Frequency statistics beyond facet counts
