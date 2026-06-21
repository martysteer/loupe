# Field Report Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a "Field Report..." dialog that lets users select columns and analysis functions, then generates a tabbed frequency report using OpenRefine's `preview-expression` API.

**Architecture:** Client-side JS dialog (same pattern as concordance). Fetches visible row indices via `get-rows`, evaluates Clojure expressions server-side via `preview-expression`, aggregates value frequencies client-side, renders tabbed results. No new Clojure code — reuses all existing functions.

**Tech Stack:** JavaScript (OpenRefine client API, jQuery, DOM.bind), HTML, CSS

---

## File Structure

| File | Responsibility |
|------|---------------|
| `extension/module/scripts/field-report-dialog.js` | Dialog constructor, function registry, presets, row fetching, preview-expression calls, result aggregation, tab rendering |
| `extension/module/scripts/field-report-dialog.html` | Dialog HTML template with bind attributes for column checkboxes, function checkboxes, preset dropdown, tabs, results area |
| `extension/module/styles/field-report-dialog.css` | Dialog layout, tab styling, frequency table styling, progress indicators |
| `extension/module/scripts/loupe.js` | Menu restructure: move concordance to new Reports group, add field report item |
| `extension/module/MOD-INF/controller.js` | Register new JS/CSS resources |

---

### Task 1: Create dialog HTML template

**Files:**
- Create: `extension/module/scripts/field-report-dialog.html`

- [ ] **Step 1: Create the HTML template**

```html
<div class="dialog-frame field-report-dialog">
  <div class="dialog-header">Loupe: Field Report</div>
  <div class="dialog-body">
    <div class="field-report-section">
      <div class="field-report-section-label">Columns: <button class="button button-small" bind="selectAllCols">All</button> <button class="button button-small" bind="selectNoCols">None</button></div>
      <div class="field-report-columns" bind="columnCheckboxes"></div>
    </div>
    <div class="field-report-section">
      <div class="field-report-section-label">
        Preset:
        <select bind="presetSelect">
          <option value="quick-scan">Quick Scan</option>
          <option value="full-audit">Full Audit</option>
          <option value="script-diacritics">Script &amp; Diacritics</option>
          <option value="custom">Custom</option>
        </select>
      </div>
      <div class="field-report-functions" bind="functionCheckboxes"></div>
    </div>
    <div class="field-report-actions">
      <button class="button" bind="generateButton">Generate Report</button>
      <button class="button" bind="closeButton">Close</button>
    </div>
    <div class="field-report-status" bind="statusText"></div>
    <div class="field-report-tabs" bind="tabBar"></div>
    <div class="field-report-results-container" bind="resultsContainer"></div>
  </div>
</div>
```

- [ ] **Step 2: Verify file created**

Run: `cat extension/module/scripts/field-report-dialog.html | head -5`
Expected: First 5 lines of the HTML template

- [ ] **Step 3: Commit**

```bash
git add extension/module/scripts/field-report-dialog.html
git commit -m "feat: add field report dialog HTML template"
```

---

### Task 2: Create dialog CSS

**Files:**
- Create: `extension/module/styles/field-report-dialog.css`

- [ ] **Step 1: Create the CSS file**

```css
.field-report-dialog {
  min-width: 800px;
}

.field-report-section {
  margin-bottom: 12px;
}

.field-report-section-label {
  font-weight: bold;
  margin-bottom: 4px;
}

.field-report-columns,
.field-report-functions {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 16px;
}

.field-report-columns label,
.field-report-functions label {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  white-space: nowrap;
}

.button-small {
  padding: 1px 6px;
  font-size: 11px;
}

.field-report-actions {
  margin-bottom: 8px;
}

.field-report-actions .button {
  margin-right: 8px;
}

.field-report-status {
  margin-bottom: 8px;
  font-style: italic;
  color: #666;
}

.field-report-tabs {
  display: flex;
  border-bottom: 2px solid #ddd;
  margin-bottom: 0;
}

.field-report-tab {
  padding: 6px 16px;
  cursor: pointer;
  border: 1px solid transparent;
  border-bottom: none;
  margin-bottom: -2px;
  font-size: 12px;
  background: #f5f5f5;
}

.field-report-tab:hover {
  background: #e8e8e8;
}

.field-report-tab.active {
  background: #fff;
  border-color: #ddd;
  border-bottom-color: #fff;
  font-weight: bold;
}

.field-report-results-container {
  max-height: 400px;
  overflow-y: auto;
  border: 1px solid #ddd;
  border-top: none;
  padding: 12px;
}

.field-report-function-block {
  margin-bottom: 16px;
}

.field-report-function-block h4 {
  margin: 0 0 4px 0;
  font-size: 13px;
  color: #333;
}

.field-report-freq-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
  margin-bottom: 4px;
}

.field-report-freq-table td {
  padding: 2px 8px;
  border-bottom: 1px solid #eee;
}

.field-report-freq-table td:first-child {
  font-family: monospace;
}

.field-report-freq-table td:nth-child(2) {
  text-align: right;
  color: #666;
  white-space: nowrap;
  width: 60px;
}

.field-report-freq-table td:nth-child(3) {
  text-align: right;
  color: #999;
  white-space: nowrap;
  width: 60px;
}

.field-report-more {
  font-size: 11px;
  color: #999;
  font-style: italic;
}

.field-report-error {
  color: #c00;
  font-style: italic;
}

.field-report-row-warning {
  background: #fff8e1;
  padding: 6px 12px;
  margin-bottom: 12px;
  border: 1px solid #ffe082;
  font-size: 12px;
}
```

- [ ] **Step 2: Commit**

```bash
git add extension/module/styles/field-report-dialog.css
git commit -m "feat: add field report dialog CSS"
```

---

### Task 3: Create dialog JS — constructor, function registry, presets

This is the first part of the dialog JS. It defines the function registry (all loupe functions with their expressions), preset definitions, and the dialog constructor that wires up the UI.

**Files:**
- Create: `extension/module/scripts/field-report-dialog.js`

- [ ] **Step 1: Create field-report-dialog.js with constructor and registry**

```javascript
// Function registry — all loupe facet functions available for reporting.
// Glossary/index functions excluded (they return collections, not single values).
var LoupeFieldReportFunctions = [
  // Quality
  { id: "all-issues", label: "All Issues", group: "Quality", expression: "clojure:((resolve 'loupe.checks.quality/all-issues) value)" },
  { id: "quality-grade", label: "Quality Grade", group: "Quality", expression: "clojure:((resolve 'loupe.checks.quality/quality-grade) value)" },
  { id: "quality-score", label: "Quality Score", group: "Quality", expression: "clojure:((resolve 'loupe.checks.quality/quality-score) value)" },
  { id: "encoding-issues", label: "Encoding Issues", group: "Quality", expression: "clojure:((resolve 'loupe.checks.encoding/encoding-issues) value)" },
  { id: "whitespace-types", label: "Whitespace Types", group: "Quality", expression: "clojure:((resolve 'loupe.checks.whitespace/whitespace-types) value)" },
  { id: "normalization", label: "Normalization Status", group: "Quality", expression: "clojure:((resolve 'loupe.checks.normalization/normalization-status) value)" },
  { id: "reconciliation-ready", label: "Reconciliation Ready", group: "Quality", expression: "clojure:((resolve 'loupe.checks.quality/reconciliation-ready) value)" },
  { id: "fresh-hell", label: "Fresh Hell Detector", group: "Quality", expression: "clojure:((resolve 'loupe.checks.quality/fresh-hell) value)" },
  { id: "autopsy", label: "Autopsy Report", group: "Quality", expression: "clojure:((resolve 'loupe.checks.quality/autopsy-report) value)" },
  // Scripts & Characters
  { id: "primary-script", label: "Primary Script", group: "Scripts", expression: "clojure:((resolve 'loupe.checks.script/primary-script) value)" },
  { id: "mixed-scripts", label: "Mixed Scripts", group: "Scripts", expression: "clojure:((resolve 'loupe.checks.script/mixed-scripts) value)" },
  { id: "directionality", label: "Directionality", group: "Scripts", expression: "clojure:((resolve 'loupe.checks.script/directionality) value)" },
  { id: "diacritics-marks", label: "Diacritics: Marks", group: "Scripts", expression: "clojure:((resolve 'loupe.checks.diacritics/unique-marks) value)" },
  { id: "diacritics-letters", label: "Diacritics: Letters", group: "Scripts", expression: "clojure:((resolve 'loupe.checks.diacritics/diacritic-letters) value)" },
  { id: "diacritics-nonascii", label: "Diacritics: Non-ASCII", group: "Scripts", expression: "clojure:((resolve 'loupe.checks.diacritics/non-ascii-chars) value)" },
  { id: "diacritics-type", label: "Diacritics: Type", group: "Scripts", expression: "clojure:((resolve 'loupe.checks.diacritics/diacritic-type) value)" },
  { id: "diacritics-count", label: "Diacritics: Count", group: "Scripts", expression: "clojure:((resolve 'loupe.checks.diacritics/diacritic-count) value)" },
  { id: "case-pattern", label: "Case Pattern", group: "Scripts", expression: "clojure:((resolve 'loupe.checks.casing/case-pattern) value)" },
  { id: "case-complexity", label: "Case Complexity", group: "Scripts", expression: "clojure:((resolve 'loupe.checks.casing/case-complexity) value)" },
  { id: "word-case", label: "Word Case Pattern", group: "Scripts", expression: "clojure:((resolve 'loupe.checks.casing/word-case-pattern) value)" },
  // Content Analysis
  { id: "punctuation", label: "Punctuation", group: "Content", expression: "clojure:((resolve 'loupe.checks.punctuation/unique-punctuation) value)" },
  { id: "quote-style", label: "Quote Style", group: "Content", expression: "clojure:((resolve 'loupe.checks.punctuation/quote-style) value)" },
  { id: "numeric-system", label: "Numeric System", group: "Content", expression: "clojure:((resolve 'loupe.checks.content/numeric-system) value)" },
  { id: "emoji", label: "Emoji", group: "Content", expression: "clojure:((resolve 'loupe.checks.content/emoji-summary) value)" },
  { id: "word-stats", label: "Word Stats", group: "Content", expression: "clojure:((resolve 'loupe.checks.content/word-stats) value)" },
  { id: "ligatures", label: "Ligatures", group: "Content", expression: "clojure:((resolve 'loupe.checks.content/ligatures) value)" },
  { id: "char-count", label: "Character Count", group: "Content", expression: "clojure:((resolve 'loupe.checks.content/char-count) value)" },
  { id: "byte-count", label: "Byte Count", group: "Content", expression: "clojure:((resolve 'loupe.checks.content/byte-count) value)" },
  { id: "string-shape", label: "String Shape", group: "Content", expression: "clojure:((resolve 'loupe.checks.content/string-shape) value)" },
  { id: "char-categories", label: "Character Categories", group: "Content", expression: "clojure:((resolve 'loupe.checks.content/char-category-breakdown) value)" },
  { id: "dominant-char-type", label: "Dominant Character Type", group: "Content", expression: "clojure:((resolve 'loupe.checks.content/dominant-char-type) value)" },
  { id: "unicode-variants", label: "Unicode Variants", group: "Content", expression: "clojure:((resolve 'loupe.checks.content/unicode-variants) value)" }
];

// Preset definitions — each maps to a set of function IDs.
var LoupeFieldReportPresets = {
  "quick-scan": [
    "quality-grade", "primary-script", "whitespace-types",
    "normalization", "unicode-variants", "encoding-issues"
  ],
  "full-audit": LoupeFieldReportFunctions.map(function(f) { return f.id; }),
  "script-diacritics": [
    "primary-script", "mixed-scripts", "directionality",
    "diacritics-type", "diacritics-count", "case-pattern"
  ]
};

function LoupeFieldReportDialog(column) {
  this._column = column;
  this._rowCap = 5000;
  this._valueCap = 20;
  this._results = {};  // { columnName: { functionId: { value: count, ... } } }
  this._createDialog();
}

LoupeFieldReportDialog.prototype._createDialog = function() {
  var self = this;
  var frame = $(DOM.loadHTML("loupe", "scripts/field-report-dialog.html"));

  this._elmts = DOM.bind(frame);

  // Build column checkboxes
  var columns = theProject.columnModel.columns;
  for (var i = 0; i < columns.length; i++) {
    var col = columns[i];
    var label = $("<label></label>");
    var cb = $("<input type='checkbox' />").attr("data-col-name", col.name);
    if (this._column && col.name === this._column.name) {
      cb.prop("checked", true);
    }
    label.append(cb).append(document.createTextNode(" " + col.name));
    this._elmts.columnCheckboxes.append(label);
  }

  // All/None buttons
  this._elmts.selectAllCols.click(function() {
    self._elmts.columnCheckboxes.find("input").prop("checked", true);
    self._updateGenerateButton();
  });
  this._elmts.selectNoCols.click(function() {
    self._elmts.columnCheckboxes.find("input").prop("checked", false);
    self._updateGenerateButton();
  });

  // Build function checkboxes
  var currentGroup = "";
  for (var j = 0; j < LoupeFieldReportFunctions.length; j++) {
    var fn = LoupeFieldReportFunctions[j];
    var fnLabel = $("<label></label>");
    var fnCb = $("<input type='checkbox' />").attr("data-fn-id", fn.id);
    fnLabel.append(fnCb).append(document.createTextNode(" " + fn.label));
    this._elmts.functionCheckboxes.append(fnLabel);
  }

  // Apply default preset
  this._applyPreset("quick-scan");

  // Preset change handler
  this._elmts.presetSelect.change(function() {
    var preset = $(this).val();
    if (preset !== "custom") {
      self._applyPreset(preset);
    }
  });

  // Track manual changes to function checkboxes
  this._elmts.functionCheckboxes.on("change", "input", function() {
    self._detectCustomPreset();
    self._updateGenerateButton();
  });

  // Track column checkbox changes
  this._elmts.columnCheckboxes.on("change", "input", function() {
    self._updateGenerateButton();
  });

  // Generate button
  this._elmts.generateButton.click(function() {
    self._generate();
  });

  // Close button
  this._elmts.closeButton.click(function() {
    DialogSystem.dismissUntil(self._level - 1);
  });

  this._level = DialogSystem.showDialog(frame);
  this._updateGenerateButton();
};

LoupeFieldReportDialog.prototype._applyPreset = function(presetName) {
  var ids = LoupeFieldReportPresets[presetName] || [];
  this._elmts.functionCheckboxes.find("input").each(function() {
    $(this).prop("checked", ids.indexOf($(this).attr("data-fn-id")) >= 0);
  });
  this._elmts.presetSelect.val(presetName);
  this._updateGenerateButton();
};

LoupeFieldReportDialog.prototype._detectCustomPreset = function() {
  var checkedIds = [];
  this._elmts.functionCheckboxes.find("input:checked").each(function() {
    checkedIds.push($(this).attr("data-fn-id"));
  });
  checkedIds.sort();

  var isCustom = true;
  var presetNames = ["quick-scan", "full-audit", "script-diacritics"];
  for (var i = 0; i < presetNames.length; i++) {
    var presetIds = LoupeFieldReportPresets[presetNames[i]].slice().sort();
    if (checkedIds.length === presetIds.length && checkedIds.join(",") === presetIds.join(",")) {
      this._elmts.presetSelect.val(presetNames[i]);
      isCustom = false;
      break;
    }
  }
  if (isCustom) {
    this._elmts.presetSelect.val("custom");
  }
};

LoupeFieldReportDialog.prototype._updateGenerateButton = function() {
  var hasCols = this._elmts.columnCheckboxes.find("input:checked").length > 0;
  var hasFns = this._elmts.functionCheckboxes.find("input:checked").length > 0;
  this._elmts.generateButton.prop("disabled", !(hasCols && hasFns));
};
```

- [ ] **Step 2: Verify file created and syntax OK**

Run: `node -c extension/module/scripts/field-report-dialog.js`
Expected: prints nothing (valid syntax) or "extension/module/scripts/field-report-dialog.js" with no errors

- [ ] **Step 3: Commit**

```bash
git add extension/module/scripts/field-report-dialog.js
git commit -m "feat: add field report dialog constructor, registry, presets"
```

---

### Task 4: Add data fetching and preview-expression evaluation

This adds the `_generate` method (the main workflow), `_fetchRowIndices` (get visible row indices via get-rows), and `_evaluateFunction` (call preview-expression for one column+function combo).

**Files:**
- Modify: `extension/module/scripts/field-report-dialog.js`

- [ ] **Step 1: Add _generate, _fetchRowIndices, and _evaluateFunction methods**

Append the following to the end of `extension/module/scripts/field-report-dialog.js`:

```javascript
LoupeFieldReportDialog.prototype._generate = function() {
  var self = this;

  // Collect selected columns
  this._selectedColumns = [];
  this._elmts.columnCheckboxes.find("input:checked").each(function() {
    var colName = $(this).attr("data-col-name");
    for (var i = 0; i < theProject.columnModel.columns.length; i++) {
      if (theProject.columnModel.columns[i].name === colName) {
        self._selectedColumns.push(theProject.columnModel.columns[i]);
        break;
      }
    }
  });

  // Collect selected functions
  this._selectedFunctions = [];
  this._elmts.functionCheckboxes.find("input:checked").each(function() {
    var fnId = $(this).attr("data-fn-id");
    for (var i = 0; i < LoupeFieldReportFunctions.length; i++) {
      if (LoupeFieldReportFunctions[i].id === fnId) {
        self._selectedFunctions.push(LoupeFieldReportFunctions[i]);
        break;
      }
    }
  });

  if (this._selectedColumns.length === 0 || this._selectedFunctions.length === 0) return;

  this._results = {};
  this._elmts.tabBar.empty();
  this._elmts.resultsContainer.empty();
  this._elmts.statusText.text("Fetching row indices...");
  this._elmts.generateButton.prop("disabled", true);

  this._fetchRowIndices(function(rowIndices, totalFiltered) {
    if (rowIndices.length === 0) {
      self._elmts.statusText.text("No visible rows to analyze.");
      self._elmts.generateButton.prop("disabled", false);
      return;
    }
    self._rowIndices = rowIndices;
    self._totalFiltered = totalFiltered;
    self._runEvaluations();
  });
};

LoupeFieldReportDialog.prototype._fetchRowIndices = function(callback) {
  var self = this;
  var engine = ui.browsingEngine.getJSON();
  var indices = [];
  var start = 0;
  var batchSize = 200;

  function fetchBatch() {
    $.post(
      "command/core/get-rows",
      {
        project: theProject.id,
        engine: JSON.stringify(engine),
        start: start,
        limit: batchSize
      },
      function(data) {
        if (!data || !data.rows) {
          self._elmts.statusText.text("Error fetching rows.");
          self._elmts.generateButton.prop("disabled", false);
          return;
        }

        for (var r = 0; r < data.rows.length; r++) {
          indices.push(data.rows[r].i);
          if (indices.length >= self._rowCap) break;
        }

        if (indices.length >= self._rowCap || start + batchSize >= data.filtered) {
          callback(indices, data.filtered);
        } else {
          start += batchSize;
          self._elmts.statusText.text("Fetching rows... " + indices.length + " so far");
          fetchBatch();
        }
      },
      "json"
    ).fail(function() {
      self._elmts.statusText.text("Error fetching rows.");
      self._elmts.generateButton.prop("disabled", false);
    });
  }

  fetchBatch();
};

LoupeFieldReportDialog.prototype._runEvaluations = function() {
  var self = this;
  var combos = [];

  // Build list of all column+function combos
  for (var c = 0; c < this._selectedColumns.length; c++) {
    for (var f = 0; f < this._selectedFunctions.length; f++) {
      combos.push({
        column: this._selectedColumns[c],
        fn: this._selectedFunctions[f]
      });
    }
  }

  var total = combos.length;
  var current = 0;

  // Initialize results structure
  for (var c2 = 0; c2 < this._selectedColumns.length; c2++) {
    this._results[this._selectedColumns[c2].name] = {};
  }

  function runNext() {
    if (current >= combos.length) {
      self._renderReport();
      return;
    }

    var combo = combos[current];
    current++;
    self._elmts.statusText.text(
      "Analyzing " + combo.column.name + ": " + combo.fn.label +
      "... (" + current + "/" + total + ")"
    );

    self._evaluateFunction(combo.column, combo.fn, function(freqs) {
      self._results[combo.column.name][combo.fn.id] = freqs;
      runNext();
    });
  }

  runNext();
};

LoupeFieldReportDialog.prototype._evaluateFunction = function(column, fn, callback) {
  var self = this;
  var freqs = {};
  var batchSize = 500;
  var batches = [];

  // Split row indices into batches
  for (var i = 0; i < this._rowIndices.length; i += batchSize) {
    batches.push(this._rowIndices.slice(i, i + batchSize));
  }

  var batchIndex = 0;

  function runBatch() {
    if (batchIndex >= batches.length) {
      callback(freqs);
      return;
    }

    var batch = batches[batchIndex];
    batchIndex++;

    $.post(
      "command/core/preview-expression",
      {
        project: theProject.id,
        cellIndex: column.cellIndex,
        rowIndices: JSON.stringify(batch),
        expression: fn.expression
      },
      function(data) {
        if (data && data.code === "ok" && data.results) {
          for (var r = 0; r < data.results.length; r++) {
            var val = data.results[r].value;
            if (val === undefined || val === null) val = "(null)";
            val = String(val);
            freqs[val] = (freqs[val] || 0) + 1;
          }
        } else if (data && data.code === "error") {
          freqs["Error: " + (data.message || "unknown")] = -1;
        }
        runBatch();
      },
      "json"
    ).fail(function() {
      freqs["Error: request failed"] = -1;
      runBatch();
    });
  }

  runBatch();
};
```

- [ ] **Step 2: Verify syntax**

Run: `node -c extension/module/scripts/field-report-dialog.js`
Expected: No errors

- [ ] **Step 3: Commit**

```bash
git add extension/module/scripts/field-report-dialog.js
git commit -m "feat: add field report data fetching and expression evaluation"
```

---

### Task 5: Add report rendering (tabs + frequency tables)

This adds `_renderReport` (builds tabs and per-column result display) and `_renderColumnResults` (renders frequency tables for each function).

**Files:**
- Modify: `extension/module/scripts/field-report-dialog.js`

- [ ] **Step 1: Add _renderReport and _renderColumnResults methods**

Append the following to the end of `extension/module/scripts/field-report-dialog.js`:

```javascript
LoupeFieldReportDialog.prototype._renderReport = function() {
  var self = this;
  this._elmts.tabBar.empty();
  this._elmts.resultsContainer.empty();
  this._elmts.generateButton.prop("disabled", false);

  if (this._selectedColumns.length === 0) {
    this._elmts.statusText.text("No results.");
    return;
  }

  var statusMsg = this._rowIndices.length + " rows analyzed";
  if (this._totalFiltered > this._rowCap) {
    statusMsg = "Showing results for " + this._rowIndices.length +
      " of " + this._totalFiltered + " visible rows";
  }
  this._elmts.statusText.text(statusMsg);

  // Create a content div for each column (hidden by default)
  this._tabContents = {};
  for (var i = 0; i < this._selectedColumns.length; i++) {
    var col = this._selectedColumns[i];
    var tab = $("<div class='field-report-tab'></div>").text(col.name);
    tab.attr("data-col-name", col.name);
    this._elmts.tabBar.append(tab);

    var content = $("<div></div>").hide();
    this._renderColumnResults(content, col.name);
    this._elmts.resultsContainer.append(content);
    this._tabContents[col.name] = content;
  }

  // Row cap warning
  if (this._totalFiltered > this._rowCap) {
    var warning = $("<div class='field-report-row-warning'></div>").text(
      "Analyzed " + this._rowIndices.length + " of " +
      this._totalFiltered + " visible rows. Apply filters to narrow the dataset."
    );
    this._elmts.resultsContainer.prepend(warning);
  }

  // Tab click handler
  this._elmts.tabBar.on("click", ".field-report-tab", function() {
    var colName = $(this).attr("data-col-name");
    self._elmts.tabBar.find(".field-report-tab").removeClass("active");
    $(this).addClass("active");
    for (var key in self._tabContents) {
      self._tabContents[key].hide();
    }
    self._tabContents[colName].show();
  });

  // Activate first tab
  this._elmts.tabBar.find(".field-report-tab").first().click();
};

LoupeFieldReportDialog.prototype._renderColumnResults = function(container, colName) {
  var colResults = this._results[colName] || {};

  for (var i = 0; i < this._selectedFunctions.length; i++) {
    var fn = this._selectedFunctions[i];
    var freqs = colResults[fn.id];

    var block = $("<div class='field-report-function-block'></div>");
    block.append($("<h4></h4>").text(fn.label));

    if (!freqs) {
      block.append($("<div class='field-report-error'></div>").text("No data"));
      container.append(block);
      continue;
    }

    // Check for error entries
    var errorKey = null;
    for (var key in freqs) {
      if (freqs[key] === -1) {
        errorKey = key;
        break;
      }
    }

    if (errorKey) {
      block.append($("<div class='field-report-error'></div>").text(errorKey));
      container.append(block);
      continue;
    }

    // Sort by count descending
    var entries = [];
    for (var val in freqs) {
      entries.push({ value: val, count: freqs[val] });
    }
    entries.sort(function(a, b) { return b.count - a.count; });

    var totalCount = 0;
    for (var e = 0; e < entries.length; e++) {
      totalCount += entries[e].count;
    }

    // Build frequency table (top N values)
    var table = $("<table class='field-report-freq-table'></table>");
    var displayCount = Math.min(entries.length, this._valueCap);
    for (var j = 0; j < displayCount; j++) {
      var entry = entries[j];
      var pct = totalCount > 0 ? ((entry.count / totalCount) * 100).toFixed(1) : "0.0";
      var tr = $("<tr></tr>");
      tr.append($("<td></td>").text(entry.value));
      tr.append($("<td></td>").text(entry.count));
      tr.append($("<td></td>").text(pct + "%"));
      table.append(tr);
    }
    block.append(table);

    // "...and N more" footer
    if (entries.length > this._valueCap) {
      block.append(
        $("<div class='field-report-more'></div>").text(
          "...and " + (entries.length - this._valueCap) + " more values"
        )
      );
    }

    container.append(block);
  }
};
```

- [ ] **Step 2: Verify syntax**

Run: `node -c extension/module/scripts/field-report-dialog.js`
Expected: No errors

- [ ] **Step 3: Commit**

```bash
git add extension/module/scripts/field-report-dialog.js
git commit -m "feat: add field report tab rendering and frequency tables"
```

---

### Task 6: Restructure menus and register resources

Move concordance from Glossary & Index to a new 5th group "Loupe: Reports", add Field Report item, and register resources in controller.js.

**Files:**
- Modify: `extension/module/scripts/loupe.js:467-491`
- Modify: `extension/module/MOD-INF/controller.js:36-51`

- [ ] **Step 1: Remove concordance from Glossary & Index submenu in loupe.js**

In `extension/module/scripts/loupe.js`, find the concordance entry at the end of the Glossary & Index submenu (lines 479-486) and the separator before it (line 478). Replace:

```javascript
        {},
        {
          id: "loupe/concordance",
          label: "Concordance...",
          click: function() {
            new LoupeConcordanceDialog(column);
          }
        }
      ]
    }
```

with:

```javascript
      ]
    },
    // Loupe: Reports submenu
    {
      id: "loupe/reports",
      label: "Loupe: Reports",
      submenu: [
        {
          id: "loupe/concordance",
          label: "Concordance...",
          click: function() {
            new LoupeConcordanceDialog(column);
          }
        },
        {
          id: "loupe/field-report",
          label: "Field Report...",
          click: function() {
            new LoupeFieldReportDialog(column);
          }
        }
      ]
    }
```

The closing `]);` and `});` remain unchanged after this block.

- [ ] **Step 2: Register field-report-dialog.js and field-report-dialog.css in controller.js**

In `extension/module/MOD-INF/controller.js`, change the scripts array (lines 39-42) from:

```javascript
    [
      "scripts/loupe.js",
      "scripts/concordance-dialog.js"
    ]
```

to:

```javascript
    [
      "scripts/loupe.js",
      "scripts/concordance-dialog.js",
      "scripts/field-report-dialog.js"
    ]
```

And change the styles array (lines 48-50) from:

```javascript
    [
      "styles/concordance-dialog.css"
    ]
```

to:

```javascript
    [
      "styles/concordance-dialog.css",
      "styles/field-report-dialog.css"
    ]
```

- [ ] **Step 3: Verify loupe.js syntax**

Run: `node -c extension/module/scripts/loupe.js`
Expected: No errors

- [ ] **Step 4: Commit**

```bash
git add extension/module/scripts/loupe.js extension/module/MOD-INF/controller.js
git commit -m "feat: add Reports menu group with concordance and field report"
```

---

### Task 7: Build extension and verify

Build the extension to verify everything compiles and packages correctly.

**Files:**
- None (build verification only)

- [ ] **Step 1: Run full test suite**

Run: `lein test`
Expected: All tests pass (36 tests, 162 assertions, 0 failures)

- [ ] **Step 2: Build extension**

Run: `make extension`
Expected: Compiles all namespaces, creates `loupe.jar`, copies to extension

- [ ] **Step 3: Verify all new files are in place**

Run: `ls -la extension/module/scripts/field-report-dialog.* extension/module/styles/field-report-dialog.css`
Expected: Lists `field-report-dialog.js`, `field-report-dialog.html`, `field-report-dialog.css`

- [ ] **Step 4: Commit (if any build artifacts changed)**

If `make extension` updated the jar:

```bash
git add extension/module/MOD-INF/lib/loupe.jar
git commit -m "build: rebuild extension with field report resources"
```

(If loupe.jar is gitignored, skip this step.)

---

### Task 8: Update documentation

Update README and OpenRefine Data Quality Facets.md to document the new report feature and menu restructure.

**Files:**
- Modify: `README.md`
- Modify: `OpenRefine Data Quality Facets.md`

- [ ] **Step 1: Update README.md**

In `README.md`, find the Usage section's menu list (around line 36-39). Change:

```markdown
- **Loupe: Glossary & Index** — word index by prefix, word/bigram/trigram glossary, KWIC concordance
```

to:

```markdown
- **Loupe: Glossary & Index** — word index by prefix, word/bigram/trigram glossary
- **Loupe: Reports** — KWIC concordance, multi-column field report
```

- [ ] **Step 2: Update OpenRefine Data Quality Facets.md**

At the end of the document, just before the final tagline (`*claudie.com...`), add a new section:

```markdown
## Field Report

**What it does:** Opens an interactive dialog where you select columns and analysis functions, then generates a tabbed frequency report across the dataset. Each column gets a tab showing value distributions for every selected function — like running multiple facets at once and seeing the results side by side.

**When to use:** Start here for a high-level overview of data quality across multiple columns. Identifies which columns have problems and what kind of problems they are, so you know where to drill down with specific facets. Available from any column header menu under Loupe: Reports.

**Presets:**
- **Quick Scan** — Quality Grade, Primary Script, Whitespace Types, Normalization, Unicode Variants, Encoding Issues
- **Full Audit** — All 33 single-value analysis functions
- **Script & Diacritics** — Primary Script, Mixed Scripts, Directionality, Diacritics Type/Count, Case Pattern

**Limits:** Analyzes up to 5,000 visible rows. Shows top 20 values per function. Respects active facets/filters.
```

- [ ] **Step 3: Commit**

```bash
git add README.md "OpenRefine Data Quality Facets.md"
git commit -m "docs: document field report feature and menu restructure"
```

---

## Self-Review Checklist

**Spec coverage:**
- [x] Menu changes: 5th group "Loupe: Reports" with concordance + field report — Task 6
- [x] New files: dialog JS, HTML, CSS — Tasks 1, 2, 3, 4, 5
- [x] Modified files: loupe.js, controller.js — Task 6
- [x] Function registry with all non-collection functions — Task 3
- [x] Presets: Quick Scan, Full Audit, Script & Diacritics, Custom — Task 3
- [x] Column selection with All/None — Task 3
- [x] Row index fetching via get-rows — Task 4
- [x] preview-expression evaluation with batching — Task 4
- [x] Sequential execution with progress — Task 4
- [x] Tabbed results with frequency tables — Task 5
- [x] Row cap 5000, value cap 20 — Tasks 4 and 5
- [x] Error handling: no columns, no functions, 0 rows, API errors — Tasks 3, 4, 5
- [x] Display-only (no click-to-filter) — Task 5
- [x] Documentation — Task 8
- [x] No Clojure changes — confirmed, no tasks touch .clj files

**Placeholder scan:** No TBD, TODO, or vague steps found.

**Type consistency:**
- `LoupeFieldReportDialog` — consistent across all tasks
- `LoupeFieldReportFunctions` — defined in Task 3, referenced in Tasks 3, 5
- `LoupeFieldReportPresets` — defined in Task 3, referenced in Task 3
- `this._results` structure `{ colName: { fnId: { value: count } } }` — set in Task 4 `_runEvaluations`, read in Task 5 `_renderColumnResults`
- `this._selectedColumns`, `this._selectedFunctions` — set in Task 4 `_generate`, read in Tasks 4, 5
- `this._rowIndices`, `this._totalFiltered` — set in Task 4 `_generate` callback, read in Tasks 4, 5
- `column.cellIndex` — used in Task 4 `_evaluateFunction`, matches OpenRefine's column model property

**Note on CSRF:** The spec mentions fetching a CSRF token via `get-csrf-token`. In practice, OpenRefine's jQuery setup includes the CSRF token automatically in `$.post` requests via cookies/headers when running in the same browser context. The concordance dialog uses `$.post` without explicit CSRF handling and works fine on OpenRefine 3.10. If CSRF becomes an issue during manual testing, add a `$.get("command/core/get-csrf-token", ...)` call at the start of `_generate` and pass the token as a parameter to `preview-expression` calls.
