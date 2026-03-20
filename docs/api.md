# API Reference

> This docs was updated at: 2026-03-20

















The API reference under [`docs/api/`](api/index.md) is generated from the Javadocs in
`src/main/java`.

!!! info "Generation Source Of Truth"
    The source of truth is the code and its Javadocs. Regenerate the reference with:
    ```bash
    make docs-gen
    ```
    To validate the full MkDocs site locally, run:
    ```bash
    make docs-build
    ```
    Avoid editing `docs/api/**` by hand without also fixing the originating Javadocs or generator.

## Main Classes

| Class | Description |
|-------|-------------|
| `Responder` | Core HTTP client for OpenAI Responses API |
| `Agent` | High-level agent abstraction with tools and guardrails |
| `AgenticContext` | Per-conversation state container |
| `CreateResponsePayload` | Fluent builder for request construction |
| `Response` | API response with output, usage, and metadata |
| `FunctionTool` | Base class for implementing tools |
| `FunctionToolStore` | Registry for callable tools |

## Package Structure

```
com.paragon
├── responses              # Core Responder API
├── responses.spec         # Request/response models and tool types
├── responses.streaming    # Response streaming
├── agents                 # Agent framework and multi-agent patterns
├── messaging              # WhatsApp messaging module
├── web                    # Web extraction
├── telemetry              # Observability integration
├── embeddings             # Embeddings providers
├── mcp                    # MCP client support
├── prompts                # Prompt management
└── harness                # Harness engineering utilities
```

## Quick Links

- [API Index](api/index.md)
- [Responder Guide](guides/responder.md)
- [Agents Guide](guides/agents.md)
- [Function Tools Guide](guides/tools.md)
