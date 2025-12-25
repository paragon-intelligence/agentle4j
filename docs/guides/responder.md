# Responder Guide

The `Responder` is the core low-level HTTP client for the OpenAI Responses API. It handles all API communication, streaming, and provider configuration.

## Overview

```mermaid
flowchart LR
    A[Your Code] --> B[Responder]
    B --> C[OkHttp Client]
    C --> D[API Provider]
    D --> C
    C --> B
    B --> A
```

## Creating a Responder

### Basic Configuration

```java
Responder responder = Responder.builder()
    .openRouter()
    .apiKey("your-api-key")
    .build();
```

### With Custom Configuration

```java
Responder responder = Responder.builder()
    .openRouter()
    .apiKey("your-api-key")
    .timeout(Duration.ofSeconds(60))
    .telemetry(new OtelProcessor(tracer, meter))
    .build();
```

## Supported Providers

| Provider | Builder Method | Description |
|----------|---------------|-------------|
| OpenRouter | `.openRouter()` | Multi-model gateway |
| OpenAI | `.openai()` | Direct OpenAI access |
| Groq | `.groq()` | Fast inference |
| Custom | `.custom(url)` | Any OpenAI-compatible API |

## Making Requests

### Synchronous (Blocking)

```java
Response response = responder.respond(payload).join();
System.out.println(response.outputText());
```

### Asynchronous

```java
responder.respond(payload)
    .thenAccept(response -> {
        System.out.println(response.outputText());
    })
    .exceptionally(error -> {
        error.printStackTrace();
        return null;
    });
```

## Building Payloads

The `CreateResponsePayload.builder()` provides a fluent API for constructing requests:

```java
var payload = CreateResponsePayload.builder()
    .model("openai/gpt-4o")
    .addDeveloperMessage("You are a helpful assistant.")  // System prompt
    .addUserMessage("Hello!")                              // User input
    .temperature(0.7)                                      // Creativity
    .maxTokens(1000)                                       // Response limit
    .build();
```

### Available Options

| Method | Description |
|--------|-------------|
| `.model(String)` | Set the model to use |
| `.addDeveloperMessage(String)` | Add system/developer instructions |
| `.addUserMessage(String)` | Add user message |
| `.addAssistantMessage(String)` | Add assistant message (for context) |
| `.temperature(double)` | Set temperature (0.0-2.0) |
| `.maxTokens(int)` | Limit response length |
| `.topP(double)` | Nucleus sampling |
| `.streaming()` | Enable streaming mode |
| `.withStructuredOutput(Class)` | Enable structured output |

## Response Object

The `Response` object contains all information about the API response:

```java
Response response = responder.respond(payload).join();

// Get the text output
String text = response.outputText();

// Get token usage
int inputTokens = response.usage().inputTokens();
int outputTokens = response.usage().outputTokens();

// Get the response ID
String id = response.id();

// Get the model used
String model = response.model();
```

## Error Handling

```java
responder.respond(payload)
    .thenAccept(response -> {
        // Handle success
    })
    .exceptionally(error -> {
        if (error.getCause() instanceof RateLimitException) {
            // Handle rate limiting
        } else if (error.getCause() instanceof AuthenticationException) {
            // Handle auth errors
        }
        return null;
    });
```

## Best Practices

!!! tip "Reuse Responder Instances"
    Create one `Responder` instance and reuse it across your application. It's thread-safe and maintains connection pooling.

!!! warning "API Key Security"
    Never hardcode API keys. Use environment variables or a secrets manager.

```java
// ✅ Good
String apiKey = System.getenv("OPENROUTER_API_KEY");

// ❌ Bad
String apiKey = "sk-xxxxx";
```

## Next Steps

- [Streaming Guide](streaming.md) - Real-time response streaming
- [Agents Guide](agents.md) - Higher-level agent abstraction
