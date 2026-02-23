# :material-at: FunctionMetadata

> This docs was updated at: 2026-02-23

`com.paragon.responses.annotations.FunctionMetadata` &nbsp;·&nbsp; **Annotation**

---

Annotation to define function tool metadata. Apply this to FunctionTool subclasses to specify
name, description, and confirmation requirements.

## Annotation Elements

### `name`

```java
String name()
```

The name of the function tool.

---

### `description`

```java
String description() default ""
```

The description of the function tool.

---

### `requiresConfirmation`

```java
boolean requiresConfirmation() default false
```

Whether this tool requires human confirmation before execution.

When true, the tool will trigger the `onToolCallPending` or `onPause` callback
in AgentStream, allowing human-in-the-loop approval workflows.

When false (default), the tool executes automatically without confirmation.

Example:
{@code
