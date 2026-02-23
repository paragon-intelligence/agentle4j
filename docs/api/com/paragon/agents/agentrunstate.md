# :material-code-braces: AgentRunState

> This docs was updated at: 2026-02-23

`com.paragon.agents.AgentRunState` &nbsp;·&nbsp; **Class**

Implements `Serializable`

---

Serializable state of a paused agent run.

When an agent run is paused (e.g., waiting for human approval of a tool call), this state
captures everything needed to resume the run later - even days later.

Usage:

```java
// Pause when tool needs approval
AgentRunState state = agent.interact("Do something")
    .onToolCallPending((call, pause) -> {
        AgentRunState pausedState = pause.pauseForApproval(call);
        saveToDatabase(pausedState);  // Persist for later
    })
    .start().join();
// Resume days later
AgentRunState savedState = loadFromDatabase();
savedState.approveToolCall(toolCallOutput);  // Or rejectToolCall()
AgentResult result = agent.resume(savedState);
```

*Since: 1.0*

## Methods

### `pendingApproval`

```java
static AgentRunState pendingApproval(
      @NonNull String agentName,
      @NonNull AgenticContext context,
      @NonNull FunctionToolCall pendingToolCall,
      @Nullable Response lastResponse,
      @NonNull List<ToolExecution> toolExecutions,
      int currentTurn)
```

Creates a state for a run pending tool approval.

---

### `completed`

```java
static AgentRunState completed(
      @NonNull String agentName,
      @NonNull AgenticContext context,
      @Nullable Response lastResponse,
      @NonNull List<ToolExecution> toolExecutions,
      int currentTurn)
```

Creates a state for a completed run.

---

### `failed`

```java
static AgentRunState failed(
      @NonNull String agentName, @NonNull AgenticContext context, int currentTurn)
```

Creates a state for a failed run.

---

### `approveToolCall`

```java
public void approveToolCall(@NonNull String output)
```

Approves the pending tool call with the given output.

Call this after getting user approval, then pass the state to `Agent.resume()`.

**Parameters**

| Name | Description |
|------|-------------|
| `output` | the tool output to use |

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalStateException` | if not pending approval |

---

### `rejectToolCall`

```java
public void rejectToolCall()
```

Rejects the pending tool call.

Call this if the user denies the tool execution, then pass the state to `Agent.resume()`.

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalStateException` | if not pending approval |

---

### `rejectToolCall`

```java
public void rejectToolCall(@NonNull String reason)
```

Rejects the pending tool call with a reason.

**Parameters**

| Name | Description |
|------|-------------|
| `reason` | the rejection reason (shown to the model) |

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalStateException` | if not pending approval |

