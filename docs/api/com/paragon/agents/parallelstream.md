# :material-code-braces: ParallelStream

`com.paragon.agents.ParallelStream` &nbsp;·&nbsp; **Class**

---

Streaming wrapper for ParallelAgents that provides event callbacks during parallel execution.

```java
team.runStream("Analyze market")
    .onAgentTextDelta((agent, delta) -> System.out.print("[" + agent.name() + "] " + delta))
    .onAgentComplete((agent, result) -> System.out.println(agent.name() + " done!"))
    .onComplete(results -> System.out.println("All done!"))
    .start();
```

*Since: 1.0*

## Methods

### `onAgentTextDelta`

```java
public @NonNull ParallelStream onAgentTextDelta(
      @NonNull BiConsumer<Interactable, String> callback)
```

Called for each text delta from any member.

**Parameters**

| Name | Description |
|------|-------------|
| `callback` | receives the member and text chunk |

**Returns**

this stream

---

### `onAgentComplete`

```java
public @NonNull ParallelStream onAgentComplete(
      @NonNull BiConsumer<Interactable, AgentResult> callback)
```

Called when an individual member completes.

**Parameters**

| Name | Description |
|------|-------------|
| `callback` | receives the member and its result |

**Returns**

this stream

---

### `onComplete`

```java
public @NonNull ParallelStream onComplete(@NonNull Consumer<List<AgentResult>> callback)
```

Called when all agents complete (for ALL mode).

**Parameters**

| Name | Description |
|------|-------------|
| `callback` | receives list of all results |

**Returns**

this stream

---

### `onFirstComplete`

```java
public @NonNull ParallelStream onFirstComplete(@NonNull Consumer<AgentResult> callback)
```

Called when the first agent completes (for FIRST mode).

**Parameters**

| Name | Description |
|------|-------------|
| `callback` | receives the first result |

**Returns**

this stream

---

### `onSynthesisComplete`

```java
public @NonNull ParallelStream onSynthesisComplete(@NonNull Consumer<AgentResult> callback)
```

Called when synthesis completes (for SYNTHESIZE mode).

**Parameters**

| Name | Description |
|------|-------------|
| `callback` | receives the synthesized result |

**Returns**

this stream

---

### `onError`

```java
public @NonNull ParallelStream onError(@NonNull Consumer<Throwable> callback)
```

Called when an error occurs.

**Parameters**

| Name | Description |
|------|-------------|
| `callback` | receives the error |

**Returns**

this stream

---

### `onAgentTurnStart`

```java
public @NonNull ParallelStream onAgentTurnStart(
      @NonNull BiConsumer<Interactable, Integer> callback)
```

Called at the start of each turn for any member.

**Parameters**

| Name | Description |
|------|-------------|
| `callback` | receives the member and turn number |

**Returns**

this stream

---

### `start`

```java
public @NonNull Object start()
```

Starts the streaming parallel execution. Blocks until completion.

On virtual threads, blocking is efficient and does not consume platform threads.

**Returns**

results based on mode: List<AgentResult> for ALL, AgentResult for FIRST/SYNTHESIZE

