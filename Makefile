target/git-timeline: check-graalvm-home clean
	./mvnw -Pnative package

target/git-timeline.jar: clean
	./mvnw -Puber package

.PHONY: dev
dev: target/git-timeline.jar

.PHONY: clean
clean:
	@mvn clean

.PHONY: check-graalvm-home
check-graalvm-home:
ifndef GRAALVM_HOME
	$(error Cannot build native image. The environment variable GRAALVM_HOME is undefined. \
	Download GraalVM for the app Java version and set the variable: https://www.graalvm.org/downloads)
endif