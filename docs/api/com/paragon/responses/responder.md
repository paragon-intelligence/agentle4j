# :material-code-braces: Responder

> This docs was updated at: 2026-02-23

`com.paragon.responses.Responder` &nbsp;·&nbsp; **Class**

---

Core class for sending requests to the Responses API.

Uses a synchronous-first API design that is optimized for Java 21+ virtual threads. All
blocking I/O operations are virtual-thread-friendly, allowing thousands of concurrent requests
without consuming platform threads.

Supports OpenTelemetry tracing through configurable telemetry processors. Telemetry is emitted
asynchronously and does not impact response latency.

When using OpenRouter, an optional `OpenRouterModelRegistry` can be provided to
calculate request costs based on token usage.

### Usage with Virtual Threads

```java
// Simple blocking call - cheap with virtual threads
Response response = responder.respond(payload);
// Parallel requests using virtual threads
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    var future1 = executor.submit(() -> responder.respond(payload1));
    var future2 = executor.submit(() -> responder.respond(payload2));
    Response r1 = future1.get();
    Response r2 = future2.get();
}
```

## Methods

### `respond`

```java
public @NonNull Response respond(@NonNull CreateResponsePayload payload)
```

Sends a request to the API and returns the response. Automatically generates a session ID for
telemetry.

This method blocks until the response is received. When running on virtual threads, blocking
is cheap and does not consume platform threads.

**Parameters**

| Name | Description |
|------|-------------|
| `payload` | the request payload |

**Returns**

the API response

**Throws**

| Type | Condition |
|------|-----------|
| `RuntimeException` | if the request fails after all retries |

---

### `respond`

```java
public @NonNull Response respond(
      @NonNull CreateResponsePayload payload, @Nullable TraceMetadata trace)
```

Sends a request to the API with trace metadata override.

**Parameters**

| Name | Description |
|------|-------------|
| `payload` | the request payload |
| `trace` | trace metadata (overrides instance-level configuration) |

**Returns**

the API response

---

### `respond`

```java
public @NonNull Response respond(
      @NonNull CreateResponsePayload payload, @NonNull TelemetryContext context)
```

Sends a request to the API with telemetry context for rich metadata. Automatically generates a
session ID.

**Parameters**

| Name | Description |
|------|-------------|
| `payload` | the request payload |
| `context` | telemetry context with user_id, tags, metadata |

**Returns**

the API response

---

### `respond`

```java
public @NonNull Response respond(
      @NonNull CreateResponsePayload payload,
      @NonNull TelemetryContext context,
      @Nullable TraceMetadata trace)
```

Sends a request to the API with telemetry context and trace metadata override.

**Parameters**

| Name | Description |
|------|-------------|
| `payload` | the request payload |
| `context` | telemetry context with user_id, tags, metadata |
| `trace` | trace metadata (overrides instance-level configuration) |

**Returns**

the API response

---

### `respond`

```java
public @NonNull Response respond(
      @NonNull CreateResponsePayload payload, @NonNull String sessionId)
```

Sends a request to the API with a specific session ID for telemetry correlation.

**Parameters**

| Name | Description |
|------|-------------|
| `payload` | the request payload |
| `sessionId` | unique identifier for this session (used for trace correlation) |

**Returns**

the API response

---

### `respond`

```java
public @NonNull Response respond(
      @NonNull CreateResponsePayload payload,
      @NonNull String sessionId,
      @Nullable TraceMetadata trace)
```

Sends a request to the API with session ID and trace metadata override.

**Parameters**

| Name | Description |
|------|-------------|
| `payload` | the request payload |
| `sessionId` | unique identifier for this session (used for trace correlation) |
| `trace` | trace metadata (overrides instance-level configuration) |

**Returns**

the API response

---

### `respond`

```java
public @NonNull Response respond(
      @NonNull CreateResponsePayload payload,
      @NonNull String sessionId,
      @NonNull TelemetryContext context)
```

Sends a request to the API with telemetry context for rich metadata.

**Parameters**

| Name | Description |
|------|-------------|
| `payload` | the request payload |
| `sessionId` | unique identifier for this session (used for trace correlation) |
| `context` | telemetry context with user_id, tags, metadata |

**Returns**

the API response

---

### `respond`

```java
public @NonNull Response respond(
      @NonNull CreateResponsePayload payload,
      @NonNull String sessionId,
      @NonNull TelemetryContext context,
      @Nullable TraceMetadata trace)
```

Sends a request to the API with full telemetry context and trace metadata.

This is the main respond method that all other overloads delegate to.

**Parameters**

| Name | Description |
|------|-------------|
| `payload` | the request payload |
| `sessionId` | unique identifier for this session (used for trace correlation) |
| `context` | telemetry context with user_id, tags, metadata |
| `trace` | trace metadata (overrides instance-level configuration) |

**Returns**

the API response

---

### `executeWithRetry`

```java
private @NonNull Response executeWithRetry(
      @NonNull Request request, @NonNull ResponseStartedEvent startedEvent, int attempt)
```

Executes an HTTP request with retry logic and exponential backoff.

Uses synchronous OkHttp calls which are virtual-thread-friendly.

---

### `sleepForRetry`

```java
private void sleepForRetry(int attempt)
```

Sleeps for the retry delay. Virtual-thread-friendly.

---

### `respond`

```java
public <T> @NonNull ParsedResponse<T> respond(CreateResponsePayload.Structured<T> payload)
```

Sends a structured output request and parses the response.

**Parameters**

| Name | Description |
|------|-------------|
| `payload` | the structured request payload |
| `<T>` | the type to parse the response into |

**Returns**

the parsed response

---

### `respond`

```java
public <T> @NonNull ParsedResponse<T> respond(
      CreateResponsePayload.Structured<T> payload, @Nullable TraceMetadata trace)
```

Sends a structured output request with trace metadata override.

**Parameters**

| Name | Description |
|------|-------------|
| `payload` | the structured request payload |
| `trace` | trace metadata (overrides instance-level configuration) |
| `<T>` | the type to parse the response into |

**Returns**

the parsed response

---

### `respond`

```java
public <T> @NonNull ParsedResponse<T> respond(
      CreateResponsePayload.Structured<T> payload, @NonNull String sessionId)
```

Sends a structured output request with session ID and parses the response.

**Parameters**

| Name | Description |
|------|-------------|
| `payload` | the structured request payload |
| `sessionId` | unique identifier for this session |
| `<T>` | the type to parse the response into |

**Returns**

the parsed response

---

### `respond`

```java
public <T> @NonNull ParsedResponse<T> respond(
      CreateResponsePayload.Structured<T> payload,
      @NonNull String sessionId,
      @Nullable TraceMetadata trace)
```

Sends a structured output request with session ID and trace metadata.

**Parameters**

| Name | Description |
|------|-------------|
| `payload` | the structured request payload |
| `sessionId` | unique identifier for this session |
| `trace` | trace metadata (overrides instance-level configuration) |
| `<T>` | the type to parse the response into |

**Returns**

the parsed response

---

### `respond`

```java
public @NonNull Response respond(String input)
```

Simple text-only respond method.

**Parameters**

| Name | Description |
|------|-------------|
| `input` | the user message |

**Returns**

the API response

---

### `respond`

```java
public @NonNull Response respond(String input, @Nullable TraceMetadata trace)
```

Simple text-only respond method with trace metadata.

**Parameters**

| Name | Description |
|------|-------------|
| `input` | the user message |
| `trace` | trace metadata (overrides instance-level configuration) |

**Returns**

the API response

---

### `shutdown`

```java
public void shutdown()
```

Shuts down telemetry processors gracefully. Call this when the Responder is no longer needed.

---

### `provider`

```java
public @Nullable ResponsesAPIProvider provider()
```

Returns the API provider used by this responder.

**Returns**

the provider, or null if a custom base URL was used

---

### `retryPolicy`

```java
public @NonNull RetryPolicy retryPolicy()
```

Returns the retry policy configured for this responder.

**Returns**

the retry policy

---

### `traceMetadata`

```java
public @Nullable TraceMetadata traceMetadata()
```

Returns the default trace metadata for this responder.

**Returns**

the trace metadata, or null if not set

---

### `baseUrlString`

```java
public @NonNull String baseUrlString()
```

Returns the base URL string for API requests.

**Returns**

the base URL as a string

---

### `respond`

```java
public ResponseStream<Void> respond(CreateResponsePayload.Streaming payload)
```

Sends a streaming request to the API and returns a ResponseStream for processing events. Uses
virtual threads for non-blocking async streaming.

The builder automatically returns a Streaming payload when stream=true:

```java
var payload = CreateResponsePayload.builder()
    .model("gpt-4o")
    .addUserMessage("Hello")
    .stream(true)
    .build();
responder.respond(payload)
    .onTextDelta(System.out::print)
    .onComplete(response -> System.out.println("\nDone!"))
    .onError(Throwable::printStackTrace)
    .start();
```

**Parameters**

| Name | Description |
|------|-------------|
| `payload` | the streaming request payload |

**Returns**

a ResponseStream for processing streaming events

---

### `respond`

```java
public ResponseStream<Void> respond(
      CreateResponsePayload.Streaming payload, @NonNull String sessionId)
```

Sends a streaming request with a specific session ID for telemetry correlation.

**Parameters**

| Name | Description |
|------|-------------|
| `payload` | the streaming request payload |
| `sessionId` | unique identifier for this session (used for trace correlation) |

**Returns**

a ResponseStream for processing streaming events

---

### `respond`

```java
public <T> ResponseStream<T> respond(CreateResponsePayload.StructuredStreaming<T> payload)
```

Sends a structured output streaming request. Returns a ResponseStream that can parse the final
response to the structured type.

Example usage:

```java
var payload = CreateResponsePayload.builder()
    .model("gpt-4o")
    .addUserMessage("Give me a JSON person")
    .stream(true)
    .withStructuredOutput(Person.class)
    .build();
ParsedResponse parsed = responder.respond(payload)
    .onTextDelta(System.out::print)
    .toParsedFuture()
    .get();
```

**Parameters**

| Name | Description |
|------|-------------|
| `payload` | the structured streaming request payload |
| `<T>` | the structured output type |

**Returns**

a ResponseStream for processing streaming events with structured parsing

---

### `respond`

```java
public <T> ResponseStream<T> respond(
      CreateResponsePayload.StructuredStreaming<T> payload, @NonNull String sessionId)
```

Sends a structured output streaming request with session ID.

**Parameters**

| Name | Description |
|------|-------------|
| `payload` | the structured streaming request payload |
| `sessionId` | unique identifier for this session |
| `<T>` | the structured output type |

**Returns**

a ResponseStream for processing streaming events with structured parsing

---

### `baseUrl`

```java
public Builder baseUrl(@NonNull HttpUrl baseUrl)
```

Sets a custom base URL for API requests.

---

### `addTelemetryProcessor`

```java
public Builder addTelemetryProcessor(@NonNull TelemetryProcessor processor)
```

Adds a telemetry processor for OpenTelemetry tracing.

**Parameters**

| Name | Description |
|------|-------------|
| `processor` | the processor to add (e.g., LangfuseProcessor, GrafanaProcessor) |

**Returns**

this builder

---

### `retryPolicy`

```java
public Builder retryPolicy(@NonNull RetryPolicy retryPolicy)
```

Sets the retry policy for handling transient failures.

By default, retries are enabled with 3 attempts and exponential backoff. Use `RetryPolicy.disabled()` to disable retries.

**Parameters**

| Name | Description |
|------|-------------|
| `retryPolicy` | the retry policy to use |

**Returns**

this builder

---

### `maxRetries`

```java
public Builder maxRetries(int maxRetries)
```

Sets the maximum number of retry attempts with default backoff settings.

This is a convenience method equivalent to:

```java
.retryPolicy(RetryPolicy.builder().maxRetries(n).build())
```

**Parameters**

| Name | Description |
|------|-------------|
| `maxRetries` | maximum retry attempts (0 = no retries) |

**Returns**

this builder

---

### `traceMetadata`

```java
public Builder traceMetadata(@Nullable TraceMetadata traceMetadata)
```

Sets the default trace metadata for all requests.

This can be overridden per-request by passing trace metadata to respond() methods.

**Parameters**

| Name | Description |
|------|-------------|
| `traceMetadata` | the trace metadata to use by default |

**Returns**

this builder

