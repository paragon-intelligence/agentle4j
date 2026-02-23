# :material-code-braces: SlidingWindowStrategy

> This docs was updated at: 2026-02-23

`com.paragon.agents.context.SlidingWindowStrategy` &nbsp;·&nbsp; **Class**

Implements `ContextWindowStrategy`

---

A context window strategy that removes oldest messages to fit within the token limit.

This strategy implements a sliding window approach:

  
- Counts tokens from newest to oldest messages
- Removes oldest messages when the limit is exceeded
- Always keeps the most recent user message
- Optionally preserves developer/system messages

This is the simplest and most efficient context management strategy, suitable for most use
cases where older context can be safely discarded.

### Usage Example

```java
// Basic sliding window
ContextWindowStrategy strategy = new SlidingWindowStrategy();
// Preserve developer messages at the start
ContextWindowStrategy strategy = SlidingWindowStrategy.preservingDeveloperMessages();
// Use with agent
Agent agent = Agent.builder()
    .name("Assistant")
    .model("openai/gpt-4o")
    .instructions("You are a helpful assistant.")
    .responder(responder)
    .contextWindow(strategy, 4000)
    .build();
```

**See Also**

- `ContextWindowStrategy`
- `SummarizationStrategy`

*Since: 1.0*

## Methods

### `SlidingWindowStrategy`

```java
public SlidingWindowStrategy()
```

Creates a sliding window strategy with default settings.

By default, all messages including developer messages may be removed when the context limit
is exceeded.

---

### `SlidingWindowStrategy`

```java
public SlidingWindowStrategy(boolean preserveDeveloperMessages)
```

Creates a sliding window strategy with configurable developer message preservation.

**Parameters**

| Name | Description |
|------|-------------|
| `preserveDeveloperMessages` | if true, developer messages at the start of the conversation will never be removed |

---

### `preservingDeveloperMessage`

```java
public static @NonNull SlidingWindowStrategy preservingDeveloperMessage()
```

Creates a strategy that preserves the developer message.

The developer message (system prompt) at the start of the conversation will be preserved
even when the context limit is exceeded.

**Returns**

a new strategy that preserves the developer message

---

### `preservesDeveloperMessage`

```java
public boolean preservesDeveloperMessage()
```

Returns whether this strategy preserves the developer message.

**Returns**

true if the developer message is preserved

