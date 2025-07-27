.PHONY: uber
uber:
	./mvnw -Puberjar clean package

.PHONY: bin
bin:
ifndef GRAALVM_HOME
	$(error Cannot build native image. The environment variable GRAALVM_HOME is undefined. \
	Download GraalVM for Java 21 and set the variable: https://www.graalvm.org/downloads)
endif
	./mvnw -Pnative clean package