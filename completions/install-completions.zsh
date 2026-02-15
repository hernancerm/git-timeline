## Install Zsh completions for git-timeline.
##
## Installs two files to the zsh completion directory:
##
## - _git_timeline
##   - Zsh completion function for 'git timeline'.
##   - This script generates the _git_timeline completion file.
## - git-completion-for-git-timeline.bash
##   - Git's authoritative bash completion:
##     https://github.com/git/git/blob/master/contrib/completion/git-completion.bash
##   - This Bash file is sourced by the _git_timeline completion file.
##
## How 'git timeline --<Tab>' works:
##
## The existing _git Zsh completion from:
## > https://github.com/git/git/blob/master/contrib/completion/git-completion.zsh
## handles 'git <sub>' completions. When the subcommand is 'timeline', it looks up
## _git_timeline() in $functions and calls it. _git_timeline() sources git-completion.bash
## and delegates to _git_log, giving identical completions to 'git log'.
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

# 2. DOWNLOAD AUTHORITATIVE GIT COMPLETION FILES
# ---

base_url="https://raw.githubusercontent.com/git/git/v${git_version}/contrib/completion"
bash_url="${base_url}/git-completion.bash"
bash_dest_name="git-completion-for-git-timeline.bash"
zsh_url="${base_url}/git-completion.zsh"

temp_dir=$(mktemp -d)
trap "rm -rf $temp_dir" EXIT

echo "Downloading authoritative git completion files"

echo "- Downloading git-completion.bash for v${git_version}"
if ! curl -fsSL "$bash_url" -o "$temp_dir/$bash_dest_name"; then
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

echo "- Downloading git-completion.zsh for v${git_version}"
if ! curl -fsSL "$zsh_url" -o "$temp_dir/git-completion.zsh"; then
    echo ""
    echo "ERROR: Failed to download git-completion.zsh"
    echo "  URL: $zsh_url"
    exit 1
fi

echo "- Downloaded completion files for v${git_version}"

# 3. DETECT ZSH COMPLETION DIRECTORY
# ---

detect_completion_dir() {
    local dirs=(
        "/opt/homebrew/share/zsh/site-functions"   # Homebrew macOS
        "/usr/share/zsh/site-functions"             # Linux system
        "/usr/local/share/zsh/site-functions"       # macOS system (Intel)
        "$HOME/.zsh/completions"                    # User home fallback
    )

    for dir in "${dirs[@]}"; do
        if [[ -d "$dir" ]]; then
            echo "$dir"
            return 0
        fi
    done

    echo "$HOME/.zsh/completions"
    return 0
}

completion_dir=$(detect_completion_dir)

if [[ ! -d "$completion_dir" ]]; then
    mkdir -p "$completion_dir" || {
        echo "ERROR: Could not create completion directory: $completion_dir"
        exit 1
    }
fi

# 4. GENERATE _git_timeline
#
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
#
# WHY WE EXTRACT __gitcomp* FROM git-completion.zsh RATHER THAN SOURCING IT
# --------------------------------------------------------------------------
# The natural alternative would be to simply source git-completion.zsh whole,
# which would give us all the wrapper functions without any awk. We tried this
# and it is not feasible for two reasons:
#
# 1. git-completion.zsh defines and then immediately calls _git() at the end
#    of the file (the bare '_git' on the last line). That call is intentional
#    when the file is installed *as* _git — it initialises the completion on
#    autoload. But when sourced from inside _git_timeline(), _git() fires in
#    the wrong context (wrong $service, wrong $words), producing errors like
#    "_git:N: bad output format specification".
#
# 2. Neutralising that trailing call by saving/restoring $functions[_git]
#    before and after sourcing does not work either. When _git is registered
#    by compinit as an autoload stub (not yet sourced), $functions[_git]
#    contains the internal marker string "builtin autoload -XU" rather than
#    actual function body code. Restoring that string into $functions[_git]
#    after sourcing causes "_git:1: command not found: [_git]" on subsequent
#    completions.
#
# 3. Stripping the trailing _git call from the file before installing it
#    still does not work because sourcing git-completion.zsh *defines* _git()
#    as a function, which overwrites the user's existing _git (from Homebrew
#    or the system). On the next 'git <TAB>', the wrong _git is called and
#    produces "bad output format specification".
#
# The extraction approach is the only one that is fully side-effect-free:
# we take only the __gitcomp* translation layer (7 small, stable functions)
# and leave _git entirely untouched. These functions have had identical
# signatures and structure across all tested git versions (v2.40–v2.53).
# The validation step below ensures any future structural change is caught
# immediately rather than producing a silently broken completion.
# ---

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
# Sources git-completion-for-git-timeline.bash at completion time.

# ---------------------------------------------------------------------------
# Step 1: Source git-completion.bash
#
# \${\${(%):-%x}:h} is the directory containing this file.
# git-completion-for-git-timeline.bash is installed alongside this file by
# install-completions.zsh.
# GIT_SOURCING_ZSH_COMPLETION=y tells git-completion.bash it is running in a
# zsh context; 'complete' is neutralised so bash registration calls are no-ops.
# ---------------------------------------------------------------------------
local script="\${\${(%):-%x}:h}/git-completion-for-git-timeline.bash"
if [[ ! -f "\$script" ]]; then
    _message "git-completion-for-git-timeline.bash not found next to _git_timeline; run: make install-completions"
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

# 5. INSTALL COMLETION FILES
# ---

echo "Installing to: $completion_dir"

cp "$temp_dir/$bash_dest_name" "$completion_dir/$bash_dest_name" || {
    echo "ERROR: Could not install $bash_dest_name to $completion_dir"
    exit 1
}
echo "- Installed: $bash_dest_name"

cp "$temp_dir/_git_timeline" "$completion_dir/_git_timeline" || {
    echo "ERROR: Could not install _git_timeline to $completion_dir"
    exit 1
}
echo "- Installed: _git_timeline"

echo "To update completions after a git upgrade, re-run: make install-completions"
