# :material-approximately-equal: ContextWindowStrategy

> This docs was updated at: 2026-02-23

`com.paragon.agents.context.ContextWindowStrategy` &nbsp;·&nbsp; **Interface**

---

Strategy interface for managing conversation context length.

Context window strategies are used by `Agent` to ensure
conversation history stays within the model's token limits. When the context exceeds the maximum
token count, the strategy determines how to reduce it.

Common strategies include:

  
- `SlidingWindowStrategy`: Removes oldest messages to fit within limit
- `SummarizationStrategy`: Summarizes older messages using an LLM

### Usage Example

```java
// Create agent with sliding window strategy
Agent agent = Agent.builder()
    .name("Assistant")
    .model("openai/gpt-4o")
    .instructions("You are a helpful assistant.")
    .responder(responder)
    .contextWindow(new SlidingWindowStrategy(), 4000)
    .build();
// Or use summarization
Agent agent = Agent.builder()
    .name("Assistant")
    .model("openai/gpt-4o")
    .instructions("You are a helpful assistant.")
    .responder(responder)
    .contextWindow(SummarizationStrategy.withResponder(responder, "openai/gpt-4o-mini"), 4000)
    .build();
```

**See Also**

- `SlidingWindowStrategy`
- `SummarizationStrategy`
- `TokenCounter`

*Since: 1.0*

## Methods

### `manage`

```java
List<ResponseInputItem> manage(
      @NonNull List<ResponseInputItem> history, int maxTokens, @NonNull TokenCounter counter)
```

Manages the conversation history to fit within the token limit.

This method is called before each LLM request when context management is enabled.
Implementations should return a modified list that fits within `maxTokens`.

**Parameters**

| Name | Description |
|------|-------------|
| `history` | the current conversation history (may be modified or replaced) |
| `maxTokens` | the maximum number of tokens allowed |
| `counter` | the token counter to use for measuring content |

**Returns**

the managed history that fits within the token limit

