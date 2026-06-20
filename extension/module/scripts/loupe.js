DataTableColumnHeaderUI.extendMenu(function (column, columnHeaderUI, menu) {
  // Loupe: Quality submenu
  MenuSystem.appendTo(menu, ["core/facet"], [
    {},  // separator
    {
      id: "loupe/quality",
      label: "Loupe: Quality",
      submenu: [
        {
          id: "loupe/all-issues",
          label: "All Issues",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "All Issues — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.quality/all-issues) value)"
            });
          }
        },
        {
          id: "loupe/quality-grade",
          label: "Quality Grade (A-F)",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Quality Grade — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.quality/quality-grade) value)"
            });
          }
        },
        {
          id: "loupe/quality-score",
          label: "Quality Score (0-100)",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Quality Score — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.quality/quality-score) value)"
            });
          }
        },
        {},
        {
          id: "loupe/encoding-issues",
          label: "Encoding Issues",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Encoding Issues — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.encoding/encoding-issues) value)"
            });
          }
        },
        {
          id: "loupe/whitespace-types",
          label: "Whitespace Types",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Whitespace — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.whitespace/whitespace-types) value)"
            });
          }
        },
        {
          id: "loupe/normalization",
          label: "Normalization Status",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Normalization — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.normalization/normalization-status) value)"
            });
          }
        },
        {},
        {
          id: "loupe/reconciliation-ready",
          label: "Reconciliation Ready",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Reconciliation Ready — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.quality/reconciliation-ready) value)"
            });
          }
        },
        {
          id: "loupe/fresh-hell",
          label: "Fresh Hell Detector",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Fresh Hell — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.quality/fresh-hell) value)"
            });
          }
        },
        {
          id: "loupe/autopsy",
          label: "Autopsy Report",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Autopsy — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.quality/autopsy-report) value)"
            });
          }
        }
      ]
    },
    // Loupe: Scripts & Characters submenu
    {
      id: "loupe/scripts",
      label: "Loupe: Scripts & Characters",
      submenu: [
        {
          id: "loupe/primary-script",
          label: "Primary Script",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Primary Script — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.script/primary-script) value)"
            });
          }
        },
        {
          id: "loupe/mixed-scripts",
          label: "Mixed Scripts",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Mixed Scripts — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.script/mixed-scripts) value)"
            });
          }
        },
        {
          id: "loupe/directionality",
          label: "Directionality",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Directionality — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.script/directionality) value)"
            });
          }
        },
        {},
        {
          id: "loupe/diacritics-marks",
          label: "Diacritics: Unique Marks",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Diacritics (Marks) — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.diacritics/unique-marks) value)"
            });
          }
        },
        {
          id: "loupe/diacritics-letters",
          label: "Diacritics: Letters",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Diacritics (Letters) — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.diacritics/diacritic-letters) value)"
            });
          }
        },
        {
          id: "loupe/diacritics-nonascii",
          label: "Diacritics: Non-ASCII",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Non-ASCII Chars — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.diacritics/non-ascii-chars) value)"
            });
          }
        },
        {
          id: "loupe/diacritics-type",
          label: "Diacritics: Type",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Diacritic Type — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.diacritics/diacritic-type) value)"
            });
          }
        },
        {
          id: "loupe/diacritics-count",
          label: "Diacritics: Count",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Diacritic Count — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.diacritics/diacritic-count) value)"
            });
          }
        },
        {},
        {
          id: "loupe/case-pattern",
          label: "Case Pattern",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Case Pattern — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.casing/case-pattern) value)"
            });
          }
        },
        {
          id: "loupe/case-complexity",
          label: "Case Complexity",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Case Complexity — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.casing/case-complexity) value)"
            });
          }
        },
        {
          id: "loupe/word-case",
          label: "Word Case Pattern",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Word Case — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.casing/word-case-pattern) value)"
            });
          }
        }
      ]
    },
    // Loupe: Content Analysis submenu
    {
      id: "loupe/content",
      label: "Loupe: Content Analysis",
      submenu: [
        {
          id: "loupe/punctuation",
          label: "Punctuation",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Punctuation — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.punctuation/unique-punctuation) value)"
            });
          }
        },
        {
          id: "loupe/quote-style",
          label: "Quote Style",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Quote Style — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.punctuation/quote-style) value)"
            });
          }
        },
        {},
        {
          id: "loupe/numeric-system",
          label: "Numeric System",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Numeric System — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.content/numeric-system) value)"
            });
          }
        },
        {
          id: "loupe/emoji",
          label: "Emoji",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Emoji — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.content/emoji-summary) value)"
            });
          }
        },
        {
          id: "loupe/word-stats",
          label: "Word Stats",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Word Stats — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.content/word-stats) value)"
            });
          }
        },
        {
          id: "loupe/ligatures",
          label: "Ligatures",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Ligatures — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.content/ligatures) value)"
            });
          }
        },
        {},
        {
          id: "loupe/char-count",
          label: "Character Count",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Char Count — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.content/char-count) value)"
            });
          }
        },
        {
          id: "loupe/byte-count",
          label: "Byte Count",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Byte Count — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.content/byte-count) value)"
            });
          }
        },
        {
          id: "loupe/string-shape",
          label: "String Shape",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "String Shape — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.content/string-shape) value)"
            });
          }
        },
        {},
        {
          id: "loupe/char-categories",
          label: "Character Categories",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Char Categories — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.content/char-category-breakdown) value)"
            });
          }
        },
        {
          id: "loupe/dominant-char-type",
          label: "Dominant Character Type",
          click: function() {
            ui.browsingEngine.addFacet("list", {
              "name": "Dominant Type — " + column.name,
              "columnName": column.name,
              "expression": "clojure:((resolve 'loupe.checks.content/dominant-char-type) value)"
            });
          }
        }
      ]
    }
  ]);
});
