# Feature Requests — Agentle Framework

These requests emerged from building the TBL Session Architect cookbook (`cookbooks/tbl/`), a
complex multi-agent pipeline with a SupervisorAgent orchestrating six specialists, including a
ParallelAgents node. They represent the gap between what the framework can do today and what a
developer building production-grade multi-agent systems needs.

Each request is self-contained: motivation, current limitation, proposed API, affected files,
and a concrete before/after example.

---

## §1 — Fix: True Token-Level Streaming in `AgentStream`

### Problem

`AgentStream.streamLLMResponse()` (line 428–441) calls the blocking `responder.respond(payload)`
and emits the **entire response text at once** via `onTextDelta`. The method even has a
self-documenting `// TODO: Integrate with ResponseStream for true streaming within each turn`
comment. As a result, `onTextDelta` is misleadingly named — it is batch notification, not
streaming.

```java
// Current (AgentStream.java:432–440)
private Response streamLLMResponse(AgenticContext context) {
    CreateResponsePayload payload = agent.buildPayloadInternal(context);
    // For now, use non-streaming since we need the full agentic loop
    // TODO: Integrate with ResponseStream for true streaming within each turn
    Response response = responder.respond(payload);                  // ← blocks until full response
    if (onTextDelta != null && response.outputText() != null) {
        onTextDelta.accept(response.outputText());                   // ← fires once with full text
    }
    return response;
}
```

This makes `AgentStream` useless for UI streaming (chat interfaces, progress indicators, live
output). Every UI that uses `onTextDelta` today is receiving a batch, not a stream.

### Proposed Change

**`Responder` already supports streaming** via payload-type overloading — the same pattern used
by `Interactable.interact()`. No new method is needed on `Responder`. The existing overload
`respond(CreateResponsePayload.Streaming)` → `ResponseStream<Void>` already handles this.

The fix is entirely inside `AgentStream.streamLLMResponse()`: build a
`CreateResponsePayload.Streaming` variant of the payload, call the existing streaming overload,
wire the `onTextDelta` callback onto the `ResponseStream`, then block with
`ResponseStream.get()` to obtain the final `Response` that the agentic loop needs for tool-call
detection.

```java
// Proposed — only AgentStream.java changes
private Response streamLLMResponse(AgenticContext context) {
    CreateResponsePayload base = agent.buildPayloadInternal(context);
    CreateResponsePayload.Streaming streamingPayload = base.asStreaming();
    ResponseStream<Void> stream = responder.respond(streamingPayload);
    if (onTextDelta != null) {
        stream.onTextDelta(onTextDelta);   // fires per token during blocking get()
    }
    return stream.get();                   // blocks until complete; returns full Response with tool calls
}
```

This is virtual-thread-friendly: `ResponseStream.get()` blocks cheaply on a virtual thread
while firing `onTextDelta` per token, and the returned `Response` contains all tool calls so
the agentic loop continues unchanged.

### Affected Files

- `src/main/java/com/paragon/agents/AgentStream.java` — update `streamLLMResponse()`

### Impact

Every existing `AgentStream` user that registered `onTextDelta` will immediately get true
token-by-token streaming with no API change on their side. No breaking changes.

---

## §2 — New: `AgentEventBus` in `AgenticContext`

### Problem

When a `SupervisorAgent` (or any agent using `InteractableSubAgentTool`) invokes a worker agent,
the worker runs in complete observational isolation. No events from the worker's execution are
visible to the outer stream. The `AgentStream` on the supervisor fires `onToolExecuted` only
_after_ the worker finishes, with a single `ToolExecution` record — but nothing during.

For multi-agent pipelines this means:
- You cannot show the user "Research Curator is working..." as it streams
- You cannot display tool calls made by worker agents in real time
- You cannot distinguish which worker produced which tokens when running in parallel
- `onTextDelta` on the outer stream is silent while workers run

### Proposed Change

**2a. Define `AgentEventBus`:**

```java
/**
 * Event bus that propagates agent lifecycle events through the call stack.
 * Set on an AgenticContext; all child contexts inherit it, enabling nested
 * agent invocations to surface events to the outermost AgentStream.
 */
public interface AgentEventBus {

    /** Fires when an agent starts a new turn. */
    void publishAgentStart(@NonNull String agentName, int turn);

    /** Fires for each token emitted by an agent (requires true streaming, see §1). */
    void publishAgentTextDelta(@NonNull String agentName, @NonNull String delta);

    /** Fires when an agent executes one of its own tools. */
    void publishAgentToolExecuted(@NonNull String agentName, @NonNull ToolExecution execution);

    /** Fires when an agent's full agentic loop completes. */
    void publishAgentComplete(@NonNull String agentName, @NonNull AgentResult result);

    /** No-op implementation used when no bus is registered. */
    AgentEventBus NOOP = new AgentEventBus() {
        public void publishAgentStart(String n, int t) {}
        public void publishAgentTextDelta(String n, String d) {}
        public void publishAgentToolExecuted(String n, ToolExecution e) {}
        public void publishAgentComplete(String n, AgentResult r) {}
    };
}
```

**2b. Add `eventBus` to `AgenticContext`:**

```java
// AgenticContext additions
public @NonNull AgentEventBus eventBus() {
    return eventBus != null ? eventBus : AgentEventBus.NOOP;
}

public void setEventBus(@NonNull AgentEventBus bus) {
    this.eventBus = Objects.requireNonNull(bus);
}
```

**2c. Propagate bus to child contexts in `createChildContext()`:**

```java
// AgenticContext.createChildContext() — already inherits state/history
// additionally inherit event bus:
child.setEventBus(this.eventBus());
```

**2d. `AgentStream` creates and registers a `DefaultAgentEventBus` before the loop:**

```java
// AgentStream.runAgenticLoop() — before the while loop
DefaultAgentEventBus bus = new DefaultAgentEventBus(
    onAgentStart, onAgentTextDelta, onAgentToolExecuted, onAgentComplete);
context.setEventBus(bus);
```

`DefaultAgentEventBus` is a simple internal implementation that dispatches to the registered
`AgentStream` callbacks (see §3).

### Affected Files

- `src/main/java/com/paragon/agents/AgentEventBus.java` — new interface
- `src/main/java/com/paragon/agents/AgenticContext.java` — add `eventBus` field + methods
- `src/main/java/com/paragon/agents/AgentStream.java` — create and register bus before loop

---

## §3 — New: Nested Agent Event Hooks on `AgentStream`

### Problem

`AgentStream` exposes hooks for the _outermost_ agent's lifecycle. There are no hooks for events
originating from worker agents inside that agent's tool calls. Developers building multi-agent
systems have no way to observe the inner pipeline.

### Proposed Change

Add four new hooks to `AgentStream`, backed by the `AgentEventBus` from §2:

```java
/**
 * Called when a nested (worker) agent starts a new turn.
 * Fires for every turn, not just the first — use the turn number to distinguish start.
 *
 * @param handler receives (agentName, turnNumber)
 */
public @NonNull AgentStream onAgentStart(@NonNull BiConsumer<String, Integer> handler) {
    this.onAgentStart = Objects.requireNonNull(handler);
    return this;
}

/**
 * Called for each token emitted by a nested agent.
 * Requires fix §1 (true streaming) to function at token granularity.
 * Without §1, fires once per turn with the full turn text.
 *
 * @param handler receives (agentName, textDelta)
 */
public @NonNull AgentStream onAgentTextDelta(@NonNull BiConsumer<String, String> handler) {
    this.onAgentTextDelta = Objects.requireNonNull(handler);
    return this;
}

/**
 * Called when a nested agent executes one of its own tools.
 *
 * @param handler receives (agentName, toolExecution)
 */
public @NonNull AgentStream onAgentToolExecuted(
        @NonNull BiConsumer<String, ToolExecution> handler) {
    this.onAgentToolExecuted = Objects.requireNonNull(handler);
    return this;
}

/**
 * Called when a nested agent's full agentic loop completes.
 * This is the point at which the worker's output is available as input to the next step.
 *
 * @param handler receives (agentName, agentResult)
 */
public @NonNull AgentStream onAgentComplete(@NonNull BiConsumer<String, AgentResult> handler) {
    this.onAgentComplete = Objects.requireNonNull(handler);
    return this;
}
```

**Usage example** (from `TblPipelineStream.java` in the cookbook):

```java
orchestrator.asStreaming().interact(prompt)
    .onAgentStart((name, turn) -> {
        if (turn == 1) System.out.println("▶ " + name + " starting...");
    })
    .onAgentTextDelta((name, delta) -> {
        System.out.print("[" + name + "] " + delta);
    })
    .onAgentToolExecuted((name, exec) -> {
        System.out.println("  [tool] " + name + " → " + exec.toolName());
    })
    .onAgentComplete((name, result) -> {
        System.out.println("✓ " + name + " done (" + result.turnsUsed() + " turns)");
    })
    .onComplete(result -> System.out.println("\nPipeline complete."))
    .start();
```

**With the `TblPipelineStream` wrapper** (translates agent names to typed phases):

```java
useCase.executeStream(input)
    .onPhaseStart(phase -> System.out.println("▶ " + phase.label()))
    .onAgentTextDelta((phase, delta) -> System.out.print(delta))
    .onPhaseComplete((phase, result) -> System.out.println("✓ " + phase.label()))
    .onComplete(result -> System.out.println("Session kit ready."))
    .start();
```

### Affected Files

- `src/main/java/com/paragon/agents/AgentStream.java` — add 4 fields + 4 registration methods
- `src/main/java/com/paragon/agents/AgentStream.java` (inner `DefaultAgentEventBus`) — new class

---

## §4 — New: Nested Event Propagation in `InteractableSubAgentTool`

### Problem

`InteractableSubAgentTool.call()` invokes `target.interact(childContext)` with no event
propagation. Even after §2 adds the `AgentEventBus`, the tool must explicitly:

1. Publish `agentStart` before calling the target
2. Propagate the bus to the child context so the target's own agent loop can publish text deltas
3. Publish `agentComplete` after the target returns

### Proposed Change

```java
// InteractableSubAgentTool.call() — updated
@Override
public @NonNull FunctionToolCallOutput call(@Nullable InteractableParams params) {
    if (params == null || params.request() == null || params.request().isEmpty()) {
        return FunctionToolCallOutput.error("Request cannot be empty");
    }

    try {
        AgenticContext childContext = buildChildContext(params.request());

        // Inherit event bus from parent context and announce start
        AgenticContext.current().ifPresent(parent -> {
            AgentEventBus bus = parent.eventBus();
            childContext.setEventBus(bus);              // propagate to child
            bus.publishAgentStart(target.name(), 1);    // announce to outer stream
        });

        AgentResult result = target.interact(childContext);

        // Announce completion
        AgenticContext.current().ifPresent(parent ->
            parent.eventBus().publishAgentComplete(target.name(), result));

        if (result.isError()) {
            return FunctionToolCallOutput.error(
                "'" + target.name() + "' failed: " + result.error().getMessage());
        }
        return FunctionToolCallOutput.success(result.output());

    } catch (Exception e) {
        AgenticContext.current().ifPresent(parent ->
            parent.eventBus().publishAgentComplete(target.name(),
                AgentResult.error(e, AgenticContext.create(), 0)));
        return FunctionToolCallOutput.error("'" + target.name() + "' error: " + e.getMessage());
    }
}
```

### Affected Files

- `src/main/java/com/paragon/agents/InteractableSubAgentTool.java` — update `call()`

---

## §5 — New: Agent-Level Events Inside `AgentStream`'s Own Loop

### Problem

The `AgentEventBus` (§2) is designed to propagate events from _worker_ agents up to the outer
stream. But the outer agent itself — the one that _owns_ the `AgentStream` — also needs to
publish its own turn-start and text-delta events through the bus, so that if _it_ is used as
a worker inside a higher-level stream, its events propagate upward correctly.

This is the "infinite nesting" case: Agent A runs as a supervisor, which calls Agent B as a
worker, which in turn calls Agent C as a worker. Agent C's events should bubble all the way up
to Agent A's `AgentStream`.

### Proposed Change

Inside `AgentStream.runAgenticLoop()`, wire the agent's own events through the bus:

```java
// AgentStream.runAgenticLoop() — additions inside the while loop

// Publish own turn start to the bus (for when this agent is itself a worker)
context.eventBus().publishAgentStart(agent.name(), turn);

// After streamLLMResponse, publish the text delta through the bus too
// (streamLLMResponse already fires onTextDelta for the local handler;
//  this additionally propagates to any parent bus)
String text = lastResponse.outputText();
if (text != null && !text.isBlank()) {
    context.eventBus().publishAgentTextDelta(agent.name(), text);
}

// After each tool execution, publish through the bus
context.eventBus().publishAgentToolExecuted(agent.name(), exec);

// At the end of runAgenticLoop, before returning successResult:
context.eventBus().publishAgentComplete(agent.name(), successResult);
```

### Affected Files

- `src/main/java/com/paragon/agents/AgentStream.java` — update `runAgenticLoop()`

---

## §6 — New: Nested Event Propagation in `ParallelAgents` (via `ParallelStream`)

### Problem

When a `ParallelAgents` instance is used as a worker inside a `SupervisorAgent`
(i.e., wrapped by `InteractableSubAgentTool`), it is invoked via `target.interact()`. This
calls `ParallelAgents.interact()` → `runAll()` → individual agent `interact()` calls. No
streaming and no event bus propagation happens for the individual members.

After §4, the `InteractableSubAgentTool` will announce the `ParallelAgents` as a single agent
(`publishAgentStart("AnalysisPhase", 1)`). But the individual member agents (StudentSimulator,
ActivityArchitect) will be silent — their events will not surface.

### Proposed Change

`ParallelAgents.interact()` should propagate the event bus to each member's context:

```java
// ParallelAgents.runAll() — updated fork loop
for (Interactable member : members) {
    AgenticContext ctx = context.copy();
    ctx.withTraceContext(parentTraceId, parentSpanId);
    // propagate bus so member agents publish events to the same bus
    ctx.setEventBus(context.eventBus());
    subtasks.add(scope.fork(() -> member.interact(ctx)));
}
```

With §5 in place, each member agent's loop will call `publishAgentStart`, `publishAgentTextDelta`,
and `publishAgentComplete` on the shared bus, and those events will bubble up through the full
pipeline to the outermost `AgentStream`.

### Affected Files

- `src/main/java/com/paragon/agents/ParallelAgents.java` — update `runAll()` and `runFirst()`

---

## §7 — New: `AgentStream.onTurnDelta` (Distinguished from `onTextDelta`)

### Problem

After §1, `onTextDelta` will fire true token-by-token for the outermost agent's text.
After §3, `onAgentTextDelta` will fire for nested agents' text. But `onTextDelta` is currently
defined only at the "outer agent" level. In a supervisor scenario, the supervisor's "text" is
mostly internal reasoning and tool invocation commands — not output the user should see.

Developers need a clean way to say "give me only the final synthesis tokens" versus "give me
tokens from all agents."

### Proposed Change

No new hook is strictly needed — the distinction is already covered:

- `onTextDelta` = outer agent (the supervisor/orchestrator) tokens
- `onAgentTextDelta` = worker agent tokens

But the Javadoc on `onTextDelta` should be updated to clarify this distinction explicitly.

Additionally, consider adding:

```java
/**
 * Called for every token from ANY agent in the pipeline (outer + all workers).
 * Useful for progress indicators that show total pipeline activity.
 *
 * @param handler receives (agentName, textDelta)
 */
public @NonNull AgentStream onAnyTextDelta(@NonNull BiConsumer<String, String> handler);
```

### Affected Files

- `src/main/java/com/paragon/agents/AgentStream.java` — add `onAnyTextDelta`, update Javadoc

---

## §8 — New: `NetworkStream` Nested Events

### Problem

`AgentNetwork` (peer discussion pattern) has its own `NetworkStream` class. The same nested
event gap exists: when an `AgentNetwork` is used as a worker inside a supervisor, individual
peer contributions are invisible to the outer stream.

### Proposed Change

Apply the same `AgentEventBus` propagation from §4 and §6:
- `NetworkStream.startAll()` should set the inherited event bus on each peer's context
- Each peer's `AgentStream` then fires through the shared bus

Additionally, `NetworkStream` could gain an `onPeerContribution` callback:

```java
/**
 * Called when a peer agent in the network produces its contribution for a round.
 *
 * @param handler receives (peerAgent, roundNumber, contribution)
 */
public @NonNull NetworkStream onPeerContribution(
    @NonNull TriConsumer<Interactable, Integer, String> handler);
```

### Affected Files

- `src/main/java/com/paragon/agents/NetworkStream.java`
- `src/main/java/com/paragon/agents/AgentNetwork.java`

---

## §9 — Enhancement: `ParallelStream` return type refinement

### Problem

`ParallelStream.start()` returns `Object` (line 147), requiring an unchecked cast at the call
site. This is a usability issue for all three modes (ALL → `List<AgentResult>`, FIRST →
`AgentResult`, SYNTHESIZE → `AgentResult`).

### Proposed Change

Introduce typed terminal methods instead of a single `start()`:

```java
// Replace start() with mode-specific terminals
public @NonNull List<AgentResult> startAll();       // for ALL mode
public @NonNull AgentResult startFirst();           // for FIRST mode
public @NonNull AgentResult startAndSynthesize();   // for SYNTHESIZE mode
```

Keep `start()` as a convenience that delegates to the correct typed method based on mode,
but return `AgentResult` (using the composite result for ALL mode — consistent with
`ParallelAgents.interact()`).

### Affected Files

- `src/main/java/com/paragon/agents/ParallelStream.java`

---

## Implementation Priority

| # | Feature | Effort | Impact | Priority |
|---|---------|--------|--------|----------|
| §1 | True token streaming | Medium | Very high — affects every streaming user | **P0** |
| §2 | `AgentEventBus` in context | Low | Foundational — all other §§ depend on it | **P0** |
| §3 | New `AgentStream` hooks | Low | High — directly enables rich pipeline UX | **P1** |
| §4 | `InteractableSubAgentTool` propagation | Low | Required for nested events to work | **P1** |
| §5 | Agent self-publish in loop | Low | Required for infinite-nesting correctness | **P1** |
| §6 | `ParallelAgents` bus propagation | Low | Enables parallel sub-agent visibility | **P1** |
| §7 | `onAnyTextDelta` + Javadoc | Very low | Nice-to-have, clarity | P2 |
| §8 | `NetworkStream` nested events | Medium | Completes the pattern for all agent types | P2 |
| §9 | `ParallelStream` typed terminals | Very low | Ergonomics | P2 |

---

## Complete API Summary (after all changes)

```java
// AgentStream — all hooks after §1–§7
agentStream
    // Outer agent (supervisor/orchestrator itself)
    .onTurnStart(turn -> ...)                                 // existing
    .onTextDelta(chunk -> ...)                                // §1: now true token streaming
    .onTurnComplete(response -> ...)                          // existing
    .onToolCallPending((call, approve) -> ...)                // existing
    .onPause(state -> ...)                                    // existing
    .onToolExecuted(exec -> ...)                              // existing (worker tool calls)
    .onGuardrailFailed(failed -> ...)                         // existing
    .onHandoff(handoff -> ...)                                // existing
    .onComplete(result -> ...)                                // existing
    .onError(e -> ...)                                        // existing

    // Nested agents (workers invoked via InteractableSubAgentTool) — §3
    .onAgentStart((agentName, turn) -> ...)                   // NEW
    .onAgentTextDelta((agentName, delta) -> ...)              // NEW — §1 required for tokens
    .onAgentToolExecuted((agentName, exec) -> ...)            // NEW
    .onAgentComplete((agentName, result) -> ...)              // NEW

    // Convenience — §7
    .onAnyTextDelta((agentName, delta) -> ...)                // NEW

    .start();
```

---

## Affected Files — Master List

```
src/main/java/com/paragon/agents/AgentStream.java          (§1: streamLLMResponse; §3,§5,§7: new hooks + DefaultAgentEventBus)
src/main/java/com/paragon/agents/AgentEventBus.java        (§2: new interface)
src/main/java/com/paragon/agents/AgenticContext.java       (§2: eventBus field + methods)
src/main/java/com/paragon/agents/InteractableSubAgentTool.java  (§4: propagate bus + announce events)
src/main/java/com/paragon/agents/ParallelAgents.java       (§6: propagate bus in runAll/runFirst)
src/main/java/com/paragon/agents/ParallelStream.java       (§9: typed terminal methods)
src/main/java/com/paragon/agents/NetworkStream.java        (§8: onPeerContribution + bus propagation)
src/main/java/com/paragon/agents/AgentNetwork.java         (§8: bus propagation)
```
