# :material-approximately-equal: AgentHook

`com.paragon.harness.AgentHook` &nbsp;·&nbsp; **Interface**

---

Lifecycle hook that intercepts agent and tool execution events.

Implement this interface to inject cross-cutting concerns (logging, rate limiting,
cost tracking, circuit breakers, etc.) without modifying agent logic.

Hooks are executed in registration order. All default methods are no-ops, so
you only override the lifecycle events you care about.

Example — log every tool call:
{@code
AgentHook loggingHook = new AgentHook() {

**See Also**

- `HookRegistry`

*Since: 1.0*

## Methods

### `beforeRun`

```java
default void beforeRun(@NonNull AgenticContext context)
```

Called before the agent's agentic loop starts.

**Parameters**

| Name | Description |
|------|-------------|
| `context` | the conversation context at the start of this run |

---

### `afterRun`

```java
default void afterRun(@NonNull AgentResult result, @NonNull AgenticContext context)
```

Called after the agent's agentic loop completes (success or failure).

**Parameters**

| Name | Description |
|------|-------------|
| `result` | the result of the run |
| `context` | the conversation context at completion |

---

### `beforeToolCall`

```java
default void beforeToolCall(@NonNull FunctionToolCall call, @NonNull AgenticContext context)
```

Called before a tool is invoked. Can be used to block or log tool calls.

**Parameters**

| Name | Description |
|------|-------------|
| `call` | the tool call that is about to be executed |
| `context` | the current conversation context |

---

### `afterToolCall`

```java
default void afterToolCall(
      @NonNull FunctionToolCall call,
      @NonNull ToolExecution execution,
      @NonNull AgenticContext context)
```

Called after a tool has been invoked.

**Parameters**

| Name | Description |
|------|-------------|
| `call` | the tool call that was executed |
| `execution` | the execution record containing output and timing |
| `context` | the current conversation context |

