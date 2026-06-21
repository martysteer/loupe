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
