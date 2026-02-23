# Responder Guide

> This docs was updated at: 2026-02-23

The `Responder` is the core HTTP client for the OpenAI Responses API. It handles all API communication, streaming, telemetry, and provider configuration.

---

## Overview

```mermaid
flowchart LR
    subgraph Your Application
        A[Your Code]
    end
    
    subgraph Agentle4j
        B[Responder]
        C[Payload Builder]
        D[OkHttp Client]
    end
    
    subgraph Providers
        E[OpenRouter]
        F[OpenAI]
        G[Groq]
        H[Custom API]
    end
    
    A --> B --> C --> D
    D <--> E
    D <--> F
    D <--> G
    D <--> H
```

The `Responder` is **thread-safe** and **reusable**. Create one instance and share it across your application.

---

## Creating a Responder

### Basic Setup

```java
// Minimal configuration
Responder responder = Responder.builder()
    .openRouter()
    .apiKey("your-api-key")
    .build();
```

### Full Configuration

```java
import java.time.Duration;

Responder responder = Responder.builder()
    .openRouter()
    .apiKey(System.getenv("OPENROUTER_API_KEY"))
    
    // Telemetry (optional)
    .addTelemetryProcessor(LangfuseProcessor.fromEnv())
    
    .build();
```

### Configuration Options

| Option | Default | Description |
|--------|---------|-------------|
| `.apiKey(String)` | Required | Your API key |
| `.addTelemetryProcessor(TelemetryProcessor)` | None | Observability integration |
| `.maxRetries(int)` | 3 | Max retry attempts for transient failures |
| `.retryPolicy(RetryPolicy)` | defaults() | Full retry configuration |

---

## Retry Configuration

Responder automatically retries on transient failures with exponential backoff:

### Default Behavior

By default, Responder retries **3 times** on:
- **429** - Rate limiting
- **500, 502, 503, 504** - Server errors
- Network failures (connection timeout, etc.)

### Simple Configuration

```java
// Set max retries (uses default backoff settings)
Responder responder = Responder.builder()
    .openRouter()
    .apiKey(apiKey)
    .maxRetries(5)  // Retry up to 5 times
    .build();

// Disable retries
Responder responder = Responder.builder()
    .openRouter()
    .apiKey(apiKey)
    .maxRetries(0)  // No retries
    .build();
```

### Advanced Configuration

```java
import com.paragon.responses.RetryPolicy;

RetryPolicy policy = RetryPolicy.builder()
    .maxRetries(5)
    .initialDelay(Duration.ofMillis(500))  // Start with 500ms delay
    .maxDelay(Duration.ofSeconds(30))      // Cap at 30 seconds
    .multiplier(2.0)                       // Double delay each retry
    .retryableStatusCodes(Set.of(429, 503)) // Only retry these codes
    .build();

Responder responder = Responder.builder()
    .openRouter()
    .apiKey(apiKey)
    .retryPolicy(policy)
    .build();
```

### Backoff Calculation

Retry delays follow exponential backoff:

| Attempt | Delay (default) |
|---------|----------------|
| 1 | 1 second |
| 2 | 2 seconds |
| 3 | 4 seconds |
| 4+ | Up to 30 seconds (max) |

---

## Supported Providers

=== "OpenRouter"

    Access 300+ models through a single API:
    
    ```java
    Responder responder = Responder.builder()
        .openRouter()
        .apiKey(System.getenv("OPENROUTER_API_KEY"))
        .build();
    ```
    
    **Available Models (examples):**
    - `openai/gpt-4o`, `openai/gpt-4o-mini`
    - `anthropic/claude-3.5-sonnet`, `anthropic/claude-3-opus`
    - `google/gemini-pro`, `google/gemini-1.5-pro`
    - `meta-llama/llama-3.1-70b-instruct`
    - [See all models →](https://openrouter.ai/models)

=== "OpenAI Direct"

    ```java
    Responder responder = Responder.builder()
        .openAi()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .build();
    ```
    
    **Available Models:**
    - `gpt-4o`, `gpt-4o-mini`
    - `gpt-4-turbo`
    - `o1-preview`, `o1-mini`

=== "Groq"

    Ultra-fast inference:
    
    ```java
    Responder responder = Responder.builder()
        .baseUrl(HttpUrl.parse("https://api.groq.com/openai/v1"))
        .apiKey(System.getenv("GROQ_API_KEY"))
        .build();
    ```
    
    **Available Models:**
    - `llama-3.1-70b-versatile`
    - `llama-3.1-8b-instant`
    - `mixtral-8x7b-32768`

=== "Custom Endpoint"

    Any OpenAI-compatible API:
    
    ```java
    Responder responder = Responder.builder()
        .baseUrl(HttpUrl.parse("https://api.your-company.com/v1"))
        .apiKey("your-key")
        .build();
    ```

---

## Building Payloads

The `CreateResponsePayload.builder()` provides a fluent API:

### Basic Payload

```java
var payload = CreateResponsePayload.builder()
    .model("openai/gpt-4o")
    .addDeveloperMessage("You are a helpful assistant.")
    .addUserMessage("Hello!")
    .build();
```

### All Options

```java
var payload = CreateResponsePayload.builder()
    // Required
    .model("openai/gpt-4o")
    
    // Messages
    .addDeveloperMessage("System prompt here")  // First message (optional)
    .addUserMessage("User's question")           // User input
    .addAssistantMessage("Previous response")    // For multi-turn
    
    // Generation parameters
    .temperature(0.7)          // Creativity (0.0-2.0)
    .topP(0.9)                 // Nucleus sampling
    .maxTokens(1000)           // Response length limit
    .presencePenalty(0.0)      // Reduce repetition
    .frequencyPenalty(0.0)     // Reduce common tokens
    
    // Advanced
    .user("user-123")          // User identifier for abuse detection
    
    .build();
```

### Parameter Reference

| Parameter | Range | Description |
|-----------|-------|-------------|
| `temperature` | 0.0-2.0 | Higher = more creative, lower = more focused |
| `topP` | 0.0-1.0 | Nucleus sampling threshold |
| `maxTokens` | 1+ | Maximum response tokens |
| `presencePenalty` | -2.0 to 2.0 | Penalize tokens already in context |
| `frequencyPenalty` | -2.0 to 2.0 | Penalize tokens by frequency |

### Temperature Examples

```java
// Factual/deterministic (code generation, Q&A)
.temperature(0.0)

// Balanced (general chat)
.temperature(0.7)

// Creative (stories, brainstorming)
.temperature(1.2)
```

---

## Making Requests

### Synchronous (Default)

```java
// Simple blocking call - efficient with Virtual Threads
Response response = responder.respond(payload);
System.out.println(response.outputText());
```

### Parallel Requests

With Java 25 Virtual Threads, you can efficiently run parallel requests:

```java
import java.util.concurrent.*;

// Run requests in parallel using virtual threads
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    var future1 = executor.submit(() -> responder.respond(payload1));
    var future2 = executor.submit(() -> responder.respond(payload2));
    var future3 = executor.submit(() -> responder.respond(payload3));
    
    Response response1 = future1.get();
    Response response2 = future2.get();
    Response response3 = future3.get();
}
```

---

## Streaming

Enable streaming for real-time responses:

```java
var payload = CreateResponsePayload.builder()
    .model("openai/gpt-4o")
    .addUserMessage("Write a poem about Java")
    .streaming()  // Enable streaming
    .build();

responder.respond(payload)
    .onTextDelta(delta -> {
        System.out.print(delta);
        System.out.flush();
    })
    .onComplete(response -> {
        System.out.println("\n\nTokens: " + response.usage().totalTokens());
    })
    .onError(Throwable::printStackTrace)
    .start();
```

### Streaming Callbacks

| Callback | When Called |
|----------|-------------|
| `.onTextDelta(String)` | Each text chunk arrives |
| `.onComplete(Response)` | Stream finished successfully |
| `.onError(Throwable)` | Error occurred |

---

## Structured Output

Get type-safe JSON responses:

```java
public record Person(String name, int age, String occupation) {}

var payload = CreateResponsePayload.builder()
    .model("openai/gpt-4o")
    .addUserMessage("Create a fictional software engineer")
    .withStructuredOutput(Person.class)
    .build();

ParsedResponse<Person> response = responder.respond(payload);
Person person = response.outputParsed();
```

---

## Response Object

The `Response` contains all available information:

```java
Response response = responder.respond(payload);

// Text output
String text = response.outputText();

// Token usage
Usage usage = response.usage();
int inputTokens = usage.inputTokens();
int outputTokens = usage.outputTokens();
int totalTokens = usage.totalTokens();

// Metadata
String id = response.id();
String model = response.model();
long createdAt = response.createdAt();

// Output items (for complex responses)
List<ResponseOutputItem> items = response.output();
```

---

## Error Handling

Agentle provides a **structured exception hierarchy** for type-safe error handling.

### Exception Types

| Exception | When Thrown | Retryable? |
|-----------|-------------|------------|
| `RateLimitException` | API rate limited (429) | ✅ Yes |
| `AuthenticationException` | Invalid API key (401/403) | ❌ No |
| `ServerException` | Server error (5xx) | ✅ Yes |
| `InvalidRequestException` | Bad request (4xx) | ❌ No |
| `StreamingException` | Connection dropped during streaming | ✅ Usually |
| `ConfigurationException` | Missing required config | ❌ No |

### Type-Safe Error Handling

```java
try {
    Response response = responder.respond(payload);
} catch (RateLimitException e) {
    System.err.println("Rate limited. Retry after: " + e.retryAfter());
} catch (AuthenticationException e) {
    System.err.println("Auth failed: " + e.suggestion());
} catch (ServerException e) {
    System.err.println("Server error " + e.statusCode());
} catch (ApiException e) {
    System.err.println("API error: " + e.getMessage());
}
```

### Checking if Retryable

All Agentle exceptions implement `isRetryable()`:

```java
if (error instanceof AgentleException e && e.isRetryable()) {
    // Safe to retry - the built-in retry policy will have already tried
    // This only fires after all retries are exhausted
}
```

> [!TIP]
> The built-in retry policy automatically retries rate limits (429) and server errors (5xx) with exponential backoff. You only need to handle errors that remain after all retries.

### Streaming Error Recovery

```java
responder.respond(streamingPayload)
    .onError(error -> {
        if (error instanceof StreamingException se) {
            // Recover partial output
            String partial = se.partialOutput();
            if (partial != null) {
                savePartialOutput(partial);
            }
            System.err.println("Streaming failed after " + se.bytesReceived() + " bytes");
        }
    })
    .start();
```

---

## Best Practices

### ✅ Do

```java
// Reuse the Responder instance
private final Responder responder;

public MyService(String apiKey) {
    this.responder = Responder.builder()
        .openRouter()
        .apiKey(apiKey)
        .build();
}

// Load API keys from environment
String apiKey = System.getenv("OPENROUTER_API_KEY");

// Handle errors appropriately
try {
    Response response = responder.respond(payload);
} catch (ApiException e) {
    // handle
}
```

### ❌ Don't

```java
// Don't create new Responder for each request
public String chat(String message) {
    Responder r = Responder.builder()...build();  // Bad!
    return r.respond(payload).join().outputText();
}

// Don't hardcode API keys
Responder responder = Responder.builder()
    .apiKey("sk-xxxxxxxxxxxxx")  // Bad!
    .build();

// Don't ignore errors
Response response = responder.respond(payload);  // Add error handling!
```

---

## Next Steps

- [Agents Guide](agents.md) - Higher-level agent abstraction
- [Streaming Guide](streaming.md) - Advanced streaming patterns
- [Function Tools Guide](tools.md) - Let AI call your functions
