# :material-database: AgentDefinition

`com.paragon.agents.AgentDefinition` &nbsp;·&nbsp; **Record**

---

A richly annotated agent definition record designed for **LLM structured output**.

Every field carries a `JsonPropertyDescription` that produces a JSON Schema description.
When used as the output type of `interactStructured()`, the LLM sees these descriptions
and knows exactly what each field means.

This record contains **only behavioral fields** — things the LLM can reason about.
Infrastructure concerns (model, API provider, API keys, HTTP config) are provided externally
via `String)` or
`String, List)`.

### Meta-Agent Pattern

```java
// A meta-agent that creates other agents
Interactable.Structured metaAgent = Agent.builder()
    .name("AgentFactory")
    .model("openai/gpt-4o")
    .instructions("""
        You create agent definitions. Available tools you can assign:
        - "search_kb": Searches the company knowledge base
        - "create_ticket": Creates a support ticket
        Available guardrails:
        - "profanity_filter": blocks profanity
        - "max_length": limits input to 10k chars
        """)
    .structured(AgentDefinition.class)
    .responder(responder)
    .build();
AgentDefinition def = metaAgent.interactStructured(
    "Create a customer support agent that speaks Spanish"
).output();
// You provide infrastructure: responder, model, and available tools
Interactable agent = def.toInteractable(responder, "openai/gpt-4o", availableTools);
```

**See Also**

- `InteractableBlueprint.AgentBlueprint`
- `Interactable.Structured`

*Since: 1.0*

## Methods

### `toContextBlueprint`

```java
public ContextBlueprint toContextBlueprint()
```

Converts to the blueprint's `ContextBlueprint`.

---

### `toInteractable`

```java
public @NonNull Interactable toInteractable(
      @NonNull Responder responder,
      @NonNull String model,
      @NonNull List<FunctionTool<?>> availableTools)
```

Reconstructs a fully functional `Interactable` agent.

The caller provides all infrastructure: the Responder (API client) and the model to use.
Tool names from the definition are matched against the provided available tools by
comparing each tool's `name()` (from `@FunctionMetadata`).

```java
List> tools = List.of(searchTool, ticketTool, refundTool);
Interactable agent = definition.toInteractable(responder, "openai/gpt-4o", tools);
```

**Parameters**

| Name | Description |
|------|-------------|
| `responder` | the Responder for LLM API calls |
| `model` | the LLM model identifier (e.g., "openai/gpt-4o") |
| `availableTools` | all tools the agent may use; only those matching `.toolNames()` are attached |

**Returns**

a fully functional Agent

---

### `toInteractable`

```java
public @NonNull Interactable toInteractable(
      @NonNull Responder responder, @NonNull String model)
```

Reconstructs a fully functional `Interactable` agent without tools.

Convenience overload for agents that don't use tools. Any tool names in the definition
are ignored.

**Parameters**

| Name | Description |
|------|-------------|
| `responder` | the Responder for LLM API calls |
| `model` | the LLM model identifier |

**Returns**

a fully functional Agent

---

### `toBlueprint`

```java
public @NonNull AgentBlueprint toBlueprint(
      @NonNull ResponderBlueprint responderBlueprint,
      @NonNull String model,
      @NonNull List<FunctionTool<?>> availableTools)
```

Converts this definition to an `AgentBlueprint`, bridging to the blueprint
serialization system.

Requires a `ResponderBlueprint` and a model because blueprints are self-contained.

**Parameters**

| Name | Description |
|------|-------------|
| `responderBlueprint` | the responder configuration |
| `model` | the LLM model identifier |
| `availableTools` | tools to resolve tool names to class names |

**Returns**

an AgentBlueprint equivalent

---

### `fromBlueprint`

```java
public static @NonNull AgentDefinition fromBlueprint(
      @NonNull AgentBlueprint blueprint, @NonNull List<FunctionTool<?>> availableTools)
```

Creates an `AgentDefinition` from an existing `AgentBlueprint`.

Infrastructure fields (model, responder) are stripped; behavioral fields are preserved.
Tool class names are reverse-looked-up against the provided tools to recover human-readable
names. If a tool class name can't be resolved, it is skipped.

**Parameters**

| Name | Description |
|------|-------------|
| `blueprint` | the source blueprint |
| `availableTools` | tools for reverse name lookup |

**Returns**

an AgentDefinition with only behavioral fields

---

### `fromBlueprint`

```java
public static @NonNull AgentDefinition fromBlueprint(@NonNull AgentBlueprint blueprint)
```

Creates an `AgentDefinition` from an existing `AgentBlueprint` without tool
name resolution.

**Parameters**

| Name | Description |
|------|-------------|
| `blueprint` | the source blueprint |

**Returns**

an AgentDefinition (tool names will be empty since they can't be resolved)

