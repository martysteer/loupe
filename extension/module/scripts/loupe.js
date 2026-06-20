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
          "expression": "clojure:(do (require 'loupe.checks) (loupe.checks/encoding-issues value))"
        });
      }
    }
  ]);
});
