# :material-code-braces: HookRegistry

> This docs was updated at: 2026-03-21

`com.paragon.harness.HookRegistry` &nbsp;·&nbsp; **Class**

---

Ordered registry of `AgentHook` instances executed around agent lifecycle events.

Hooks are invoked in registration order for `before` events and in reverse order
for `after` events (stack semantics).

Hook failures are caught and logged but do not interrupt agent execution, ensuring
that harness infrastructure never breaks agent functionality.

Example:

```java
HookRegistry hooks = HookRegistry.create()
    .add(new LoggingHook())
    .add(new CostTrackingHook())
    .add(new RateLimitingHook(100));
// Wire into agent via Harness builder or directly:
Agent agent = Agent.builder()
    .hookRegistry(hooks)
    .build();
```

**See Also**

- `AgentHook`

*Since: 1.0*

## Methods

### `create`

```java
public static @NonNull HookRegistry create()
```

Creates an empty registry.

---

### `of`

```java
public static @NonNull HookRegistry of(@NonNull AgentHook... hooks)
```

Creates a registry pre-populated with the given hooks.

---

### `add`

```java
public @NonNull HookRegistry add(@NonNull AgentHook hook)
```

Adds a hook to the registry.

**Parameters**

| Name | Description |
|------|-------------|
| `hook` | the hook to add |

**Returns**

this registry (for chaining)

---

### `size`

```java
public int size()
```

Returns the number of registered hooks.

---

### `isEmpty`

```java
public boolean isEmpty()
```

Returns true if no hooks are registered.

---

### `fireBeforeRun`

```java
public void fireBeforeRun(@NonNull AgenticContext context)
```

Dispatches `AgentHook.beforeRun` to all hooks in order.

---

### `fireAfterRun`

```java
public void fireAfterRun(@NonNull AgentResult result, @NonNull AgenticContext context)
```

Dispatches `AgentHook.afterRun` to all hooks in reverse order.

---

### `fireBeforeToolCall`

```java
public void fireBeforeToolCall(@NonNull FunctionToolCall call, @NonNull AgenticContext context)
```

Dispatches `AgentHook.beforeToolCall` to all hooks in order.

---

### `fireAfterToolCall`

```java
public void fireAfterToolCall(
      @NonNull FunctionToolCall call,
      @NonNull ToolExecution execution,
      @NonNull AgenticContext context)
```

Dispatches `AgentHook.afterToolCall` to all hooks in reverse order.
