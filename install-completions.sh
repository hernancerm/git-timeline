#!/bin/bash

set -e

# ============================================================================
# install-completions.sh - Install zsh completions for git-timeline
#
# This script:
# 1. Detects your installed git version
# 2. Downloads the authoritative git completion files from GitHub at that
#    exact version (git-completion.bash and git-completion.zsh)
# 3. Generates _git-timeline using git's own zsh-to-bash bridge pattern
# 4. Installs git-completion.bash and _git-timeline to the zsh completion dir
#
# Re-running is safe and idempotent. Re-run after a git upgrade to update.
# ============================================================================

# ============================================================================
# SECTION 1: DETECT GIT VERSION
# ============================================================================

if ! command -v git &> /dev/null; then
    echo "ERROR: git is not installed or not in PATH"
    exit 1
fi

# Strip everything after the version number (e.g. "(Apple Git-130)" on macOS)
git_version=$(git --version | sed 's/^git version //' | awk '{print $1}')
if [ -z "$git_version" ]; then
    echo "ERROR: Could not parse git version from 'git --version'"
    exit 1
fi

echo "Detected git version: $git_version"

# ============================================================================
# SECTION 2: DOWNLOAD AUTHORITATIVE GIT COMPLETION FILES
# ============================================================================

base_url="https://raw.githubusercontent.com/git/git/v${git_version}/contrib/completion"
bash_url="${base_url}/git-completion.bash"
zsh_url="${base_url}/git-completion.zsh"

temp_dir=$(mktemp -d)
trap "rm -rf $temp_dir" EXIT

echo "Downloading git-completion.bash for v${git_version}..."
if ! curl -fsSL "$bash_url" -o "$temp_dir/git-completion.bash"; then
    echo ""
    echo "ERROR: Failed to download git-completion.bash"
    echo "  URL: $bash_url"
    echo ""
    echo "Possible causes:"
    echo "  - No internet connection"
    echo "  - Git version v${git_version} not found on GitHub"
    echo "    (Some OS-bundled git versions use non-standard version strings)"
    echo ""
    echo "Try: curl -I $bash_url"
    exit 1
fi

echo "Downloading git-completion.zsh for v${git_version}..."
if ! curl -fsSL "$zsh_url" -o "$temp_dir/git-completion.zsh"; then
    echo ""
    echo "ERROR: Failed to download git-completion.zsh"
    echo "  URL: $zsh_url"
    exit 1
fi

echo "Downloaded completion files for git v${git_version}"

# ============================================================================
# SECTION 3: DETECT ZSH COMPLETION DIRECTORY
# ============================================================================

detect_completion_dir() {
    local dirs=(
        "/opt/homebrew/share/zsh/site-functions"   # Homebrew macOS
        "/usr/share/zsh/site-functions"             # Linux system
        "/usr/local/share/zsh/site-functions"       # macOS system (Intel)
        "$HOME/.zsh/completions"                    # User home fallback
    )

    for dir in "${dirs[@]}"; do
        if [ -d "$dir" ]; then
            echo "$dir"
            return 0
        fi
    done

    # Default: user directory (will be created below)
    echo "$HOME/.zsh/completions"
    return 0
}

completion_dir=$(detect_completion_dir)

if [ ! -d "$completion_dir" ]; then
    mkdir -p "$completion_dir" || {
        echo "ERROR: Could not create completion directory: $completion_dir"
        exit 1
    }
fi

# ============================================================================
# SECTION 4: EXTRACT ZSH WRAPPER FUNCTIONS FROM git-completion.zsh
#
# git-completion.zsh defines zsh-compatible wrappers for bash completion
# primitives (__gitcomp, __gitcomp_nl, etc.) and helper functions.
# We extract these verbatim and embed them in _git-timeline so that the
# generated file is fully self-contained (no dependency on a system _git).
#
# We extract from the first __gitcomp function definition through the last
# closing brace before the _git_zsh / __git_zsh_* / _git() functions —
# i.e. everything between the sourcing block and the zsh main dispatch.
# ============================================================================

# Extract wrapper functions: from first '__gitcomp ()' to end of
# '__git_complete_command ()' block — these are the zsh<->bash bridge helpers.
# We use awk to grab from '__gitcomp ()' up through '_git_zsh ()' exclusive.
zsh_wrappers=$(awk '
    /^__gitcomp \(\)/ { printing=1 }
    printing { print }
    /^_git_zsh \(\)/ { printing=0 }
' "$temp_dir/git-completion.zsh")

# Also grab __git_complete_command and __git_zsh_bash_func, which are needed
# by _git_timeline to call _git_log in ksh-emulation mode.
zsh_helpers=$(awk '
    /^__git_complete_command \(\)/ { printing=1 }
    printing { print }
    /^\}/ && printing { print ""; printing=0 }
' "$temp_dir/git-completion.zsh" | head -20)

git_complete_command=$(awk '
    /^__git_complete_command \(\)$/ { p=1 }
    p { print }
    /^\}$/ && p { p=0 }
' "$temp_dir/git-completion.zsh")

# ============================================================================
# SECTION 5: GENERATE _git-timeline
#
# The generated file uses git's own zsh-to-bash bridge pattern:
#   1. Source git-completion.bash (with GIT_SOURCING_ZSH_COMPLETION=y so
#      bash `complete` calls become no-ops in zsh context)
#   2. Define zsh wrapper functions (__gitcomp* etc.) copied from
#      git-completion.zsh so bash completion output is translated to zsh
#   3. Define _git_timeline() which calls _git_log (from git-completion.bash)
#      via `emulate ksh -c` for bash/ksh compatibility
#   4. Call _git_timeline to trigger completion
# ============================================================================

echo ""
echo "Generating _git-timeline..."

cat > "$temp_dir/_git-timeline" << HEADER
#compdef git-timeline

# Auto-generated by install-completions.sh — do not edit manually.
# Regenerate with: make install-completions
#
# Git version: ${git_version}
#
# Uses git's official zsh-to-bash bridge pattern from git-completion.zsh.
# Sources git-completion.bash to get all git completion functions (_git_log,
# __gitcomp_builtin, etc.), then provides zsh-compatible wrappers so the bash
# completion output is correctly translated to zsh compadd calls.

# ---------------------------------------------------------------------------
# Step 1: Source git-completion.bash
#
# \${(%):-%x} is the path of this file; :h gives its directory.
# git-completion.bash is installed alongside this file by install-completions.sh.
# GIT_SOURCING_ZSH_COMPLETION=y tells git-completion.bash it is running in a
# zsh context; the bash 'complete' builtin is neutralised so its registration
# calls are no-ops.
# ---------------------------------------------------------------------------
local script="\${\${(%):-%x}:h}/git-completion.bash"
if [[ ! -f "\$script" ]]; then
    _message "git-completion.bash not found next to _git-timeline; run: make install-completions"
    return 1
fi
local old_complete="\$functions[complete]"
functions[complete]=:
GIT_SOURCING_ZSH_COMPLETION=y . "\$script"
functions[complete]="\$old_complete"

# ---------------------------------------------------------------------------
# Step 2: Zsh-compatible wrappers for bash completion primitives
#
# Copied verbatim from git-completion.zsh v${git_version} (official git source).
# These translate bash COMPREPLY / __gitcomp calls into zsh compadd calls.
# ---------------------------------------------------------------------------
HEADER

# Append the wrapper functions extracted from the official git-completion.zsh
# Everything from __gitcomp() through (not including) _git_zsh()
awk '
    /^__gitcomp \(\)/ { printing=1 }
    /^_git_zsh \(\)/ { exit }
    printing { print }
' "$temp_dir/git-completion.zsh" >> "$temp_dir/_git-timeline"

# Append __git_complete_command (needed to call _git_log in ksh mode)
awk '
    /^__git_complete_command \(\)$/ { p=1 }
    p { print }
    /^\}$/ && p { p=0; exit }
' "$temp_dir/git-completion.zsh" >> "$temp_dir/_git-timeline"

cat >> "$temp_dir/_git-timeline" << 'FOOTER'

# ---------------------------------------------------------------------------
# Step 3: Entry point
#
# _git_timeline() is called by zsh's completion system when completing
# `git-timeline <TAB>`. It sets up the bash-style cursor variables and
# delegates to _git_log (loaded from git-completion.bash above) via
# `emulate ksh -c` for bash/ksh compatibility — the same mechanism used
# by git-completion.zsh's __git_complete_command().
# ---------------------------------------------------------------------------
_git_timeline()
{
    # Set up bash-style cursor variables while still in zsh context (1-indexed
    # arrays). CURRENT is a zsh 1-based index; words[CURRENT] is the word
    # under the cursor. These must be captured before any ksh emulation
    # switches arrays to 0-indexed.
    local cur cword prev
    local __git_repo_path
    local __git_cmd_idx=1

    cur=${words[CURRENT]}
    prev=${words[CURRENT-1]}
    let cword=CURRENT-1

    # Run _git_log in ksh emulation mode for bash compatibility.
    # Using `emulate ksh -c` (not `emulate -L ksh` at function top) ensures
    # the local variables set above are visible to _git_log as expected.
    emulate ksh -c _git_log
}

_git_timeline
FOOTER

echo "Generated _git-timeline"

# ============================================================================
# SECTION 6: INSTALL FILES
# ============================================================================

echo ""
echo "Installing to: $completion_dir"

cp "$temp_dir/git-completion.bash" "$completion_dir/git-completion.bash" || {
    echo "ERROR: Could not install git-completion.bash to $completion_dir"
    exit 1
}
echo "  Installed: git-completion.bash"

cp "$temp_dir/_git-timeline" "$completion_dir/_git-timeline" || {
    echo "ERROR: Could not install _git-timeline to $completion_dir"
    exit 1
}
echo "  Installed: _git-timeline"

# ============================================================================
# SECTION 7: SUMMARY
# ============================================================================

echo ""
echo "=========================================="
echo "Zsh completions installed successfully!"
echo "=========================================="
echo ""
echo "Git version:    $git_version"
echo "Installed to:   $completion_dir"
echo ""
echo "Files installed:"
echo "  _git-timeline       (zsh completion for 'git-timeline')"
echo "  git-completion.bash (git's bash completion, sourced by _git-timeline)"
echo ""
echo "Next steps:"
echo "  1. Restart your shell:  exec zsh"
echo "  2. Try:                 git-timeline --<TAB>"
echo ""
echo "To update completions after a git upgrade, re-run: make install-completions"
echo ""
