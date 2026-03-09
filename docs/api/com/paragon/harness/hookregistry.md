# :material-code-braces: HookRegistry

> This docs was updated at: 2026-03-09










`com.paragon.harness.HookRegistry` &nbsp;·&nbsp; **Class**

---

Ordered registry of `AgentHook` instances dispatched around agent lifecycle events.

Hooks are invoked in registration order for `before` events and in reverse order for `after` events (stack semantics). Hook failures are caught and logged but never propagate — harness infrastructure cannot break agent functionality.

**See Also**

- `AgentHook`
- `Harness`

*Since: 1.0*

## Factory Methods

### `create`

```java
public static @NonNull HookRegistry create()
```

Creates an empty registry.

---

### `of(AgentHook...)`

```java
public static @NonNull HookRegistry of(@NonNull AgentHook... hooks)
```

Creates a registry pre-populated with the given hooks.

## Methods

### `add`

```java
public @NonNull HookRegistry add(@NonNull AgentHook hook)
```

Adds a hook to the registry. Returns `this` for chaining.

---

### `fireBeforeRun`

```java
public void fireBeforeRun(@NonNull AgenticContext context)
```

Dispatches `beforeRun` to all hooks in registration order.

---

### `fireAfterRun`

```java
public void fireAfterRun(@NonNull AgentResult result, @NonNull AgenticContext context)
```

Dispatches `afterRun` to all hooks in reverse order.

---

### `fireBeforeToolCall`

```java
public void fireBeforeToolCall(@NonNull FunctionToolCall call, @NonNull AgenticContext context)
```

Dispatches `beforeToolCall` to all hooks in registration order.

---

### `fireAfterToolCall`

```java
public void fireAfterToolCall(
    @NonNull FunctionToolCall call,
    @NonNull ToolExecution execution,
    @NonNull AgenticContext context)
```

Dispatches `afterToolCall` to all hooks in reverse order.

## Integration with Agent

```java
HookRegistry hooks = HookRegistry.create()
    .add(new LoggingHook())
    .add(new CostTrackingHook())
    .add(new AlertingHook());

Agent agent = Agent.builder()
    .name("Assistant")
    .hookRegistry(hooks)
    .build();
```

`Agent.Builder.hookRegistry(HookRegistry)` wires the registry into the agent's agentic loop. Hooks fire at:

- `beforeRun` / `afterRun` — around the entire interaction
- `beforeToolCall` / `afterToolCall` — around each individual tool invocation
