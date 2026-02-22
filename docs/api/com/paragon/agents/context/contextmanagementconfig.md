# :material-code-braces: ContextManagementConfig

`com.paragon.agents.context.ContextManagementConfig` &nbsp;·&nbsp; **Class**

---

Configuration for context management in agents.

This class encapsulates all settings for managing conversation context length, including the
strategy to use, the maximum token limit, and the token counter.

### Usage Example

```java
// Basic sliding window configuration
ContextManagementConfig config = ContextManagementConfig.builder()
    .strategy(new SlidingWindowStrategy())
    .maxTokens(4000)
    .build();
// With custom token counter
ContextManagementConfig config = ContextManagementConfig.builder()
    .strategy(new SlidingWindowStrategy())
    .maxTokens(4000)
    .tokenCounter(new MyCustomTokenCounter())
    .build();
// Use with agent
Agent agent = Agent.builder()
    .name("Assistant")
    .model("openai/gpt-4o")
    .responder(responder)
    .contextManagement(config)
    .build();
```

**See Also**

- `ContextWindowStrategy`
- `SlidingWindowStrategy`
- `SummarizationStrategy`

*Since: 1.0*

## Methods

### `builder`

```java
public static @NonNull Builder builder()
```

Creates a new builder for ContextManagementConfig.

**Returns**

a new builder instance

---

### `strategy`

```java
public @NonNull ContextWindowStrategy strategy()
```

Returns the context window strategy.

**Returns**

the strategy

---

### `maxTokens`

```java
public int maxTokens()
```

Returns the maximum number of tokens allowed in context.

**Returns**

the max tokens limit

---

### `tokenCounter`

```java
public @NonNull TokenCounter tokenCounter()
```

Returns the token counter used for measuring content.

**Returns**

the token counter

---

### `strategy`

```java
public @NonNull Builder strategy(@NonNull ContextWindowStrategy strategy)
```

Sets the context window strategy (required).

**Parameters**

| Name | Description |
|------|-------------|
| `strategy` | the strategy to use for managing context |

**Returns**

this builder

**See Also**

- `SlidingWindowStrategy`
- `SummarizationStrategy`

---

### `maxTokens`

```java
public @NonNull Builder maxTokens(int maxTokens)
```

Sets the maximum number of tokens allowed in context (required).

When context exceeds this limit, the strategy will be applied to reduce it.

**Parameters**

| Name | Description |
|------|-------------|
| `maxTokens` | the maximum token limit |

**Returns**

this builder

---

### `tokenCounter`

```java
public @NonNull Builder tokenCounter(@NonNull TokenCounter tokenCounter)
```

Sets a custom token counter.

If not set, `SimpleTokenCounter` is used by default.

**Parameters**

| Name | Description |
|------|-------------|
| `tokenCounter` | the token counter to use |

**Returns**

this builder

---

### `build`

```java
public @NonNull ContextManagementConfig build()
```

Builds the ContextManagementConfig.

**Returns**

a new ContextManagementConfig

**Throws**

| Type | Condition |
|------|-----------|
| `NullPointerException` | if strategy is null |
| `IllegalArgumentException` | if maxTokens is not positive |

