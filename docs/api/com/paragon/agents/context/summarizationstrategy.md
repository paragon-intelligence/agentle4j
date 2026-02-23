# :material-code-braces: SummarizationStrategy

> This docs was updated at: 2026-02-23

`com.paragon.agents.context.SummarizationStrategy` &nbsp;·&nbsp; **Class**

Implements `ContextWindowStrategy`

---

A context window strategy that summarizes older messages when context exceeds the limit.

This strategy:

  
- Keeps recent messages intact
- Summarizes older messages using an LLM
- Replaces summarized messages with a single summary message
- Caches summaries to avoid redundant API calls

This strategy is more expensive (requires an LLM call) but preserves more context than simple
truncation, making it suitable for conversations where older context contains important
information.

### Usage Example

```java
// Use agent's responder with a fast model for summarization
ContextWindowStrategy strategy = SummarizationStrategy.builder()
    .responder(responder)
    .model("openai/gpt-4o-mini")
    .build();
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
- `SlidingWindowStrategy`

*Since: 1.0*

## Methods

### `builder`

```java
public static @NonNull Builder builder()
```

Creates a new builder for SummarizationStrategy.

**Returns**

a new builder instance

---

### `withResponder`

```java
public static @NonNull SummarizationStrategy withResponder(
      @NonNull Responder responder, @NonNull String model)
```

Creates a summarization strategy with the given responder and model.

**Parameters**

| Name | Description |
|------|-------------|
| `responder` | the responder to use for summarization calls |
| `model` | the model to use for summarization |

**Returns**

a new summarization strategy

---

### `generateSummary`

```java
private String generateSummary(List<ResponseInputItem> messages)
```

Generates a summary of the given messages using the configured responder.

---

### `formatMessage`

```java
private String formatMessage(ResponseInputItem item)
```

Formats a message for summarization.

---

### `model`

```java
public @NonNull String model()
```

Returns the model used for summarization.

**Returns**

the model identifier

---

### `keepRecentMessages`

```java
public int keepRecentMessages()
```

Returns the number of recent messages to keep.

**Returns**

the keep recent messages count

---

### `responder`

```java
public @NonNull Builder responder(@NonNull Responder responder)
```

Sets the responder to use for summarization calls.

**Parameters**

| Name | Description |
|------|-------------|
| `responder` | the responder |

**Returns**

this builder

---

### `model`

```java
public @NonNull Builder model(@NonNull String model)
```

Sets the model to use for summarization.

A fast, cheaper model is recommended (e.g., "openai/gpt-4o-mini").

**Parameters**

| Name | Description |
|------|-------------|
| `model` | the model identifier |

**Returns**

this builder

---

### `summarizationPrompt`

```java
public @NonNull Builder summarizationPrompt(@NonNull String prompt)
```

Sets a custom prompt for summarization.

The prompt should contain a single `%s` placeholder where the conversation text will
be inserted.

**Parameters**

| Name | Description |
|------|-------------|
| `prompt` | the summarization prompt |

**Returns**

this builder

---

### `keepRecentMessages`

```java
public @NonNull Builder keepRecentMessages(int count)
```

Sets the number of recent messages to keep without summarization.

Defaults to 5 messages.

**Parameters**

| Name | Description |
|------|-------------|
| `count` | the number of messages to keep |

**Returns**

this builder

---

### `build`

```java
public @NonNull SummarizationStrategy build()
```

Builds the SummarizationStrategy.

**Returns**

a new SummarizationStrategy

**Throws**

| Type | Condition |
|------|-----------|
| `NullPointerException` | if responder or model is null |

