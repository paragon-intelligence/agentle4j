# Changelog

All notable changes to Agentle4j will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).


## [0.3.0] - 2026-01-04

### Added

- **Sub-Agents (Agent-as-Tool)** - Invoke agents as tools within a parent agent's execution loop
  - `SubAgentTool` class for wrapping an Agent as a FunctionTool
  - `Agent.Builder.addSubAgent()` methods with description or config
  - `SubAgentTool.Config` for context sharing options:
    - `shareState(boolean)` - Share custom state like userId (default: true)
    - `shareHistory(boolean)` - Include full conversation context (default: false)
  - Tool naming follows `invoke_[agent_snake_case_name]` pattern

- **Prompt Management** - External prompt providers for managing agent instructions
  - `PromptProvider` interface for abstracting prompt sources
  - `FilesystemPromptProvider` - Load prompts from local files
  - `LangfusePromptProvider` - Fetch prompts from Langfuse API with:
    - Basic authentication support
    - Configurable retry with exponential backoff
    - Polymorphic response parsing (text and chat prompts)
  - `PromptProviderException` for prompt retrieval failures

- **Enhanced Prompt Templating** - Advanced template syntax in `Prompt.java`
  - Variable interpolation: `{{variable}}`
  - Nested properties: `{{user.name}}`
  - Conditional blocks: `{{#if condition}}...{{/if}}`
  - Iteration blocks: `{{#each items}}...{{/each}}`

### Improved

- **Test Coverage** - Increased overall coverage to 88%
  - Comprehensive tests for `SubAgentTool` (25 tests)
  - Enhanced `Agent.java` coverage (95%+)
  - `PromptProvider` implementations fully tested

### Documentation

- Added Sub-agents section to README and agents guide
- Added Prompt Management guide
- Enhanced Spring Boot integration guide with agent management patterns
- Improved multi-agent documentation with comparison tables

## [0.2.0] - 2025-12-27

### Added

- **Context Management** - Pluggable strategies to manage conversation context length
  - `ContextManagementConfig` - Configuration with builder pattern
  - `SlidingWindowStrategy` - Removes oldest messages when context exceeds limit
  - `SummarizationStrategy` - Summarizes older messages using an LLM
  - `TokenCounter` interface with `SimpleTokenCounter` (chars/4 estimation)
  
- **Retry Policies** - Configurable retry with exponential backoff
  - `RetryPolicy` class for customizing retry behavior
  - `Agent.Builder.retryPolicy()` and `maxRetries()` methods
  - Automatic retry for rate limits (429) and server errors (5xx)

- **Exception Hierarchy** - Structured error handling
  - `AgentleException` - Base exception for all library errors
  - `ApiException` - HTTP/API errors with status codes
  - `RateLimitException`, `AuthenticationException`, `ServerException`
  - `StreamingException`, `GuardrailException`, `ToolExecutionException`

- **Trace Correlation** - Automatic trace propagation across agents
  - Parent trace/span IDs flow through handoffs
  - `TelemetryContext` for custom metadata and tags
  - Multi-agent trace correlation in observability

### Changed

- Renamed `parsed()` to `outputParsed()` for consistency
- `FunctionToolCallOutput.json()` replaced with `FunctionToolCallOutput.success()` (explicit serialization)
- Responder builder now uses `baseUrl()` instead of `apiBaseUrl()`

### Documentation

- Added Spring Boot integration guide
- Enhanced observability guide with Langfuse examples
- Improved tool execution and vision examples

## [0.1.1] - 2025-12-26

### Added

- Per-tool confirmation for human-in-the-loop workflows
- Enhanced streaming guide with partial JSON parsing
- Vision documentation

## [0.1.0] - 2025-12-25

### Added

- 🎉 **Initial Release**
- Core `Responder` API for OpenAI Responses API
- Support for multiple providers (OpenRouter, OpenAI, Groq, custom endpoints)
- Real-time streaming with virtual threads
- Structured outputs with type-safe Java records
- Function calling with auto-generated JSON schemas
- Agent framework with:
  - Tools
  - Guardrails (input/output validation)
  - Memory (cross-conversation persistence)
  - Handoffs (multi-agent routing)
- `RouterAgent` for dedicated classification
- `ParallelAgents` for concurrent execution
- OpenTelemetry integration for observability
- Vision support (image input)
- Async-first API with `CompletableFuture`

### Dependencies

- OkHttp 5.x for HTTP client
- Jackson for JSON serialization
- SLF4J for logging
- JSpecify for nullability annotations

---

[0.3.0]: https://github.com/paragon-intelligence/agentle4j/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/paragon-intelligence/agentle4j/compare/v0.1.1...v0.2.0
[0.1.1]: https://github.com/paragon-intelligence/agentle4j/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/paragon-intelligence/agentle4j/releases/tag/v0.1.0
