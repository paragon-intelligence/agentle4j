# :material-approximately-equal: AgentHook

> This docs was updated at: 2026-03-15















`com.paragon.harness.AgentHook` &nbsp;·&nbsp; **Interface**

---

Lifecycle hook that intercepts agent and tool execution events.

Implement this interface to inject cross-cutting concerns — logging, rate limiting, cost tracking, circuit breakers — without modifying agent logic. All methods have no-op defaults, so you only override the events you care about.

**See Also**

- `HookRegistry`
- `Harness`

*Since: 1.0*

## Methods

### `beforeRun`

```java
default void beforeRun(@NonNull AgenticContext context)
```

Called before the agent's agentic loop starts.

---

### `afterRun`

```java
default void afterRun(@NonNull AgentResult result, @NonNull AgenticContext context)
```

Called after the agent's agentic loop completes (success or failure).

---

### `beforeToolCall`

```java
default void beforeToolCall(@NonNull FunctionToolCall call, @NonNull AgenticContext context)
```

Called before a tool is invoked.

---

### `afterToolCall`

```java
default void afterToolCall(
    @NonNull FunctionToolCall call,
    @NonNull ToolExecution execution,
    @NonNull AgenticContext context)
```

Called after a tool has been invoked.

## Usage

```java
// Log every tool call with its duration
AgentHook timingHook = new AgentHook() {
    private final Map<String, Instant> starts = new ConcurrentHashMap<>();

    @Override
    public void beforeToolCall(FunctionToolCall call, AgenticContext ctx) {
        starts.put(call.callId(), Instant.now());
    }

    @Override
    public void afterToolCall(FunctionToolCall call, ToolExecution exec, AgenticContext ctx) {
        Instant start = starts.remove(call.callId());
        long ms = Duration.between(start, Instant.now()).toMillis();
        System.out.printf("Tool %s took %dms (success=%s)%n",
            call.name(), ms, exec.isSuccess());
    }
};

Agent agent = Agent.builder()
    .hookRegistry(HookRegistry.of(timingHook))
    .build();
```
