# :material-code-braces: RouterStream

`com.paragon.agents.RouterStream` &nbsp;·&nbsp; **Class**

---

Streaming wrapper for RouterAgent that provides event callbacks during routing and execution.

RouterStream first classifies the input, then executes the selected agent with streaming.

```java
AgenticContext context = AgenticContext.create()
    .addMessage(Message.user("Help with billing"));
router.routeStream(context)
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

### `onRoutingFailed`

```java
public @NonNull RouterStream onRoutingFailed(@NonNull Consumer<String> callback)
```

Called when no route is found for the input (empty input or no matching route).

**Parameters**

| Name | Description |
|------|-------------|
| `callback` | receives a description of the routing failure |

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

### `onToolCallPending`

```java
public @NonNull RouterStream onToolCallPending(AgentStream.ToolConfirmationHandler handler)
```

Called when a tool call requires confirmation (human-in-the-loop), forwarded from the child agent.

**Parameters**

| Name | Description |
|------|-------------|
| `handler` | receives the pending tool call and approval callback |

**Returns**

this stream

---

### `onPause`

```java
public @NonNull RouterStream onPause(AgentStream.PauseHandler handler)
```

Called when the child agent should pause for async approval, forwarded from the child agent.

**Parameters**

| Name | Description |
|------|-------------|
| `handler` | receives the serializable run state |

**Returns**

this stream

---

### `onGuardrailFailed`

```java
public @NonNull RouterStream onGuardrailFailed(@NonNull Consumer<GuardrailResult.Failed> callback)
```

Called when an output guardrail fails in the child agent.

**Parameters**

| Name | Description |
|------|-------------|
| `callback` | receives the failed guardrail result |

**Returns**

this stream

---

### `onClientSideTool`

```java
public @NonNull RouterStream onClientSideTool(@NonNull Consumer<FunctionToolCall> callback)
```

Called when a client-side tool (`stopsLoop = true`) is detected in the child agent.

**Parameters**

| Name | Description |
|------|-------------|
| `callback` | receives the tool call that triggered the exit |

**Returns**

this stream

---

### `startBlocking`

```java
public @NonNull AgentResult startBlocking()
```

Starts the streaming router execution. Blocks until completion.

On virtual threads, blocking is efficient and does not consume platform threads.

**Returns**

the final result

---

### `start`

```java
public @NonNull AgentResult start()
```

Starts the streaming router execution. Blocks until completion.

On virtual threads, blocking is efficient and does not consume platform threads.

**Returns**

the final result

