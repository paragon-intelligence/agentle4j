# :material-code-braces: Handoff

`com.paragon.agents.Handoff` &nbsp;·&nbsp; **Class**

---

A handoff defines when and to which agent control should be transferred.

Handoffs enable multi-agent collaboration by allowing one agent to delegate to another when a
task falls outside its expertise. Handoffs are exposed to the model as tools - when the model
calls a handoff tool, control is automatically transferred to the target agent.

### Usage Example

```java
Agent supportAgent = Agent.builder()
    .name("CustomerSupport")
    .instructions("Handle customer support issues")
    .build();
Agent salesAgent = Agent.builder()
    .name("Sales")
    .instructions("Handle sales inquiries")
    .addHandoff(Handoff.to(supportAgent)
        .withDescription("Transfer to support for technical issues"))
    .build();
// When salesAgent.interact() detects a handoff tool call,
// it automatically invokes supportAgent.interact()
```

**See Also**

- `Agent`

*Since: 1.0*

## Fields

### `awarenessMessage`

```java
private final @Nullable String awarenessMessage
```

null = use default message; empty string = disabled

---

### `awarenessMessage`

```java
private @Nullable String awarenessMessage = null
```

null = default; empty = disabled

## Methods

### `to`

```java
public static @NonNull Builder to(@NonNull Agent targetAgent)
```

Creates a handoff builder targeting the specified agent.

The handoff name defaults to "transfer_to_[agent_name]" and description defaults to the
agent's instructions.

**Parameters**

| Name | Description |
|------|-------------|
| `targetAgent` | the agent to transfer control to |

**Returns**

a builder for configuring the handoff

---

### `name`

```java
public @NonNull String name()
```

Returns the name of this handoff (used as the tool name).

**Returns**

the handoff name

---

### `description`

```java
public @NonNull String description()
```

Returns the description explaining when to use this handoff.

**Returns**

the handoff description

---

### `targetAgent`

```java
public @NonNull Agent targetAgent()
```

Returns the target agent that will receive control.

**Returns**

the target agent

---

### `buildAwarenessMessage`

```java
public @Nullable String buildAwarenessMessage(@NonNull String parentAgentName)
```

Builds the awareness message to inject into the child agent's context.

Returns `null` if awareness is disabled via `Builder.withoutAwarenessMessage()`.

**Parameters**

| Name | Description |
|------|-------------|
| `parentAgentName` | the name of the agent transferring control |

**Returns**

the message to inject, or `null` if disabled

---

### `asTool`

```java
public @NonNull FunctionTool<HandoffParams> asTool()
```

Converts this handoff to a FunctionTool that can be passed to the LLM.

**Returns**

a FunctionTool representing this handoff

---

### `withName`

```java
public @NonNull Builder withName(@NonNull String name)
```

Sets a custom name for the handoff tool.

Defaults to "transfer_to_[agent_name]".

**Parameters**

| Name | Description |
|------|-------------|
| `name` | the handoff name |

**Returns**

this builder

---

### `withDescription`

```java
public @NonNull Builder withDescription(@NonNull String description)
```

Sets a custom description explaining when to use this handoff.

Defaults to the target agent's instructions.

**Parameters**

| Name | Description |
|------|-------------|
| `description` | the handoff description |

**Returns**

this builder

---

### `withAwarenessMessage`

```java
public @NonNull Builder withAwarenessMessage(@NonNull String message)
```

Sets a custom awareness message to inject into the child agent's context on handoff.

**Parameters**

| Name | Description |
|------|-------------|
| `message` | the message to inject as a developer-priority message |

**Returns**

this builder

---

### `withoutAwarenessMessage`

```java
public @NonNull Builder withoutAwarenessMessage()
```

Disables the automatic awareness message injection entirely.

**Returns**

this builder

---

### `build`

```java
public @NonNull Handoff build()
```

Builds the Handoff instance.

**Returns**

the configured handoff

