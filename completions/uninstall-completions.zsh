## Uninstall Zsh completions for git-timeline.
##
## Usage: `BREW=1 zsh uninstall-completions.zsh`

# 1. DETECT ZSH COMPLETION DIRECTORY
# ---
# Must match the directory used at install time.

local completion_dir=""

if [[ "${BREW:-0}" == "1" ]]; then
    if (( ! $+commands[brew] )); then
        echo "ERROR: BREW=1 set but brew is not installed or not in PATH"
        exit 1
    fi
    completion_dir="$(brew --prefix)/share/zsh/site-functions"
else
    echo "ERROR: No environment variable set. Supported: BREW=1"
    exit 1
fi

# 2. SEARCH FOR COMPLETION FILES
# ---

local search_dirs=("$completion_dir")

local found_files=()

for dir in "${search_dirs[@]}"; do
    if [[ -d "$dir" ]]; then
        if [[ -f "$dir/_git_timeline" ]]; then
            found_files+=("$dir/_git_timeline")
        fi
    fi
done

# 3. CHECK IF COMPLETION FILES WERE FOUND
# ---

if (( ${#found_files[@]} == 0 )); then
    echo "No git-timeline completion files found. Searched in:"
    for dir in "${search_dirs[@]}"; do
        echo "- $dir"
    done
    exit 0
fi

# 4. DISPLAY COMPLETION FILES TO BE DELETED
# ---

echo "Found the following completion files:"
for file in "${found_files[@]}"; do
    echo "- $file"
done

# 5. ASK FOR CONFIRMATION BEFORE DELETING
# ---

echo "This will DELETE the files listed above"
if ! read -q "?Continue? (y/N) "; then
    echo ""
    echo "Cancelled. No files were deleted"
    exit 0
fi
echo ""

# 6. DELETE FILES
# ---

for file in "${found_files[@]}"; do
    if rm -f "$file" 2>/dev/null; then
        echo "Deleted: $file"
    else
        echo "Failed to delete: $file"
    fi
done

echo "Completion files uninstalled successfully"
