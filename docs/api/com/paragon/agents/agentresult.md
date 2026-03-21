# :material-code-braces: AgentResult

> This docs was updated at: 2026-03-21

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
    System.out.println("Messages returned: " + result.messages().size());
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

### `clientSideTool`

```java
public static @NonNull AgentResult clientSideTool(
      @NonNull FunctionToolCall call, @NonNull AgenticContext context, int turnsUsed)
```

Creates a client-side tool result when a `stopsLoop = true` tool is called.

The call is NOT persisted to history and the tool's `call()` method is NOT invoked.
This is a clean, non-error exit from the agentic loop.

**Parameters**

| Name | Description |
|------|-------------|
| `call` | the function tool call that triggered the exit |
| `context` | the agent context at time of exit |
| `turnsUsed` | number of LLM turns used |

**Returns**

a client-side tool result

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

### `hasOutput`

```java
public boolean hasOutput()
```

Returns whether this result has output text available.

**Returns**

true if output text is present

---

### `outputOrEmpty`

```java
public @NonNull String outputOrEmpty()
```

Returns the output text, or an empty string when unavailable.

**Returns**

the output text, or an empty string

---

### `outputOr`

```java
public @NonNull String outputOr(@NonNull String fallback)
```

Returns the output text, or the provided fallback when unavailable.

**Parameters**

| Name | Description |
|------|-------------|
| `fallback` | the fallback text to use when no output is available |

**Returns**

the output text, or the fallback

---

### `finalResponse`

```java
public @Nullable Response finalResponse()
```

Returns the final API response.

**Returns**

the last Response from the LLM, or null if not available

---

### `hasFinalResponse`

```java
public boolean hasFinalResponse()
```

Returns whether a final API response is available.

**Returns**

true if a final response is present

---

### `history`

```java
public @NonNull List<ResponseInputItem> history()
```

Returns the complete conversation history.

**Returns**

an unmodifiable list of all messages

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

Returns all messages present in the result history.

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

Returns all user messages present in the result history.

**Returns**

an immutable list of user messages

---

### `assistantMessages`

```java
public @NonNull List<Message> assistantMessages()
```

Returns all assistant messages present in the result history.

**Returns**

an immutable list of assistant messages

---

### `developerMessages`

```java
public @NonNull List<Message> developerMessages()
```

Returns all developer messages present in the result history.

**Returns**

an immutable list of developer messages

---

### `toolCalls`

```java
public @NonNull List<FunctionToolCall> toolCalls()
```

Returns all function tool calls present in the result history.

**Returns**

an immutable list of function tool calls

---

### `toolOutputs`

```java
public @NonNull List<FunctionToolCallOutput> toolOutputs()
```

Returns all function tool outputs present in the result history.

**Returns**

an immutable list of function tool outputs

---

### `lastMessage`

```java
public @NonNull Optional<Message> lastMessage()
```

Returns the most recent message from the result history.

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

Returns the most recent user message from the result history.

**Returns**

an Optional containing the last user message, or empty if none exist

---

### `lastAssistantMessage`

```java
public @NonNull Optional<Message> lastAssistantMessage()
```

Returns the most recent assistant message from the result history.

**Returns**

an Optional containing the last assistant message, or empty if none exist

---

### `lastDeveloperMessage`

```java
public @NonNull Optional<Message> lastDeveloperMessage()
```

Returns the most recent developer message from the result history.

**Returns**

an Optional containing the last developer message, or empty if none exist

---

### `lastUserMessageText`

```java
public @NonNull Optional<String> lastUserMessageText()
```

Returns the first text segment from the most recent user message in the result history.

**Returns**

an Optional containing the last user text, or empty if none found

---

### `lastUserMessageText`

```java
public @NonNull String lastUserMessageText(@NonNull String fallback)
```

Returns the first text segment from the most recent user message, or a fallback value.

**Parameters**

| Name | Description |
|------|-------------|
| `fallback` | the fallback value when no user message text is found |

**Returns**

the last user message text, or the fallback

---

### `toolExecutions`

```java
public @NonNull List<ToolExecution> toolExecutions()
```

Returns all tool executions that occurred during the run.

**Returns**

an unmodifiable list of tool executions

---

### `hasToolExecutions`

```java
public boolean hasToolExecutions()
```

Returns whether any tool executions occurred during the run.

**Returns**

true if tool executions are present

---

### `toolExecutions`

```java
public @NonNull List<ToolExecution> toolExecutions(@NonNull String toolName)
```

Returns all tool executions for the requested tool name.

**Parameters**

| Name | Description |
|------|-------------|
| `toolName` | the tool name to filter by |

**Returns**

an immutable list of matching tool executions

---

### `lastToolExecution`

```java
public @NonNull Optional<ToolExecution> lastToolExecution()
```

Returns the most recent tool execution, if any.

**Returns**

an Optional containing the last tool execution, or empty if none occurred

---

### `successfulToolExecutions`

```java
public @NonNull List<ToolExecution> successfulToolExecutions()
```

Returns the successful tool executions from this run.

**Returns**

an immutable list of successful tool executions

---

### `failedToolExecutions`

```java
public @NonNull List<ToolExecution> failedToolExecutions()
```

Returns the failed or incomplete tool executions from this run.

**Returns**

an immutable list of failed tool executions

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

### `errorMessage`

```java
public @Nullable String errorMessage()
```

Returns the error message if one occurred.

**Returns**

the error message, or null if no error is present

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

### `parsedOptional`

```java
public @NonNull Optional<?> parsedOptional()
```

Returns the parsed structured output, if available.

**Returns**

an Optional containing the parsed output

---

### `parsedOptional`

```java
public <T> @NonNull Optional<T> parsedOptional(@NonNull Class<T> type)
```

Returns the parsed structured output when it matches the requested type.

**Parameters**

| Name | Description |
|------|-------------|
| `type` | the requested parsed output type |
| `<T>` | the requested parsed output type |

**Returns**

an Optional containing the typed parsed output, or empty if unavailable/incompatible

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

### `isClientSideTool`

```java
public boolean isClientSideTool()
```

Checks if the loop was stopped by a client-side tool (`stopsLoop = true`).

**Returns**

true if a stopsLoop tool triggered the exit

---

### `clientSideToolCall`

```java
public @Nullable FunctionToolCall clientSideToolCall()
```

Returns the tool call that stopped the loop, if applicable.

**Returns**

the client-side tool call, or null if not a client-side tool exit

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

