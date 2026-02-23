# :material-code-braces: SupervisorAgent

> This docs was updated at: 2026-02-23

`com.paragon.agents.SupervisorAgent` &nbsp;·&nbsp; **Class**

Implements `Interactable`

---

Implements the Supervisor pattern: a central agent that coordinates multiple worker agents.

Unlike `ParallelAgents` which runs agents concurrently without coordination, or `Handoff` which transfers control permanently, SupervisorAgent maintains central control and can
delegate tasks to workers, receive their outputs, and make decisions about next steps.

The supervisor uses its instructions to:

  
- Decompose complex tasks into subtasks
- Delegate subtasks to appropriate workers (as tools)
- Aggregate and synthesize worker outputs
- Make decisions about task completion

**Virtual Thread Design:** Uses synchronous API optimized for Java 21+ virtual threads.
Blocking calls are cheap and efficient with virtual threads.

### Usage Example

```java
Agent researcher = Agent.builder().name("Researcher")...build();
Agent writer = Agent.builder().name("Writer")...build();
SupervisorAgent supervisor = SupervisorAgent.builder()
    .model("openai/gpt-4o")
    .responder(responder)
    .instructions("Coordinate workers to research and write reports")
    .addWorker(researcher, "research and gather facts")
    .addWorker(writer, "write and format content")
    .build();
// Supervisor orchestrates workers to complete the task
AgentResult result = supervisor.orchestrate("Write a report on AI trends");
```

**See Also**

- `ParallelAgents`
- `Handoff`
- `SubAgentTool`

*Since: 1.0*

## Methods

### `builder`

```java
public static @NonNull Builder builder()
```

Creates a new SupervisorAgent builder.

---

### `name`

```java
public @NonNull String name()
```

Returns the supervisor's name.

**Returns**

the name

---

### `workers`

```java
public @NonNull List<Worker> workers()
```

Returns the workers managed by this supervisor.

**Returns**

unmodifiable list of workers

---

### `underlyingAgent`

```java
Agent underlyingAgent()
```

Returns the underlying supervisor agent for advanced usage.

---

### `name`

```java
public @NonNull Builder name(@NonNull String name)
```

Sets the supervisor name.

**Parameters**

| Name | Description |
|------|-------------|
| `name` | the name |

**Returns**

this builder

---

### `model`

```java
public @NonNull Builder model(@NonNull String model)
```

Sets the model for the supervisor agent.

**Parameters**

| Name | Description |
|------|-------------|
| `model` | the model identifier |

**Returns**

this builder

---

### `instructions`

```java
public @NonNull Builder instructions(@NonNull String instructions)
```

Sets the supervisor's instructions for coordinating workers.

**Parameters**

| Name | Description |
|------|-------------|
| `instructions` | the instructions |

**Returns**

this builder

---

### `responder`

```java
public @NonNull Builder responder(@NonNull Responder responder)
```

Sets the responder for API calls.

**Parameters**

| Name | Description |
|------|-------------|
| `responder` | the responder |

**Returns**

this builder

---

### `addWorker`

```java
public @NonNull Builder addWorker(@NonNull Interactable worker, @NonNull String description)
```

Adds a worker with a description of its capabilities.

Workers can be any Interactable: Agent, RouterAgent, ParallelAgents, etc.

**Parameters**

| Name | Description |
|------|-------------|
| `worker` | the worker |
| `description` | when to use this worker |

**Returns**

this builder

---

### `maxTurns`

```java
public @NonNull Builder maxTurns(int maxTurns)
```

Sets the maximum number of turns for the supervisor.

**Parameters**

| Name | Description |
|------|-------------|
| `maxTurns` | the max turns |

**Returns**

this builder

---

### `addSkill`

```java
public @NonNull Builder addSkill(@NonNull Skill skill)
```

Adds a skill that augments the supervisor's capabilities.

**Parameters**

| Name | Description |
|------|-------------|
| `skill` | the skill to add |

**Returns**

this builder

**See Also**

- `Skill`

---

### `addSkillFrom`

```java
public @NonNull Builder addSkillFrom(@NonNull SkillProvider provider, @NonNull String skillId)
```

Loads and adds a skill from a provider.

**Parameters**

| Name | Description |
|------|-------------|
| `provider` | the skill provider |
| `skillId` | the skill identifier |

**Returns**

this builder

---

### `skillStore`

```java
public @NonNull Builder skillStore(@NonNull SkillStore store)
```

Registers all skills from a SkillStore.

**Parameters**

| Name | Description |
|------|-------------|
| `store` | the skill store containing skills to add |

**Returns**

this builder

---

### `traceMetadata`

```java
public @NonNull Builder traceMetadata(@Nullable TraceMetadata trace)
```

Sets the trace metadata for API requests (optional).

**Parameters**

| Name | Description |
|------|-------------|
| `trace` | the trace metadata |

**Returns**

this builder

---

### `structured`

```java
public <T> @NonNull StructuredBuilder<T> structured(@NonNull Class<T> outputType)
```

Configures this supervisor to produce structured output of the specified type.

Returns a `StructuredBuilder` that builds a `SupervisorAgent.Structured`
instead of a regular SupervisorAgent.

Example:

```java
var supervisor = SupervisorAgent.builder()
    .model("openai/gpt-4o")
    .responder(responder)
    .instructions("Coordinate workers to produce a report")
    .addWorker(researcher, "research facts")
    .structured(Report.class)
    .build();
StructuredAgentResult result = supervisor.interactStructured("AI trends");
Report report = result.output();
```

**Parameters**

| Name | Description |
|------|-------------|
| `<T>` | the output type |
| `outputType` | the class of the structured output |

**Returns**

a structured builder

---

### `build`

```java
public @NonNull SupervisorAgent build()
```

Builds the SupervisorAgent.

**Returns**

the configured supervisor

---

### `build`

```java
public SupervisorAgent.Structured<T> build()
```

Builds the type-safe structured supervisor agent.

**Returns**

the configured Structured supervisor

---

### `outputType`

```java
public @NonNull Class<T> outputType()
```

Returns the structured output type.
