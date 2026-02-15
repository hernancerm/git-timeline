# Zsh Completions for git-timeline

`git timeline --<TAB>` completes identically to `git log --<TAB>`. Completions are installed by `install-completions.sh` and match the user's exact installed git version. Run `make install-completions` after install, and again after any git upgrade.

## How it works

Git ships two completion files: `git-completion.bash` contains all subcommand completion logic as bash functions (including `_git_log`), and `git-completion.zsh` is a zsh-to-bash bridge that sources the bash file and provides zsh-compatible wrappers (`__gitcomp`, `__gitcomp_nl`, etc.) that translate bash `COMPREPLY` output into zsh `compadd` calls. The installed `_git` zsh completion (from git or Homebrew) handles `git <subcommand>` dispatch: when the subcommand is `timeline`, it calls `__git_complete_command timeline`, which looks up `_git_timeline` in `$functions` and invokes it via `emulate ksh -c`.

`install-completions.sh` detects the installed git version, downloads both authoritative files from `github.com/git/git` at that exact version tag, and generates `_git_timeline`. The generated file: (1) sources `git-completion.bash` with `GIT_SOURCING_ZSH_COMPLETION=y` to load `_git_log` while suppressing bash `complete` registrations; (2) embeds the `__gitcomp*` wrapper functions extracted from `git-completion.zsh`; (3) sets `cur`/`cword`/`prev` from zsh's 1-indexed `words[CURRENT]` before invoking `emulate ksh -c _git_log`. Step 3's ordering is critical: `emulate ksh` switches arrays to 0-indexed, so `words[CURRENT]` must be captured beforehand or `cur` ends up empty and no completions are produced.

## Why the wrappers are extracted rather than git-completion.zsh being sourced whole

Sourcing `git-completion.zsh` is not feasible. The file ends with a bare `_git` call (intentional when installed *as* `_git` — it fires on autoload), which triggers in the wrong context when sourced from `_git_timeline`, producing `bad output format specification`. Neutralising `_git` via `functions[_git]=:` before sourcing and restoring it after fails because an autoload stub's `$functions[_git]` contains the internal marker `"builtin autoload -XU"`, not function body code; restoring that string breaks `_git` on subsequent calls. Stripping the trailing `_git` call still fails because sourcing the file redefines `_git()` itself, overwriting whatever `_git` the user has installed. Extracting only the seven `__gitcomp*` wrappers is the only side-effect-free approach: `_git` is never touched.

## Registered via `#compdef -`

`_git_timeline` uses `#compdef -` as its first line. This causes `compinit` to register the file as an autoload stub in `$functions` without binding it to any command — which is what `__git_complete_command` checks. Without this directive, `compinit` ignores the file entirely and cold-start `git timeline --<TAB>` finds no completion function.
