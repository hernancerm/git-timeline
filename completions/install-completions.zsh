## Install Zsh completions for git-timeline.
##
## Usage: `HOMEBREW=1 zsh install-completions.zsh`
##        Use this env var when having Homebrew.
##
## Installs _git_timeline to the zsh completion directory.
##
## - _git_timeline
##   - Zsh completion function for 'git timeline'.
##   - This script generates the _git_timeline completion file.
##
## How 'git timeline --<Tab>' works:
##
## The existing _git Zsh completion from:
## > https://github.com/git/git/blob/master/contrib/completion/git-completion.zsh
## handles 'git <sub>' completions. When the subcommand is 'timeline', it looks up
## _git_timeline() in $functions and calls it. _git_timeline() sources git-completion.bash
## (already present on the system as part of a standard git install) and delegates to
## _git_log, giving identical completions to 'git log'.
##
## Re-running is safe. Re-run after a Git upgrade to update.

setopt ERR_EXIT

# 1. DETECT GIT VERSION
# ---

if (( ! $+commands[git] )); then
    echo "ERROR: git is not installed or not in PATH"
    exit 1
fi

# Strip OS-specific suffixes (e.g. "(Apple Git-130)" on macOS)
git_version=$(git --version | sed 's/^git version //' | awk '{print $1}')
if [[ -z "$git_version" ]]; then
    echo "ERROR: Could not parse git version from 'git --version'"
    exit 1
fi

echo "Detected git version: $git_version"

# 2. LOCATE EXISTING git-completion.bash
# ---
# git-completion.bash is already present on the system as part of a standard
# git install. We locate it using glob patterns rather than downloading it.

local bash_completion_script=""
local zstyle_script=""

# When brew is available, check its prefix path first and use it for the zstyle
# line (stable across git upgrades). Otherwise fall back to well-known paths.
if (( $+commands[brew] )); then
    local brew_bash="$(brew --prefix)/etc/bash_completion.d/git-completion.bash"
    if [[ -f "$brew_bash" ]]; then
        bash_completion_script="$brew_bash"
        zstyle_script='$(brew --prefix)/etc/bash_completion.d/git-completion.bash'
    fi
fi

if [[ -z "$bash_completion_script" ]]; then
    local -a bash_candidates
    bash_candidates=(
        $HOME/.local/share/bash-completion/completions/git(N[1])
        /usr/share/bash-completion/completions/git(N[1])
        /etc/bash_completion.d/git(N[1])
    )
    for c in "${bash_candidates[@]}"; do
        if [[ -f "$c" ]]; then
            bash_completion_script="$c"
            zstyle_script="$c"
            break
        fi
    done
fi

if [[ -z "$bash_completion_script" ]]; then
    echo ""
    echo "ERROR: git-completion.bash not found on this system."
    echo ""
    echo "Searched in:"
    echo "  \$(brew --prefix)/etc/bash_completion.d/git-completion.bash"
    echo "  \$HOME/.local/share/bash-completion/completions/git"
    echo "  /usr/share/bash-completion/completions/git"
    echo "  /etc/bash_completion.d/git"
    echo ""
    echo "git-completion.bash is included with a standard git install. If you"
    echo "installed git via Homebrew, try: brew install git"
    exit 1
fi

echo "Found git-completion.bash: $bash_completion_script"

# 3. DOWNLOAD git-completion.zsh (temporary, for awk extraction only)
# ---
# git-completion.zsh is not installed locally by Homebrew. We download it
# temporarily to extract the __gitcomp* wrapper functions via awk. The file
# is cleaned up on exit and never installed anywhere.

base_url="https://raw.githubusercontent.com/git/git/v${git_version}/contrib/completion"
zsh_url="${base_url}/git-completion.zsh"

temp_dir=$(mktemp -d)
trap "rm -rf $temp_dir" EXIT

echo "Downloading git-completion.zsh for v${git_version} (temporary, for code extraction)"
if ! curl -fsSL "$zsh_url" -o "$temp_dir/git-completion.zsh"; then
    echo ""
    echo "ERROR: Failed to download git-completion.zsh"
    echo "  URL: $zsh_url"
    echo ""
    echo "Possible causes:"
    echo "  - No internet connection"
    echo "  - Git version v${git_version} not found on GitHub"
    echo "    (Some OS-bundled git versions use non-standard version strings)"
    echo ""
    echo "Try: curl -I $zsh_url"
    exit 1
fi

# 4. DETECT ZSH COMPLETION DIRECTORY
# ---
# Default: /usr/local/share/zsh/site-functions
# Set HOMEBREW=1 to install to $(brew --prefix)/share/zsh/site-functions instead.

local completion_dir="/usr/local/share/zsh/site-functions"

if [[ "${HOMEBREW:-0}" == "1" ]]; then
    if (( ! $+commands[brew] )); then
        echo "ERROR: HOMEBREW=1 set but brew is not installed or not in PATH"
        exit 1
    fi
    completion_dir="$(brew --prefix)/share/zsh/site-functions"
fi

if [[ ! -d "$completion_dir" ]]; then
    mkdir -p "$completion_dir" || {
        echo "ERROR: Could not create completion directory: $completion_dir"
        exit 1
    }
fi

# 5. GENERATE _git_timeline
# ---
# '#compdef -' tells compinit to autoload this file as a function stub without
# binding it to any command. This makes _git_timeline() available in $functions
# from shell init, so the _git bridge's __git_complete_command() can find and
# call it when completing 'git timeline --<TAB>'.
#
# The function uses git's zsh-to-bash bridge pattern from git-completion.zsh:
#   1. Source git-completion.bash (GIT_SOURCING_ZSH_COMPLETION=y suppresses
#      bash 'complete' registrations that are no-ops in zsh)
#   2. Define zsh wrapper functions (__gitcomp* etc.) copied verbatim from
#      git-completion.zsh — these translate bash completion output to zsh compadd
#   3. Set up bash-style cursor variables (cur/cword/prev) from zsh's 1-indexed
#      CURRENT/words[] before ksh emulation switches arrays to 0-indexed
#   4. Call _git_log via 'emulate ksh -c' for bash/ksh compatibility

echo "Generating _git_timeline"

cat > "$temp_dir/_git_timeline" << HEADER
#compdef -

# Auto-generated by install-completions.zsh — do not edit manually.
# Regenerate with: make install-completions
#
# Git version: ${git_version}
#
# '#compdef -' causes compinit to register this file as an autoloadable
# function stub without binding it to any command. After 'exec zsh', the
# _git completion bridge finds _git_timeline() in \$functions and calls it
# when completing 'git timeline --<TAB>'.
#
# Uses git's official zsh-to-bash bridge pattern from git-completion.zsh v${git_version}.

# ---------------------------------------------------------------------------
# Step 1: Source git-completion.bash
#
# Path resolved at install time to the git-completion.bash already present
# on this system. GIT_SOURCING_ZSH_COMPLETION=y tells git-completion.bash it
# is running in a zsh context; 'complete' is neutralised so bash registration
# calls are no-ops.
# ---------------------------------------------------------------------------
local script="${bash_completion_script}"
if [[ ! -f "\$script" ]]; then
    _message "git-completion.bash not found at \$script; run: make install-completions"
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

# Append __gitcomp* wrappers: everything from __gitcomp() up to _git_zsh()
awk '
    /^__gitcomp \(\)/  { printing=1 }
    /^_git_zsh \(\)/   { exit }
    printing           { print }
' "$temp_dir/git-completion.zsh" >> "$temp_dir/_git_timeline"

# Validate that the expected wrapper functions were actually extracted.
# If git ever renames or restructures these, fail loudly rather than silently
# installing a broken completion file.
for fn in __gitcomp __gitcomp_direct __gitcomp_nl __gitcomp_file; do
    if ! grep -q "^${fn} ()" "$temp_dir/_git_timeline"; then
        echo ""
        echo "ERROR: Failed to extract ${fn}() from git-completion.zsh v${git_version}"
        echo "  The structure of git-completion.zsh may have changed in this version."
        echo "  Please file an issue at https://github.com/hernancerm/git-timeline"
        exit 1
    fi
done

cat >> "$temp_dir/_git_timeline" << 'FOOTER'

# ---------------------------------------------------------------------------
# Step 3: Entry point
#
# Set up bash-style cursor variables while still in zsh context (1-indexed
# arrays). CURRENT is a zsh 1-based index; words[CURRENT] is the word under
# the cursor. These must be captured before ksh emulation switches arrays to
# 0-indexed — otherwise words[CURRENT] is out of bounds and cur="".
#
# Then call _git_log via 'emulate ksh -c' (not 'emulate -L ksh' at function
# top) so the locals set above remain visible inside _git_log.
# ---------------------------------------------------------------------------
local cur cword prev
local __git_repo_path
local __git_cmd_idx=1

cur=${words[CURRENT]}
prev=${words[CURRENT-1]}
let cword=CURRENT-1

emulate ksh -c _git_log
FOOTER

echo "- Generated _git_timeline"

# 6. INSTALL _git_timeline
# ---

echo "Installing to: $completion_dir"

cp "$temp_dir/_git_timeline" "$completion_dir/_git_timeline" || {
    echo "ERROR: Could not install _git_timeline to $completion_dir"
    exit 1
}
echo "- Installed: _git_timeline"

# 7. CAVEATS
# ---

echo "For completions to work add this line to your ~/.zshrc:"
echo "  zstyle ':completion:*:*:git:*' script ${zstyle_script}"
echo "To update completions after a git upgrade, re-run: make install-completions"
