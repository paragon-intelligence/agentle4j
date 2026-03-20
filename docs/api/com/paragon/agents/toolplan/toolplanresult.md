# :material-database: ToolPlanResult

`com.paragon.agents.toolplan.ToolPlanResult` &nbsp;·&nbsp; **Record**

---

The result of executing a `ToolPlan`.

## Methods

### `toOutputSummary`

```java
public @NonNull String toOutputSummary()
```

Returns a formatted JSON summary of the output step results, suitable for returning to the LLM
context.

**Returns**

JSON string with step IDs mapped to their outputs

