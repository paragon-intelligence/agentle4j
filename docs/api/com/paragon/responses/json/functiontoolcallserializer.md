# :material-code-braces: FunctionToolCallSerializer

`com.paragon.responses.json.FunctionToolCallSerializer` &nbsp;·&nbsp; **Class**

Extends `StdSerializer<FunctionToolCall>`

---

Custom Jackson serializer for `FunctionToolCall`.

When `FunctionToolCall` appears in a `List` (which uses
`@JsonTypeInfo(EXISTING_PROPERTY)`), the outer type resolver does NOT inject the
`"type"` field. Without this serializer, the serialized JSON lacks `"type":
"function_call"`, causing OpenRouter to reject the payload.

This serializer explicitly writes `"type": "function_call"` along with all other fields.

## Methods

### `serializeWithType`

```java
public void serializeWithType(
      FunctionToolCall value,
      JsonGenerator gen,
      SerializationContext provider,
      TypeSerializer typeSer)
      throws tools.jackson.core.JacksonException
```

Called when serializing within a polymorphic context (e.g., `List`
with `@JsonTypeInfo`). We embed `"type"` directly in the JSON object, so skip the
external type wrapper and delegate to `.serialize`.
