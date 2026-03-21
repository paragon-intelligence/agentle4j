# :material-code-braces: FunctionToolCallOutputSerializer

> This docs was updated at: 2026-03-21

`com.paragon.responses.json.FunctionToolCallOutputSerializer` &nbsp;·&nbsp; **Class**

Extends `StdSerializer<FunctionToolCallOutput>`

---

Custom Jackson serializer for `FunctionToolCallOutput`.

The OpenAI Responses API requires:

  
- `type`: always `"function_call_output"`
- `call_id`: the matching function call ID
- `output`: a plain **string** (not an object)

Without this serializer, Jackson would serialize `output` as a nested JSON object
(e.g., `{"text": "..."`}) because `FunctionToolCallOutputKind` has no type
information. That causes OpenRouter (and OpenAI) to reject the payload with
`"expected string, received object"`.

## Methods

### `serializeWithType`

```java
public void serializeWithType(
      FunctionToolCallOutput value,
      JsonGenerator gen,
      SerializationContext provider,
      TypeSerializer typeSer)
      throws tools.jackson.core.JacksonException
```

Called when serializing within a polymorphic context (e.g., `List`
with `@JsonTypeInfo`). Since we embed `"type"` directly in the JSON object,
we skip the external type wrapper and delegate to `.serialize`.
