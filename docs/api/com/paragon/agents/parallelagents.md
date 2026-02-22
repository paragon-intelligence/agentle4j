# :material-code-braces: ParallelAgents

`com.paragon.agents.ParallelAgents` &nbsp;·&nbsp; **Class**

Implements `Interactable`

---

Orchestrates parallel execution of multiple agents.

From Chapter 7 (Multi-Agent Collaboration): "Multiple agents work on different parts of a
problem simultaneously, and their results are later combined."

**Virtual Thread Design:** Uses Java 21+ structured concurrency for parallel execution. All
agents run on virtual threads, making parallel execution cheap and efficient.

**Trace Correlation:** All parallel agents share the same parent traceId, enabling
end-to-end debugging of fan-out patterns.

### Usage Example

```java
Agent researcher = Agent.builder().name("Researcher")...build();
Agent analyst = Agent.builder().name("Analyst")...build();
Agent writer = Agent.builder().name("Writer")...build();
ParallelAgents team = ParallelAgents.of(researcher, analyst);
// Blocking call - uses virtual threads internally
List results = team.run("Analyze market trends");
// Get just the first to complete
AgentResult fastest = team.runFirst("Quick analysis needed");
// Synthesize with another agent
AgentResult combined = team.runAndSynthesize("What's the outlook?", writer);
// Streaming support
team.runStream("Analyze trends")
    .onAgentTextDelta((agent, delta) -> System.out.print("[" + agent.name() + "] " + delta))
    .onComplete(results -> System.out.println("Done!"))
    .start();
```

*Since: 1.0*

## Methods

### `of`

```java
public static @NonNull ParallelAgents of(@NonNull Interactable... members)
```

Creates a parallel orchestrator with the given members.

Members can be any Interactable: Agent, RouterAgent, ParallelAgents, etc.

**Parameters**

| Name | Description |
|------|-------------|
| `members` | the members to run in parallel (at least one required) |

**Returns**

a new ParallelAgents instance

---

### `of`

```java
public static @NonNull ParallelAgents of(@NonNull List<Interactable> members)
```

Creates a parallel orchestrator from a list of members.

**Parameters**

| Name | Description |
|------|-------------|
| `members` | the list of members (at least one required) |

**Returns**

a new ParallelAgents instance

---

### `named`

```java
public static @NonNull ParallelAgents named(
      @NonNull String name, @NonNull Interactable... members)
```

Creates a named parallel orchestrator with the given members.

**Parameters**

| Name | Description |
|------|-------------|
| `name` | the name for this orchestrator |
| `members` | the members to run in parallel |

**Returns**

a new ParallelAgents instance

---

### `name`

```java
public @NonNull String name()
```

{@inheritDoc}

---

### `members`

```java
public @NonNull List<Interactable> members()
```

Returns the members in this orchestrator.

**Returns**

unmodifiable list of members

---

### `runAll`

```java
public @NonNull List<AgentResult> runAll(@NonNull String input)
```

Runs all agents concurrently with the same input.

Each agent receives the same input and processes it independently with a fresh context. Uses
virtual threads for parallel execution.

**Parameters**

| Name | Description |
|------|-------------|
| `input` | the input text for all agents |

**Returns**

list of results, in the same order as agents()

---

### `runAll`

```java
public @NonNull List<AgentResult> runAll(@NonNull Text text)
```

Runs all agents concurrently with Text content.

**Parameters**

| Name | Description |
|------|-------------|
| `text` | the text content for all agents |

**Returns**

list of results

---

### `runAll`

```java
public @NonNull List<AgentResult> runAll(@NonNull Message message)
```

Runs all agents concurrently with a Message.

**Parameters**

| Name | Description |
|------|-------------|
| `message` | the message for all agents |

**Returns**

list of results

---

### `runAll`

```java
public @NonNull List<AgentResult> runAll(@NonNull Prompt prompt)
```

Runs all agents concurrently with a Prompt.

The prompt's text content is extracted and used as the input.

**Parameters**

| Name | Description |
|------|-------------|
| `prompt` | the prompt for all agents |

**Returns**

list of results

---

### `runAll`

```java
public @NonNull List<AgentResult> runAll(@NonNull AgenticContext context)
```

Runs all agents concurrently using an existing context.

Each agent receives a copy of the context to prevent interference. All parallel agents share
the same parent traceId for trace correlation.

Uses structured concurrency (Java 21+) to run all agents on virtual threads.

This is the core method. All other runAll overloads delegate here.

**Parameters**

| Name | Description |
|------|-------------|
| `context` | the context to copy for each agent |

**Returns**

list of results, in the same order as agents()

---

### `runFirst`

```java
public @NonNull AgentResult runFirst(@NonNull String input)
```

Runs all agents concurrently and returns the first to complete.

Useful when you want the fastest response and don't need all results.

**Parameters**

| Name | Description |
|------|-------------|
| `input` | the input text for all agents |

**Returns**

the first result

---

### `runFirst`

```java
public @NonNull AgentResult runFirst(@NonNull Text text)
```

Runs all agents concurrently with Text content and returns the first to complete.

**Parameters**

| Name | Description |
|------|-------------|
| `text` | the text content for all agents |

**Returns**

the first result

---

### `runFirst`

```java
public @NonNull AgentResult runFirst(@NonNull Message message)
```

Runs all agents concurrently with a Message and returns the first to complete.

**Parameters**

| Name | Description |
|------|-------------|
| `message` | the message for all agents |

**Returns**

the first result

---

### `runFirst`

```java
public @NonNull AgentResult runFirst(@NonNull Prompt prompt)
```

Runs all agents concurrently with a Prompt and returns the first to complete.

The prompt's text content is extracted and used as the input.

**Parameters**

| Name | Description |
|------|-------------|
| `prompt` | the prompt for all agents |

**Returns**

the first result

---

### `runFirst`

```java
public @NonNull AgentResult runFirst(@NonNull AgenticContext context)
```

Runs all agents concurrently using an existing context and returns the first to complete.

Uses structured concurrency with ShutdownOnSuccess to cancel other agents once one
completes.

**Parameters**

| Name | Description |
|------|-------------|
| `context` | the context to copy for each agent |

**Returns**

the first result

---

### `runAndSynthesize`

```java
public @NonNull AgentResult runAndSynthesize(
      @NonNull String input, @NonNull Interactable synthesizer)
```

Runs all members concurrently, then synthesizes their outputs with a synthesizer.

The synthesizer receives a formatted summary of all member outputs and produces a combined
result. This is the "fan-out, fan-in" pattern.

**Parameters**

| Name | Description |
|------|-------------|
| `input` | the input text for all members |
| `synthesizer` | the Interactable that combines results |

**Returns**

the synthesized result

---

### `runAndSynthesize`

```java
public @NonNull AgentResult runAndSynthesize(
      @NonNull Text text, @NonNull Interactable synthesizer)
```

Runs all members concurrently with Text content, then synthesizes with a synthesizer.

**Parameters**

| Name | Description |
|------|-------------|
| `text` | the text content for all members |
| `synthesizer` | the Interactable that combines results |

**Returns**

the synthesized result

---

### `runAndSynthesize`

```java
public @NonNull AgentResult runAndSynthesize(
      @NonNull Message message, @NonNull Interactable synthesizer)
```

Runs all members concurrently with a Message, then synthesizes with a synthesizer.

**Parameters**

| Name | Description |
|------|-------------|
| `message` | the message for all members |
| `synthesizer` | the Interactable that combines results |

**Returns**

the synthesized result

---

### `runAndSynthesize`

```java
public @NonNull AgentResult runAndSynthesize(
      @NonNull Prompt prompt, @NonNull Interactable synthesizer)
```

Runs all members concurrently with a Prompt, then synthesizes with a synthesizer.

The prompt's text content is extracted and used as the input.

**Parameters**

| Name | Description |
|------|-------------|
| `prompt` | the prompt for all members |
| `synthesizer` | the Interactable that combines results |

**Returns**

the synthesized result

---

### `runAndSynthesize`

```java
public @NonNull AgentResult runAndSynthesize(
      @NonNull AgenticContext context, @NonNull Interactable synthesizer)
```

Runs all members concurrently using an existing context, then synthesizes with a synthesizer.

This is the core method. All other runAndSynthesize overloads delegate here.

**Parameters**

| Name | Description |
|------|-------------|
| `context` | the context to copy for each member |
| `synthesizer` | the Interactable that combines results |

**Returns**

the synthesized result

---

### `runAllStream`

```java
public @NonNull ParallelStream runAllStream(@NonNull String input)
```

Runs all agents concurrently with streaming.

**Parameters**

| Name | Description |
|------|-------------|
| `input` | the input text for all agents |

**Returns**

a ParallelStream for processing streaming events

---

### `runAllStream`

```java
public @NonNull ParallelStream runAllStream(@NonNull Prompt prompt)
```

Runs all agents concurrently with streaming using a Prompt.

The prompt's text content is extracted and used as the input.

**Parameters**

| Name | Description |
|------|-------------|
| `prompt` | the prompt for all agents |

**Returns**

a ParallelStream for processing streaming events

---

### `runAllStream`

```java
public @NonNull ParallelStream runAllStream(@NonNull AgenticContext context)
```

Runs all agents concurrently with streaming using an existing context.

**Parameters**

| Name | Description |
|------|-------------|
| `context` | the context to copy for each agent |

**Returns**

a ParallelStream for processing streaming events

---

### `runFirstStream`

```java
public @NonNull ParallelStream runFirstStream(@NonNull String input)
```

Runs all agents concurrently with streaming and returns when first completes.

**Parameters**

| Name | Description |
|------|-------------|
| `input` | the input text for all agents |

**Returns**

a ParallelStream for processing streaming events

---

### `runFirstStream`

```java
public @NonNull ParallelStream runFirstStream(@NonNull Prompt prompt)
```

Runs all agents concurrently with streaming using a Prompt and returns when first completes.

The prompt's text content is extracted and used as the input.

**Parameters**

| Name | Description |
|------|-------------|
| `prompt` | the prompt for all agents |

**Returns**

a ParallelStream for processing streaming events

---

### `runFirstStream`

```java
public @NonNull ParallelStream runFirstStream(@NonNull AgenticContext context)
```

Runs all agents concurrently with streaming and returns when first completes.

**Parameters**

| Name | Description |
|------|-------------|
| `context` | the context to copy for each agent |

**Returns**

a ParallelStream for processing streaming events

---

### `runAndSynthesizeStream`

```java
public @NonNull ParallelStream runAndSynthesizeStream(
      @NonNull String input, @NonNull Interactable synthesizer)
```

Runs all agents concurrently with streaming, then synthesizes results.

**Parameters**

| Name | Description |
|------|-------------|
| `input` | the input text for all agents |
| `synthesizer` | the agent that combines results |

**Returns**

a ParallelStream for processing streaming events

---

### `runAndSynthesizeStream`

```java
public @NonNull ParallelStream runAndSynthesizeStream(
      @NonNull Prompt prompt, @NonNull Interactable synthesizer)
```

Runs all agents concurrently with streaming using a Prompt, then synthesizes results.

The prompt's text content is extracted and used as the input.

**Parameters**

| Name | Description |
|------|-------------|
| `prompt` | the prompt for all agents |
| `synthesizer` | the agent that combines results |

**Returns**

a ParallelStream for processing streaming events

---

### `runAndSynthesizeStream`

```java
public @NonNull ParallelStream runAndSynthesizeStream(
      @NonNull AgenticContext context, @NonNull Interactable synthesizer)
```

Runs all agents concurrently with streaming, then synthesizes results.

**Parameters**

| Name | Description |
|------|-------------|
| `context` | the context to copy for each agent |
| `synthesizer` | the agent that combines results |

**Returns**

a ParallelStream for processing streaming events

---

### `interact`

```java
public @NonNull AgentResult interact(@NonNull AgenticContext context)
```

{@inheritDoc} Runs all agents; first to complete is primary, others in relatedResults().

---

### `interact`

```java
public @NonNull AgentResult interact(
      @NonNull AgenticContext context, @Nullable TraceMetadata trace)
```

{@inheritDoc} Runs all agents; trace propagated through peer interactions.

---

### `interactStream`

```java
public @NonNull AgentStream interactStream(@NonNull AgenticContext context)
```

{@inheritDoc}

Streams and returns when the first agent completes. For streaming all agents, use `.runAllStream(String)` with the explicit ParallelStream return type.

---

### `interactStream`

```java
public @NonNull AgentStream interactStream(
      @NonNull AgenticContext context, @Nullable TraceMetadata trace)
```

{@inheritDoc} Streams first member; trace propagated through peer interactions.
