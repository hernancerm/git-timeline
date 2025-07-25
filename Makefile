target/git-timeline: check-graalvm-home
	./mvnw -Pnative package

.PHONY: check-graalvm-home
check-graalvm-home:
ifndef GRAALVM_HOME
	$(error Cannot build native image. The environment variable GRAALVM_HOME is undefined. \
	Download GraalVM for the app Java version and set the variable: https://www.graalvm.org/downloads)
endif