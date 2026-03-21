# :material-code-braces: MessageSerializer

> This docs was updated at: 2026-03-21

`com.paragon.responses.json.MessageSerializer` &nbsp;·&nbsp; **Class**

Extends `StdSerializer<Message>`

---

Custom Jackson serializer for `Message` and its subclasses.

The OpenAI Responses API (and OpenRouter) requires `content` to be a plain string
rather than an array of content objects. Without this serializer, Jackson would serialize
`content` as `[{"type":"input_text","text":"..."`]} which is rejected with
`"Invalid input: expected string, received array"`.

This serializer writes `content` as a plain string when the message has exactly one
`Text` content item (the common case for conversation history), and falls back to the
array format otherwise (for multi-content messages with images, files, etc.).

## Methods

### `serializeWithType`

```java
public void serializeWithType(
      Message value,
      JsonGenerator gen,
      SerializationContext provider,
      TypeSerializer typeSer)
      throws tools.jackson.core.JacksonException
```

Called when serializing within a polymorphic context (e.g., `List`
with `@JsonTypeInfo`). Since we embed `"type"` directly in the JSON object,
we skip the external type wrapper and delegate to `.serialize`.
