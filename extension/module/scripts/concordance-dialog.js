function LoupeConcordanceDialog(column) {
  this._column = column;
  this._resultLimit = 500;
  this._createDialog();
}

LoupeConcordanceDialog.prototype._createDialog = function() {
  var self = this;
  var frame = $(DOM.loadHTML("loupe", "scripts/concordance-dialog.html"));

  this._elmts = DOM.bind(frame);
  this._elmts.columnName.text(this._column.name);

  this._elmts.searchButton.click(function() {
    self._doSearch();
  });

  this._elmts.keywordInput.keypress(function(e) {
    if (e.which === 13) {
      self._doSearch();
    }
  });

  this._elmts.closeButton.click(function() {
    DialogSystem.dismissUntil(self._level - 1);
  });

  this._level = DialogSystem.showDialog(frame);
  this._elmts.keywordInput.focus();
};

LoupeConcordanceDialog.prototype._tokenize = function(text) {
  // Word boundary tokenization — splits on non-letter/digit boundaries,
  // keeps apostrophes within words (e.g. "it's" stays as one token).
  if (!text) return [];
  var tokens = text.match(/[\p{L}\p{N}]+(?:['\u2019][\p{L}\p{N}]+)*/gu);
  return tokens || [];
};

LoupeConcordanceDialog.prototype._doSearch = function() {
  var self = this;
  var keyword = this._elmts.keywordInput.val().trim();
  if (!keyword) return;

  var contextSize = parseInt(this._elmts.contextSize.val()) || 3;
  var caseSensitive = this._elmts.caseSensitiveCheck.is(":checked");

  this._elmts.statusText.text("Fetching rows...");
  this._elmts.resultsBody.empty();

  // Fetch visible rows using OpenRefine's get-rows command
  var engine = ui.browsingEngine.getJSON();
  this._fetchAllRows(engine, keyword, contextSize, caseSensitive);
};

LoupeConcordanceDialog.prototype._fetchAllRows = function(engine, keyword, contextSize, caseSensitive) {
  var self = this;
  var results = [];
  var start = 0;
  var batchSize = 200;
  var colName = this._column.name;
  var totalRows = theProject.rowModel.total;

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
          return;
        }

        // Find column index
        var colIndex = -1;
        for (var c = 0; c < theProject.columnModel.columns.length; c++) {
          if (theProject.columnModel.columns[c].name === colName) {
            colIndex = theProject.columnModel.columns[c].cellIndex;
            break;
          }
        }

        if (colIndex < 0) {
          self._elmts.statusText.text("Column not found.");
          return;
        }

        // Process each row
        for (var r = 0; r < data.rows.length && results.length < self._resultLimit; r++) {
          var row = data.rows[r];
          var cell = row.cells[colIndex];
          if (!cell || cell.v == null) continue;

          var text = String(cell.v);
          var words = self._tokenize(text);
          var rowIndex = row.i;

          for (var i = 0; i < words.length && results.length < self._resultLimit; i++) {
            var match = caseSensitive
              ? (words[i] === keyword)
              : (words[i].toLowerCase() === keyword.toLowerCase());

            if (match) {
              var leftStart = Math.max(0, i - contextSize);
              var rightEnd = Math.min(words.length, i + contextSize + 1);
              results.push({
                row: rowIndex,
                left: words.slice(leftStart, i).join(" "),
                keyword: words[i],
                right: words.slice(i + 1, rightEnd).join(" ")
              });
            }
          }
        }

        self._elmts.statusText.text("Searching... " + results.length + " matches so far");

        // Check if we need more rows
        if (results.length >= self._resultLimit || start + batchSize >= data.filtered) {
          self._renderResults(results, results.length);
        } else {
          start += batchSize;
          fetchBatch();
        }
      },
      "json"
    ).fail(function() {
      self._elmts.statusText.text("Request failed.");
    });
  }

  fetchBatch();
};

LoupeConcordanceDialog.prototype._renderResults = function(results, total) {
  var tbody = this._elmts.resultsBody;
  tbody.empty();

  if (results.length === 0) {
    this._elmts.statusText.text("No matches found.");
    return;
  }

  var statusMsg = "Showing " + results.length + " match" + (results.length !== 1 ? "es" : "");
  if (results.length >= this._resultLimit) {
    statusMsg += " (limited to " + this._resultLimit + ")";
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
