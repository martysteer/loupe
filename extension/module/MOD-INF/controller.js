var ClientSideResourceManager = Packages.com.google.refine.ClientSideResourceManager;

function init() {
  ClientSideResourceManager.addPaths(
    "project/scripts",
    module,
    [
      "scripts/loupe.js"
    ]
  );
}
