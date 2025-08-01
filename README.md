# git-timeline

git-timeline is a small wrapper for git-log which improves readability.

## Features

- One-line format.
- Indication of merge commits.
- Indication of commits with differing author and committer.
- Accepts all options from git-log (including `--graph`).
- Clickable text linking to a pull-request in VCS host.
- Clickable text linking to a ticket in issue tracker.
- Paging works as expected.
- Works well on big repos.

## Constraints

- Currently, no support for listing commits from a repo in a dir different from cwd.
- Currently, should not work in Windows due to at least the pager.

## Installation

> [!NOTE]
> Currently, only macOS ARM64 is supported through Homebrew. Windows is not yet supported.
> For macOS x86 and Linux either build yourself the native image or use the uberjar from
> the releases page.

1. Add the Homebrew tap:

```text
brew tap hernancerm/formulas https://github.com/hernancerm/formulas
```

2. Install git-timeline:

```text
brew install git-timeline
```

Optional: `~/.gitconfig`: Create an alias to type `git l` instead of `git timeline`:

```text
[alias]
    l = timeline
```

## Upgrade

1. Fetch the newest version of all formulae.

```text
brew update
```

2. Upgrade git-timeline.

```text
brew upgrade git-timeline
```

## Configuration

- Date format: Use the option `--date` as defined in the
  [documentation of git-log](https://git-scm.com/docs/git-log#Documentation/git-log.txt---dateformat).
- Pager command: Use the environment variable `GIT_PAGER` or `PAGER`.

## Versioning

- `x.y`. Here is what an increment on each part means:
  - `x`: At least one breaking change is included in the release.
  - `y`: Only non-breaking changes, of any kind, are included in the release.
