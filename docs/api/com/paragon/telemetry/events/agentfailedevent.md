# :material-database: AgentFailedEvent

> This docs was updated at: 2026-02-23

`com.paragon.telemetry.events.AgentFailedEvent` &nbsp;·&nbsp; **Record**

---

Telemetry event emitted when an agent execution fails.

Contains context about the failure including agent name, phase, error details, and trace
correlation.

## Methods

### `from`

```java
public static AgentFailedEvent from(
      @NonNull AgentExecutionException exception,
      @NonNull String sessionId,
      @Nullable String traceId,
      @Nullable String spanId,
      @Nullable String parentSpanId)
```

Creates an AgentFailedEvent from an AgentExecutionException.

**Parameters**

| Name | Description |
|------|-------------|
| `exception` | the exception |
| `sessionId` | the session ID |
| `traceId` | the trace ID |
| `spanId` | the span ID |
| `parentSpanId` | the parent span ID |

**Returns**

a new AgentFailedEvent

---

### `from`

```java
public static AgentFailedEvent from(
      @NonNull String agentName,
      int turnsCompleted,
      @NonNull Throwable exception,
      @NonNull String sessionId,
      @Nullable String traceId,
      @Nullable String spanId,
      @Nullable String parentSpanId)
```

Creates an AgentFailedEvent from a generic exception.

**Parameters**

| Name | Description |
|------|-------------|
| `agentName` | the agent name |
| `turnsCompleted` | turns completed before failure |
| `exception` | the exception |
| `sessionId` | the session ID |
| `traceId` | the trace ID |
| `spanId` | the span ID |
| `parentSpanId` | the parent span ID |

**Returns**

a new AgentFailedEvent

