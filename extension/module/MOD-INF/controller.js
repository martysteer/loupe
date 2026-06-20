var ClientSideResourceManager = Packages.com.google.refine.ClientSideResourceManager;

function init() {
  // Load loupe.checks namespace from our bundled jar.
  // Butterfly's module classloader doesn't feed into Clojure's require mechanism,
  // so we manually load the AOT-compiled init class with a URLClassLoader that
  // can see both loupe.jar and Clojure's classes. This registers the namespace
  // and its vars in Clojure's global registry.
  try {
    var modulePath = module.getPath();
    var jarFile = new java.io.File(modulePath, "MOD-INF/lib/loupe.jar");
    var jarUrl = jarFile.toURI().toURL();

    var parentCL = java.lang.Thread.currentThread().getContextClassLoader();
    var urls = java.lang.reflect.Array.newInstance(java.net.URL, 1);
    urls[0] = jarUrl;
    var jarCL = new java.net.URLClassLoader(urls, parentCL);

    java.lang.Class.forName("loupe.checks__init", true, jarCL);
  } catch (e) {
    java.lang.System.err.println("Loupe: Failed to load loupe.checks namespace: " + e);
  }

  ClientSideResourceManager.addPaths(
    "project/scripts",
    module,
    [
      "scripts/loupe.js"
    ]
  );
}
