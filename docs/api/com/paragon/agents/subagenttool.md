# :material-code-braces: SubAgentTool

> This docs was updated at: 2026-02-23

`com.paragon.agents.SubAgentTool` &nbsp;·&nbsp; **Class**

Extends `FunctionTool<SubAgentTool.SubAgentParams>`

---

Wraps an Agent as a FunctionTool, enabling agent composition.

Unlike handoffs which transfer control permanently, sub-agents are invoked like tools: the
parent agent calls the sub-agent, receives its output, and continues processing.

This is a thin wrapper around `InteractableSubAgentTool` that provides a typed `.targetAgent()` accessor for backward compatibility.

### Usage Example

```java
Agent dataAnalyst = Agent.builder()
    .name("DataAnalyst")
    .instructions("You analyze data and return insights.")
    .responder(responder)
    .build();
Agent orchestrator = Agent.builder()
    .name("Orchestrator")
    .instructions("Use the data analyst when you need deep analysis.")
    .addSubAgent(dataAnalyst, "For data analysis and statistical insights")
    .responder(responder)
    .build();
```

### Context Sharing

By default, sub-agents inherit the parent's custom state (userId, sessionId, etc.) but start
with a fresh conversation history. Use `Config.shareHistory()` to include the full
conversation context.

**See Also**

- `Agent.Builder#addSubAgent(Agent, String)`
- `InteractableSubAgentTool`
- `Config`

*Since: 1.0*

## Methods

### `SubAgentTool`

```java
public SubAgentTool(@NonNull Agent targetAgent, @NonNull String description)
```

Creates a SubAgentTool with default configuration.

**Parameters**

| Name | Description |
|------|-------------|
| `targetAgent` | the agent to invoke as a tool |
| `description` | description of when to use this sub-agent |

---

### `SubAgentTool`

```java
public SubAgentTool(@NonNull Agent targetAgent, @NonNull Config config)
```

Creates a SubAgentTool with the specified configuration.

**Parameters**

| Name | Description |
|------|-------------|
| `targetAgent` | the agent to invoke as a tool |
| `config` | configuration for context sharing and description |

---

### `targetAgent`

```java
public @NonNull Agent targetAgent()
```

Returns the target agent.

**Returns**

the wrapped agent

---

### `sharesState`

```java
public boolean sharesState()
```

Returns whether state is shared with the sub-agent.

**Returns**

true if state is shared

---

### `sharesHistory`

```java
public boolean sharesHistory()
```

Returns whether history is shared with the sub-agent.

**Returns**

true if history is shared

