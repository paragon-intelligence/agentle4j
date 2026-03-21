# :material-code-braces: ToolPlanException

> This docs was updated at: 2026-03-21

`com.paragon.agents.toolplan.ToolPlanException` &nbsp;·&nbsp; **Class**

Extends `RuntimeException`

---

Exception thrown when a tool plan fails validation or execution.

Contains an optional `.stepId()` identifying which step caused the failure.

## Methods

### `stepId`

```java
public @Nullable String stepId()
```

Returns the step ID that caused this exception, or null if not step-specific.
