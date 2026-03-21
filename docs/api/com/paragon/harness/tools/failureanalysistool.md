# :material-code-braces: FailureAnalysisTool

> This docs was updated at: 2026-03-21

`com.paragon.harness.tools.FailureAnalysisTool` &nbsp;·&nbsp; **Class**

Extends `FunctionTool<FailureAnalysisTool.AnalysisRequest>`

---

A `FunctionTool` that reads a batch of `com.paragon.harness.AgentRunReport`
files and returns a structured failure summary for analysis.

Feed this tool to a meta-agent to automate harness improvement recommendations:

```java
RunReportExporter exporter = RunReportExporter.create(Path.of("./reports"));
Agent metaAgent = Agent.builder()
    .name("HarnessImprover")
    .instructions("""
        You are a harness improvement agent. Use the analyze_failures tool to read
        recent agent run reports and suggest concrete improvements to the harness
        configuration (guardrails, retry limits, tool timeouts, etc.).
        """)
    .addTool(new FailureAnalysisTool(exporter))
    .build();
metaAgent.interact("Analyze recent failures and suggest improvements");
```

*Since: 1.0*

## Methods

### `FailureAnalysisTool`

```java
public FailureAnalysisTool(@NonNull RunReportExporter exporter)
```

Creates a FailureAnalysisTool backed by the given exporter.

**Parameters**

| Name | Description |
|------|-------------|
| `exporter` | the exporter to read reports from |

