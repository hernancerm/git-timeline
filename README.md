<a href="https://github.com/hernancerm/git-timeline/actions/workflows/ci.yml" target="_blank">
  <img src="https://github.com/hernancerm/git-timeline/actions/workflows/ci.yml/badge.svg" />
</a>

# git-timeline

git-timeline is a **drop-in replacement** for git-log which improves the readability of its output. Example:

```
...
e8bd7843* Feb-28-2026  Hernán Cervera* Merge pull request #382 from hernancerm/xyz-8.2
a1de8921  Jan-11-2026  Hernán Cervera  docs: clean up README.md
...
```

- The commit hashes and the PR numbers are clickable (via OSC 8 hyperlinks).
- `*` appended to the commit's hash means that the commit is a merge.
- `*` appended to the author means that it differs from the committer.


## Usage

git-timeline is a **drop-in replacement** for `git log`. So `git timeline` accepts ANY option
supported by [git-log](https://git-scm.com/docs/git-log).

git-timeline has very few additional options. Learn them through `git timeline -h`.

## Features

- Commit hashes, PR numbers and issue numbers are formatted as [hyperlinks (OSC 8)](https://gist.github.com/egmontkob/eb114294efbcd5adb1944c9f3cb5feda) to the hosting provider.
- Indication of some commit characteristics: An asterisk next to the hash means the commit is a merge; next
  to the author name means the author and committer have different names.
- One-line format including date (`%ad`) and author (`%an`).
- Pass-through of all opts/args to git-log. Notable mentions:
  - [`--pretty`](https://git-scm.com/docs/git-log#Documentation/git-log.txt---prettyformat) and [`--format`](https://git-scm.com/docs/git-log#Documentation/git-log.txt---prettyformat) take over the commit format.
  - Works with [`--graph`](https://git-scm.com/docs/git-log#Documentation/git-log.txt---graph).

## Limitations

- Not tested in any way in Windows.

## Installation

> [!NOTE]
> Only macOS is supported through Homebrew. For Linux see the section below
> [Build from source](#build-from-source).

1. Add the Homebrew tap:

```text
brew tap hernancerm/formulas https://github.com/hernancerm/formulas
```

2. Install git-timeline:

```text
brew install git-timeline
```

3. Verify installation by executing in a git repo:

```
git timeline
```

Optional: `~/.gitconfig`: Create the below alias to shorten `git timeline` to `git l`.

```text
[alias]
    l = timeline
```

## Upgrade

1. Fetch the newest version of all formulas.

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
- Pager command: Use the env var `GIT_PAGER`, `core.pager` from gitconfig or env var `PAGER`.
- Color and paging: Both are on when the output goes to a terminal and off when it is piped
  or redirected, as in git-log. Use `--color=always` to keep color through a pipe.

## Completions

Completions only work for `git timeline`, not `git-timeline`. They mirror the completions of
`git log`.

### Zsh

There are two `_git` completion implementations in the wild and they look up a different function
name, so pick the file that matches yours. Install it into any directory on your `$FPATH`, then
restart your shell.

If your Git is installed with Homebrew:

```text
curl -L -o "$(brew --prefix)/share/zsh/site-functions/_git_timeline" \
  https://raw.githubusercontent.com/hernancerm/git-timeline/refs/heads/main/completions/_git_timeline
```

On Ubuntu/Debian:

```text
curl -L -o ~/.zsh/completions/_git-timeline \
  https://raw.githubusercontent.com/hernancerm/git-timeline/refs/heads/main/completions/_git-timeline
```

### Troubleshooting

If `git timeline --<TAB>` doesn't work, `_git` probably couldn't find
`git-completion.bash`. Try in `~/.zshrc`:

```text
# Point at the right `git-completion.bash`. Fixes `git timeline` completions.
zstyle ':completion:*:*:git:*' script $(brew --prefix)/share/zsh/site-functions/git-completion.bash
```

Running `brew reinstall git` often fixes this. Check its Caveats output for the completions
directory. On other systems the file is usually at
`/usr/share/bash-completion/completions/git`.

## Performance

Measured on the [vim](https://github.com/vim/vim) repo (24,475 commits) with
[hyperfine](https://github.com/sharkdp/hyperfine) on an Apple M4 Pro. Used the mean of >=20 runs:

| Command | Native binary | Uber JAR |
| --- | --- | --- |
| `git-timeline --color=always -1 > /dev/null` (startup) | 47 ms | 106 ms |
| `git-timeline --color=always --no-pager > /dev/null` (whole history) | 205 ms | 303 ms |
| `git-timeline --color=always --no-pager --graph > /dev/null` | 212 ms | 311 ms |

For reference, the same runs against `git log` itself, where `<fields>` is
`--date=format:%b-%d-%Y --pretty=format:'%H %h %p %C(auto)%d %cn %an %ad %s'`:

| Command | git log |
| --- | --- |
| `git log --color=always <fields> -1 > /dev/null` (startup) | 36 ms |
| `git log --color=always <fields> > /dev/null` (whole history) | 178 ms |
| `git log --color=always <fields> --graph > /dev/null` | 183 ms |

Startup is the most impactful figure in normal daily use.

## Build from source

The project is written in Java 25.

The steps below should work for macOS (arm64 and x86) and Linux (arm64 and x86).

### Native binary

GraalVM Native Image is used to compile Java to a native binary.

1. Download the Java 25 JDK provided by GraalVM.

    Either do it manually through the website: https://www.graalvm.org/downloads/

    Or use mise (https://mise.jdx.dev): `mise install java@graalvm-community-25.0.2`

    Or use SDKMAN! (https://sdkman.io): `sdk install java 25.0.2-graal`

2. Ensure the JDK used is the GraalVM JDK. The output of `java -version` should contain GraalVM.

   Otherwise, set the env var `GRAALVM_HOME` to point to your GraalVM JDK.

3. On a clone of this repo run `make bin`.

The binary is created at `./target/git-timeline`.

Execute it with `./target/git-timeline`.

### Uber JAR (executable JAR)

1. Download a Java 25 JDK.

2. Set the env var `JAVA_HOME`.

3. On a clone of this repo run `make uber`.

The uber JAR is created at `./target/git-timeline.jar`.

Execute it with `java -jar ./target/git-timeline.jar`.

## Versioning

Since version 2.2.0, git-timeline follows [semantic
versioning](https://semver.org/spec/v2.0.0.html).

The public API is the command line interface: the options and the exit codes. The printed commit
lines are not part of the public API, so changes to them are not breaking changes.

Releases are listed in [CHANGELOG.md](./CHANGELOG.md).

