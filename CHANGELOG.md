# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and as of 2.2.0 this project adheres to
[Semantic Versioning](https://semver.org/spec/v2.0.0.html). Versions up to 2.1 used a
`MAJOR.MINOR` scheme where `MINOR` covered both features and fixes.

## [Unreleased]

### Added

- Zsh completions for `git timeline`, mirroring those of `git log`.
- Prebuilt Linux arm64 and x86_64 binaries published on release, installable with Homebrew.
- A `.sha256` published next to each release download.

### Changed

- Date format separator changed from a space to a dash, e.g. `Feb-28-2026`.
- Git lookups are started at once and the unused ones are skipped.
- Hyperlinks are applied in a single pass over each line.
- Requires Java 25 to build.

### Removed

- Prebuilt macOS x86_64 binary. GraalVM 25 ships no macOS Intel build, so it cannot be
  produced. Intel Macs can still use the uber JAR.

### Fixed

- Color and paging are now disabled when the output is piped or redirected.
- Clone URLs in formats other than the common ones no longer throw `IllegalStateException`.
- Commit messages containing XML-like text no longer break parsing.
- A user supplied `--pretty`/`--format` no longer loses the git-timeline date.
- The exit code of git-log is now read only after it has exited.

## [2.1] - 2025-12-31

### Added

- Prebuilt macOS x86 binary published on release.
- MIT license.

### Fixed

- No hyperlinking attempted in repos without a remote.

## [2.0] - 2025-08-07

### Added

- Commit hashes are hyperlinked to the hosting provider.
- The pager can be set through `core.pager` in gitconfig.

### Changed

- The merge and differing-author markers are now an asterisk instead of an `M`.

### Removed

- Options `--paginate` and `-p`. Paging is on by default.
- Short form `-P` of `--no-pager`.

### Fixed

- Version-like strings in the subject line are no longer hyperlinked as issue numbers.

## [1.1] - 2025-07-31

### Changed

- The merge marker is omitted under `--graph`, where the graph already shows merges.

### Fixed

- Extra empty line printed after `--help`.

## [1.0] - 2025-07-31

Initial release.

### Added

- One-line commit format with date (`%ad`) and author (`%an`).
- Hyperlinks (OSC 8) for GitHub and Bitbucket PR and issue numbers, and for Jira keys.
- Markers for merge commits and for commits whose author and committer differ.
- Paged output, respecting `GIT_PAGER` and `PAGER`.
- Pass-through of all options and arguments to git-log, including `--graph` and `--color`.

[Unreleased]: https://github.com/hernancerm/git-timeline/compare/2.1...HEAD
[2.1]: https://github.com/hernancerm/git-timeline/compare/2.0...2.1
[2.0]: https://github.com/hernancerm/git-timeline/compare/1.1...2.0
[1.1]: https://github.com/hernancerm/git-timeline/compare/1.0...1.1
[1.0]: https://github.com/hernancerm/git-timeline/releases/tag/1.0
