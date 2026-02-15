#!/bin/bash

# ============================================================================
# uninstall-completions.sh - Uninstall zsh completions for git-timeline
#
# This script:
# 1. Finds installed git-timeline completion files
# 2. Shows user which files will be deleted
# 3. Prompts for confirmation (y/n)
# 4. Deletes files only if user confirms
# ============================================================================

echo "Looking for installed git-timeline completion files..."
echo ""

# Search in standard locations
search_dirs=(
    "/opt/homebrew/share/zsh/site-functions"
    "/usr/share/zsh/site-functions"
    "/usr/local/share/zsh/site-functions"
    "$HOME/.zsh/completions"
)

found_files=()

for dir in "${search_dirs[@]}"; do
    if [ -d "$dir" ]; then
        if [ -f "$dir/_git-timeline" ]; then
            found_files+=("$dir/_git-timeline")
        fi
        if [ -f "$dir/git-completion.bash" ]; then
            found_files+=("$dir/git-completion.bash")
        fi
    fi
done

# ============================================================================
# CHECK IF FILES WERE FOUND
# ============================================================================

if [ ${#found_files[@]} -eq 0 ]; then
    echo "No git-timeline completion files found."
    echo ""
    echo "Searched in:"
    for dir in "${search_dirs[@]}"; do
        echo "  - $dir"
    done
    echo ""
    exit 0
fi

# ============================================================================
# DISPLAY FILES TO BE DELETED
# ============================================================================

echo "Found the following completion files:"
echo ""
for file in "${found_files[@]}"; do
    echo "  - $file"
done
echo ""

# ============================================================================
# PROMPT USER FOR CONFIRMATION
# ============================================================================

echo "This will DELETE the files listed above."
echo ""
read -p "Continue? (y/N) " -n 1 -r response
echo ""

if [[ ! "$response" =~ ^[Yy]$ ]]; then
    echo "Cancelled. No files were deleted."
    exit 0
fi

# ============================================================================
# DELETE FILES
# ============================================================================

echo ""
echo "Deleting completion files..."

for file in "${found_files[@]}"; do
    if rm -f "$file" 2>/dev/null; then
        echo "✓ Deleted: $file"
    else
        echo "✗ Failed to delete: $file"
    fi
done

echo ""
echo "=========================================="
echo "✓ Completions uninstalled successfully!"
echo "=========================================="
echo ""
echo "Next steps:"
echo "  1. Restart your zsh shell (exec zsh) or open a new terminal"
echo "  2. Completions will no longer be available for git-timeline"
echo ""
