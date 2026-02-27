# API Reference

> This docs was updated at: 2026-02-23

The complete API documentation is generated from Javadoc.

!!! info "API Documentation Location"
    When built with the full pipeline (including `mvn javadoc:javadoc`), the Javadoc will be available here.
    
    For local builds, run:
    ```bash
    mvn javadoc:javadoc
    mkdir -p docs/api
    cp -r target/site/apidocs/* docs/api/
    ```

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
├── responses          # Core Responder and Response classes
├── responses.spec     # Request payload builders and DTOs
├── responses.dto      # Additional data transfer objects
├── agents             # Agent framework
├── tools              # Function calling tools
└── telemetry          # Observability integration
```

## Quick Links

- [Responder Guide](guides/responder.md)
- [Agents Guide](guides/agents.md)
- [Function Tools Guide](guides/tools.md)
