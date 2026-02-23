# :material-code-braces: ComputerToolCall

> This docs was updated at: 2026-02-23

`com.paragon.responses.spec.ComputerToolCall` &nbsp;·&nbsp; **Class**

Extends `ToolCall` &nbsp;·&nbsp; Implements `Item`, `ResponseOutput`

---

A tool call to a computer use tool. See the computer use guide for more
information.

## Methods

### `ComputerToolCall`

```java
public ComputerToolCall(
      @NonNull ComputerUseAction action,
      @NonNull String id,
      @NonNull String callId,
      @NonNull List<PendingSafetyCheck> pendingSafetyChecks,
      @NonNull ComputerToolCallStatus status)
```

@param action action to perform in the computer being used

**Parameters**

| Name | Description |
|------|-------------|
| `callId` | An identifier used when responding to the tool call with output. |
| `pendingSafetyChecks` | The pending safety checks for the computer call. |
| `status` | The status of the item. One of in_progress, completed, or incomplete. Populated when items are returned via API. |

