# Zsh Completions for git-timeline

This document explains how zsh completions work for `git-timeline` and how to install, use, update, and uninstall them.

## Overview

`git-timeline` supports zsh shell completions that work identically to `git log` completions. The completions automatically match your installed git version.

### Key Features

- ✅ Completions exactly match your installed git version
- ✅ Auto-detects git version and fetches completions from GitHub
- ✅ Works on macOS, Linux, and other Unix systems
- ✅ Works with any git installation method (Homebrew, package manager, from-source)
- ✅ Auto-updates completions when you upgrade git
- ✅ Both `git-timeline` and `git timeline` forms supported

## Installation

### From Homebrew

Completions are installed automatically:

```bash
brew install git-timeline
```

No additional steps needed!

### From Source

After building git-timeline:

```bash
make uber                      # Build the JAR executable
make install-completions      # Install zsh completions
```

### Manual Installation

If the above doesn't work, you can manually run the installation script:

```bash
bash install-completions.sh
```

## How It Works

When you run `make install-completions`:

1. **Detects your git version** (e.g., `git version 2.53.0`)

2. **Downloads two authoritative files from GitHub** at that exact version tag:
   - `git-completion.bash` — git's bash completion functions (defines `_git_log` and all other subcommand completers)
   - `git-completion.zsh` — git's official zsh-to-bash bridge (defines `__gitcomp*` zsh wrappers)

3. **Generates `_git-timeline`** — a self-contained zsh completion file that:
   - Sources `git-completion.bash` using git's own bridge technique
   - Embeds the `__gitcomp*` zsh wrapper functions verbatim from `git-completion.zsh`
   - Defines `_git_timeline()` which calls `_git_log` via `emulate ksh -c`

4. **Installs two files** to your zsh completion directory:
   - `_git-timeline` — the generated zsh completion
   - `git-completion.bash` — git's bash completion (sourced at completion time by `_git-timeline`)

   Installed to the first existing directory from:
   - `/opt/homebrew/share/zsh/site-functions` (Homebrew macOS)
   - `/usr/share/zsh/site-functions` (Linux system)
   - `/usr/local/share/zsh/site-functions` (alternate macOS system)
   - `~/.zsh/completions` (user home fallback, created if needed)

### The Zsh-to-Bash Bridge (Why This Works)

Git ships two completion files that work together via a proven bridge pattern:

**`git-completion.bash`** defines all the actual completion logic as bash functions:
- `_git_log` — completes all `git log` options (which `git-timeline` delegates to)
- `__gitcomp_builtin` — calls `git <cmd> --git-completion-helper` to get options
- `__git_complete_refs`, `__git_heads`, etc. — complete refs, branches, tags

**`git-completion.zsh`** bridges bash→zsh by:

1. **Sourcing the bash file** with a compatibility flag:
   ```zsh
   GIT_SOURCING_ZSH_COMPLETION=y . git-completion.bash
   ```
   This suppresses `complete` calls (bash-only) while loading all functions.

2. **Providing zsh wrappers** that translate bash completion output to zsh:
   - `__gitcomp()` → `compadd` (translates bash COMPREPLY to zsh compadd)
   - `__gitcomp_nl()` → `compadd` for newline-separated lists
   - `__gitcomp_direct()` → `compadd` for pre-filtered lists
   - And others...

3. **Running bash functions in ksh-compat mode**:
   ```zsh
   emulate ksh -c _git_log
   ```

**`_git-timeline`** uses the identical pattern — it is generated fresh from the same authoritative sources at your exact git version, so completions always match your installed git.

- ✅ **Authoritative sources** — downloaded directly from git's GitHub at your version tag
- ✅ **Version-exact** — `v2.47.1` installs `v2.47.1` completion files, not whatever ships with your OS
- ✅ **Self-contained** — `_git-timeline` embeds the bridge; no dependency on how system `_git` is installed
- ✅ **Idempotent** — re-running always overwrites with the correct version

### Version Compatibility

- You install git version 2.53.0
- We download git-completion.zsh and git-completion.bash **for version 2.53.0**
- Completions match your git exactly
- When you upgrade git to 2.60.0, simply re-run `make install-completions`
- We auto-detect the new version and download 2.60.0 files
- Completions automatically updated!

## Usage

### Basic Examples

```bash
# Complete git log options
git-timeline --aut<TAB>        # → --author, --all-match, etc.
git timeline --dat<TAB>        # → --date, --date-order, etc.
git-timeline --form<TAB>       # → --format
git timeline --oneline<TAB>    # → --oneline

# Both command forms work identically
git-timeline --<TAB>           # Shows all options
git timeline --<TAB>           # Shows all options

# Git-timeline-specific options
git-timeline --hel<TAB>        # → --help
git-timeline --ver<TAB>        # → --version
git-timeline --no-p<TAB>       # → --no-pager
```

### Argument Completion

```bash
# Complete branch names
git-timeline --author <name><TAB>    # Suggests authors
git timeline main..<TAB>             # Suggests remote branches

# Date format completion (if specified with =)
git-timeline --date=<TAB>            # Suggests date formats
```

## Updating Completions

### Automatic Update When Upgrading Git

```bash
# You upgrade git (e.g., via Homebrew)
brew upgrade git

# Re-run the install script to update completions
make install-completions

# Completions are now updated for your new git version
```

The script automatically detects your new git version and downloads the correct completion functions.

## Uninstalling Completions

To remove zsh completions:

```bash
make uninstall-completions
```

This will:
1. Find all installed git-timeline completion files
2. Show you which files will be deleted
3. Ask for confirmation (y/n)
4. Delete only if you confirm

### Example Output

```
Looking for installed git-timeline completion files...

Found the following completion files:
  - /opt/homebrew/share/zsh/site-functions/_git-timeline
  - /opt/homebrew/share/zsh/site-functions/git-completion.bash

This will DELETE the files listed above.

Continue? (y/N) y

Deleting completion files...
✓ Deleted: /opt/homebrew/share/zsh/site-functions/_git-timeline
✓ Deleted: /opt/homebrew/share/zsh/site-functions/git-completion.bash

==========================================
✓ Completions uninstalled successfully!
==========================================
```

## Troubleshooting

### Completions Not Working

**Check if files are installed:**

```bash
ls ~/.zsh/completions/_git*
# or for system location:
ls /usr/share/zsh/site-functions/_git*
# or for Homebrew:
ls /opt/homebrew/share/zsh/site-functions/_git*
```

**If using user directory (`~/.zsh/completions`), check your `~/.zshrc`:**

Your `~/.zshrc` should contain:

```bash
fpath=(~/.zsh/completions $fpath)
autoload -Uz compinit && compinit
```

**Restart your shell:**

```bash
exec zsh
```

### Network Error During Installation

**Error message:** "Failed to download git completion for version X.Y.Z"

**Causes:**
- No internet connection
- GitHub is unreachable
- Firewall blocking GitHub access

**Solutions:**
1. Check your internet connection
2. Try again later
3. Verify GitHub is accessible (try `curl https://github.com`)

### Completions Match git, Not git-timeline Specific

This is expected! `git-timeline` is a drop-in replacement for `git log`, so all its completions are the same as `git log`. The completions include:
- All git log options
- git-timeline-specific options (`--help`, `--version`, `--no-pager`)

### Different Completions Between Versions

If you recently upgraded git and completions seem different:

```bash
# Regenerate completions for your new git version
make install-completions
```

## Supported Git Versions

- **Minimum:** git 2.30.0 (tested and working well)
- **Current:** git 2.53.0+ (fully supported)
- **Future:** Any version (the script automatically adapts)

Very old versions (< 2.30.0) may have compatibility issues but are not officially supported.

## For Developers / Advanced Users

### Generated Files Location

Two files are installed to the zsh site-functions directory:
- `_git-timeline` — zsh completion for `git-timeline`, generated by the install script
- `git-completion.bash` — git's authoritative bash completion, downloaded from GitHub

These are **generated/downloaded files**, not source files, and are not committed to the repository.

### Regenerating Completions

To regenerate completions at any time:

```bash
# Remove old completions
make uninstall-completions

# Regenerate and install
make install-completions
```

### Custom Git Versions

The script auto-detects your git version. If you have a non-standard git installation:

```bash
# Check what version the script detects:
git --version

# If it doesn't match your actual git, you may need to update your PATH
# or check your git installation
which git
```

## Future Enhancements

Planned support for:
- Bash shell completions
- Fish shell completions
- Completion caching for offline use

---

**Questions?** File an issue on GitHub: https://github.com/hernancerm/git-timeline
