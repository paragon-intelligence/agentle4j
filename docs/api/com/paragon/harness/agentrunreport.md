# :material-code-braces: AgentRunReport

> This docs was updated at: 2026-03-21

`com.paragon.harness.AgentRunReport` &nbsp;·&nbsp; **Class**

---

Enriched summary of an `AgentResult` annotated with timing, retry counts,
tool error statistics, and guardrail trigger counts.

Reports can be written to disk via `RunReportExporter` and later fed to a
meta-agent for harness improvement analysis.

*Since: 1.0*

## Methods

### `from`

```java
public static @NonNull AgentRunReport from(
      @NonNull String agentName,
      @NonNull AgentResult result,
      @NonNull Instant startedAt,
      @NonNull Instant completedAt,
      int retryCount,
      int guardrailTriggerCount)
```

Creates an AgentRunReport from an AgentResult and timing information.

**Parameters**

| Name | Description |
|------|-------------|
| `agentName` | the agent name |
| `result` | the agent result to report on |
| `startedAt` | when the run started |
| `completedAt` | when the run completed |
| `retryCount` | how many self-correction retries occurred |
| `guardrailTriggerCount` | how many guardrails fired |

**Returns**

a new report

---

### `toSummary`

```java
public @NonNull String toSummary()
```

Returns a human-readable summary of this report.
