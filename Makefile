.PHONY: help build test test-all test-responder test-single clean format format-check compile package install run-tests-verbose help-all

# Default target - show help
.DEFAULT_GOAL := help

# Detect Operating System
ifeq ($(OS),Windows_NT)
    DETECTED_OS := Windows
    JDKS_DIR := $(USERPROFILE)\\.jdks
    PATH_SEP := ;
else
    DETECTED_OS := $(shell uname -s)
    JDKS_DIR := $(HOME)/.jdks
    PATH_SEP := :
endif

# Smart Java 25 Detection (Cross-Platform)
# Check if current Java version is 25, if not, find and use Java 25 from .jdks
ifeq ($(DETECTED_OS),Windows)
    # Windows: Use PowerShell
    CURRENT_JAVA_VERSION := $(shell powershell -NoProfile -Command "$$v = (java -version 2>&1 | Select-String -Pattern 'version'); if ($$v -match '\"(\d+)') { $$matches[1] }")
    JAVA25_DIR := $(shell powershell -NoProfile -Command "Get-ChildItem -Path '$(JDKS_DIR)' -Directory -Filter '*25*' -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName")
else
    # macOS/Linux: Use standard Unix commands
    CURRENT_JAVA_VERSION := $(shell java -version 2>&1 | grep -oE 'version "?[0-9]+' | grep -oE '[0-9]+' | head -1)
    JAVA25_DIR := $(shell find $(JDKS_DIR) -maxdepth 1 -type d -name '*25*' 2>/dev/null | head -1)
endif

ifneq ($(CURRENT_JAVA_VERSION),25)
    # Current Java is not 25, try to find Java 25 in .jdks
    ifneq ($(JAVA25_DIR),)
        export JAVA_HOME := $(JAVA25_DIR)
        ifeq ($(DETECTED_OS),Windows)
            export PATH := $(JAVA_HOME)\\bin$(PATH_SEP)$(PATH)
        else
            export PATH := $(JAVA_HOME)/bin$(PATH_SEP)$(PATH)
        endif
        $(info ℹ️  [$(DETECTED_OS)] Current Java is version $(CURRENT_JAVA_VERSION), using Java 25 from: $(JAVA25_DIR))
    else
        $(warning ⚠️  Java 25 not found in $(JDKS_DIR). Build may fail. Please install Java 25.)
    endif
else
    $(info ✅ Java 25 is already active on $(DETECTED_OS))
endif

# Google Java Format Configuration
JAVA_FORMATTER := java -jar google-java-format.jar
ifeq ($(DETECTED_OS),Windows)
    FORMAT_CMD = powershell -NoProfile -Command "Get-ChildItem -Path src -Recurse -Filter *.java | ForEach-Object { $$_.FullName } | ForEach-Object { & $(JAVA_FORMATTER) --replace $$_ }"
    FORMAT_CHECK_CMD = powershell -NoProfile -Command "$$files = Get-ChildItem -Path src -Recurse -Filter *.java; $$exitCode = 0; foreach ($$file in $$files) { & $(JAVA_FORMATTER) --dry-run --set-exit-if-changed $$file.FullName 2>&1 | Out-Null; if ($$LASTEXITCODE -ne 0) { $$exitCode = 1 } }; exit $$exitCode"
else
    FORMAT_CMD = find src -name "*.java" | xargs $(JAVA_FORMATTER) --replace
    FORMAT_CHECK_CMD = find src -name "*.java" | xargs $(JAVA_FORMATTER) --dry-run --set-exit-if-changed > /dev/null 2>&1
endif


# Colors for terminal output
BLUE := \033[0;34m
GREEN := \033[0;32m
YELLOW := \033[0;33m
RED := \033[0;31m
NC := \033[0m # No Color

##@ General

help: ## Display this help message
	@echo "$(BLUE)╔════════════════════════════════════════════════════════════╗$(NC)"
	@echo "$(BLUE)║           Agentle Java - Makefile Commands                 ║$(NC)"
	@echo "$(BLUE)╚════════════════════════════════════════════════════════════╝$(NC)"
	@echo ""
	@awk 'BEGIN {FS = ":.*##"; printf "Usage:\n  make $(YELLOW)<target>$(NC)\n"} /^[a-zA-Z_0-9-]+:.*?##/ { printf "  $(GREEN)%-20s$(NC) %s\n", $$1, $$2 } /^##@/ { printf "\n$(BLUE)%s$(NC)\n", substr($$0, 5) } ' $(MAKEFILE_LIST)
	@echo ""

##@ Building

compile: ## Compile the project
	@echo "$(BLUE)🔨 Compiling project...$(NC)"
	@mvn compile

build: ## Build the project (compile + tests)
	@echo "$(BLUE)🏗️  Building project...$(NC)"
	@mvn clean install

package: ## Package the project into JAR
	@echo "$(BLUE)📦 Packaging project...$(NC)"
	@mvn package

install: ## Install to local Maven repository
	@echo "$(BLUE)📥 Installing to local Maven repository...$(NC)"
	@mvn install

##@ Testing

test: ## Run all tests
	@echo "$(BLUE)🧪 Running all tests...$(NC)"
	@mvn test

test-responder: ## Run Responder tests only
	@echo "$(BLUE)🧪 Running Responder tests...$(NC)"
	@mvn test -Dtest=ResponderTest

test-single: ## Run a single test class (usage: make test-single CLASS=YourTestClass)
	@if [ -z "$(CLASS)" ]; then \
		echo "$(RED)❌ Error: CLASS parameter required$(NC)"; \
		echo "$(YELLOW)Usage: make test-single CLASS=YourTestClass$(NC)"; \
		exit 1; \
	fi
	@echo "$(BLUE)🧪 Running $(CLASS) tests...$(NC)"
	@mvn test -Dtest=$(CLASS)

test-verbose: ## Run tests with verbose output
	@echo "$(BLUE)🧪 Running tests (verbose)...$(NC)"
	@mvn test

test-coverage: ## Run tests with coverage report
	@echo "$(BLUE)📊 Running tests with coverage...$(NC)"
	@mvn clean test jacoco:report

##@ Code Quality

format: ## Format all Java code using Google Java Format (2-space indentation)
	@echo "$(BLUE)🎨 Formatting Java code with 2-space indentation...$(NC)"
	@$(FORMAT_CMD)
	@echo "$(GREEN)✅ Code formatting complete!$(NC)"

format-check: ## Check if code is properly formatted (without modifying)
	@echo "$(BLUE)🔍 Checking code formatting...$(NC)"
	@$(FORMAT_CHECK_CMD) && echo "$(GREEN)✅ All files are properly formatted!$(NC)" || (echo "$(RED)❌ Some files need formatting. Run 'make format' to fix.$(NC)" && exit 1)

verify: ## Verify the project (compile + test + package)
	@echo "$(BLUE)✓ Verifying project...$(NC)"
	@mvn verify

##@ Cleaning

clean: ## Clean build artifacts
	@echo "$(BLUE)🧹 Cleaning build artifacts...$(NC)"
	@mvn clean
	@echo "$(GREEN)✅ Clean complete!$(NC)"

clean-all: clean ## Clean everything including IDE files
	@echo "$(BLUE)🧹 Cleaning IDE files...$(NC)"
	@find . -type d -name ".idea" -exec rm -rf {} + 2>/dev/null || true
	@find . -type f -name "*.iml" -delete 2>/dev/null || true
	@find . -type d -name "target" -exec rm -rf {} + 2>/dev/null || true
	@echo "$(GREEN)✅ Deep clean complete!$(NC)"

##@ Development

watch-test: ## Watch for changes and run tests automatically (requires entr)
	@echo "$(BLUE)👀 Watching for changes...$(NC)"
	@echo "$(YELLOW)Press Ctrl+C to stop$(NC)"
	@find src -name "*.java" | entr -c mvn test -Dtest=ResponderTest

quick-test: ## Quick test run (no compilation, skip slow tests)
	@echo "$(BLUE)⚡ Running quick tests...$(NC)"
	@mvn surefire:test

dependency-tree: ## Show dependency tree
	@echo "$(BLUE)🌳 Dependency tree:$(NC)"
	@mvn dependency:tree

dependency-analyze: ## Analyze dependencies for issues
	@echo "$(BLUE)🔍 Analyzing dependencies...$(NC)"
	@mvn dependency:analyze

##@ Benchmarks

benchmark: ## Run full benchmark suite (instantiation time + memory)
	@echo "$(BLUE)⏱️  Running full benchmark suite...$(NC)"
	@mvn test-compile
	@mvn exec:java -Dexec.mainClass="com.paragon.benchmarks.BenchmarkRunner" -Dexec.classpathScope=test
	@echo "$(GREEN)✅ Benchmarks complete! Results in benchmark-results/$(NC)"

benchmark-quick: ## Run quick benchmarks (for development/CI)
	@echo "$(BLUE)⚡ Running quick benchmarks...$(NC)"
	@mvn test-compile
	@mvn exec:java -Dexec.mainClass="com.paragon.benchmarks.BenchmarkRunner" -Dexec.args="--quick" -Dexec.classpathScope=test
	@echo "$(GREEN)✅ Quick benchmarks complete!$(NC)"

benchmark-responder: ## Run Responder benchmarks only
	@echo "$(BLUE)⏱️  Running Responder benchmarks...$(NC)"
	@mvn test-compile
	@mvn exec:java -Dexec.mainClass="com.paragon.benchmarks.ResponderBenchmark" -Dexec.classpathScope=test

benchmark-agent: ## Run Operator (Agent) benchmarks only
	@echo "$(BLUE)⏱️  Running Operator benchmarks...$(NC)"
	@mvn test-compile
	@mvn exec:java -Dexec.mainClass="com.paragon.benchmarks.OperatorBenchmark" -Dexec.classpathScope=test

benchmark-memory: ## Run memory allocation benchmarks with GC profiler
	@echo "$(BLUE)📊 Running memory benchmarks...$(NC)"
	@mvn test-compile
	@mvn exec:java -Dexec.mainClass="com.paragon.benchmarks.MemoryBenchmark" -Dexec.classpathScope=test

##@ Information

info: ## Show project information
	@echo "$(BLUE)╔════════════════════════════════════════════════════════════╗$(NC)"
	@echo "$(BLUE)║                  Project Information                       ║$(NC)"
	@echo "$(BLUE)╚════════════════════════════════════════════════════════════╝$(NC)"
	@echo ""
	@echo "$(GREEN)Project:$(NC)      Agentle Java"
	@echo "$(GREEN)Group ID:$(NC)     com.paragon"
	@echo "$(GREEN)Artifact:$(NC)     agentle"
	@echo "$(GREEN)Version:$(NC)      1.0-SNAPSHOT"
	@echo "$(GREEN)Java:$(NC)         25 (with preview features)"
	@echo "$(GREEN)Build Tool:$(NC)   Maven"
	@echo ""
	@echo "$(BLUE)Useful Files:$(NC)"
	@echo "  • pom.xml         - Maven configuration"
	@echo "  • format.sh       - Code formatter script"
	@echo "  • Makefile        - This file"
	@echo ""

version: ## Show Maven and Java versions
	@echo "$(BLUE)Version Information:$(NC)"
	@mvn --version
	@echo ""
	@echo "$(BLUE)Google Java Format:$(NC)"
	@$(JAVA_FORMATTER) --version 2>&1 || echo "JAR not found"

##@ CI/CD

ci-build: clean ## CI build (clean + compile + test + package)
	@echo "$(BLUE)🚀 Running CI build pipeline...$(NC)"
	@mvn clean verify
	@echo "$(GREEN)✅ CI build complete!$(NC)"

ci-test: ## CI test (with coverage)
	@echo "$(BLUE)🧪 Running CI tests...$(NC)"
	@mvn clean test
	@echo "$(GREEN)✅ CI tests complete!$(NC)"

##@ Aliases (shortcuts)

t: test ## Shortcut for 'make test'

c: compile ## Shortcut for 'make compile'

b: build ## Shortcut for 'make build'

f: format ## Shortcut for 'make format'

h: help ## Shortcut for 'make help'
