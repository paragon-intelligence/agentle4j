# :material-code-braces: AgentResult

> This docs was updated at: 2026-02-23

`com.paragon.agents.AgentResult` &nbsp;·&nbsp; **Class**

---

The result of an agent interaction, containing the final output and execution metadata.

AgentResult captures everything that happened during an agent run:

  
- The final text output
- The last API response from the LLM
- Complete conversation history
- All tool executions that occurred
- Number of LLM turns used
- Any handoff that was triggered
- Any error that occurred
- Related results from parallel/composite execution

### Usage Example

```java
AgentResult result = agent.interact("What's the status of order #12345?");
if (result.isSuccess()) {
    System.out.println(result.output());
} else if (result.isHandoff()) {
    // Handoff was auto-executed, result contains final output
    System.out.println("Handled by: " + result.handoffAgent().name());
    System.out.println(result.output());
} else {
    System.err.println("Error: " + result.error().getMessage());
}
```

**See Also**

- `Agent`

*Since: 1.0*

## Methods

### `success`

```java
public static @NonNull AgentResult success(
      @NonNull String output,
      @NonNull Response response,
      @NonNull AgenticContext context,
      @NonNull List<ToolExecution> toolExecutions,
      int turnsUsed)
```

Creates a successful result.

**Parameters**

| Name | Description |
|------|-------------|
| `output` | the final text output |
| `response` | the final API response |
| `context` | the agent context with history |
| `toolExecutions` | all tool executions |
| `turnsUsed` | number of LLM turns |

**Returns**

a success result

---

### `success`

```java
public static @NonNull AgentResult success(@NonNull String output)
```

Convenience method for creating a simple successful result (for testing).

**Parameters**

| Name | Description |
|------|-------------|
| `output` | the final text output |

**Returns**

a minimal success result

---

### `successWithParsed`

```java
public static <T> @NonNull AgentResult successWithParsed(
      @NonNull String output,
      @NonNull T parsed,
      @NonNull Response response,
      @NonNull AgenticContext context,
      @NonNull List<ToolExecution> toolExecutions,
      int turnsUsed)
```

Creates a successful result with parsed structured output.

**Parameters**

| Name | Description |
|------|-------------|
| `output` | the final text output (JSON) |
| `parsed` | the parsed structured object |
| `response` | the final API response |
| `context` | the agent context with history |
| `toolExecutions` | all tool executions |
| `turnsUsed` | number of LLM turns |
| `<T>` | the parsed type |

**Returns**

a success result with parsed output

---

### `handoff`

```java
public static @NonNull AgentResult handoff(
      @NonNull Agent handoffAgent,
      @NonNull AgentResult innerResult,
      @NonNull AgenticContext context)
```

Creates a handoff result (after auto-executing the target agent).

**Parameters**

| Name | Description |
|------|-------------|
| `handoffAgent` | the agent that was handed off to |
| `innerResult` | the result from the handoff agent |
| `context` | the original context with combined history |

**Returns**

a handoff result

---

### `error`

```java
public static @NonNull AgentResult error(
      @NonNull Throwable error, @NonNull AgenticContext context, int turnsUsed)
```

Creates an error result.

**Parameters**

| Name | Description |
|------|-------------|
| `error` | the error that occurred |
| `context` | the agent context at time of error |
| `turnsUsed` | number of LLM turns before error |

**Returns**

an error result

---

### `error`

```java
public static @NonNull AgentResult error(@NonNull Throwable error)
```

Convenience method for creating a simple error result (for testing).

**Parameters**

| Name | Description |
|------|-------------|
| `error` | the error that occurred |

**Returns**

a minimal error result

---

### `guardrailFailed`

```java
public static @NonNull AgentResult guardrailFailed(
      @NonNull String reason, @NonNull AgenticContext context)
```

Creates an error result from a guardrail failure.

**Parameters**

| Name | Description |
|------|-------------|
| `reason` | the guardrail failure reason |
| `context` | the agent context |

**Returns**

an error result

---

### `paused`

```java
public static @NonNull AgentResult paused(
      @NonNull AgentRunState state, @NonNull AgenticContext context)
```

Creates a paused result for human-in-the-loop tool approval.

The run can be resumed later with `Agent.resume(state)`.

**Parameters**

| Name | Description |
|------|-------------|
| `state` | the serializable run state |
| `context` | the agent context |

**Returns**

a paused result

---

### `composite`

```java
public static @NonNull AgentResult composite(
      @NonNull AgentResult primary, @NonNull List<AgentResult> related)
```

Creates a composite result containing a primary result and related results.

This is useful for patterns like ParallelAgents where multiple agents run concurrently and
one result is selected as primary while the others are preserved as related results.

**Parameters**

| Name | Description |
|------|-------------|
| `primary` | the primary result (e.g., first to complete) |
| `related` | the other related results |

**Returns**

a result containing both primary and related results

---

### `output`

```java
public @Nullable String output()
```

Returns the final text output from the agent.

**Returns**

the output text, or null if the run failed before producing output

---

### `finalResponse`

```java
public @Nullable Response finalResponse()
```

Returns the final API response.

**Returns**

the last Response from the LLM, or null if not available

---

### `history`

```java
public @NonNull List<ResponseInputItem> history()
```

Returns the complete conversation history.

**Returns**

an unmodifiable list of all messages

---

### `toolExecutions`

```java
public @NonNull List<ToolExecution> toolExecutions()
```

Returns all tool executions that occurred during the run.

**Returns**

an unmodifiable list of tool executions

---

### `turnsUsed`

```java
public int turnsUsed()
```

Returns the number of LLM turns used.

**Returns**

the turn count

---

### `handoffAgent`

```java
public @Nullable Agent handoffAgent()
```

Returns the agent that was handed off to, if a handoff occurred.

**Returns**

the handoff target agent, or null if no handoff

---

### `error`

```java
public @Nullable Throwable error()
```

Returns the error if the run failed.

**Returns**

the error, or null if successful

---

### `parsed`

```java
public <T> @Nullable T parsed()
```

Returns the parsed structured output if applicable.

**Parameters**

| Name | Description |
|------|-------------|
| `<T>` | the expected type |

**Returns**

the parsed object, or null if not a structured output run

---

### `isSuccess`

```java
public boolean isSuccess()
```

Checks if this is a successful result.

**Returns**

true if no error and no handoff occurred

---

### `isHandoff`

```java
public boolean isHandoff()
```

Checks if a handoff occurred (to another agent).

**Returns**

true if control was transferred to another agent

---

### `isError`

```java
public boolean isError()
```

Checks if an error occurred.

**Returns**

true if an error occurred

---

### `hasParsed`

```java
public boolean hasParsed()
```

Checks if this result contains parsed structured output.

**Returns**

true if parsed output is available

---

### `isPaused`

```java
public boolean isPaused()
```

Checks if this run is paused waiting for approval.

**Returns**

true if paused

---

### `pausedState`

```java
public @Nullable AgentRunState pausedState()
```

Returns the paused state if this run is paused.

**Returns**

the paused state, or null if not paused

---

### `relatedResults`

```java
public @NonNull List<AgentResult> relatedResults()
```

Returns related results from parallel or composite execution.

When using patterns like parallel execution, this contains the results from other agents
that ran alongside the primary result.

**Returns**

an unmodifiable list of related results (empty if none)

---

### `hasRelatedResults`

```java
public boolean hasRelatedResults()
```

Checks if this result has related results from parallel execution.

**Returns**

true if related results are present

---

### `toStructured`

```java
public <T> @NonNull StructuredAgentResult<T> toStructured(
      @NonNull Class<T> outputType, @NonNull ObjectMapper objectMapper)
```

Parses this result's output text into a typed `StructuredAgentResult`.

If this result is an error, the error is propagated. Otherwise the output JSON is
deserialized to the given type.

**Parameters**

| Name | Description |
|------|-------------|
| `outputType` | the target class to deserialize into |
| `objectMapper` | the Jackson mapper for JSON parsing |
| `<T>` | the output type |

**Returns**

a structured result with parsed output, or an error result if parsing fails

