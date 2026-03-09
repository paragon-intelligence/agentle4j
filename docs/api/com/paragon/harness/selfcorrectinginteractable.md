# :material-code-braces: SelfCorrectingInteractable

> This docs was updated at: 2026-03-09









`com.paragon.harness.SelfCorrectingInteractable` &nbsp;·&nbsp; **Class**

Implements `Interactable`

---

Decorator that wraps any `Interactable` with a self-correction retry loop.

When the wrapped agent produces a result that satisfies the `retryOn` predicate (e.g., an error or guardrail failure), this decorator:

1. Formats the error into the configured feedback template
2. Injects the feedback as a user message into the conversation context
3. Re-runs the agent (up to `maxRetries` times)
4. Returns the final result (successful or last failure)

LangChain benchmarks show this pattern provides the largest accuracy improvement of any harness feature by closing the feedback loop within a single session.

**See Also**

- `SelfCorrectionConfig`
- `Harness`

*Since: 1.0*

## Factory Methods

### `wrap(Interactable, SelfCorrectionConfig)`

```java
public static @NonNull SelfCorrectingInteractable wrap(
    @NonNull Interactable agent,
    @NonNull SelfCorrectionConfig config)
```

Wraps an interactable with the given self-correction configuration.

---

### `wrap(Interactable)`

```java
public static @NonNull SelfCorrectingInteractable wrap(@NonNull Interactable agent)
```

Wraps an interactable with default settings: 3 retries, retry on any error.

## Usage

```java
Interactable agent = Agent.builder()
    .name("CodeWriter")
    .addOutputGuardrail((output, ctx) ->
        output.contains("class ") ? GuardrailResult.passed() : GuardrailResult.failed("No class found"))
    .build();

SelfCorrectionConfig config = SelfCorrectionConfig.builder()
    .maxRetries(3)
    .retryOn(result -> result.isError())
    .feedbackTemplate("""
        Your previous attempt failed:

        {error}

        Please fix the issue and try again.
        """)
    .build();

Interactable correcting = SelfCorrectingInteractable.wrap(agent, config);
AgentResult result = correcting.interact("Write a Java class for binary search");
```

## Notes

- Streaming (`interactStream`) delegates directly to the wrapped agent — self-correction is a blocking concept
- The name of the wrapped interactable is suffixed with `[SelfCorrecting]`
- Each retry adds one user message to the conversation context; the agent sees the full correction history
