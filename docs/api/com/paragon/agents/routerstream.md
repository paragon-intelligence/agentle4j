# :material-code-braces: RouterStream

`com.paragon.agents.RouterStream` &nbsp;·&nbsp; **Class**

---

Streaming wrapper for RouterAgent that provides event callbacks during routing and execution.

RouterStream first classifies the input, then executes the selected agent with streaming.

```java
router.routeStream("Help with billing")
    .onRouteSelected(agent -> System.out.println("Routed to: " + agent.name()))
    .onTextDelta(System.out::print)
    .onComplete(result -> System.out.println("\nDone!"))
    .start();
```

*Since: 1.0*

## Methods

### `onRouteSelected`

```java
public @NonNull RouterStream onRouteSelected(@NonNull Consumer<Interactable> callback)
```

Called when a route is selected.

**Parameters**

| Name | Description |
|------|-------------|
| `callback` | receives the selected Interactable |

**Returns**

this stream

---

### `onTextDelta`

```java
public @NonNull RouterStream onTextDelta(@NonNull Consumer<String> callback)
```

Called for each text delta during streaming.

**Parameters**

| Name | Description |
|------|-------------|
| `callback` | receives text chunks |

**Returns**

this stream

---

### `onComplete`

```java
public @NonNull RouterStream onComplete(@NonNull Consumer<AgentResult> callback)
```

Called when streaming completes successfully.

**Parameters**

| Name | Description |
|------|-------------|
| `callback` | receives the final result |

**Returns**

this stream

---

### `onError`

```java
public @NonNull RouterStream onError(@NonNull Consumer<Throwable> callback)
```

Called when an error occurs.

**Parameters**

| Name | Description |
|------|-------------|
| `callback` | receives the error |

**Returns**

this stream

---

### `onTurnStart`

```java
public @NonNull RouterStream onTurnStart(@NonNull Consumer<Integer> callback)
```

Called at the start of each turn.

**Parameters**

| Name | Description |
|------|-------------|
| `callback` | receives the turn number |

**Returns**

this stream

---

### `onToolExecuted`

```java
public @NonNull RouterStream onToolExecuted(@NonNull Consumer<ToolExecution> callback)
```

Called when a tool execution completes.

**Parameters**

| Name | Description |
|------|-------------|
| `callback` | receives the tool execution result |

**Returns**

this stream

---

### `onHandoff`

```java
public @NonNull RouterStream onHandoff(@NonNull Consumer<Handoff> callback)
```

Called when a handoff occurs within the selected agent.

**Parameters**

| Name | Description |
|------|-------------|
| `callback` | receives the handoff |

**Returns**

this stream

---

### `start`

```java
public @NonNull AgentResult start()
```

Starts the streaming router execution. Blocks until completion.

On virtual threads, blocking is efficient and does not consume platform threads.

**Returns**

the final result

