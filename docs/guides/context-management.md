# Context Window Management

> This docs was updated at: 2026-02-23

Long-running conversations can exceed your model's context window limit. Agentle4j provides pluggable strategies to manage context length automatically.

## Quick Start

```java
import com.paragon.agents.context.*;

Agent agent = Agent.builder()
    .name("Assistant")
    .model("openai/gpt-4o")
    .instructions("You are a helpful assistant.")
    .responder(responder)
    .contextManagement(ContextManagementConfig.builder()
        .strategy(new SlidingWindowStrategy())
        .maxTokens(4000)
        .build())
    .build();
```

## Configuration

Use `ContextManagementConfig.builder()` to configure context management:

```java
ContextManagementConfig config = ContextManagementConfig.builder()
    .strategy(new SlidingWindowStrategy())  // Required: the management strategy
    .maxTokens(4000)                         // Required: maximum token limit
    .tokenCounter(new SimpleTokenCounter())  // Optional: custom token counter
    .build();
```

## Available Strategies

### SlidingWindowStrategy

Removes oldest messages when context exceeds the limit:

```java
// Basic sliding window
ContextWindowStrategy strategy = new SlidingWindowStrategy();

// Preserve developer message at the start of conversation
ContextWindowStrategy preserving = SlidingWindowStrategy.preservingDeveloperMessage();
```

**Best for:**
- Simple use cases where older context is less important
- Low latency (no API calls required)
- Cost-sensitive applications

### SummarizationStrategy

Summarizes older messages using an LLM:

```java
ContextWindowStrategy strategy = SummarizationStrategy.builder()
    .responder(responder)
    .model("openai/gpt-4o-mini") // Use a fast, cheap model
    .keepRecentMessages(5)        // Keep last 5 messages intact
    .build();

// Or use the factory method
ContextWindowStrategy quick = SummarizationStrategy.withResponder(
    responder, "openai/gpt-4o-mini");
```

**Best for:**
- Conversations where older context contains important information
- Use cases requiring high context retention
- When you can afford the latency of an additional LLM call

## Custom Token Counter

By default, `SimpleTokenCounter` estimates tokens as `text.length() / 4`. You can provide a custom implementation:

```java
public class MyTokenCounter implements TokenCounter {
    @Override
    public int countTokens(ResponseInputItem item) {
        // Your custom implementation
    }
    
    @Override
    public int countText(String text) {
        // Use a proper tokenizer like jtokkit
    }
    
    @Override
    public int countImage(Image image) {
        // Calculate based on image dimensions
    }
}

ContextManagementConfig config = ContextManagementConfig.builder()
    .strategy(new SlidingWindowStrategy())
    .maxTokens(4000)
    .tokenCounter(new MyTokenCounter())
    .build();
```

## Token Estimation

`SimpleTokenCounter` uses these estimates:

| Content Type | Token Estimate |
|-------------|----------------|
| Text | `text.length() / 4` |
| High-detail image | 765 tokens |
| Low-detail image | 85 tokens |
| Auto-detail image | 170 tokens |

## When to Use Context Management

Enable context management when:

- Your agent handles long conversations
- Users may send many messages in a session  
- You're seeing context length errors from the API
- You want to optimize costs by limiting context size

## Best Practices

1. **Choose the right limit**: Set `maxTokens` based on your model's context window minus space for the response
2. **Use summarization for important context**: If older messages contain decisions or preferences, use `SummarizationStrategy`
3. **Preserve system prompts**: Use `SlidingWindowStrategy.preservingDeveloperMessage()` to keep system instructions
4. **Monitor token usage**: Check response usage metrics to tune your limit
