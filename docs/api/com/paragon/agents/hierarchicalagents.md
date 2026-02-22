# :material-code-braces: HierarchicalAgents

`com.paragon.agents.HierarchicalAgents` &nbsp;·&nbsp; **Class**

Implements `Interactable`

---

Implements the Hierarchical pattern: multi-layered supervisor structure.

Expands upon the supervisor concept to create a multi-layered organizational structure with
multiple levels of supervisors. Higher-level supervisors oversee lower-level ones, and
ultimately, operational agents at the lowest tier.

Key characteristics:

  
- Tree structure with executive at root
- Managers delegate to their team workers
- Escalation when workers cannot complete tasks
- Distributed decision-making within defined boundaries

**Virtual Thread Design:** Uses synchronous API optimized for Java 21+ virtual threads.
Blocking calls are cheap and efficient with virtual threads.

### Usage Example

```java
// Create worker agents
Agent devAgent = Agent.builder().name("Developer")...build();
Agent qaAgent = Agent.builder().name("QA")...build();
Agent salesRep = Agent.builder().name("SalesRep")...build();
// Create managers
Agent techManager = Agent.builder().name("TechManager")...build();
Agent salesManager = Agent.builder().name("SalesManager")...build();
// Create executive
Agent executive = Agent.builder().name("CEO")...build();
HierarchicalAgents hierarchy = HierarchicalAgents.builder()
    .executive(executive)
    .addDepartment("Engineering", techManager, devAgent, qaAgent)
    .addDepartment("Sales", salesManager, salesRep)
    .build();
// Executive delegates through managers to workers
AgentResult result = hierarchy.execute("Launch new product feature");
```

*Since: 1.0*

## Methods

### `builder`

```java
public static @NonNull Builder builder()
```

Creates a new HierarchicalAgents builder.

---

### `executive`

```java
public @NonNull Agent executive()
```

Returns the executive agent at the top of the hierarchy.

**Returns**

the executive agent

---

### `name`

```java
public @NonNull String name()
```

{@inheritDoc}

---

### `departments`

```java
public @NonNull Map<String, Department> departments()
```

Returns all departments in this hierarchy.

**Returns**

unmodifiable map of department names to departments

---

### `sendToDepartment`

```java
public @NonNull AgentResult sendToDepartment(
      @NonNull String departmentName, @NonNull String task)
```

Sends a task directly to a specific department.

Bypasses the executive and sends the task directly to the department manager.

**Parameters**

| Name | Description |
|------|-------------|
| `departmentName` | the department name |
| `task` | the task description |

**Returns**

the department result

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalArgumentException` | if department doesn't exist |

---

### `Department`

```java
public Department(@NonNull Agent manager, @NonNull List<Interactable> workers)
```

Creates a department (supervisor is set internally).

---

### `executive`

```java
public @NonNull Builder executive(@NonNull Agent executive)
```

Sets the executive agent at the top of the hierarchy.

**Parameters**

| Name | Description |
|------|-------------|
| `executive` | the executive agent |

**Returns**

this builder

---

### `addDepartment`

```java
public @NonNull Builder addDepartment(
        @NonNull String name, @NonNull Agent manager, @NonNull Interactable... workers)
```

Adds a department with a manager and workers.

Workers can be any Interactable: Agent, RouterAgent, ParallelAgents, etc.

**Parameters**

| Name | Description |
|------|-------------|
| `name` | the department name |
| `manager` | the department manager (must be an Agent for responder/model access) |
| `workers` | the workers in this department |

**Returns**

this builder

---

### `addDepartment`

```java
public @NonNull Builder addDepartment(
        @NonNull String name, @NonNull Agent manager, @NonNull List<Interactable> workers)
```

Adds a department with a manager and worker list.

**Parameters**

| Name | Description |
|------|-------------|
| `name` | the department name |
| `manager` | the department manager (must be an Agent for responder/model access) |
| `workers` | the workers in this department |

**Returns**

this builder

---

### `maxTurns`

```java
public @NonNull Builder maxTurns(int maxTurns)
```

Sets the maximum turns for each level of the hierarchy.

**Parameters**

| Name | Description |
|------|-------------|
| `maxTurns` | the max turns |

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

Configures this hierarchy to produce structured output of the specified type.

Returns a `StructuredBuilder` that builds a `HierarchicalAgents.Structured`
instead of a regular HierarchicalAgents.

The executive's final output is parsed as the specified type.

Example:

```java
var hierarchy = HierarchicalAgents.builder()
    .executive(executive)
    .addDepartment("Engineering", techManager, devAgent)
    .structured(ProjectPlan.class)
    .build();
StructuredAgentResult result = hierarchy.interactStructured("Plan sprint");
ProjectPlan plan = result.output();
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
public @NonNull HierarchicalAgents build()
```

Builds the HierarchicalAgents.

**Returns**

the configured hierarchy

---

### `build`

```java
public HierarchicalAgents.Structured<T> build()
```

Builds the type-safe structured hierarchy.

**Returns**

the configured Structured hierarchy

---

### `outputType`

```java
public @NonNull Class<T> outputType()
```

Returns the structured output type.

---

### `executive`

```java
public @NonNull Agent executive()
```

Returns the executive agent at the top of the hierarchy.

---

### `departments`

```java
public @NonNull Map<String, Department> departments()
```

Returns all departments in this hierarchy.
