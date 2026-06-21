# Field Report Feature

*Design spec for loupe OpenRefine extension — multi-column, multi-function overview report dialog.*

---

## Summary

Add a "Field Report..." dialog that lets users select columns and analysis functions, then generates a tabbed frequency report across the dataset. Provides a high-level overview so users know where to drill down with specific facets. Runs entirely client-side using OpenRefine's `preview-expression` API to evaluate existing Clojure functions server-side.

---

## Architecture

### Menu changes

Create 5th menu group: **Loupe: Reports** containing:
- "Concordance..." (moved from Glossary & Index)
- "Field Report..." (new)

The Glossary & Index group keeps its 8 facet items, loses only the Concordance menu item.

### New files

- `extension/module/scripts/field-report-dialog.js` — Dialog logic, API calls, result aggregation
- `extension/module/scripts/field-report-dialog.html` — Dialog HTML template
- `extension/module/styles/field-report-dialog.css` — Report table and tab styling

### Modified files

- `extension/module/scripts/loupe.js` — Restructure menus: move concordance to Reports group, add field report item
- `extension/module/MOD-INF/controller.js` — Register new JS/CSS resources
- `OpenRefine Data Quality Facets.md` — Document report feature
- `README.md` — Mention report feature

### No Clojure changes

Zero new Clojure code. All analysis reuses existing functions via the `preview-expression` API. No changes to `project.clj` or AOT compilation.

---

## Data Flow

1. User opens dialog from any column's menu (that column pre-selected; if opened from "All" column, no columns pre-selected)
2. User selects columns (checkboxes) and functions (via preset or individual checkboxes)
3. User clicks "Generate Report"
4. JS fetches visible row indices:
   - Call `get-rows` in batches of 200 (same pattern as concordance)
   - Collect `row.i` from each returned row to build index array
   - Cap at 5000 row indices; warn if more visible rows exist
5. For each column × function combo:
   - Call `preview-expression` with row indices (batched in chunks of 500), the column's `cellIndex`, and the Clojure expression string
   - Aggregate results: count frequency of each returned value
6. Render results in tabbed display

### API: preview-expression

```
POST /command/core/preview-expression
Parameters:
  project     — theProject.id
  cellIndex   — column's cellIndex (integer)
  rowIndices  — JSON array of row indices, e.g. [0, 1, 5, 12, ...]
  expression  — e.g. "clojure:((resolve 'loupe.checks.quality/quality-grade) value)"
Response:
  { "code": "ok", "results": [{"value": "A - Clean"}, {"value": "C - Minor Issues"}, ...] }
```

### CSRF token

OpenRefine 3.10+ requires a CSRF token. Fetch via `GET /command/core/get-csrf-token` before making preview calls. Pass as `csrf_token` parameter.

### Sequencing

Run column × function combos sequentially (not parallel) to avoid overwhelming OpenRefine. Update progress text after each combo completes.

---

## Dialog Layout

```
+-------------------------------------------------------------+
| Loupe: Field Report                                         |
|                                                             |
| Columns:                                                    |
| [x] Title  [x] Author  [ ] Date  [ ] Subject  [All] [None]|
|                                                             |
| Preset: [Quick Scan v]                                      |
| [x] Quality Grade    [x] Primary Script   [x] Whitespace   |
| [x] Normalization    [x] Unicode Variants [x] Encoding     |
| [ ] Case Pattern     [ ] Quote Style      [ ] All Issues   |
| [ ] Diacritics: Type [ ] Numeric System   [ ] Word Stats   |
| [ ] ...                                                     |
|                                                             |
| [Generate Report]                              [Close]      |
|                                                             |
| +------+------+------+                                      |
| |Title |Author|      |  <- tabs                             |
| +------+------+------+                                      |
| | Quality Grade         (875 rows analyzed)                 |
| |   A - Clean           723  (82.6%)                        |
| |   C - Minor Issues     98  (11.2%)                        |
| |   D - Major Issues     41   (4.7%)                        |
| |   F - Critical          13   (1.5%)                       |
| |                                                           |
| | Primary Script                                            |
| |   Latin                812  (92.8%)                       |
| |   Arabic                47   (5.4%)                       |
| |   Mixed: Latin+Arabic   16   (1.8%)                       |
| +-----------------------------------------------------------+
+-------------------------------------------------------------+
```

---

## Presets

Each preset is a named set of function selections. Selecting a preset checks/unchecks function checkboxes. User can then tweak individual checkboxes (preset dropdown shows "Custom" when user modifies selections).

### Quick Scan (default)

6 functions for a fast overview:
- Quality Grade
- Primary Script
- Whitespace Types
- Normalization Status
- Unicode Variants
- Encoding Issues

### Full Audit

All single-value functions (excludes glossary/index which return collections):
- All Quality functions: All Issues, Quality Grade, Quality Score, Encoding Issues, Whitespace Types, Normalization Status, Reconciliation Ready, Fresh Hell Detector, Autopsy Report
- All Scripts & Characters functions: Primary Script, Mixed Scripts, Directionality, all 5 Diacritics functions, Case Pattern, Case Complexity, Word Case Pattern
- All Content Analysis functions: Punctuation, Quote Style, Numeric System, Emoji, Word Stats, Ligatures, Character Count, Byte Count, String Shape, Character Categories, Dominant Character Type, Unicode Variants

### Script & Diacritics

6 functions focused on writing systems:
- Primary Script
- Mixed Scripts
- Directionality
- Diacritics: Type
- Diacritics: Count
- Case Pattern

### Custom

Shown when user modifies any preset's selections. Not a selectable preset — it's the label that appears when checkboxes don't match any named preset.

---

## Available Functions

All existing loupe facet functions that return single string values. Glossary and index functions are excluded because they return collections (vectors) which don't aggregate into simple frequency counts.

Each function is defined as:
```javascript
{ id: "quality-grade", label: "Quality Grade", expression: "clojure:((resolve 'loupe.checks.quality/quality-grade) value)" }
```

The full function registry is a JS array in `field-report-dialog.js`, one entry per existing menu item (minus glossary/index items).

---

## Result Display

### Tabs

One tab per selected column. Clicking a tab shows that column's results. First selected column's tab active by default.

### Per-function results

Each function renders as a mini frequency table:
- Function name as heading
- Row count note: "(875 rows analyzed)"  — shown once at top of tab, not per function
- Table rows: value, count, percentage — sorted by count descending
- Top 20 values shown; if more: "...and N more values" footer
- EMPTY values counted and shown like any other value

### Display-only

Results are read-only. No click-to-filter interaction.

### Progress

During generation, the results area shows progress text: "Analyzing Title: Quality Grade... (3/30)" where 30 = total column × function combos.

---

## Error Handling & Edge Cases

### Input validation

- No columns selected: "Generate Report" button disabled
- No functions selected: "Generate Report" button disabled
- Both conditions checked on change events

### Data edge cases

- 0 visible rows (all filtered out): Show "No visible rows to analyze"
- Column has all null/empty cells: Shows "EMPTY: N (100%)" in results
- preview-expression returns error for a function: Show "Error: [message]" inline for that function block, continue with remaining functions

### Row cap

- Maximum 5000 visible rows analyzed
- If more visible rows exist: warning at top of results — "Showing results for 5000 of 12,847 visible rows"
- No option to override

### Result cap

- Top 20 distinct values per function per column
- If more: "...and N more values" footer text

---

## Testing

### No unit tests needed

No new Clojure code — nothing to unit test. All analysis reuses tested functions.

### Manual testing

- Open dialog, verify column checkboxes match project columns
- Verify presets check/uncheck correct functions
- Generate report on small dataset, verify counts match manual facet application
- Test with active filters — verify only visible rows analyzed
- Test with columns containing null/empty cells
- Test row cap warning with large dataset
- Verify concordance still works after menu restructure

---

## Future Work (out of scope)

- Click result value to apply facet filter
- Export report as CSV/PDF
- Compare reports across filtered subsets
- Cache results for re-use across tab switches (currently re-generates on each "Generate Report" click — results persist until dialog closes or user generates again)
