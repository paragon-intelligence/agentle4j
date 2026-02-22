# :material-code-braces: ResponseStream

`com.paragon.responses.streaming.ResponseStream` &nbsp;·&nbsp; **Class**

---

A streaming response wrapper for OpenAI Responses API Server-Sent Events (SSE).

Provides a fluent, callback-based API for processing streaming events. Uses Java 21+ virtual
threads for non-blocking async processing.

### Usage Examples:

```java
// Simple text streaming
responder.respondStream(payload)
    .onTextDelta(System.out::print)
    .onComplete(response -> System.out.println("\nDone!"))
    .onError(Throwable::printStackTrace)
    .start();
// Wait for completion (blocking)
Response response = responder.respondStream(payload).get();
// Collect all text (blocking)
String text = responder.respondStream(payload).getText();
// Structured output streaming (blocking)
ParsedResponse parsed = responder.respondStream(structuredPayload)
    .getParsed();
```

## Methods

### `ResponseStream`

```java
public ResponseStream(
      @NonNull OkHttpClient httpClient,
      @NonNull Request request,
      @NonNull ObjectMapper objectMapper,
      @Nullable Class<T> responseType)
```

Creates a new ResponseStream.

**Parameters**

| Name | Description |
|------|-------------|
| `httpClient` | the OkHttp client |
| `request` | the prepared HTTP request |
| `objectMapper` | the Jackson ObjectMapper for event deserialization |
| `responseType` | the structured output type, or null for regular streaming |

---

### `onTextDelta`

```java
public @NonNull ResponseStream<T> onTextDelta(@NonNull Consumer<String> handler)
```

Registers a handler for text delta events. Called for each chunk of text as it streams.

**Parameters**

| Name | Description |
|------|-------------|
| `handler` | Consumer that receives text deltas |

**Returns**

this ResponseStream for chaining

---

### `onComplete`

```java
public @NonNull ResponseStream<T> onComplete(@NonNull Consumer<Response> handler)
```

Registers a handler for stream completion. Called once when the response is complete.

**Parameters**

| Name | Description |
|------|-------------|
| `handler` | Consumer that receives the final Response |

**Returns**

this ResponseStream for chaining

---

### `onError`

```java
public @NonNull ResponseStream<T> onError(@NonNull Consumer<Throwable> handler)
```

Registers a handler for errors. Called if an error occurs during streaming.

**Parameters**

| Name | Description |
|------|-------------|
| `handler` | Consumer that receives the error |

**Returns**

this ResponseStream for chaining

---

### `onEvent`

```java
public @NonNull ResponseStream<T> onEvent(@NonNull Consumer<StreamingEvent> handler)
```

Registers a handler for all streaming events. Called for every event received.

**Parameters**

| Name | Description |
|------|-------------|
| `handler` | Consumer that receives each StreamingEvent |

**Returns**

this ResponseStream for chaining

---

### `onParsedComplete`

```java
public @NonNull ResponseStream<T> onParsedComplete(@NonNull Consumer<ParsedResponse<T>> handler)
```

Registers a handler for typed completion (structured output only). Called once when the
response is complete, with parsed result.

This is the recommended callback for structured streaming as it provides direct access to
the parsed object.

Example:

```java
responder.respond(structuredPayload)
    .onTextDelta(System.out::print)
    .onParsedComplete(parsed -> {
        Person person = parsed.parsed();
        System.out.println("Name: " + person.name());
    })
    .start();
```

**Parameters**

| Name | Description |
|------|-------------|
| `handler` | Consumer that receives the ParsedResponse with typed content |

**Returns**

this ResponseStream for chaining

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalStateException` | if this is not a structured output stream |

---

### `onPartialParsed`

```java
public <P> @NonNull ResponseStream<T> onPartialParsed(
      @NonNull Class<P> partialType, @NonNull Consumer<P> handler)
```

Registers a handler for partial parsed updates during streaming. Called on each text delta with
a partially-filled instance.

This enables real-time UI updates as JSON fields are populated. The target class should have
all fields as `@Nullable` to accept partially-filled objects.

Example:

```java
record PartialPerson(@Nullable String name, @Nullable Integer age) {}
responder.respond(structuredPayload)
    .onPartialParsed(PartialPerson.class, partial -> {
        if (partial.name() != null) {
            updateNameField(partial.name());
        }
    })
    .start();
```

**Parameters**

| Name | Description |
|------|-------------|
| `partialType` | the nullable wrapper class for partial parsing |
| `handler` | Consumer that receives partially-filled instances |
| `<P>` | the partial type (should have all nullable fields) |

**Returns**

this ResponseStream for chaining

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalStateException` | if this is not a structured output stream |

---

### `onPartialJson`

```java
public @NonNull ResponseStream<T> onPartialJson(@NonNull Consumer<Map<String, Object>> handler)
```

Registers a handler for partial JSON updates during streaming as a Map.

This is the **zero-class** approach to partial parsing - no need to create a separate
partial class with nullable fields. Simply access fields as they become available.

Example:

```java
responder.respond(structuredPayload)
    .onPartialJson(fields -> {
        if (fields.containsKey("name")) {
            updateNameField(fields.get("name").toString());
        }
        if (fields.containsKey("age")) {
            updateAgeField((Integer) fields.get("age"));
        }
    })
    .start();
```

**Parameters**

| Name | Description |
|------|-------------|
| `handler` | Consumer that receives partially-parsed JSON as a Map |

**Returns**

this ResponseStream for chaining

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalStateException` | if this is not a structured output stream |

---

### `onToolCall`

```java
public @NonNull ResponseStream<T> onToolCall(@NonNull BiConsumer<String, String> handler)
```

Registers a handler for tool call detection during streaming. Called when a function tool call
is complete with its name and arguments.

Example:

```java
responder.respond(payload)
    .onToolCall((toolName, argsJson) -> {
        System.out.println("Tool called: " + toolName);
        System.out.println("Arguments: " + argsJson);
    })
    .start();
```

**Parameters**

| Name | Description |
|------|-------------|
| `handler` | BiConsumer that receives (tool name, JSON arguments) |

**Returns**

this ResponseStream for chaining

---

### `withToolStore`

```java
public @NonNull ResponseStream<T> withToolStore(@NonNull FunctionToolStore store)
```

Registers a FunctionToolStore for automatic tool execution during streaming.

When a tool call is detected and a matching tool is found in the store, it will be executed
automatically. Use with `.onToolResult` to receive execution results.

Example:

```java
var toolStore = FunctionToolStore.create()
    .add(new GetWeatherTool())
    .add(new GetTimeTool());
responder.respond(payload)
    .withToolStore(toolStore)
    .onToolResult((toolName, result) -> {
        System.out.println("Tool " + toolName + " returned: " + result.output());
    })
    .start();
```

**Parameters**

| Name | Description |
|------|-------------|
| `store` | the FunctionToolStore containing tool implementations |

**Returns**

this ResponseStream for chaining

---

### `onToolResult`

```java
public @NonNull ResponseStream<T> onToolResult(
      @NonNull BiConsumer<String, FunctionToolCallOutput> handler)
```

Registers a handler for tool execution results. Called after a tool is auto-executed via `.withToolStore`.

**Parameters**

| Name | Description |
|------|-------------|
| `handler` | BiConsumer that receives (tool name, tool output) |

**Returns**

this ResponseStream for chaining

---

### `start`

```java
public void start()
```

Starts streaming on a virtual thread. Non-blocking - returns immediately.

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalStateException` | if already started |

---

### `get`

```java
public @NonNull Response get()
```

Blocks until streaming completes and returns the final Response. Automatically starts streaming
if not already started.

On virtual threads, blocking is efficient and does not consume platform threads.

**Returns**

the final Response

**Throws**

| Type | Condition |
|------|-----------|
| `RuntimeException` | if streaming fails |

---

### `getParsed`

```java
public @NonNull ParsedResponse<T> getParsed()
```

Blocks until streaming completes and returns a parsed structured response. Only available for
structured output streams.

**Returns**

the ParsedResponse with typed content

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalStateException` | if this is not a structured output stream |
| `RuntimeException` | if streaming or parsing fails |

---

### `getText`

```java
public @NonNull String getText()
```

Collects all text deltas and returns the complete text. Blocks until streaming completes.

**Returns**

the concatenated text

**Throws**

| Type | Condition |
|------|-----------|
| `RuntimeException` | if streaming fails |

---

### `cancel`

```java
public void cancel()
```

Cancels the stream. Safe to call multiple times.

---

### `isCancelled`

```java
public boolean isCancelled()
```

Checks if the stream has been cancelled.

**Returns**

true if cancelled

