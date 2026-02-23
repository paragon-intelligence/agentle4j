# :material-code-braces: RouterAgent

> This docs was updated at: 2026-02-23

`com.paragon.agents.RouterAgent` &nbsp;·&nbsp; **Class**

Implements `Interactable`

---

A specialized agent for routing inputs to appropriate target agents.

Unlike general agents with complex instructions, RouterAgent focuses purely on classification
and routing, avoiding the noise of full agent instructions.

**Virtual Thread Design:** Uses synchronous API optimized for Java 21+ virtual threads.
Blocking calls are cheap and efficient with virtual threads.

### Usage Example

```java
Agent billingAgent = Agent.builder().name("Billing")...build();
Agent techSupport = Agent.builder().name("TechSupport")...build();
Agent salesAgent = Agent.builder().name("Sales")...build();
RouterAgent router = RouterAgent.builder()
    .model("openai/gpt-4o")
    .responder(responder)
    .addRoute(billingAgent, "billing, invoices, payments, charges")
    .addRoute(techSupport, "technical issues, bugs, errors, not working")
    .addRoute(salesAgent, "pricing, new features, demos, upgrades")
    .build();
// Route and execute - blocking, uses virtual threads
AgentResult result = router.route("I have a question about my invoice");
System.out.println("Handled by: " + result.handoffTarget().name());
// Or just classify without executing
Optional agent = router.classify("My app keeps crashing");
System.out.println("Would route to: " + agent.map(Agent::name).orElse("none"));
// Streaming support
router.routeStream("Help me with billing")
    .onTextDelta(System.out::print)
    .onComplete(result -> System.out.println("\nDone!"))
    .start();
```

*Since: 1.0*

## Methods

### `builder`

```java
public static @NonNull Builder builder()
```

Creates a new RouterAgent builder.

**Returns**

a new builder

---

### `name`

```java
public @NonNull String name()
```

{@inheritDoc}

---

### `classify`

```java
/**
   * Classifies the input and returns the selected route target without executing.
   *
   * <p>Useful when you need to know which target would handle the input before committing.
   *
   * @param input the user input to classify
   * @return Optional containing the selected Interactable, or empty if no match and no fallback
   */
  public @NonNull Optional<Interactable> classify(@NonNull String input)
```

Routes the input to the most appropriate agent and executes it.

**Parameters**

| Name | Description |
|------|-------------|
| `input` | the user input to route |

**Returns**

the result from the selected agent

---

### `classify`

```java
public @NonNull Optional<Interactable> classify(@NonNull String input)
```

Classifies the input and returns the selected route target without executing.

Useful when you need to know which target would handle the input before committing.

**Parameters**

| Name | Description |
|------|-------------|
| `input` | the user input to classify |

**Returns**

Optional containing the selected Interactable, or empty if no match and no fallback

---

### `classify`

```java
public @NonNull Optional<Interactable> classify(@NonNull Prompt prompt)
```

Classifies the Prompt and returns the selected route target without executing.

The prompt's text content is extracted and used as the input.

**Parameters**

| Name | Description |
|------|-------------|
| `prompt` | the prompt to classify |

**Returns**

Optional containing the selected Interactable, or empty if no match and no fallback

---

### `routes`

```java
public @NonNull List<Route> routes()
```

Returns the routes configured for this router.

**Returns**

unmodifiable list of routes

---

### `routeStream`

```java
public @NonNull RouterStream routeStream(@NonNull AgenticContext context)
```

Creates a RouterStream for streaming route execution with callback support.

**Parameters**

| Name | Description |
|------|-------------|
| `context` | the conversation context |

**Returns**

a RouterStream for processing streaming events

---

### `name`

```java
public @NonNull Builder name(@NonNull String name)
```

Sets the name for this router.

**Parameters**

| Name | Description |
|------|-------------|
| `name` | the router name |

**Returns**

this builder

---

### `model`

```java
public @NonNull Builder model(@NonNull String model)
```

Sets the model for classification.

**Parameters**

| Name | Description |
|------|-------------|
| `model` | the model identifier (e.g., "openai/gpt-4o-mini") |

**Returns**

this builder

---

### `responder`

```java
public @NonNull Builder responder(@NonNull Responder responder)
```

Sets the responder for LLM calls.

**Parameters**

| Name | Description |
|------|-------------|
| `responder` | the responder |

**Returns**

this builder

---

### `addRoute`

```java
public @NonNull Builder addRoute(@NonNull Interactable target, @NonNull String description)
```

Adds a route to a target.

The target can be any Interactable: Agent, RouterAgent, ParallelAgents, etc.

**Parameters**

| Name | Description |
|------|-------------|
| `target` | the target Interactable |
| `description` | keywords/phrases this target handles (e.g., "billing, invoices, payments") |

**Returns**

this builder

---

### `fallback`

```java
public @NonNull Builder fallback(@NonNull Interactable fallback)
```

Sets the fallback when no route matches.

The fallback can be any Interactable.

**Parameters**

| Name | Description |
|------|-------------|
| `fallback` | the fallback Interactable |

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

Configures this router to produce structured output of the specified type.

Returns a `StructuredBuilder` that builds a `RouterAgent.Structured` instead
of a regular RouterAgent.

All routed agents are expected to produce output parseable as the specified type.

Example:

```java
var router = RouterAgent.builder()
    .model("openai/gpt-4o")
    .responder(responder)
    .addRoute(billingAgent, "billing questions")
    .addRoute(techAgent, "technical issues")
    .structured(TicketResponse.class)
    .build();
StructuredAgentResult result = router.interactStructured("Invoice issue");
TicketResponse ticket = result.output();
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
public @NonNull RouterAgent build()
```

Builds the RouterAgent.

**Returns**

a new RouterAgent

---

### `build`

```java
public RouterAgent.Structured<T> build()
```

Builds the type-safe structured router agent.

**Returns**

the configured Structured router

---

### `outputType`

```java
public @NonNull Class<T> outputType()
```

Returns the structured output type.

---

### `classify`

```java
public @NonNull Optional<Interactable> classify(@NonNull String input)
```

Classifies the input and returns the selected route target without executing.

Delegates to the underlying RouterAgent.

**Parameters**

| Name | Description |
|------|-------------|
| `input` | the user input to classify |

**Returns**

Optional containing the selected Interactable, or empty if no match

