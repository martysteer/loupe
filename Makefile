EXTENSION_DIR = extension/module/MOD-INF/lib
INSTALL_DIR = $(HOME)/Library/Application Support/OpenRefine/extensions/loupe

.PHONY: jar test extension install clean zip

jar:
	lein jar

test:
	lein test

extension: jar
	mkdir -p $(EXTENSION_DIR)
	cp target/loupe.jar $(EXTENSION_DIR)/loupe.jar

install: extension
	rm -rf "$(INSTALL_DIR)"
	mkdir -p "$(INSTALL_DIR)"
	cp -R extension/module/ "$(INSTALL_DIR)/"

clean:
	lein clean
	rm -f $(EXTENSION_DIR)/loupe.jar

zip: extension
	mkdir -p dist
	cd extension && zip -r ../dist/loupe.zip module/
