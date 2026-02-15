## Uninstall Zsh completions for git-timeline.

# 1. SEARCH FOR COMPLETION FILES
# ---

# Search in standard locations
local search_dirs=(
    "/opt/homebrew/share/zsh/site-functions"
    "/usr/share/zsh/site-functions"
    "/usr/local/share/zsh/site-functions"
    "$HOME/.zsh/completions"
)

local found_files=()

for dir in "${search_dirs[@]}"; do
    if [[ -d "$dir" ]]; then
        if [[ -f "$dir/_git_timeline" ]]; then
            found_files+=("$dir/_git_timeline")
        fi
        if [[ -f "$dir/git-completion-for-git-timeline.bash" ]]; then
            found_files+=("$dir/git-completion-for-git-timeline.bash")
        fi
    fi
done

# 2. CHECK IF COMPLETION FILES WERE FOUND
# ---

if (( ${#found_files[@]} == 0 )); then
    echo "No git-timeline completion files found. Searched in:"
    for dir in "${search_dirs[@]}"; do
        echo "- $dir"
    done
    exit 0
fi

# 3. DISPLAY COMPLETION FILES TO BE DELETED
# ---

echo "Found the following completion files:"
for file in "${found_files[@]}"; do
    echo "- $file"
done

# 4. ASK FOR CONFIRMATION BEFORE DELETING
# ---

echo "This will DELETE the files listed above"
if ! read -q "?Continue? (y/N) "; then
    echo ""
    echo "Cancelled. No files were deleted"
    exit 0
fi
echo ""
echo "Completion files uninstalled successfully"
