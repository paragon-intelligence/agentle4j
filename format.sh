#!/bin/bash
# Google Java Format - Code Formatter Script
# This script formats all Java files in the project using Google Java Format
# with Google's default style (2-space indentation)

echo "🎨 Formatting Java code with 2-space indentation..."

# Find and format all Java files with Google style (2 spaces)
google-java-format --replace $(find src -name "*.java")

echo "✅ Code formatting complete!"
echo ""
echo "📊 Summary of changes:"
git diff --stat src/ 2>/dev/null || echo "No git repository found or no changes made"
