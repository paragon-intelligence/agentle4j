# :material-code-braces: NetworkStream

`com.paragon.agents.NetworkStream` &nbsp;·&nbsp; **Class**

---

Streaming wrapper for AgentNetwork that provides event callbacks during network discussions.

```java
network.discussStream("Should AI be regulated?")
    .onPeerTextDelta((peer, delta) -> System.out.print("[" + peer.name() + "] " + delta))
    .onRoundStart(round -> System.out.println("=== Round " + round + " ==="))
    .onComplete(result -> System.out.println("Discussion finished!"))
    .start();
```

*Since: 1.0*

## Methods

### `onPeerTextDelta`

```java
public @NonNull NetworkStream onPeerTextDelta(
      @NonNull BiConsumer<Interactable, String> callback)
```

Called for each text delta from any peer.

**Parameters**

| Name | Description |
|------|-------------|
| `callback` | receives the peer and text chunk |

**Returns**

this stream

---

### `onPeerComplete`

```java
public @NonNull NetworkStream onPeerComplete(
      @NonNull BiConsumer<Interactable, AgentResult> callback)
```

Called when an individual peer completes its contribution.

**Parameters**

| Name | Description |
|------|-------------|
| `callback` | receives the peer and its result |

**Returns**

this stream

---

### `onRoundStart`

```java
public @NonNull NetworkStream onRoundStart(@NonNull Consumer<Integer> callback)
```

Called when a new discussion round begins.

**Parameters**

| Name | Description |
|------|-------------|
| `callback` | receives the round number (1-indexed) |

**Returns**

this stream

---

### `onRoundComplete`

```java
public @NonNull NetworkStream onRoundComplete(
      @NonNull Consumer<List<AgentNetwork.Contribution>> callback)
```

Called when a discussion round completes.

**Parameters**

| Name | Description |
|------|-------------|
| `callback` | receives the contributions from that round |

**Returns**

this stream

---

### `onSynthesisTextDelta`

```java
public @NonNull NetworkStream onSynthesisTextDelta(@NonNull Consumer<String> callback)
```

Called for each text delta from the synthesizer agent (if configured).

**Parameters**

| Name | Description |
|------|-------------|
| `callback` | receives the text chunk |

**Returns**

this stream

---

### `onComplete`

```java
public @NonNull NetworkStream onComplete(@NonNull Consumer<AgentNetwork.NetworkResult> callback)
```

Called when the network discussion completes.

**Parameters**

| Name | Description |
|------|-------------|
| `callback` | receives the final network result |

**Returns**

this stream

---

### `onError`

```java
public @NonNull NetworkStream onError(@NonNull Consumer<Throwable> callback)
```

Called when an error occurs.

**Parameters**

| Name | Description |
|------|-------------|
| `callback` | receives the error |

**Returns**

this stream

---

### `onPeerToolExecuted`

```java
public @NonNull NetworkStream onPeerToolExecuted(
      @NonNull BiConsumer<Interactable, ToolExecution> callback)
```

Called when a peer executes a tool.

**Parameters**

| Name | Description |
|------|-------------|
| `callback` | receives the peer and the tool execution result |

**Returns**

this stream

---

### `onPeerGuardrailFailed`

```java
public @NonNull NetworkStream onPeerGuardrailFailed(
      @NonNull BiConsumer<Interactable, GuardrailResult.Failed> callback)
```

Called when a guardrail fails for a peer.

**Parameters**

| Name | Description |
|------|-------------|
| `callback` | receives the peer and the failed guardrail result |

**Returns**

this stream

---

### `onPeerError`

```java
public @NonNull NetworkStream onPeerError(
      @NonNull BiConsumer<Interactable, AgentResult> callback)
```

Called when a peer's result is an error. Fires alongside `onPeerComplete` only when
the peer result has `isError() == true`.

**Parameters**

| Name | Description |
|------|-------------|
| `callback` | receives the peer and its errored result |

**Returns**

this stream

---

### `start`

```java
public AgentNetwork.NetworkResult start()
```

Starts the streaming network execution. Blocks until completion.

On virtual threads, blocking is efficient and does not consume platform threads.

**Returns**

the network result

