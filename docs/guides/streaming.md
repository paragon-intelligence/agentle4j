# Streaming Guide

Agentle4j provides real-time streaming of LLM responses using virtual threads for efficient, non-blocking I/O.

## Basic Streaming

Enable streaming with the `.streaming()` builder method:

```java
var payload = CreateResponsePayload.builder()
    .model("openai/gpt-4o")
    .addUserMessage("Write a poem about Java")
    .streaming()  // Enable streaming
    .build();

responder.respond(payload)
    .onTextDelta(delta -> {
        System.out.print(delta);  // Print each chunk
        System.out.flush();
    })
    .onComplete(response -> {
        System.out.println("\n✅ Done!");
    })
    .onError(Throwable::printStackTrace)
    .start();
```

## Stream Callbacks

| Callback | Description |
|----------|-------------|
| `.onTextDelta(String)` | Called for each text chunk |
| `.onComplete(Response)` | Called when streaming completes |
| `.onError(Throwable)` | Called on errors |
| `.onToolCall(name, args)` | Called when a tool call is detected |
| `.onToolResult(name, result)` | Called when tool execution completes |

## Structured Streaming

Stream structured output while parsing:

```java
public record Article(String title, String content, List<String> tags) {}

var payload = CreateResponsePayload.builder()
    .model("openai/gpt-4o")
    .addUserMessage("Write an article about AI")
    .withStructuredOutput(Article.class)
    .streaming()
    .build();

responder.respond(payload)
    .onTextDelta(System.out::print)  // Watch JSON being generated
    .onParsedComplete(parsed -> {
        Article article = parsed.outputParsed();
        System.out.println("Title: " + article.title());
    })
    .start();
```

### Partial Parsing with `onPartialJson`

Access fields as they stream without extra classes:

```java
responder.respond(structuredPayload)
    .onPartialJson(fields -> {
        if (fields.containsKey("title")) {
            updateTitleUI(fields.get("title").toString());
        }
        if (fields.containsKey("content")) {
            updateContentUI(fields.get("content").toString());
        }
    })
    .start();
```

### Typed Partial Parsing

For type-safe partial updates, define a nullable mirror class:

```java
record PartialArticle(
    @Nullable String title,
    @Nullable String content,
    @Nullable List<String> tags) {}

responder.respond(structuredPayload)
    .onPartialParsed(PartialArticle.class, partial -> {
        if (partial.title() != null) {
            updateTitleUI(partial.title());
        }
    })
    .start();
```

## Streaming with Tools

Handle tool calls during streaming:

```java
var payload = CreateResponsePayload.builder()
    .model("openai/gpt-4o")
    .addUserMessage("What's the weather in Tokyo?")
    .addTool(weatherTool)
    .streaming()
    .build();

responder.respond(payload)
    .onTextDelta(System.out::print)
    // Detect tool calls
    .onToolCall((toolName, argsJson) -> {
        System.out.println("🔧 Tool called: " + toolName);
    })
    // Auto-execute with tool store
    .withToolStore(toolStore)
    .onToolResult((toolName, result) -> {
        System.out.println("✅ Result: " + result.output());
    })
    .start();
```

## Agent Streaming

Stream full agentic loop with events:

```java
agent.interactStream("Research and summarize AI trends")
    .onTurnStart(turn -> System.out.println("Turn " + turn))
    .onTextDelta(System.out::print)
    .onTurnComplete(response -> {})
    .onToolExecuted(exec -> System.out.println("🔧 " + exec.toolName()))
    .onHandoff(handoff -> System.out.println("→ " + handoff.targetAgent().name()))
    .onGuardrailFailed(failed -> System.err.println("⛔ " + failed.reason()))
    .onComplete(result -> System.out.println("\n✅ Done!"))
    .onError(Throwable::printStackTrace)
    .start();
```

## Human-in-the-Loop

Control tool execution with approval:

```java
agent.interactStream("Send an email to John")
    .onToolCallPending((toolCall, approve) -> {
        boolean userApproved = askUser("Execute " + toolCall.name() + "?");
        approve.accept(userApproved);
    })
    .onToolExecuted(exec -> System.out.println("✅ " + exec.toolName()))
    .start();
```

### Pause and Resume

For long approval processes:

```java
agent.interactStream("Delete all records")
    .onPause(state -> {
        saveToDatabase(state);  // Serialize state
        notifyApprover(state.pendingToolCall());
    })
    .start();

// Later, after approval:
AgentRunState savedState = loadFromDatabase();
savedState.approveToolCall();
AgentResult result = agent.resume(savedState).join();
```

## Best Practices

!!! tip "Flush Output"
    When printing to console, use `System.out.flush()` after each delta for immediate display.

!!! tip "Error Handling"
    Always provide an `.onError()` callback to handle network issues gracefully.

!!! warning "Thread Safety"
    Callbacks may be invoked from virtual threads. Ensure your callback code is thread-safe.

## Next Steps

- [Function Tools Guide](tools.md) - Create tools for streaming
- [Observability Guide](observability.md) - Monitor streaming requests
