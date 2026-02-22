# :material-code-braces: AgentNetwork

`com.paragon.agents.AgentNetwork` &nbsp;·&nbsp; **Class**

Implements `Interactable`

---

Implements the Network pattern: decentralized peer-to-peer agent communication.

Unlike `SupervisorAgent` which has a central coordinator, or `ParallelAgents`
which runs agents independently, AgentNetwork enables agents to communicate with each other in
rounds, building on each other's contributions.

Key characteristics:

  
- No central coordinator - agents are peers
- Each agent sees previous agents' contributions
- Communication happens in rounds until convergence or max rounds
- Resilient - failure of one agent doesn't cripple the network

**Virtual Thread Design:** Uses synchronous API optimized for Java 21+ virtual threads.
Blocking calls are cheap and efficient with virtual threads.

### Usage Example

```java
Agent optimist = Agent.builder().name("Optimist")
    .instructions("Argue the positive aspects").build();
Agent pessimist = Agent.builder().name("Pessimist")
    .instructions("Argue the negative aspects").build();
Agent moderate = Agent.builder().name("Moderate")
    .instructions("Find balanced middle ground").build();
AgentNetwork network = AgentNetwork.builder()
    .addPeer(optimist)
    .addPeer(pessimist)
    .addPeer(moderate)
    .maxRounds(3)
    .build();
// Agents discuss in rounds, each seeing previous contributions
NetworkResult result = network.discuss("Should we adopt AI widely?");
result.contributions().forEach(c ->
    System.out.println(c.agent().name() + ": " + c.output()));
```

*Since: 1.0*

## Methods

### `builder`

```java
public static @NonNull Builder builder()
```

Creates a new AgentNetwork builder.

---

### `name`

```java
public @NonNull String name()
```

{@inheritDoc}

---

### `peers`

```java
public @NonNull List<Interactable> peers()
```

Returns the peers in this network.

**Returns**

unmodifiable list of peers

---

### `maxRounds`

```java
public int maxRounds()
```

Returns the maximum number of discussion rounds.

**Returns**

the max rounds

---

### `getSynthesizer`

```java
Interactable getSynthesizer()
```

Returns the synthesizer if configured.

**Returns**

the synthesizer, or null if not set

---

### `discuss`

```java
public @NonNull NetworkResult discuss(@NonNull String topic)
```

Initiates a discussion among all peer agents.

Agents contribute in sequence within each round, with each agent seeing all previous
contributions. Discussion continues for the configured number of rounds.

**Parameters**

| Name | Description |
|------|-------------|
| `topic` | the discussion topic |

**Returns**

the network result containing all contributions

---

### `discuss`

```java
public @NonNull NetworkResult discuss(@NonNull Text text)
```

Initiates a discussion with Text content.

**Parameters**

| Name | Description |
|------|-------------|
| `text` | the discussion topic |

**Returns**

the network result

---

### `discuss`

```java
public @NonNull NetworkResult discuss(@NonNull Message message)
```

Initiates a discussion with a Message.

**Parameters**

| Name | Description |
|------|-------------|
| `message` | the discussion message |

**Returns**

the network result

---

### `discuss`

```java
public @NonNull NetworkResult discuss(@NonNull Prompt prompt)
```

Initiates a discussion with a Prompt.

The prompt's text content is extracted and used as the input.

**Parameters**

| Name | Description |
|------|-------------|
| `prompt` | the discussion prompt |

**Returns**

the network result

---

### `discuss`

```java
public @NonNull NetworkResult discuss(@NonNull AgenticContext context)
```

Initiates a discussion using an existing context.

This is the core discuss method. The context is shared across all agents, building up a
conversation history as agents contribute.

**Parameters**

| Name | Description |
|------|-------------|
| `context` | the context with discussion history |

**Returns**

the network result

---

### `broadcast`

```java
public @NonNull List<Contribution> broadcast(@NonNull String message)
```

Broadcasts a message to all peers simultaneously and collects responses.

Unlike discuss(), broadcast() runs all agents in parallel without sequential visibility.
Each agent only sees the original message, not other agents' responses.

Uses structured concurrency (Java 21+) for parallel execution.

**Parameters**

| Name | Description |
|------|-------------|
| `message` | the message to broadcast |

**Returns**

list of contributions

---

### `broadcast`

```java
public @NonNull List<Contribution> broadcast(@NonNull Prompt prompt)
```

Broadcasts a Prompt to all peers simultaneously and collects responses.

The prompt's text content is extracted and used as the input.

**Parameters**

| Name | Description |
|------|-------------|
| `prompt` | the prompt to broadcast |

**Returns**

list of contributions

---

### `discussStream`

```java
public @NonNull NetworkStream discussStream(@NonNull String topic)
```

Initiates a streaming discussion among all peer agents.

Returns a `NetworkStream` that allows registering callbacks for text deltas, round
progression, and completion events.

**Parameters**

| Name | Description |
|------|-------------|
| `topic` | the discussion topic |

**Returns**

a NetworkStream for processing streaming events

---

### `discussStream`

```java
public @NonNull NetworkStream discussStream(@NonNull Prompt prompt)
```

Initiates a streaming discussion with a Prompt.

The prompt's text content is extracted and used as the input.

**Parameters**

| Name | Description |
|------|-------------|
| `prompt` | the discussion prompt |

**Returns**

a NetworkStream for processing streaming events

---

### `discussStream`

```java
public @NonNull NetworkStream discussStream(@NonNull AgenticContext context)
```

Initiates a streaming discussion using an existing context.

**Parameters**

| Name | Description |
|------|-------------|
| `context` | the context with discussion history |

**Returns**

a NetworkStream for processing streaming events

---

### `broadcastStream`

```java
public @NonNull NetworkStream broadcastStream(@NonNull String message)
```

Broadcasts a message to all peers simultaneously with streaming.

Unlike discussStream(), broadcastStream() runs all agents in parallel without sequential
visibility. Each agent only sees the original message, not other agents' responses.

**Parameters**

| Name | Description |
|------|-------------|
| `message` | the message to broadcast |

**Returns**

a NetworkStream for processing streaming events

---

### `broadcastStream`

```java
public @NonNull NetworkStream broadcastStream(@NonNull Prompt prompt)
```

Broadcasts a Prompt to all peers simultaneously with streaming.

The prompt's text content is extracted and used as the input.

**Parameters**

| Name | Description |
|------|-------------|
| `prompt` | the prompt to broadcast |

**Returns**

a NetworkStream for processing streaming events

---

### `interact`

```java
public @NonNull AgentResult interact(@NonNull AgenticContext context)
```

{@inheritDoc}

Runs a discussion and returns an AgentResult. If a synthesizer is configured, returns the
synthesized output. Otherwise, returns the last contribution's output.

---

### `interact`

```java
public @NonNull AgentResult interact(
      @NonNull AgenticContext context, @Nullable TraceMetadata trace)
```

{@inheritDoc}

Runs a discussion and returns an AgentResult. Trace metadata is propagated through peer
interactions.

---

### `interactStream`

```java
public @NonNull AgentStream interactStream(@NonNull AgenticContext context)
```

{@inheritDoc}

If a synthesizer is configured, runs the discussion and streams the synthesis. Otherwise,
returns a completed stream with the discussion result.

---

### `interactStream`

```java
public @NonNull AgentStream interactStream(
      @NonNull AgenticContext context, @Nullable TraceMetadata trace)
```

{@inheritDoc}

If a synthesizer is configured, runs the discussion and streams the synthesis. Trace
metadata is propagated through peer interactions.

---

### `contributionsFrom`

```java
public @NonNull List<Contribution> contributionsFrom(@NonNull Interactable peer)
```

Returns all contributions from a specific peer.

---

### `contributionsFromRound`

```java
public @NonNull List<Contribution> contributionsFromRound(int round)
```

Returns all contributions from a specific round.

---

### `lastContribution`

```java
public @Nullable Contribution lastContribution()
```

Returns the final contribution (last in sequence).

---

### `name`

```java
public @NonNull Builder name(@NonNull String name)
```

Sets the name for this network.

**Parameters**

| Name | Description |
|------|-------------|
| `name` | the network name |

**Returns**

this builder

---

### `addPeer`

```java
public @NonNull Builder addPeer(@NonNull Interactable peer)
```

Adds a peer to the network.

Peers can be any Interactable: Agent, RouterAgent, ParallelAgents, etc.

**Parameters**

| Name | Description |
|------|-------------|
| `peer` | the peer to add |

**Returns**

this builder

---

### `addPeers`

```java
public @NonNull Builder addPeers(@NonNull Interactable... peers)
```

Adds multiple peers to the network.

**Parameters**

| Name | Description |
|------|-------------|
| `peers` | the peers to add |

**Returns**

this builder

---

### `maxRounds`

```java
public @NonNull Builder maxRounds(int maxRounds)
```

Sets the maximum number of discussion rounds.

In each round, all peers contribute sequentially.

**Parameters**

| Name | Description |
|------|-------------|
| `maxRounds` | the max rounds (default: 2) |

**Returns**

this builder

---

### `synthesizer`

```java
public @NonNull Builder synthesizer(@NonNull Interactable synthesizer)
```

Sets an optional synthesizer that combines all contributions.

The synthesizer can be any Interactable: Agent, RouterAgent, etc.

**Parameters**

| Name | Description |
|------|-------------|
| `synthesizer` | the synthesizer |

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

### `build`

```java
public @NonNull AgentNetwork build()
```

Builds the AgentNetwork.

**Returns**

the configured network

