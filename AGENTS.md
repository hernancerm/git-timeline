# AGENTS.md - Coding Guidelines for git-timeline

## Project Overview

**git-timeline** is a Java 25 CLI tool that improves git-log readability by formatting and hyperlinking commits to remote repositories (GitHub, Bitbucket). It uses Maven for build/test management and GraalVM for native image compilation.

---

## Build, Lint, and Test Commands

### Compile
```bash
./mvnw clean compile
```

### Run All Tests
```bash
./mvnw clean test
```

### Run Single Test Class
```bash
./mvnw clean test -Dtest=GitLogFormatterTest
```

### Run Single Test Method
```bash
./mvnw clean test -Dtest=GitLogFormatterTest#format_givenCommitWithRemote_thenFormatCommit
```

### Build Uber JAR (executable JAR with all dependencies)
```bash
make uber
# Output: target/git-timeline.jar
# Run with: java -jar ./target/git-timeline.jar
```

### Build Native Image (requires GRAALVM_HOME set)
```bash
make bin
# Output: target/git-timeline
# Requires: GRAALVM_HOME environment variable pointing to GraalVM Java 25
```

### Full Release Build (both JAR and native binary)
```bash
make release
# Output: release/git-timeline.jar and release/git-timeline
```

---

## Code Style Guidelines

### Package Structure
- Use package `me.hernancerm.*` for all classes
- Keep package names lowercase
- One public class per file
- Related utility classes in same package

### Imports
- Import static methods for frequently used utilities:
  ```java
  import static me.hernancerm.GitRemote.Platform.GITHUB_COM;
  import static org.jline.jansi.Ansi.ansi;
  import static org.junit.jupiter.api.Assertions.*;
  ```
- Organize imports in order: java, javax, org (3rd-party), me.hernancerm
- One import per line
- Remove unused imports

### Formatting
- Use 4-space indentation (no tabs)
- Line length: reasonable limit (IDE default)
- Opening braces on same line: `public class Foo {`
- Use var for local variables with obvious types: `var isMergeCommit = true;`
- Use text blocks for multi-line strings: `"""..."""`

### Type System & Modern Java Features
- Target Java 25+ features
- Use `var` keyword for local variable type inference
- Use switch expressions where appropriate
- Use text blocks for multi-line strings
- Use records for immutable data classes when applicable
- Leverage Lombok @Data for getter/setter/equals/hashCode/toString generation

### Naming Conventions
- Classes: PascalCase (GitLogFormatter, GitTimeline)
- Methods/variables: camelCase (parseArgs, isGraphEnabled, gitLogFormatter)
- Constants: UPPER_SNAKE_CASE (NAME, VERSION, GRAALVM_HOME)
- Test classes: {ClassName}Test suffix
- Test methods: descriptive pattern like `format_givenCommitWithRemote_thenFormatCommit`

### Error Handling
- Use `Objects.requireNonNull()` for null validation:
  ```java
  Objects.requireNonNull(remote, "Cannot hyperlink when remote is null");
  ```
- Throw IllegalArgumentException or IllegalStateException for invalid arguments
- Use try-catch for IOException and checked exceptions from external processes
- Propagate unexpected exceptions unless explicitly handled
- Provide meaningful error messages in exceptions

### Testing Pattern - Given-When-Then
Follow the Given-When-Then (Arrange-Act-Assert) structure:
```java
@Test
void format_givenCommitWithRemote_thenFormatCommit() {
    // Given
    GitCommit commit = getCommit();
    
    // When
    String result = gitLogFormatter.format(commit);
    
    // Then
    assertNotNull(result);
}
```

### JUnit 5 Testing
- Use `@Test` annotation for test methods
- Use `@BeforeEach` for per-test setup
- Use assertions from `org.junit.jupiter.api.Assertions.*`
- Test class visibility: package-private (no public modifier)
- Test method visibility: package-private

### Lombok Annotations
- Use `@Data` for POJOs with all getters/setters/equals/hashCode/toString:
  ```java
  @Data
  public class GitCommit {
      private String fullHash;
      private String abbreviatedHash;
  }
  ```
- Use `@Getter` and `@Setter` selectively if @Data is too broad
- Avoid @Data on classes with custom logic; use explicit getters/setters

### Method Design
- Keep methods focused and small (< 20-30 lines preferred)
- Use meaningful method names that describe behavior
- Accept dependencies via constructor injection (not static access)
- Use private helper methods for internal logic
- Avoid long parameter lists; use builder or parameter objects for 4+ parameters

### Comments
- Add comments only for non-obvious logic
- Explain the "why", not the "what" (code should be self-explanatory)
- Use inline comments sparingly
- Document external dependencies (e.g., links to git-scm.com/docs)

---

## Project Layout

```
.
├── src/main/java/me/hernancerm/     # Source code
│   ├── App.java                       # Entry point
│   ├── GitTimeline.java               # Main CLI logic
│   ├── GitLogFormatter.java           # Output formatting
│   ├── GitLogProcessBuilder.java      # Process execution
│   ├── GitCommit.java                 # Data model
│   ├── GitRemote.java                 # Remote repo metadata
│   ├── GitLogArgs.java                # Parsed arguments
│   ├── ShellCommandParser.java        # Argument parsing
│   └── AnsiUtils.java                 # ANSI/terminal utilities
├── src/test/java/me/hernancerm/      # Tests (same structure)
├── pom.xml                            # Maven configuration
├── Makefile                           # Build shortcuts
└── README.md                          # User documentation
```

---

## Important Notes

- **Java Version**: Java 25 is required. Use SDKMAN or similar to manage versions.
- **Native Image**: Building native images with `make bin` is slower than `make uber` but produces faster executables.
- **ANSI Output**: The project uses Jansi for cross-platform ANSI color support. Always use Ansi.ansi() for coloring.
- **Hyperlinks**: Commits are formatted as terminal hyperlinks to GitHub/Bitbucket. Follow existing patterns in GitLogFormatter.
- **No External Tools**: This is a pure Java project with no external dependencies except testing and utility libraries.
