# :material-code-braces: SelfCorrectingInteractable

`com.paragon.harness.SelfCorrectingInteractable` &nbsp;·&nbsp; **Class**

Implements `Interactable`

---

Decorator that wraps any `Interactable` with a self-correction loop.

When the wrapped agent produces a result that satisfies the `retryOn` predicate
(e.g., an error or guardrail failure), this decorator:

  
- Formats the error into the configured feedback template
- Injects the feedback as a user message into the conversation context
- Re-runs the agent (up to `maxRetries` times)
- Returns the final result (successful or last failure)

LangChain data shows this pattern gives the largest benchmark improvements of any
harness feature, because it closes the feedback loop within a single session.

Example:

```java
Interactable agent = Agent.builder()
    .name("CodeWriter")
    .addOutputGuardrail(syntaxChecker)
    .build();
SelfCorrectionConfig config = SelfCorrectionConfig.builder()
    .maxRetries(3)
    .retryOn(result -> result.isError())
    .build();
Interactable correcting = SelfCorrectingInteractable.wrap(agent, config);
AgentResult result = correcting.interact("Write a Python function that sorts a list");
```

**See Also**

- `SelfCorrectionConfig`

*Since: 1.0*

## Methods

### `wrap`

```java
public static @NonNull SelfCorrectingInteractable wrap(
      @NonNull Interactable agent, @NonNull SelfCorrectionConfig config)
```

Wraps an interactable with the given self-correction configuration.

**Parameters**

| Name | Description |
|------|-------------|
| `agent` | the agent to wrap |
| `config` | the self-correction configuration |

**Returns**

a self-correcting interactable

---

### `wrap`

```java
public static @NonNull SelfCorrectingInteractable wrap(@NonNull Interactable agent)
```

Wraps an interactable with default self-correction configuration (3 retries, retry on error).

**Parameters**

| Name | Description |
|------|-------------|
| `agent` | the agent to wrap |

**Returns**

a self-correcting interactable

---

### `onRetryStart`

```java
public @NonNull SelfCorrectingInteractable onRetryStart(@NonNull RetryStartHandler handler)
```

Called before each retry attempt, with the attempt number, error message, and failed result.

**Parameters**

| Name | Description |
|------|-------------|
| `handler` | receives (attempt, errorMessage, failedResult) |

**Returns**

this instance for chaining

---

### `onRetryComplete`

```java
public @NonNull SelfCorrectingInteractable onRetryComplete(
      @NonNull BiConsumer<Integer, AgentResult> handler)
```

Called after each retry attempt completes, with the attempt number and new result.

**Parameters**

| Name | Description |
|------|-------------|
| `handler` | receives (attempt, newResult) |

**Returns**

this instance for chaining

---

### `onMaxRetriesExhausted`

```java
public @NonNull SelfCorrectingInteractable onMaxRetriesExhausted(
      @NonNull Consumer<AgentResult> handler)
```

Called when all retries are exhausted and the result still fails the retry predicate.

**Parameters**

| Name | Description |
|------|-------------|
| `handler` | receives the final failed result |

**Returns**

this instance for chaining

---

### `asStreaming`

```java
public com.paragon.agents.Interactable.@NonNull Streaming asStreaming()
```

Returns a streaming view backed by the delegate's streaming.

Self-correction is a blocking concept; streaming delegates directly to the wrapped agent.

**Returns**

an `com.paragon.agents.Interactable.Streaming` backed by the delegate

---

### `onRetry`

```java
void onRetry(int attempt, @NonNull String error, @NonNull AgentResult failed)
```

Called at the start of each retry attempt.

**Parameters**

| Name | Description |
|------|-------------|
| `attempt` | the 1-indexed attempt number |
| `error` | the error message extracted from the failed result |
| `failed` | the failed `AgentResult` that triggered the retry |

