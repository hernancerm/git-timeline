## Uninstall Zsh completions for git-timeline.
##
## This script:
## 1. Finds installed git-timeline completion files.
## 2. Shows user which files will be deleted.
## 3. Prompts for confirmation (y/n).
## 4. Deletes files only if user confirms.

# 1. SEARCH FOR COMPLETION FILES
# ---

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

# 5. DELETE FILES
# ---

for file in "${found_files[@]}"; do
    if rm -f "$file" 2>/dev/null; then
        echo "Deleted: $file"
    else
        echo "Failed to delete: $file"
    fi
done

echo "Completion files uninstalled successfully"
