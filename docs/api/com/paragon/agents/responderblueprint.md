# :material-database: ResponderBlueprint

> This docs was updated at: 2026-02-23

`com.paragon.agents.ResponderBlueprint` &nbsp;·&nbsp; **Record**

---

Serializable descriptor for a `Responder` configuration.

On reconstruction, the API key is resolved automatically from environment variables based on
the provider (e.g., `OPENROUTER_API_KEY` for OpenRouter).

## Methods

### `from`

```java
public static ResponderBlueprint from(@NonNull Responder responder)
```

Extracts a blueprint from an existing `Responder`.

---

### `toResponder`

```java
public Responder toResponder()
```

Reconstructs a `Responder` from this blueprint.

---

### `fromInput`

```java
static GuardrailReference fromInput(InputGuardrail g)
```

Creates a reference from a live guardrail instance.

---

### `fromOutput`

```java
static GuardrailReference fromOutput(OutputGuardrail g)
```

Creates a reference from a live guardrail instance.
