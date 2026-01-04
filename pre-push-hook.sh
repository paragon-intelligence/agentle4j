#!/bin/bash
# Pre-push hook: Format Java files with google-java-format before pushing to master
#
# INSTALLATION:
#   cp pre-push-hook.sh .git/hooks/pre-push
#   chmod +x .git/hooks/pre-push

# Get the directory where the script is located
REPO_ROOT=$(git rev-parse --show-toplevel)
FORMATTER_JAR="$REPO_ROOT/google-java-format.jar"

# Check if pushing to master/main
while read local_ref local_sha remote_ref remote_sha
do
    if [[ "$remote_ref" == *"refs/heads/master"* ]] || [[ "$remote_ref" == *"refs/heads/main"* ]]; then
        echo "🎨 Formatting Java files before push to master..."
        
        # Check if the formatter jar exists
        if [ ! -f "$FORMATTER_JAR" ]; then
            echo "❌ google-java-format.jar not found at $FORMATTER_JAR"
            exit 1
        fi
        
        # Find and format all Java files
        find "$REPO_ROOT/src" -name "*.java" -type f | while read file; do
            java -jar "$FORMATTER_JAR" --replace "$file"
        done
        
        # Check if there are any changes after formatting
        if ! git diff --quiet; then
            echo "📝 Formatting changes detected. Adding to commit..."
            git add -A
            git commit --amend --no-edit
            echo "✅ Code formatted and commit updated."
        else
            echo "✅ No formatting changes needed."
        fi
    fi
done

exit 0
