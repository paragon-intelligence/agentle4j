# Streaming Guide

> This docs was updated at: 2026-03-21

















Agentle4j provides real-time streaming with callback-based APIs that work well with virtual
threads. This guide covers the streaming patterns exposed by `ResponseStream` and `AgentStream`.

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
    })
    .onError(error -> {
        System.err.println("Error: " + error.getMessage());
    })
    .start();
```

`ResponseStream.start()` returns immediately and runs the stream on a virtual thread. Use
`startBlocking()`, `get()`, `getText()`, or `getParsed()` when you need to wait inline.

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

#### How It Works - Step by Step

The parser auto-completes incomplete JSON by closing unclosed strings. **Long text fields stream progressively:**

```text
┌─────────────────────────────────────────────────────────────────────────┐
│ Step 1 - First token received                                          │
├─────────────────────────────────────────────────────────────────────────┤
│ Received:  {"name":"Mar                                                 │
│ Completed: {"name":"Mar"}                                               │
│ Map:       {name: "Mar"}                                                │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│ Step 2 - More characters arrive                                         │
├─────────────────────────────────────────────────────────────────────────┤
│ Received:  {"name":"Marcus                                              │
│ Completed: {"name":"Marcus"}                                            │
│ Map:       {name: "Marcus"}                                             │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│ Step 3 - First field complete, second starting                          │
├─────────────────────────────────────────────────────────────────────────┤
│ Received:  {"name":"Marcus","bio":"Sof                                  │
│ Completed: {"name":"Marcus","bio":"Sof"}                                │
│ Map:       {name: "Marcus", bio: "Sof"}                                 │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│ Step 4 - Long text continues to stream                                  │
├─────────────────────────────────────────────────────────────────────────┤
│ Received:  {"name":"Marcus","bio":"Software engineer with 10 years of   │
│ Completed: {"name":"Marcus","bio":"Software engineer with 10 years of"} │
│ Map:       {name: "Marcus", bio: "Software engineer with 10 years of"}  │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│ Step 5 - Final complete JSON                                            │
├─────────────────────────────────────────────────────────────────────────┤
│ Received:  {"name":"Marcus","bio":"Software engineer","age":32}         │
│ Completed: {"name":"Marcus","bio":"Software engineer","age":32}         │
│ Map:       {name: "Marcus", bio: "Software engineer", age: 32}          │
└─────────────────────────────────────────────────────────────────────────┘
```

> [!TIP]
> **The `bio` field updates continuously** as text is generated - from `"Sof"` to `"Software"` to `"Software engineer with..."`. This enables real-time UI updates as long text fields are being written.

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
agent.asStreaming().interact("Research and summarize AI trends, then email me the report")
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
        System.out.println("Total turns: " + result.turnsUsed());
    })
    .onError(Throwable::printStackTrace)
    
    .start();
```

### `AgentStream` vs `ResponseStream`

| | `ResponseStream` | `AgentStream` |
|---|---|---|
| **Level** | Raw SSE layer | Full agentic loop |
| **Handles** | One LLM call | Tools, guardrails, handoffs, HITL, multi-turn |
| **Obtained from** | `responder.respond(streamingPayload)` | `agent.asStreaming().interact(ctx)` |
| **Returns** | `Response` | `AgentResult` |

Use `ResponseStream` when you need maximum control over a single LLM call. Use `AgentStream`
for everything involving agents.

### Full Callback Reference

| Callback | Fires when | Parameter |
|---|---|---|
| `onTurnStart` | Each turn begins | `int` (turn #) |
| `onTextDelta` | LLM text chunk arrives (real SSE) | `String` |
| `onTurnComplete` | Turn's LLM call finishes | `Response` |
| `onToolCallPending` | Tool with `requiresConfirmation=true` detected | `FunctionToolCall, Consumer<Boolean>` |
| `onPause` | Long-running HITL approval needed | `AgentRunState` (serializable) |
| `onToolExecuted` | Tool executed | `ToolExecution` (result + timing) |
| `onGuardrailFailed` | Output guardrail rejected | `GuardrailResult.Failed` |
| `onHandoff` | Handoff to another agent triggered | `Handoff` |
| `onClientSideTool` | `stopsLoop=true` tool detected | `FunctionToolCall` |
| `onPartialJson` | Partial structured output chunk | `Map<String, Object>` |
| `onComplete` | Loop completes | `AgentResult` |
| `onError` | Unrecoverable error | `Throwable` |

### `start()` vs `startBlocking()`

| Method | Behaviour | When to use |
|---|---|---|
| `start()` | Fire-and-forget on a virtual thread; returns immediately | When you need non-blocking dispatch (e.g., within a reactive pipeline) |
| `startBlocking()` | Runs inline on the caller's thread; returns the final `AgentResult` | When you need the result before continuing (most common case) |

```java
// Non-blocking — callbacks fire on the virtual thread
agent.asStreaming().interact(ctx).onTextDelta(System.out::print).start();

// Blocking — result available immediately after return
AgentResult result = agent.asStreaming().interact(ctx)
    .onTextDelta(System.out::print)
    .startBlocking();
```

### `onClientSideTool` — Reacting to UI-Signal Tools

When an agent calls a tool annotated with `stopsLoop = true`, the loop exits immediately and
`onClientSideTool` fires. The tool is not executed and not saved to history.

```java
agent.asStreaming().interact("Help me pick a color")
    .onClientSideTool(call -> {
        // call.arguments() is JSON — parse and show UI
        AskUserTool.Params p = objectMapper.readValue(call.arguments(), AskUserTool.Params.class);
        showDialog(p.question());
    })
    .onComplete(result -> {
        // result.isClientSideTool() == true here
    })
    .startBlocking();
```

### `onPartialJson` — Structured Output Streaming

When `agent.outputType(MyRecord.class)` or `Agent.builder().structured(MyRecord.class)` is configured on the interactable that actually calls the model, partial JSON fields arrive incrementally:

```java
agent.asStreaming().interact(ctx)
    .onPartialJson(fields -> {
        if (fields.containsKey("title")) {
            updateTitleUI(fields.get("title").toString());
        }
    })
    .onComplete(result -> {
        // result.outputParsed(MyRecord.class) for the final typed value
    })
    .startBlocking();
```

`returns(...)` does **not** enable `onPartialJson`. It is a boundary parser for the final result only, so it does not change the source agent's streaming payload or request schema.

### TraceMetadata in Streaming

Pass `TraceMetadata` to correlate streaming runs with your observability tooling:

```java
TraceMetadata trace = TraceMetadata.builder()
    .workflowName("customer-support")
    .userId("u-123")
    .build();

agent.asStreaming().interact(context, trace)
    .onComplete(result -> System.out.println("Done"))
    .startBlocking();
```

### Hooks in Streaming

`HookRegistry` hooks (`beforeRun`, `afterRun`, `beforeToolCall`, `afterToolCall`) fire during
`AgentStream` execution in the same way they do for blocking `interact()`. Wire them via the
agent builder:

```java
HookRegistry hooks = HookRegistry.create();
hooks.add(new AgentHook() {
    @Override public void beforeToolCall(FunctionToolCall call, AgenticContext ctx) {
        metrics.increment("tool_calls");
    }
});

Agent agent = Agent.builder()
    .hookRegistry(hooks)
    // ...
    .build();

agent.asStreaming().interact(ctx)
    .onComplete(result -> System.out.println("Done"))
    .startBlocking();
// beforeToolCall fires for every tool call during streaming
```

### Agent Streaming Callbacks (Legacy Table)

| Callback | When Called |
|----------|-------------|
| `.onTurnStart(int)` | Each LLM call |
| `.onTurnComplete(Response)` | Each LLM response |
| `.onTextDelta(String)` | Text chunks |
| `.onToolCallPending(ToolConfirmationHandler)` | Tool call pending approval (human-in-the-loop) |
| `.onPause(PauseHandler)` | Run paused for async approval (resumes with `Agent.resume()`) |
| `.onToolExecuted(ToolExecution)` | Tool completed |
| `.onHandoff(Handoff)` | Agent routing |
| `.onGuardrailFailed(GuardrailResult.Failed)` | Blocked by guardrail |
| `.onClientSideTool(FunctionToolCall)` | Client-side tool detected (`stopsLoop=true`) |
| `.onPartialJson(Map)` | Partial structured output (requires `outputType` on agent) |
| `.onComplete(AgentResult)` | All done |
| `.onError(Throwable)` | Error occurred |

---

## Multi-Agent Streaming Callback Coverage

| Pattern | `onTextDelta` | `onTurnStart` | `onToolExecuted` | `onToolCallPending` | `onPause` | `onGuardrailFailed` | `onClientSideTool` |
|---|---|---|---|---|---|---|---|
| `AgentStream` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `RouterStream` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `ParallelStream` ALL | ✅ | ✅ | ✅ | — | — | ✅ | ✅ |
| `ParallelStream` FIRST | ✅ | ✅ | ✅ | — | — | ✅ | ✅ |
| `ParallelStream` SYNTH | ✅ | ✅ | ✅ | — | — | ✅ | ✅ |
| `NetworkStream` | ✅ (`onPeerTextDelta`) | — | ✅ (`onPeerToolExecuted`) | — | — | ✅ (`onPeerGuardrailFailed`) | — |

> `onToolCallPending` / `onPause` in ParallelStream are deferred until the parallel HITL
> pattern is designed (blocking all threads vs. just one is ambiguous). `onPause` in network
> and parallel modes requires multi-agent state serialisation.

---

## Human-in-the-Loop Streaming

Add approval workflows for sensitive tool calls:

### Synchronous Approval

```java
agent.asStreaming().interact("Send an email to john@example.com")
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
agent.asStreaming().interact("Delete all customer records")
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
        // Execute tool manually and pass output
        String toolOutput = executeTool(state.pendingToolCall());
        state.approveToolCall(toolOutput);
    } else {
        state.rejectToolCall("Manager denied");
    }
    
    // Resume agent\n    AgentResult result = agent.resume(state);", "StartLine": 385}

    
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

// Always call start()
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

// Missing .onError()!
responder.respond(payload)
    .onTextDelta(System.out::print)
    // Missing .onError()!
    .start();
```

---

## Failure Modes

> [!CAUTION]
> Streaming with partial JSON parsing has edge cases that can cause issues in production.
> See [Streaming Failure Modes](streaming_failure_modes.md) for guidance on:
> - Invalid intermediate JSON handling
> - Schema drift mid-stream
> - Tool-call interrupts
> - Connection drop recovery

---

## Next Steps

- [Agents Guide](agents.md) - Agent streaming with tools
- [Function Tools Guide](tools.md) - Tools in streaming context
- [Streaming Failure Modes](streaming_failure_modes.md) - Edge cases and best practices
