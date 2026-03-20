# :material-code-braces: ProgressLogTool

`com.paragon.harness.tools.ProgressLogTool` &nbsp;·&nbsp; **Class**

---

Exposes a `ProgressLog` as two `FunctionTool`s: one for reading and one for appending.

Usage:

```java
ProgressLog log = ProgressLog.create();
Agent agent = Agent.builder()
    .addTools(ProgressLogTool.all(log).toArray(new FunctionTool[0]))
    .build();
```

*Since: 1.0*

## Methods

### `all`

```java
public static @NonNull List<FunctionTool<?>> all(@NonNull ProgressLog log)
```

Creates all progress log tools (read + append).

**Parameters**

| Name | Description |
|------|-------------|
| `log` | the progress log to expose |

**Returns**

list of tools

