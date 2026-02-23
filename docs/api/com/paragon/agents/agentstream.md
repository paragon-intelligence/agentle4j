# :material-code-braces: AgentStream

> This docs was updated at: 2026-02-23

`com.paragon.agents.AgentStream` &nbsp;·&nbsp; **Class**

---

Streaming agent interaction with full agentic loop.

Unlike simple streaming which only streams one LLM response, AgentStream runs the complete
agentic loop including guardrails, tool execution, and multi-turn conversations, emitting events
at each phase.

Example usage:

```java
agent.interactStream("Help me debug this code", context)
    .onTurnStart(turn -> System.out.println("--- Turn " + turn + " ---"))
    .onTextDelta(chunk -> System.out.print(chunk))
    .onToolCallPending((call, approve) -> {
        if (isSafe(call)) approve.accept(true);  // Human-in-the-loop
        else approve.accept(false);
    })
    .onToolExecuted(exec -> System.out.println("Tool: " + exec.toolName()))
    .onComplete(result -> System.out.println("\nDone!"))
    .onError(e -> e.printStackTrace())
    .start();
```

*Since: 1.0*

## Methods

### `AgentStream`

```java
AgentStream(
      @NonNull Agent agent,
      @NonNull AgenticContext context,
      @NonNull Responder responder,
      @NonNull ObjectMapper objectMapper)
```

Constructor for context-only (no new input).

---

### `AgentStream`

```java
AgentStream(
      @NonNull Agent agent,
      @NonNull List<ResponseInputItem> input,
      @NonNull AgenticContext context,
      @NonNull Responder responder,
      @NonNull ObjectMapper objectMapper,
      @NonNull List<ToolExecution> initialExecutions,
      int startTurn)
```

Constructor for resuming from a saved state.

---

### `AgentStream`

```java
private AgentStream(@NonNull AgentResult failedResult)
```

Creates a pre-failed AgentStream for immediate error return (e.g., guardrail failure).

---

### `failed`

```java
static @NonNull AgentStream failed(@NonNull AgentResult failedResult)
```

Creates a pre-failed stream that immediately completes with the given result.

---

### `completed`

```java
static @NonNull AgentStream completed(@NonNull AgentResult completedResult)
```

Creates a pre-completed stream that immediately returns the given result.

---

### `onTurnStart`

```java
public @NonNull AgentStream onTurnStart(@NonNull Consumer<Integer> handler)
```

Called at the start of each turn in the agentic loop.

**Parameters**

| Name | Description |
|------|-------------|
| `handler` | receives the turn number (1-indexed) |

**Returns**

this for chaining

---

### `onTextDelta`

```java
public @NonNull AgentStream onTextDelta(@NonNull Consumer<String> handler)
```

Called for each text chunk as it streams from the LLM.

**Parameters**

| Name | Description |
|------|-------------|
| `handler` | receives text deltas |

**Returns**

this for chaining

---

### `onTurnComplete`

```java
public @NonNull AgentStream onTurnComplete(@NonNull Consumer<Response> handler)
```

Called when each turn's LLM response is complete (before tool execution).

**Parameters**

| Name | Description |
|------|-------------|
| `handler` | receives the Response |

**Returns**

this for chaining

---

### `onToolCallPending`

```java
public @NonNull AgentStream onToolCallPending(@NonNull ToolConfirmationHandler handler)
```

Called when a tool call is detected, BEFORE execution. Enables human-in-the-loop.

If this handler is set, tools are NOT auto-executed. The handler must call the callback with
`true` to approve or `false` to reject.

Example:

```java
.onToolCallPending((call, approve) -> {
    System.out.println("Tool: " + call.name() + " - Args: " + call.arguments());
    boolean userApproved = askUser("Execute this tool?");
    approve.accept(userApproved);
})
```

**Parameters**

| Name | Description |
|------|-------------|
| `handler` | receives (FunctionToolCall, approval callback) |

**Returns**

this for chaining

---

### `onPause`

```java
public @NonNull AgentStream onPause(@NonNull PauseHandler handler)
```

Called when a tool call should pause for approval. Use for long-running approvals.

Unlike `onToolCallPending`, this handler receives a serializable `AgentRunState`
that can be persisted (e.g., to a database) and resumed later with `Agent.resume()`.

Example:

```java
.onPause(state -> {
    // Save to database for later approval
    saveToDatabase(state);
    System.out.println("Run paused. Pending: " + state.pendingToolCall().name());
})
```

**Parameters**

| Name | Description |
|------|-------------|
| `handler` | receives the pausable state |

**Returns**

this for chaining

---

### `onToolExecuted`

```java
public @NonNull AgentStream onToolExecuted(@NonNull Consumer<ToolExecution> handler)
```

Called after a tool is executed (whether auto or manually approved).

**Parameters**

| Name | Description |
|------|-------------|
| `handler` | receives the ToolExecution result |

**Returns**

this for chaining

---

### `onGuardrailFailed`

```java
public @NonNull AgentStream onGuardrailFailed(@NonNull Consumer<GuardrailResult.Failed> handler)
```

Called if a guardrail check fails.

**Parameters**

| Name | Description |
|------|-------------|
| `handler` | receives the failed guardrail result |

**Returns**

this for chaining

---

### `onHandoff`

```java
public @NonNull AgentStream onHandoff(@NonNull Consumer<Handoff> handler)
```

Called when a handoff to another agent is triggered.

**Parameters**

| Name | Description |
|------|-------------|
| `handler` | receives the Handoff |

**Returns**

this for chaining

---

### `onComplete`

```java
public @NonNull AgentStream onComplete(@NonNull Consumer<AgentResult> handler)
```

Called when the agentic loop completes successfully.

**Parameters**

| Name | Description |
|------|-------------|
| `handler` | receives the final AgentResult |

**Returns**

this for chaining

---

### `onError`

```java
public @NonNull AgentStream onError(@NonNull Consumer<Throwable> handler)
```

Called if an error occurs during the agentic loop.

**Parameters**

| Name | Description |
|------|-------------|
| `handler` | receives the error |

**Returns**

this for chaining

---

### `start`

```java
public @NonNull AgentResult start()
```

Starts the streaming agentic loop. Blocks until completion.

On virtual threads, blocking is efficient and does not consume platform threads.

**Returns**

the final AgentResult

---

### `startBlocking`

```java
public @NonNull AgentResult startBlocking()
```

Starts the streaming agentic loop synchronously (blocking).

**Returns**

the final AgentResult

---

### `handle`

```java
void handle(@NonNull FunctionToolCall call, @NonNull Consumer<Boolean> approvalCallback)
```

Called when a tool call is pending.

**Parameters**

| Name | Description |
|------|-------------|
| `call` | the pending tool call |
| `approvalCallback` | call with true to execute, false to reject |

---

### `onPause`

```java
void onPause(@NonNull AgentRunState state)
```

Called when an agent run should pause for tool approval.

The state is serializable and can be persisted to a database. Resume later with `Agent.resume(state)`.

**Parameters**

| Name | Description |
|------|-------------|
| `state` | the serializable run state to save |

