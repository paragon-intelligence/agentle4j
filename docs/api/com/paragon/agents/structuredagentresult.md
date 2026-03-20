# :material-code-braces: StructuredAgentResult

`com.paragon.agents.StructuredAgentResult` &nbsp;·&nbsp; **Class**

Extends `AgentResult`

---

The result of a structured agent interaction, containing the typed output.

This is the type-safe counterpart to `AgentResult` for agents with structured output.
The output is automatically deserialized to the specified type.

*Since: 1.0*

## Methods

### `parsed`

```java
public @NonNull T parsed()
```

Returns the parsed typed output from the agent.

**Returns**

the parsed output (never null on success)

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalStateException` | if the result is an error or the parsed value is unexpectedly null |

---

### `isSuccess`

```java
public boolean isSuccess()
```

Whether the interaction was successful.

**Returns**

true if no error occurred and no handoff was triggered

---

### `errorMessage`

```java
public @Nullable String errorMessage()
```

Returns the error message if one occurred.

**Returns**

the error message or null

---

### `success`

```java
public static <T> @NonNull StructuredAgentResult<T> success(
      @NonNull T output,
      @NonNull String rawOutput,
      @Nullable Response response,
      @NonNull List<ResponseInputItem> history,
      @NonNull List<ToolExecution> toolExecutions,
      int turnsUsed)
```

Creates a successful result.

**Parameters**

| Name | Description |
|------|-------------|
| `output` | the typed output |
| `rawOutput` | the raw JSON/text output |
| `response` | the final response |
| `history` | the conversation history |
| `toolExecutions` | the tool executions |
| `turnsUsed` | number of turns used |
| `<T>` | the output type |

**Returns**

a success result

---

### `success`

```java
public static <T> @NonNull StructuredAgentResult<T> success(
      @NonNull T output, @NonNull String rawOutput)
```

Convenience method for creating a simple successful result (for testing).

**Parameters**

| Name | Description |
|------|-------------|
| `output` | the typed output |
| `rawOutput` | the raw JSON/text output |
| `<T>` | the output type |

**Returns**

a minimal success result

---

### `error`

```java
public static <T> @NonNull StructuredAgentResult<T> error(
      @NonNull Throwable error,
      @Nullable String rawOutput,
      @Nullable Response response,
      @NonNull List<ResponseInputItem> history,
      @NonNull List<ToolExecution> toolExecutions,
      int turnsUsed)
```

Creates an error result.

**Parameters**

| Name | Description |
|------|-------------|
| `error` | the error that occurred |
| `rawOutput` | the raw output if any |
| `response` | the last response if any |
| `history` | the conversation history |
| `toolExecutions` | the tool executions |
| `turnsUsed` | number of turns used |
| `<T>` | the output type |

**Returns**

an error result

---

### `structuredError`

```java
public static <T> @NonNull StructuredAgentResult<T> structuredError(@NonNull Throwable error)
```

Convenience method for creating a simple error result (for testing).

**Parameters**

| Name | Description |
|------|-------------|
| `error` | the error that occurred |
| `<T>` | the output type |

**Returns**

a minimal error result

