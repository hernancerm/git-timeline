UBERJAR_MVN_CMD := ./mvnw -Puberjar clean package
NATIVE_IMAGE_MVN_CMD := ./mvnw -Pnative clean package

.PHONY: uber
uber:
	$(UBERJAR_MVN_CMD)

.PHONY: bin
bin:
	$(NATIVE_IMAGE_MVN_CMD)

.PHONY: release
release:
	@mkdir release
	$(UBERJAR_MVN_CMD) -DskipTests
	@cp -v target/git-timeline.jar release
	$(NATIVE_IMAGE_MVN_CMD) -DskipTests
	@cp -v target/git-timeline release
