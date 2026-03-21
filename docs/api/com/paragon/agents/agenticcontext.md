# :material-code-braces: AgenticContext

`com.paragon.agents.AgenticContext` &nbsp;·&nbsp; **Class**

---

Holds conversation state for an agent interaction.

This is the "short-term memory" for an agent - it tracks:

  
- Conversation history (messages exchanged between user and agent)
- Tool call results
- Custom user-defined state via key-value store

AgenticContext is designed to be passed per-run, making the `Agent` thread-safe and
reusable across multiple conversations.

### Usage Example

```java
// Create a fresh context for a new conversation
AgenticContext context = AgenticContext.create();
// Resume safely from optional history and append a new user turn
AgenticContext resumed = AgenticContext.ofHistory(loadHistory())
    .addUserMessage("I still need help");
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

Creates a new, empty AgenticContext.

**Returns**

a fresh context with no history or state

---

### `create`

```java
public static @NonNull AgenticContext create(@NonNull List<? extends ResponseInputItem> history)
```

Creates a context with pre-populated history.

**Parameters**

| Name | Description |
|------|-------------|
| `history` | the initial history items |

**Returns**

a new context with the provided history

---

### `create`

```java
public static @NonNull AgenticContext create(
      @NonNull List<? extends ResponseInputItem> history, @NonNull Map<String, Object> state)
```

Creates a context with pre-populated history and state.

**Parameters**

| Name | Description |
|------|-------------|
| `history` | the initial history items |
| `state` | the initial state map |

**Returns**

a new context with the provided history and state

---

### `create`

```java
public static @NonNull AgenticContext create(
      @NonNull List<? extends ResponseInputItem> history,
      @NonNull Map<String, Object> state,
      int turnCount)
```

Creates a context with pre-populated history, state, and turn count.

**Parameters**

| Name | Description |
|------|-------------|
| `history` | the initial history items |
| `state` | the initial state map |
| `turnCount` | the current turn count |

**Returns**

a new context with the provided history, state, and turn count

---

### `ofHistory`

```java
public static @NonNull AgenticContext ofHistory(
      @Nullable Collection<? extends ResponseInputItem> history)
```

Creates a context from a possibly-null history collection.

This is the null-safe convenience factory for callers that may or may not have persisted
history available yet.

**Parameters**

| Name | Description |
|------|-------------|
| `history` | the history collection, or null |

**Returns**

a fresh empty context when history is null/empty, otherwise a context with that history

---

### `ofInputs`

```java
public static @NonNull AgenticContext ofInputs(@NonNull ResponseInputItem... items)
```

Creates a context from the provided input items.

**Parameters**

| Name | Description |
|------|-------------|
| `items` | the input items to add |

**Returns**

a context populated with the given inputs

---

### `ofMessages`

```java
public static @NonNull AgenticContext ofMessages(@NonNull Message... messages)
```

Creates a context from the provided messages.

**Parameters**

| Name | Description |
|------|-------------|
| `messages` | the messages to add |

**Returns**

a context populated with the given messages

---

### `fromJson`

```java
static AgenticContext fromJson(
      @JsonProperty("history") @Nullable List<ResponseInputItem> history,
      @JsonProperty("state") @Nullable Map<String, Object> state,
      @JsonProperty("turn_count") int turnCount,
      @JsonProperty("parent_trace_id") @Nullable String parentTraceId,
      @JsonProperty("parent_span_id") @Nullable String parentSpanId,
      @JsonProperty("request_id") @Nullable String requestId)
```

Jackson deserialization entry point.

State map values are deserialized as standard Jackson types: JSON objects become `java.util.LinkedHashMap`, arrays become `java.util.ArrayList`, and primitives map to
their Java equivalents. For full type fidelity with custom objects, use `mapper.convertValue(ctx.getState("key").orElseThrow(), MyType.class)`.

---

### `withHistory`

```java
public static @NonNull AgenticContext withHistory(
      @NonNull List<? extends ResponseInputItem> initialHistory)
```

Creates an AgenticContext pre-populated with conversation history.

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

### `addMessages`

```java
public @NonNull AgenticContext addMessages(@NonNull Iterable<? extends Message> messages)
```

Adds multiple messages to the conversation history.

**Parameters**

| Name | Description |
|------|-------------|
| `messages` | the messages to add |

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

### `addInputs`

```java
public @NonNull AgenticContext addInputs(@NonNull Iterable<? extends ResponseInputItem> items)
```

Adds multiple response input items to the conversation history.

**Parameters**

| Name | Description |
|------|-------------|
| `items` | the input items to add |

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

### `addUserMessage`

```java
public @NonNull AgenticContext addUserMessage(@NonNull String text)
```

Adds a user text message to the conversation history.

**Parameters**

| Name | Description |
|------|-------------|
| `text` | the user text to add |

**Returns**

this context for method chaining

---

### `addAssistantMessage`

```java
public @NonNull AgenticContext addAssistantMessage(@NonNull String text)
```

Adds an assistant text message to the conversation history.

**Parameters**

| Name | Description |
|------|-------------|
| `text` | the assistant text to add |

**Returns**

this context for method chaining

---

### `addDeveloperMessage`

```java
public @NonNull AgenticContext addDeveloperMessage(@NonNull String text)
```

Adds a developer text message to the conversation history.

**Parameters**

| Name | Description |
|------|-------------|
| `text` | the developer text to add |

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

### `history`

```java
public @NonNull List<ResponseInputItem> history()
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

### `historyMutable`

```java
public @NonNull List<ResponseInputItem> historyMutable()
```

Returns a mutable copy of the conversation history for ad-hoc manipulation.

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

### `removeState`

```java
public @NonNull AgenticContext removeState(@NonNull String key)
```

Removes a value from the context's state.

**Parameters**

| Name | Description |
|------|-------------|
| `key` | the key to remove |

**Returns**

this context for method chaining

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

### `state`

```java
public @NonNull Map<String, Object> state()
```

Returns an unmodifiable view of the state map.

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

### `hasHistory`

```java
public boolean hasHistory()
```

Returns whether this context currently contains any history items.

**Returns**

true when history is not empty

---

### `historyItems`

```java
public <T> @NonNull List<T> historyItems(@NonNull Class<T> type)
```

Returns all history items assignable to the requested type.

**Parameters**

| Name | Description |
|------|-------------|
| `type` | the desired item type |
| `<T>` | the desired item type |

**Returns**

an immutable list of matching items in original order

---

### `messages`

```java
public @NonNull List<Message> messages()
```

Returns all messages in the conversation history.

**Returns**

an immutable list of messages in original order

---

### `messages`

```java
public @NonNull List<Message> messages(@NonNull MessageRole role)
```

Returns all messages with the requested role.

**Parameters**

| Name | Description |
|------|-------------|
| `role` | the role to filter by |

**Returns**

an immutable list of matching messages in original order

---

### `userMessages`

```java
public @NonNull List<Message> userMessages()
```

Returns all user messages in the conversation history.

**Returns**

an immutable list of user messages

---

### `assistantMessages`

```java
public @NonNull List<Message> assistantMessages()
```

Returns all assistant messages in the conversation history.

**Returns**

an immutable list of assistant messages

---

### `developerMessages`

```java
public @NonNull List<Message> developerMessages()
```

Returns all developer messages in the conversation history.

**Returns**

an immutable list of developer messages

---

### `toolCalls`

```java
public @NonNull List<FunctionToolCall> toolCalls()
```

Returns all function tool calls in the conversation history.

**Returns**

an immutable list of function tool calls

---

### `toolOutputs`

```java
public @NonNull List<FunctionToolCallOutput> toolOutputs()
```

Returns all function tool outputs in the conversation history.

**Returns**

an immutable list of function tool outputs

---

### `lastMessage`

```java
public @NonNull Optional<Message> lastMessage()
```

Returns the most recent message in the conversation history.

**Returns**

an Optional containing the last message, or empty if none exist

---

### `lastMessage`

```java
public @NonNull Optional<Message> lastMessage(@NonNull MessageRole role)
```

Returns the most recent message with the requested role.

**Parameters**

| Name | Description |
|------|-------------|
| `role` | the role to filter by |

**Returns**

an Optional containing the last matching message, or empty if none exist

---

### `lastUserMessage`

```java
public @NonNull Optional<Message> lastUserMessage()
```

Returns the most recent user message.

**Returns**

an Optional containing the last user message, or empty if none exist

---

### `lastAssistantMessage`

```java
public @NonNull Optional<Message> lastAssistantMessage()
```

Returns the most recent assistant message.

**Returns**

an Optional containing the last assistant message, or empty if none exist

---

### `lastDeveloperMessage`

```java
public @NonNull Optional<Message> lastDeveloperMessage()
```

Returns the most recent developer message.

**Returns**

an Optional containing the last developer message, or empty if none exist

---

### `lastUserMessageText`

```java
public @NonNull Optional<String> lastUserMessageText()
```

Returns the first text segment from the most recent user message.

This is an alias for `.extractLastUserMessageText()`.

**Returns**

an Optional containing the last user text, or empty if none found

---

### `lastUserMessageText`

```java
public @NonNull String lastUserMessageText(@NonNull String fallback)
```

Returns the first text segment from the most recent user message, or a fallback value.

This is an alias for `.extractLastUserMessageText(String)`.

**Parameters**

| Name | Description |
|------|-------------|
| `fallback` | the fallback value if no user message text is found |

**Returns**

the last user message text, or the fallback

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

