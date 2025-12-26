# Streaming Guide

Agentle4j provides real-time streaming using virtual threads for efficient, non-blocking I/O. This guide covers all streaming patterns.

---

## Why Stream?

| Benefit | Description |
|---------|-------------|
| **Better UX** | Users see responses immediately |
| **Lower Latency** | First token appears faster |
| **Progress Visibility** | Users know the AI is working |
| **Early Cancellation** | Stop long responses if not needed |

---

## Basic Streaming

### Enable Streaming

Add `.streaming()` to your payload builder:

```java
var payload = CreateResponsePayload.builder()
    .model("openai/gpt-4o")
    .addUserMessage("Write a short poem about Java programming")
    .streaming()  // Enable streaming mode
    .build();

responder.respond(payload)
    .onTextDelta(delta -> {
        System.out.print(delta);  // Print each chunk
        System.out.flush();       // Flush for immediate display
    })
    .onComplete(response -> {
        System.out.println("\n\n✅ Complete!");
        System.out.println("Total tokens: " + response.usage().totalTokens());
    })
    .onError(error -> {
        System.err.println("Error: " + error.getMessage());
    })
    .start();  // Don't forget to call start()!
```

### Callback Reference

| Callback | When Called | Parameter |
|----------|-------------|-----------|
| `.onTextDelta(String)` | Each text chunk | Text delta |
| `.onComplete(Response)` | Stream finished | Full response |
| `.onError(Throwable)` | Error occurred | Exception |

---

## Streaming with Progress Tracking

```java
import java.util.concurrent.atomic.AtomicInteger;

var payload = CreateResponsePayload.builder()
    .model("openai/gpt-4o")
    .addUserMessage("Explain microservices architecture in detail")
    .streaming()
    .build();

AtomicInteger charCount = new AtomicInteger(0);
long startTime = System.currentTimeMillis();

responder.respond(payload)
    .onTextDelta(delta -> {
        System.out.print(delta);
        System.out.flush();
        charCount.addAndGet(delta.length());
    })
    .onComplete(response -> {
        long elapsed = System.currentTimeMillis() - startTime;
        
        System.out.println("\n\n--- Stats ---");
        System.out.println("Characters: " + charCount.get());
        System.out.println("Tokens: " + response.usage().totalTokens());
        System.out.println("Time: " + elapsed + " ms");
        System.out.println("Speed: " + (charCount.get() * 1000 / elapsed) + " chars/sec");
    })
    .start();
```

---

## Structured Streaming

Stream while generating structured JSON output:

```java
// Define output schema
public record Article(
    String title,
    String content,
    List<String> tags,
    int readingTimeMinutes
) {}

// Build structured streaming payload
var payload = CreateResponsePayload.builder()
    .model("openai/gpt-4o")
    .addUserMessage("Write an article about AI in healthcare")
    .withStructuredOutput(Article.class)
    .streaming()  // Enable streaming
    .build();

responder.respond(payload)
    .onTextDelta(delta -> {
        // Watch JSON being generated character by character
        System.out.print(delta);
        System.out.flush();
    })
    .onParsedComplete(parsed -> {
        // Get the final typed object
        Article article = parsed.outputParsed();
        System.out.println("\n\n--- Parsed Result ---");
        System.out.println("Title: " + article.title());
        System.out.println("Tags: " + String.join(", ", article.tags()));
        System.out.println("Reading time: " + article.readingTimeMinutes() + " min");
    })
    .start();
```

### onPartialJson - Zero-Class Approach

Access fields as they stream without defining extra classes:

```java
responder.respond(structuredPayload)
    .onPartialJson(fields -> {
        // fields is a Map<String, Object>
        if (fields.containsKey("title")) {
            updateTitleUI(fields.get("title").toString());
        }
        if (fields.containsKey("content")) {
            updateContentUI(fields.get("content").toString());
        }
    })
    .start();
```

### onPartialParsed - Typed Partial Updates

For type-safe partial updates, define a nullable mirror class:

```java
// Nullable mirror class for partial parsing
record PartialArticle(
    @Nullable String title,
    @Nullable String content,
    @Nullable List<String> tags,
    @Nullable Integer readingTimeMinutes
) {}

responder.respond(structuredPayload)
    .onPartialParsed(PartialArticle.class, partial -> {
        if (partial.title() != null) {
            updateTitleUI(partial.title());
        }
        if (partial.content() != null) {
            appendContentUI(partial.content());
        }
    })
    .start();
```

---

## Streaming with Tool Calls

Handle tool calls in real-time during streaming:

```java
// Add tools to the payload
var payload = CreateResponsePayload.builder()
    .model("openai/gpt-4o")
    .addUserMessage("What's the weather in Tokyo and calculate 15% tip on $85.50?")
    .addTool(weatherTool)
    .addTool(calculatorTool)
    .streaming()
    .build();

responder.respond(payload)
    .onTextDelta(delta -> {
        System.out.print(delta);
        System.out.flush();
    })
    // Detect when tool call is identified
    .onToolCall((toolName, argsJson) -> {
        System.out.println("\n🔧 Tool called: " + toolName);
        System.out.println("   Arguments: " + argsJson);
    })
    // Auto-execute tools with tool store
    .withToolStore(toolStore)
    // Get tool results
    .onToolResult((toolName, result) -> {
        System.out.println("✅ " + toolName + " returned: " + result.output());
    })
    .onComplete(response -> {
        System.out.println("\n\n" + response.outputText());
    })
    .onError(e -> System.err.println("Error: " + e.getMessage()))
    .start();
```

---

## Agent Streaming

Full agentic loop with streaming and all events:

```java
agent.interactStream("Research and summarize AI trends, then email me the report")
    // Turn lifecycle
    .onTurnStart(turn -> {
        System.out.println("\n=== Turn " + turn + " ===");
    })
    .onTurnComplete(response -> {
        System.out.println("[Turn complete]");
    })
    
    // Text streaming
    .onTextDelta(delta -> {
        System.out.print(delta);
        System.out.flush();
    })
    
    // Tool execution events
    .onToolCall((name, args) -> {
        System.out.println("\n🔧 Calling: " + name);
    })
    .onToolExecuted(exec -> {
        System.out.println("✅ " + exec.toolName() + " done");
    })
    
    // Multi-agent events
    .onHandoff(handoff -> {
        System.out.println("→ Handing off to: " + handoff.targetAgent().name());
    })
    
    // Safety events
    .onGuardrailFailed(failed -> {
        System.err.println("⛔ Guardrail blocked: " + failed.reason());
    })
    
    // Completion
    .onComplete(result -> {
        System.out.println("\n\n✅ Finished!");
        System.out.println("Total turns: " + result.turnCount());
    })
    .onError(Throwable::printStackTrace)
    
    .start();
```

### Agent Streaming Callbacks

| Callback | When Called |
|----------|-------------|
| `.onTurnStart(int)` | Each LLM call |
| `.onTurnComplete(Response)` | Each LLM response |
| `.onTextDelta(String)` | Text chunks |
| `.onToolCall(name, args)` | Tool identified |
| `.onToolExecuted(ToolExecution)` | Tool completed |
| `.onHandoff(Handoff)` | Agent routing |
| `.onGuardrailFailed(GuardrailFailure)` | Blocked by guardrail |
| `.onComplete(AgentResult)` | All done |
| `.onError(Throwable)` | Error occurred |

---

## Human-in-the-Loop Streaming

Add approval workflows for sensitive tool calls:

### Synchronous Approval

```java
agent.interactStream("Send an email to john@example.com")
    .onToolCallPending((toolCall, approve) -> {
        // Show what the agent wants to do
        System.out.println("🔧 Agent wants to execute: " + toolCall.name());
        System.out.println("   Arguments: " + toolCall.arguments());
        
        // Ask for approval
        System.out.print("Approve? (y/n): ");
        boolean approved = new Scanner(System.in).nextLine().equalsIgnoreCase("y");
        
        // Accept or reject
        approve.accept(approved);
    })
    .onToolExecuted(exec -> {
        System.out.println("✅ " + exec.toolName() + " completed");
    })
    .start();
```

### Async Pause/Resume

For long approval processes (e.g., manager approval):

```java
// Start with pause capability
agent.interactStream("Delete all customer records")
    .onPause(state -> {
        // Save state to database
        String stateJson = objectMapper.writeValueAsString(state);
        database.save("pending_approval", stateJson);
        
        // Notify approver
        slackClient.sendMessage(
            "#approvals",
            "AI wants to: " + state.pendingToolCall().name()
        );
    })
    .start();

// Later, when approval received...
@PostMapping("/approve/{id}")
public void handleApproval(@PathVariable String id, @RequestBody boolean approved) {
    // Load saved state
    AgentRunState state = loadState(id);
    
    if (approved) {
        state.approveToolCall();
    } else {
        state.rejectToolCall("Manager denied");
    }
    
    // Resume agent
    AgentResult result = agent.resume(state).join();
    
    // Notify user
    sendResultToUser(result.output());
}
```

---

## Streaming to UI (Web/JavaFX)

### Server-Sent Events (SSE)

```java
@GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> streamChat(@RequestParam String message) {
    return Flux.create(sink -> {
        var payload = CreateResponsePayload.builder()
            .model("openai/gpt-4o")
            .addUserMessage(message)
            .streaming()
            .build();
        
        responder.respond(payload)
            .onTextDelta(delta -> {
                sink.next(delta);
            })
            .onComplete(response -> {
                sink.complete();
            })
            .onError(sink::error)
            .start();
    });
}
```

### JavaFX

```java
var payload = CreateResponsePayload.builder()
    .model("openai/gpt-4o")
    .addUserMessage(userInput)
    .streaming()
    .build();

responder.respond(payload)
    .onTextDelta(delta -> {
        // Update UI on JavaFX Application Thread
        Platform.runLater(() -> {
            textArea.appendText(delta);
        });
    })
    .onComplete(response -> {
        Platform.runLater(() -> {
            statusLabel.setText("Complete!");
        });
    })
    .start();
```

---

## Best Practices

### ✅ Do

```java
// Always flush when printing
.onTextDelta(delta -> {
    System.out.print(delta);
    System.out.flush();  // Important!
})

// Always provide error handler
.onError(error -> {
    logger.error("Streaming error", error);
    showErrorToUser(error.getMessage());
})

// Don't forget to call start()
responder.respond(payload)
    .onTextDelta(...)
    .start();  // Required!
```

### ❌ Don't

```java
// Don't do heavy processing in callbacks
.onTextDelta(delta -> {
    saveToDatabase(delta);  // Too slow!
    callExternalAPI(delta);  // Blocks streaming!
})

// Don't forget error handling
responder.respond(payload)
    .onTextDelta(System.out::print)
    // Missing .onError()!
    .start();
```

---

## Next Steps

- [Agents Guide](agents.md) - Agent streaming with tools
- [Function Tools Guide](tools.md) - Tools in streaming context
