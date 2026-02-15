UBERJAR_MVN_CMD := ./mvnw -Puberjar clean package
NATIVE_IMAGE_MVN_CMD := ./mvnw -Pnative clean package

.PHONY: uber
uber:
	$(UBERJAR_MVN_CMD)

.PHONY: bin
bin:
ifndef GRAALVM_HOME
	$(error Cannot build native image. The environment variable GRAALVM_HOME is undefined. \
	Download GraalVM for Java 25 and set the variable: https://www.graalvm.org/downloads)
endif
	$(NATIVE_IMAGE_MVN_CMD)

.PHONY: release
release:
	@mkdir release
	$(UBERJAR_MVN_CMD) -DskipTests
	@cp -v target/git-timeline.jar release
	$(NATIVE_IMAGE_MVN_CMD) -DskipTests
	@cp -v target/git-timeline release

.PHONY: install-completions
install-completions:
	bash install-completions.sh

.PHONY: uninstall-completions
uninstall-completions:
	bash uninstall-completions.sh

.PHONY: install
install: uber install-completions
	@echo "✓ git-timeline installed successfully!"

.PHONY: uninstall
uninstall: uninstall-completions
	@echo "✓ git-timeline completions uninstalled"
