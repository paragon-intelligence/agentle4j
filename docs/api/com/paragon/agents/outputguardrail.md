# :material-approximately-equal: OutputGuardrail

> This docs was updated at: 2026-02-23

`com.paragon.agents.OutputGuardrail` &nbsp;·&nbsp; **Interface**

---

Validates agent output before returning to the user.

Output guardrails are executed after the agent has finished processing, just before the final
result is returned. They can be used to:

  
- Filter or mask sensitive information in responses
- Enforce output length limits
- Check for inappropriate content
- Validate structured output format

### Usage Example

```java
OutputGuardrail maxLength = (output, ctx) -> {
    if (output.length() > 1000) {
        return GuardrailResult.failed("Response exceeds 1000 characters");
    }
    return GuardrailResult.passed();
};
Agent agent = Agent.builder()
    .addOutputGuardrail(maxLength)
    .build();
```

**See Also**

- `InputGuardrail`
- `GuardrailResult`

*Since: 1.0*

## Methods

### `validate`

```java
GuardrailResult validate(@NonNull String output, @NonNull AgenticContext context)
```

Validates the agent's output.

**Parameters**

| Name | Description |
|------|-------------|
| `output` | the agent's output string |
| `context` | the current agent context |

**Returns**

the validation result

---

### `named`

```java
static @NonNull OutputGuardrail named(@NonNull String id, @NonNull OutputGuardrail impl)
```

Wraps a guardrail implementation with a named ID for blueprint serialization.

Use this when defining guardrails as lambdas that need to be serializable. The guardrail is
registered in the `GuardrailRegistry` and can be reconstructed during deserialization.

```java
OutputGuardrail guard = OutputGuardrail.named("max_length", (output, ctx) -> {
    if (output.length() > 1000) return GuardrailResult.failed("Too long");
    return GuardrailResult.passed();
});
```

**Parameters**

| Name | Description |
|------|-------------|
| `id` | the unique identifier for this guardrail |
| `impl` | the guardrail implementation |

**Returns**

a named guardrail wrapping the implementation

