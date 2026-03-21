# :material-at: FunctionMetadata

> This docs was updated at: 2026-03-21

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

---

### `stopsLoop`

```java
boolean stopsLoop() default false
```

Whether this tool is client-side only and terminates the agentic loop immediately when called.

When true, the framework will:

  
- Detect the tool call in the output
- Skip persisting the call to conversation history
- Skip executing the tool's `call()` method
- Return `AgentResult.clientSideTool()` as a clean, non-error exit

This mirrors the `ask_user_input_v0` pattern used by Claude.ai, where a tool call
acts as a UI signal to the frontend rather than server-side logic.

Example:
{@code
