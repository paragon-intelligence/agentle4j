# :material-code-braces: RunReportExporter

`com.paragon.harness.RunReportExporter` &nbsp;·&nbsp; **Class**

---

Writes `AgentRunReport` instances to a filesystem directory for later analysis.

Reports are written as pretty-printed JSON files named
`{agentName`_{reportId}.json}. They can be read back via `.loadAll()` and
fed to a meta-agent via `com.paragon.harness.tools.FailureAnalysisTool`.

Example:

```java
RunReportExporter exporter = RunReportExporter.create(Path.of("./reports"));
AgentRunReport report = AgentRunReport.from(agentName, result, startedAt, Instant.now(), 0, 0);
exporter.export(report);
```

*Since: 1.0*

## Methods

### `create`

```java
public static @NonNull RunReportExporter create(@NonNull Path reportDir)
```

Creates a RunReportExporter that writes JSON reports to the given directory.

**Parameters**

| Name | Description |
|------|-------------|
| `reportDir` | the directory to write reports to |

**Returns**

a new exporter

---

### `export`

```java
public @NonNull Path export(@NonNull AgentRunReport report)
```

Exports a report to disk.

**Parameters**

| Name | Description |
|------|-------------|
| `report` | the report to export |

**Returns**

the path of the written file

---

### `loadAll`

```java
public @NonNull List<String> loadAll()
```

Loads all reports from the report directory.

**Returns**

list of report summaries (as raw JSON strings)

---

### `count`

```java
public int count()
```

Returns the number of reports stored in the report directory.

**Returns**

report count

