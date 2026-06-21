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

  this._valueCap = 20;
  this._results = {};
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
  this._elmts.statusText.text("Computing report...");
  this._elmts.generateButton.prop("disabled", true);

  // Build combos list for sequential compute-facets calls
  this._combos = [];
  for (var c = 0; c < this._selectedColumns.length; c++) {
    this._results[this._selectedColumns[c].name] = {};
    for (var f = 0; f < this._selectedFunctions.length; f++) {
      this._combos.push({
        column: this._selectedColumns[c],
        fn: this._selectedFunctions[f]
      });
    }
  }

  this._comboIndex = 0;
  var self = this;
  $.get("command/core/get-csrf-token", function(response) {
    self._csrfToken = response.token;
    self._runNextCombo();
  }, "json");
};

LoupeFieldReportDialog.prototype._runNextCombo = function() {
  var self = this;

  if (this._comboIndex >= this._combos.length) {
    this._renderReport();
    return;
  }

  var combo = this._combos[this._comboIndex];
  this._comboIndex++;

  this._elmts.statusText.text(
    "Analyzing " + combo.column.name + ": " + combo.fn.label +
    "... (" + this._comboIndex + "/" + this._combos.length + ")"
  );

  // Use compute-facets API — same code path as real facets, guarantees
  // Clojure expressions work correctly with all bindings.
  var engine = ui.browsingEngine.getJSON();
  engine.facets.push({
    "type": "list",
    "name": "__loupe_report__",
    "columnName": combo.column.name,
    "expression": combo.fn.expression,
    "omitBlank": false,
    "omitError": false,
    "selection": [],
    "selectBlank": false,
    "selectError": false,
    "invert": false
  });

  $.post(
    "command/core/compute-facets",
    {
      project: theProject.id,
      engine: JSON.stringify(engine),
      csrf_token: this._csrfToken
    },
    function(data) {
      var freqs = {};
      if (data && data.facets) {
        var ourFacet = data.facets[data.facets.length - 1];
        if (ourFacet && ourFacet.choices) {
          var totalCount = 0;
          for (var i = 0; i < ourFacet.choices.length; i++) {
            var choice = ourFacet.choices[i];
            var label = (choice.v && choice.v.l !== undefined) ? String(choice.v.l) : "(unknown)";
            freqs[label] = choice.c;
            totalCount += choice.c;
          }
          // Include blank count if present
          if (ourFacet.blankCount && ourFacet.blankCount > 0) {
            freqs["(blank)"] = ourFacet.blankCount;
            totalCount += ourFacet.blankCount;
          }
          if (ourFacet.errorCount && ourFacet.errorCount > 0) {
            freqs["(error)"] = ourFacet.errorCount;
            totalCount += ourFacet.errorCount;
          }
          self._totalRowCount = totalCount;
        }
      }
      self._results[combo.column.name][combo.fn.id] = freqs;
      self._runNextCombo();
    },
    "json"
  ).fail(function() {
    self._results[combo.column.name][combo.fn.id] = {"Error: request failed": -1};
    self._runNextCombo();
  });
};

LoupeFieldReportDialog.prototype._renderReport = function() {
  var self = this;
  this._elmts.tabBar.empty();
  this._elmts.resultsContainer.empty();
  this._elmts.generateButton.prop("disabled", false);

  if (this._selectedColumns.length === 0) {
    this._elmts.statusText.text("No results.");
    return;
  }

  var statusMsg = this._totalRowCount + " rows analyzed";
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
