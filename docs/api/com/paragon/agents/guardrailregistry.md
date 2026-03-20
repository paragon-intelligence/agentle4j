# :material-code-braces: GuardrailRegistry

`com.paragon.agents.GuardrailRegistry` &nbsp;·&nbsp; **Class**

---

Thread-safe global registry for named guardrails.

Enables serialization of lambda/anonymous guardrails by associating them with string IDs.
Guardrails registered here can be referenced by ID in `InteractableBlueprint` and
reconstructed during deserialization.

### Usage Example

```java
// Register a lambda guardrail with an ID
InputGuardrail guard = InputGuardrail.named("no_passwords", (input, ctx) -> {
    if (input.contains("password")) return GuardrailResult.failed("No passwords!");
    return GuardrailResult.passed();
});
// The guardrail is now serializable in blueprints
Agent agent = Agent.builder()
    .addInputGuardrail(guard)
    .build();
// Serialize and deserialize
String json = objectMapper.writeValueAsString(agent.toBlueprint());
Interactable restored = objectMapper.readValue(json, InteractableBlueprint.class).toInteractable();
```

**See Also**

- `InputGuardrail#named(String, InputGuardrail)`
- `OutputGuardrail#named(String, OutputGuardrail)`

*Since: 1.0*

## Methods

### `registerInput`

```java
public static void registerInput(@NonNull String id, @NonNull InputGuardrail guardrail)
```

Registers an input guardrail with the given ID.

**Parameters**

| Name | Description |
|------|-------------|
| `id` | the unique identifier |
| `guardrail` | the guardrail implementation |

---

### `registerOutput`

```java
public static void registerOutput(@NonNull String id, @NonNull OutputGuardrail guardrail)
```

Registers an output guardrail with the given ID.

**Parameters**

| Name | Description |
|------|-------------|
| `id` | the unique identifier |
| `guardrail` | the guardrail implementation |

---

### `getInput`

```java
public static @Nullable InputGuardrail getInput(@NonNull String id)
```

Retrieves a registered input guardrail by ID.

**Parameters**

| Name | Description |
|------|-------------|
| `id` | the guardrail ID |

**Returns**

the guardrail, or null if not registered

---

### `getOutput`

```java
public static @Nullable OutputGuardrail getOutput(@NonNull String id)
```

Retrieves a registered output guardrail by ID.

**Parameters**

| Name | Description |
|------|-------------|
| `id` | the guardrail ID |

**Returns**

the guardrail, or null if not registered

---

### `clear`

```java
public static void clear()
```

Removes all registered guardrails. Useful for testing.
