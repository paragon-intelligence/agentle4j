# :material-approximately-equal: InputGuardrail

`com.paragon.agents.InputGuardrail` &nbsp;·&nbsp; **Interface**

---

Validates user input before agent processing.

Input guardrails are executed at the start of each agent interaction, before the LLM is
called. They can be used to:

  
- Filter out sensitive information (passwords, PII)
- Validate input format or length
- Enforce topic restrictions
- Rate limit based on context state

### Usage Example

```java
InputGuardrail noPasswords = (input, ctx) -> {
    if (input.toLowerCase().contains("password")) {
        return GuardrailResult.failed("Cannot discuss passwords");
    }
    return GuardrailResult.passed();
};
Agent agent = Agent.builder()
    .addInputGuardrail(noPasswords)
    .build();
```

**See Also**

- `OutputGuardrail`
- `GuardrailResult`

*Since: 1.0*

## Methods

### `validate`

```java
GuardrailResult validate(@NonNull String input, @NonNull AgenticContext context)
```

Validates the user input.

**Parameters**

| Name | Description |
|------|-------------|
| `input` | the user's input string |
| `context` | the current agent context (for state-based validation) |

**Returns**

the validation result

---

### `named`

```java
static @NonNull InputGuardrail named(@NonNull String id, @NonNull InputGuardrail impl)
```

Wraps a guardrail implementation with a named ID for blueprint serialization.

Use this when defining guardrails as lambdas that need to be serializable. The guardrail is
registered in the `GuardrailRegistry` and can be reconstructed during deserialization.

```java
InputGuardrail guard = InputGuardrail.named("no_passwords", (input, ctx) -> {
    if (input.contains("password")) return GuardrailResult.failed("No passwords!");
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

