# :material-code-braces: AgentExecutionException

> This docs was updated at: 2026-02-23

`com.paragon.responses.exception.AgentExecutionException` &nbsp;·&nbsp; **Class**

Extends `AgentleException`

---

Exception thrown when an agent execution fails.

Provides agent-specific context:

  
- `.agentName()` - Name of the agent that failed
- `.phase()` - Phase where failure occurred
- `.turnsCompleted()` - Number of turns completed before failure

Example usage:

```java
AgentResult result = agent.interact("Hello").join();
if (result.isError() && result.error() instanceof AgentExecutionException e) {
    System.err.println("Agent " + e.agentName() + " failed in " + e.phase());
    System.err.println("Completed " + e.turnsCompleted() + " turns before failure");
    if (e.isRetryable()) {
        // Retry logic
    }
}
```

## Fields

### `agentName`

```java
INPUT_GUARDRAIL,
    /** LLM API call failed. */
    LLM_CALL,
    /** Tool execution failed. */
    TOOL_EXECUTION,
    /** Output guardrail validation failed. */
    OUTPUT_GUARDRAIL,
    /** Agent handoff failed. */
    HANDOFF,
    /** Response parsing failed. */
    PARSING,
    /** Max turns limit exceeded. */
    MAX_TURNS_EXCEEDED
  }

  private final @NonNull String agentName
```

Input guardrail validation failed.

---

### `agentName`

```java
LLM_CALL,
    /** Tool execution failed. */
    TOOL_EXECUTION,
    /** Output guardrail validation failed. */
    OUTPUT_GUARDRAIL,
    /** Agent handoff failed. */
    HANDOFF,
    /** Response parsing failed. */
    PARSING,
    /** Max turns limit exceeded. */
    MAX_TURNS_EXCEEDED
  }

  private final @NonNull String agentName
```

LLM API call failed.

---

### `agentName`

```java
TOOL_EXECUTION,
    /** Output guardrail validation failed. */
    OUTPUT_GUARDRAIL,
    /** Agent handoff failed. */
    HANDOFF,
    /** Response parsing failed. */
    PARSING,
    /** Max turns limit exceeded. */
    MAX_TURNS_EXCEEDED
  }

  private final @NonNull String agentName
```

Tool execution failed.

---

### `agentName`

```java
OUTPUT_GUARDRAIL,
    /** Agent handoff failed. */
    HANDOFF,
    /** Response parsing failed. */
    PARSING,
    /** Max turns limit exceeded. */
    MAX_TURNS_EXCEEDED
  }

  private final @NonNull String agentName
```

Output guardrail validation failed.

---

### `agentName`

```java
HANDOFF,
    /** Response parsing failed. */
    PARSING,
    /** Max turns limit exceeded. */
    MAX_TURNS_EXCEEDED
  }

  private final @NonNull String agentName
```

Agent handoff failed.

---

### `agentName`

```java
PARSING,
    /** Max turns limit exceeded. */
    MAX_TURNS_EXCEEDED
  }

  private final @NonNull String agentName
```

Response parsing failed.

---

### `agentName`

```java
MAX_TURNS_EXCEEDED
  }

  private final @NonNull String agentName
```

Max turns limit exceeded.

## Methods

### `AgentExecutionException`

```java
public AgentExecutionException(
      @NonNull String agentName,
      @NonNull Phase phase,
      int turnsCompleted,
      @NonNull String message)
```

Creates a new AgentExecutionException.

**Parameters**

| Name | Description |
|------|-------------|
| `agentName` | the name of the agent that failed |
| `phase` | the phase where failure occurred |
| `turnsCompleted` | number of turns completed before failure |
| `message` | the error message |

---

### `AgentExecutionException`

```java
public AgentExecutionException(
      @NonNull String agentName,
      @NonNull Phase phase,
      int turnsCompleted,
      @NonNull String message,
      @NonNull Throwable cause)
```

Creates a new AgentExecutionException with a cause.

**Parameters**

| Name | Description |
|------|-------------|
| `agentName` | the name of the agent that failed |
| `phase` | the phase where failure occurred |
| `turnsCompleted` | number of turns completed before failure |
| `message` | the error message |
| `cause` | the underlying cause |

---

### `AgentExecutionException`

```java
public AgentExecutionException(
      @NonNull String agentName,
      @NonNull Phase phase,
      int turnsCompleted,
      @Nullable String lastResponseId,
      @NonNull String message,
      @Nullable Throwable cause)
```

Creates a new AgentExecutionException with full context.

**Parameters**

| Name | Description |
|------|-------------|
| `agentName` | the name of the agent that failed |
| `phase` | the phase where failure occurred |
| `turnsCompleted` | number of turns completed before failure |
| `lastResponseId` | the last response ID before failure |
| `message` | the error message |
| `cause` | the underlying cause |

---

### `maxTurnsExceeded`

```java
public static AgentExecutionException maxTurnsExceeded(
      @NonNull String agentName, int maxTurns, int turnsCompleted)
```

Creates an exception for max turns exceeded.

**Parameters**

| Name | Description |
|------|-------------|
| `agentName` | the agent name |
| `maxTurns` | the max turns limit |
| `turnsCompleted` | the turns completed |

**Returns**

a new AgentExecutionException

---

### `llmCallFailed`

```java
public static AgentExecutionException llmCallFailed(
      @NonNull String agentName, int turnsCompleted, @NonNull Throwable cause)
```

Creates an exception for LLM call failure.

**Parameters**

| Name | Description |
|------|-------------|
| `agentName` | the agent name |
| `turnsCompleted` | turns completed before failure |
| `cause` | the underlying cause |

**Returns**

a new AgentExecutionException

---

### `parsingFailed`

```java
public static AgentExecutionException parsingFailed(
      @NonNull String agentName, int turnsCompleted, @NonNull Throwable cause)
```

Creates an exception for parsing failure.

**Parameters**

| Name | Description |
|------|-------------|
| `agentName` | the agent name |
| `turnsCompleted` | turns completed before failure |
| `cause` | the underlying cause |

**Returns**

a new AgentExecutionException

---

### `handoffFailed`

```java
public static AgentExecutionException handoffFailed(
      @NonNull String agentName,
      @NonNull String targetAgentName,
      int turnsCompleted,
      @NonNull Throwable cause)
```

Creates an exception for handoff failure.

**Parameters**

| Name | Description |
|------|-------------|
| `agentName` | the agent name |
| `targetAgentName` | the target agent name |
| `turnsCompleted` | turns completed before failure |
| `cause` | the underlying cause |

**Returns**

a new AgentExecutionException

---

### `agentName`

```java
public @NonNull String agentName()
```

Returns the name of the agent that failed.

**Returns**

the agent name

---

### `phase`

```java
public @NonNull Phase phase()
```

Returns the phase where failure occurred.

**Returns**

the failure phase

---

### `turnsCompleted`

```java
public int turnsCompleted()
```

Returns the number of turns completed before failure.

**Returns**

the turns completed

---

### `lastResponseId`

```java
public @Nullable String lastResponseId()
```

Returns the last response ID before failure.

**Returns**

the last response ID, or null if not available

