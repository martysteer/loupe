var ClientSideResourceManager = Packages.com.google.refine.ClientSideResourceManager;

function init() {
  // Load all loupe.checks.* namespaces from bundled jar
  try {
    var modulePath = module.getPath();
    var jarFile = new java.io.File(modulePath, "MOD-INF/lib/loupe.jar");
    var jarUrl = jarFile.toURI().toURL();

    var parentCL = java.lang.Thread.currentThread().getContextClassLoader();
    var urls = java.lang.reflect.Array.newInstance(java.net.URL, 1);
    urls[0] = jarUrl;
    var jarCL = new java.net.URLClassLoader(urls, parentCL);

    var namespaces = [
      "loupe.checks.encoding",
      "loupe.checks.whitespace",
      "loupe.checks.normalization",
      "loupe.checks.script",
      "loupe.checks.diacritics",
      "loupe.checks.casing",
      "loupe.checks.punctuation",
      "loupe.checks.content",
      "loupe.checks.quality",
      "loupe.checks.glossary"
    ];

    for (var i = 0; i < namespaces.length; i++) {
      var initClass = namespaces[i] + "__init";
      java.lang.Class.forName(initClass, true, jarCL);
    }
  } catch (e) {
    java.lang.System.err.println("Loupe: Failed to load namespaces: " + e);
  }

  ClientSideResourceManager.addPaths(
    "project/scripts",
    module,
    [
      "scripts/loupe.js"
    ]
  );
}
