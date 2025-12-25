# Changelog

All notable changes to Agentle4j will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] - 2024-12-25

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

[Unreleased]: https://github.com/paragon-intelligence/agentle4j/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/paragon-intelligence/agentle4j/releases/tag/v0.1.0
