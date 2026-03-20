# :material-code-braces: InteractableSubAgentTool

`com.paragon.agents.InteractableSubAgentTool` &nbsp;·&nbsp; **Class**

Extends `FunctionTool<InteractableSubAgentTool.InteractableParams>`

---

Wraps any Interactable as a FunctionTool, enabling composition of multi-agent patterns.

This is the primary tool for agent composition. It supports any Interactable implementation
including Agent, RouterAgent, ParallelAgents, AgentNetwork, and SupervisorAgent.

### Usage Example

```java
// Create a router that handles different domains
RouterAgent router = RouterAgent.builder()
    .addRoute(billingAgent, "billing questions")
    .addRoute(techAgent, "technical support")
    .build();
// Use the router as a tool in a supervisor
InteractableSubAgentTool tool = new InteractableSubAgentTool(router, "Route to specialists");
```

**See Also**

- `SubAgentTool`
- `Interactable`

*Since: 1.0*

## Methods

### `InteractableSubAgentTool`

```java
public InteractableSubAgentTool(@NonNull Interactable target, @NonNull String description)
```

Creates an InteractableSubAgentTool with a description.

**Parameters**

| Name | Description |
|------|-------------|
| `target` | the interactable to invoke as a tool |
| `description` | description of when to use this interactable |

---

### `InteractableSubAgentTool`

```java
public InteractableSubAgentTool(@NonNull Interactable target, @NonNull Config config)
```

Creates an InteractableSubAgentTool with the specified configuration.

**Parameters**

| Name | Description |
|------|-------------|
| `target` | the interactable to invoke as a tool |
| `config` | configuration for context sharing and description |

---

### `target`

```java
public @NonNull Interactable target()
```

Returns the target interactable.

**Returns**

the wrapped interactable

---

### `sharesState`

```java
public boolean sharesState()
```

Returns whether state is shared.

**Returns**

true if state is shared

---

### `sharesHistory`

```java
public boolean sharesHistory()
```

Returns whether history is shared.

**Returns**

true if history is shared

---

### `builder`

```java
public static @NonNull Builder builder()
```

Creates a new Config builder.

---

### `description`

```java
public @Nullable String description()
```

Returns the description.

---

### `shareState`

```java
public boolean shareState()
```

Returns whether state is shared.

---

### `shareHistory`

```java
public boolean shareHistory()
```

Returns whether history is shared.
