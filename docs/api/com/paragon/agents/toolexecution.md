# :material-database: ToolExecution

`com.paragon.agents.ToolExecution` &nbsp;·&nbsp; **Record**

---

Records the execution details of a single tool call during an agent run.

ToolExecution captures metadata about each tool invocation, which is useful for:

  
- Debugging and logging agent behavior
- Monitoring tool performance
- Auditing tool usage

**See Also**

- `AgentResult`

*Since: 1.0*

## Methods

### `isSuccess`

```java
public boolean isSuccess()
```

Returns true if the tool execution succeeded.

**Returns**

true if output status is COMPLETED

