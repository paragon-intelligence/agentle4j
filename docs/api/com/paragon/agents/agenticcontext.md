# :material-code-braces: AgenticContext

> This docs was updated at: 2026-02-23

`com.paragon.agents.AgenticContext` &nbsp;·&nbsp; **Class**

---

Holds conversation state for an agent interaction.

This is the "short-term memory" for an agent - it tracks:

  
- Conversation history (messages exchanged between user and agent)
- Tool call results
- Custom user-defined state via key-value store

AgentContext is designed to be passed per-run, making the `Agent` thread-safe and
reusable across multiple conversations.

### Usage Example

```java
// Create a fresh context for a new conversation
AgentContext context = AgentContext.create();
// First interaction
agent.interact("Hi, I need help with my order", context);
// Second interaction (remembers first)
agent.interact("It's order #12345", context);
// Store custom state
context.setState("orderNumber", "12345");
String orderNum = (String) context.getState("orderNumber");
```

**See Also**

- `Agent`

*Since: 1.0*

## Methods

### `create`

```java
public static @NonNull AgenticContext create()
```

Creates a new, empty AgentContext.

**Returns**

a fresh context with no history or state

---

### `withHistory`

```java
public static @NonNull AgenticContext withHistory(
      @NonNull List<ResponseInputItem> initialHistory)
```

Creates an AgentContext pre-populated with conversation history.

Useful for resuming a previous conversation or providing initial context.

**Parameters**

| Name | Description |
|------|-------------|
| `initialHistory` | the messages to pre-populate |

**Returns**

a context with the given history

---

### `addMessage`

```java
public @NonNull AgenticContext addMessage(@NonNull Message message)
```

Adds a message to the conversation history.

**Parameters**

| Name | Description |
|------|-------------|
| `message` | the message to add |

**Returns**

this context for method chaining

---

### `addInput`

```java
public @NonNull AgenticContext addInput(@NonNull ResponseInputItem item)
```

Adds a response input item to the conversation history.

**Parameters**

| Name | Description |
|------|-------------|
| `item` | the input item to add |

**Returns**

this context for method chaining

---

### `addToolResult`

```java
public @NonNull AgenticContext addToolResult(@NonNull FunctionToolCallOutput output)
```

Adds a tool result to the conversation history.

Tool results are tracked so the agent can see the output of previous tool executions.

**Parameters**

| Name | Description |
|------|-------------|
| `output` | the tool execution result (already contains callId) |

**Returns**

this context for method chaining

---

### `getHistory`

```java
public @NonNull List<ResponseInputItem> getHistory()
```

Returns an unmodifiable view of the conversation history.

**Returns**

the conversation history

---

### `getHistoryMutable`

```java
public @NonNull List<ResponseInputItem> getHistoryMutable()
```

Returns a mutable copy of the conversation history for building payloads.

**Returns**

a mutable copy of the history

---

### `setState`

```java
public @NonNull AgenticContext setState(@NonNull String key, @Nullable Object value)
```

Stores a custom value in the context's state.

**Parameters**

| Name | Description |
|------|-------------|
| `key` | the key to store under |
| `value` | the value to store (can be null to remove) |

**Returns**

this context for method chaining

---

### `getState`

```java
public Optional<Object> getState(@NonNull String key)
```

Retrieves a value from the context's state.

**Parameters**

| Name | Description |
|------|-------------|
| `key` | the key to look up |

**Returns**

an Optional containing the stored value, or empty if not found

---

### `getState`

```java
public <T> Optional<T> getState(@NonNull String key, @NonNull Class<T> type)
```

Retrieves a typed value from the context's state.

**Parameters**

| Name | Description |
|------|-------------|
| `key` | the key to look up |
| `type` | the expected type |
| `<T>` | the type parameter |

**Returns**

an Optional containing the stored value cast to the expected type, or empty if not found

**Throws**

| Type | Condition |
|------|-----------|
| `ClassCastException` | if the stored value is not of the expected type |

---

### `hasState`

```java
public boolean hasState(@NonNull String key)
```

Checks if a key exists in the state.

**Parameters**

| Name | Description |
|------|-------------|
| `key` | the key to check |

**Returns**

true if the key exists

---

### `getAllState`

```java
public @NonNull Map<String, Object> getAllState()
```

Returns an unmodifiable view of all state entries.

**Returns**

the state map

---

### `incrementTurn`

```java
int incrementTurn()
```

Increments and returns the turn count.

A "turn" is one LLM call in the agent loop.

**Returns**

the new turn count after incrementing

---

### `getTurnCount`

```java
public int getTurnCount()
```

Returns the current turn count.

**Returns**

the number of LLM calls made in this context

---

### `clear`

```java
public @NonNull AgenticContext clear()
```

Clears all history and state, resetting the context.

**Returns**

this context for method chaining

---

### `copy`

```java
public @NonNull AgenticContext copy()
```

Creates a copy of this context with the same history and state.

Useful for parallel agent execution where each agent needs an isolated copy.

**Returns**

a new context with copied history and state

---

### `historySize`

```java
public int historySize()
```

Returns the size of the conversation history.

**Returns**

the number of items in history

---

### `extractLastUserMessageText`

```java
public @NonNull Optional<String> extractLastUserMessageText()
```

Extracts the text of the last user message from the conversation history.

Iterates backwards through history to find the most recent user message and returns its
first text content.

**Returns**

an Optional containing the last user message text, or empty if none found

---

### `extractLastUserMessageText`

```java
public @NonNull String extractLastUserMessageText(@NonNull String fallback)
```

Extracts the text of the last user message, or returns a fallback value.

**Parameters**

| Name | Description |
|------|-------------|
| `fallback` | the fallback value if no user message is found |

**Returns**

the last user message text, or the fallback

---

### `ensureTraceContext`

```java
public @NonNull AgenticContext ensureTraceContext()
```

Ensures this context has trace context set, generating IDs if not already present.

**Returns**

this context for method chaining

---

### `createChildContext`

```java
public @NonNull AgenticContext createChildContext(
      boolean shareState, boolean shareHistory, @NonNull String request)
```

Creates a child context for sub-agent execution based on sharing configuration.

  
- If shareHistory is true, forks the full context including history
- If shareState is true (but not history), copies state and trace but starts fresh history
- Otherwise, creates a completely isolated context

**Parameters**

| Name | Description |
|------|-------------|
| `shareState` | whether to copy custom state to the child |
| `shareHistory` | whether to fork the full history to the child |
| `request` | the user message to add to the child context |

**Returns**

a new child context configured according to the sharing parameters

---

### `runAsCurrent`

```java
void runAsCurrent(@NonNull Runnable task)
```

Runs a task with this context bound as the current context for sub-agent execution.

Uses `ScopedValue` for virtual-thread-safe context propagation.

**Parameters**

| Name | Description |
|------|-------------|
| `task` | the task to run within this context scope |

---

### `callAsCurrent`

```java
<T> T callAsCurrent(ScopedValue.CallableOp<T, Exception> task) throws Exception
```

Calls a task with this context bound as the current context, returning the result.

Uses `ScopedValue` for virtual-thread-safe context propagation.

**Parameters**

| Name | Description |
|------|-------------|
| `task` | the task to call within this context scope |
| `<T>` | the return type |

**Returns**

the result from the task

**Throws**

| Type | Condition |
|------|-----------|
| `Exception` | if the task throws |

---

### `current`

```java
static @NonNull Optional<AgenticContext> current()
```

Returns the currently bound context from the enclosing scope, if any.

Used by sub-agent tools to discover their parent context.

**Returns**

an Optional containing the current context, or empty if none is bound

---

### `withTraceContext`

```java
public @NonNull AgenticContext withTraceContext(@NonNull String traceId, @NonNull String spanId)
```

Sets the parent trace context for distributed tracing.

When set, child spans will be linked to this parent, enabling end-to-end trace correlation
across multi-agent runs.

**Parameters**

| Name | Description |
|------|-------------|
| `traceId` | the parent trace ID (32-char hex) |
| `spanId` | the parent span ID (16-char hex) |

**Returns**

this context for method chaining

---

### `withRequestId`

```java
public @NonNull AgenticContext withRequestId(@NonNull String requestId)
```

Sets the request ID for high-level correlation.

The request ID is a user-defined identifier that groups all operations from a single user
request, even across multiple traces.

**Parameters**

| Name | Description |
|------|-------------|
| `requestId` | the unique request identifier |

**Returns**

this context for method chaining

---

### `parentTraceId`

```java
public Optional<String> parentTraceId()
```

Returns the parent trace ID, if set.

**Returns**

an Optional containing the parent trace ID, or empty if not set

---

### `parentSpanId`

```java
public Optional<String> parentSpanId()
```

Returns the parent span ID, if set.

**Returns**

an Optional containing the parent span ID, or empty if not set

---

### `requestId`

```java
public Optional<String> requestId()
```

Returns the request ID, if set.

**Returns**

an Optional containing the request ID, or empty if not set

---

### `hasTraceContext`

```java
public boolean hasTraceContext()
```

Checks if this context has trace context set.

**Returns**

true if both parentTraceId and parentSpanId are set

---

### `fork`

```java
public @NonNull AgenticContext fork(@NonNull String newParentSpanId)
```

Creates a forked copy for child agent execution with updated parent span.

Use this when handing off to a child agent. The child will have the same history but can
generate its own child spans under the given parent.

**Parameters**

| Name | Description |
|------|-------------|
| `newParentSpanId` | the span ID to use as the parent for the child |

**Returns**

a new context with the updated parent span

