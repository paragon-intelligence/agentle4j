# Streaming Failure Modes Guide

This guide documents edge cases and failure modes for streaming with partial JSON parsing. Understanding these scenarios helps you build resilient applications.

---

## Overview

| Failure Mode | What Happens | Mitigation |
|--------------|--------------|------------|
| Invalid intermediate JSON | Parser auto-closes and shows partial content | Works automatically - incomplete strings appear progressively |
| Schema drift mid-stream | Model returns unexpected fields | Jackson ignores unknown fields by default |
| Tool-call interrupts | Streaming pauses for tool execution | Handle `onToolCall` before final parsing |
| Connection drops | SSE connection terminates | Implement retry logic, use `onError` callback |

---

## 1. Partial JSON Parsing (How It Works)

### What Happens

During streaming, the model generates JSON character-by-character. Agentle's `PartialJsonParser` automatically "completes" incomplete JSON by closing unclosed strings and brackets.

**This means incomplete text content IS delivered progressively, not swallowed.**

### Step-by-Step Example

Imagine requesting a structured output for an article. Here's what happens on each text delta:

```
Step 1 - Received: {"title":"AI
         Completed: {"title":"AI"}
         Map: {"title": "AI"}

Step 2 - Received: {"title":"AI in Hea  
         Completed: {"title":"AI in Hea"}
         Map: {"title": "AI in Hea"}

Step 3 - Received: {"title":"AI in Healthcare","con
         Completed: {"title":"AI in Healthcare","con":null}
         Map: {"title": "AI in Healthcare"}

Step 4 - Received: {"title":"AI in Healthcare","content":"The future
         Completed: {"title":"AI in Healthcare","content":"The future"}
         Map: {"title": "AI in Healthcare", "content": "The future"}

Step 5 - Received: {"title":"AI in Healthcare","content":"The future of medicine is
         Completed: {"title":"AI in Healthcare","content":"The future of medicine is"}
         Map: {"title": "AI in Healthcare", "content": "The future of medicine is"}
```

> [!TIP]
> **Long text fields stream progressively!** As the model generates `"content":"The future..."`, each delta updates the map with the current partial text.

### Code Example

```java
responder.respond(articlePayload)
    .onPartialJson(fields -> {
        // Title appears first
        if (fields.containsKey("title")) {
            setTitle(fields.get("title").toString());
        }
        
        // Content streams progressively - updates many times!
        if (fields.containsKey("content")) {
            // This updates with partial text: "The", "The future", "The future of", etc.
            setContent(fields.get("content").toString());
        }
    })
    .onComplete(response -> showFinalArticle())
    .start();
```

### When You Get `null` Values

A field shows as `null` (or is absent from the map) in two cases:

1. **Key incomplete**: `{"titl` → can't determine key name yet
2. **Value not started**: `{"title":` → key exists but no value yet

```
Step: {"title":
Map: {} (empty - waiting for value)

Step: {"title":"
Map: {"title": ""} (empty string)

Step: {"title":"A
Map: {"title": "A"}
```

---

## 2. Schema Drift Mid-Stream

### What Happens

The model may return fields not defined in your schema, or omit expected fields.

```json
{"title":"AI","unexpectedField":"surprise","content":"..."}
```

### Current Behavior

By default, **Jackson ignores unknown fields**. Your partial type will parse successfully, with unknown fields discarded.

### Best Practices

```java
// Configure Jackson to be lenient (already the default)
ObjectMapper mapper = new ObjectMapper()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

// Or use @JsonIgnoreProperties on your type
@JsonIgnoreProperties(ignoreUnknown = true)
record PartialArticle(@Nullable String title, @Nullable String content) {}
```

> [!CAUTION]
> Do NOT use `FAIL_ON_UNKNOWN_PROPERTIES = true` with partial parsing - it will cause failures as different fields appear at different times.

---

## 3. Tool-Call Interrupts During Streaming

### What Happens

When streaming with tools enabled, the model may decide to call a tool. This interrupts text streaming:

```
User: "What's the weather in Tokyo?"
Streaming: "Let me check the weather" → [TOOL_CALL: get_weather] → Pause
Tool executes → Response continues
```

### Current Behavior

1. `onTextDelta` callbacks stop when tool call begins
2. `onToolCall` fires with tool name and arguments
3. If `withToolStore` is set, tool executes automatically
4. `onToolResult` fires (if configured)
5. `onComplete` fires with final response containing tool results

### Best Practices

```java
responder.respond(streamingPayload)
    .onTextDelta(delta -> {
        appendToUI(delta);  // May stop mid-sentence
    })
    .onToolCall((name, args) -> {
        showSpinner("Calling " + name + "...");  // Indicate pause
    })
    .withToolStore(toolStore)
    .onToolResult((name, result) -> {
        hideSpinner();
        // Tool executed, streaming may resume
    })
    .onComplete(response -> {
        // Final response includes tool results
        showFinalText(response.outputText());
    })
    .start();
```

> [!IMPORTANT]
> Don't assume `onTextDelta` will receive the complete response. For structured output with tools, always use `onParsedComplete` for the final typed result.

---

## 4. Connection Drops

### What Happens

The SSE (Server-Sent Events) connection may terminate unexpectedly due to:
- Network issues
- API rate limits
- Server errors
- Client timeouts

### Current Behavior

The `onError` callback fires with the exception. The stream does not auto-retry.

### Best Practices

```java
// Always provide an error handler
responder.respond(streamingPayload)
    .onTextDelta(System.out::print)
    .onError(error -> {
        if (isRetryable(error)) {
            // Implement retry with backoff
            scheduleRetry();
        } else {
            showErrorMessage(error.getMessage());
        }
    })
    .start();

// Helper to identify retryable errors
boolean isRetryable(Throwable error) {
    String msg = error.getMessage();
    return msg != null && (
        msg.contains("timeout") ||
        msg.contains("rate limit") ||
        msg.contains("503") ||
        msg.contains("Connection reset")
    );
}
```

---

## 5. Partial Output Before Error

### What Happens

The stream may produce some valid output before an error occurs:

```
Streaming: "Here is the answer: " → [ERROR: rate limit]
```

### Current Behavior

- `onTextDelta` receives partial content
- `onError` fires with the exception
- `onComplete` is NOT called
- Partial content is lost unless you buffered it

### Best Practices

```java
StringBuilder buffer = new StringBuilder();

responder.respond(streamingPayload)
    .onTextDelta(delta -> {
        buffer.append(delta);  // Buffer all output
        updateUI(buffer.toString());
    })
    .onError(error -> {
        // Partial content is in buffer
        if (buffer.length() > 0) {
            showPartialResult(buffer.toString());
        }
        showError(error.getMessage());
    })
    .start();
```

---

## Summary Checklist

- [ ] Use `@Nullable` fields in partial types
- [ ] Always provide `onError` callback
- [ ] Handle tool call interrupts gracefully
- [ ] Buffer partial output if you need to recover from errors
- [ ] Use `onParsedComplete` for the final typed result (not `onPartialParsed`)
- [ ] Don't combine `FAIL_ON_UNKNOWN_PROPERTIES = true` with partial parsing

---

## Related Guides

- [Streaming Guide](streaming.md) - Basic streaming patterns
- [Function Tools Guide](tools.md) - Tool calling with streaming
- [Observability Guide](observability.md) - Debugging with traces
