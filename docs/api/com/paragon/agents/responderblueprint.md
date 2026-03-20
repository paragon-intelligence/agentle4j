# :material-database: ResponderBlueprint

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

---

### `deserializeWithType`

```java
public InteractableBlueprint deserializeWithType(
        JsonParser p, DeserializationContext ctxt, TypeDeserializer typeDeserializer)
        throws tools.jackson.core.JacksonException
```

Called by Jackson when both `@JsonTypeInfo` and `@JsonDeserialize` are present on
the same type. Overriding this gives us full control of the token stream *before* the
`TypeDeserializer` reads and consumes the `type` property.

  
- For `$ref` / `source: file|registry` nodes we resolve the reference
      directly — no `type` field is required.
- For inline definitions we replay the full node into `typeDeserializer` so it can
      read `type` and dispatch to the correct concrete class as normal.

---

### `dispatchByType`

```java
private InteractableBlueprint dispatchByType(
        JsonNode node, DeserializationContext ctxt, JsonParser p)
        throws tools.jackson.core.JacksonException
```

Dispatches to the concrete blueprint class by reading the `type` field from `node`. Each concrete class carries `@JsonDeserialize(using=None.class)` to suppress the
inherited `BlueprintDeserializer`, so `treeToValue(node, ConcreteClass.class)`
uses the default record deserializer — no recursion.

---

### `requireField`

```java
private String requireField(JsonNode node, String field, String context, JsonParser p)
        throws tools.jackson.core.JacksonException
```

Requires a field to be present in the node, throwing a descriptive error if missing.

**Parameters**

| Name | Description |
|------|-------------|
| `node` | the JSON node |
| `field` | the required field name |
| `context` | a human-readable description of the context (e.g. "source: file") |
| `p` | the parser (for error location) |

**Returns**

the field value as text

**Throws**

| Type | Condition |
|------|-----------|
| `tools.jackson.core.JacksonException` | if the field is absent |

---

### `resolveFileRef`

```java
private InteractableBlueprint resolveFileRef(String refPath, JsonParser p) throws tools.jackson.core.JacksonException
```

Resolves a file path by reading the file and parsing it as an `InteractableBlueprint`.

**Parameters**

| Name | Description |
|------|-------------|
| `refPath` | the file path (relative to CWD or absolute) |
| `p` | the parser (for error messages) |

**Returns**

the deserialized blueprint

**Throws**

| Type | Condition |
|------|-----------|
| `tools.jackson.core.JacksonException` | if the file cannot be read or parsed |

