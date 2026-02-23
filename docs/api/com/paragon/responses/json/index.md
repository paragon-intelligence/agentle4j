# Package `com.paragon.responses.json`

> This docs was updated at: 2026-02-23

---

## :material-code-braces: Classs

| Name | Description |
|------|-------------|
| [`ClickActionDeserializer`](clickactiondeserializer.md) | Custom deserializer for ClickAction to handle @JsonUnwrapped Coordinate |
| [`ConcreteMessageDeserializer`](concretemessagedeserializer.md) | Deserializer for concrete Message subclasses that delegates to MessageDeserializer and casts to t… |
| [`CreateResponsePayloadDeserializer`](createresponsepayloaddeserializer.md) | Custom deserializer for CreateResponsePayload to handle @JsonUnwrapped OpenRouterCustomPayload |
| [`DoubleClickActionDeserializer`](doubleclickactiondeserializer.md) | Custom deserializer for DoubleClickAction to handle @JsonUnwrapped Coordinate |
| [`MessageDeserializer`](messagedeserializer.md) | Custom deserializer for Message that uses the 'role' field to determine which concrete subclass t… |
| [`MoveActionDeserializer`](moveactiondeserializer.md) | Custom deserializer for MoveAction to handle @JsonUnwrapped Coordinate |
| [`ResponsesApiModule`](responsesapimodule.md) | Jackson module that registers all custom serializers and deserializers for the Responses API spec… |
| [`ScrollActionDeserializer`](scrollactiondeserializer.md) | Custom deserializer for ScrollAction to handle @JsonUnwrapped Coordinate |

## :material-database: Records

| Name | Description |
|------|-------------|
| [`JacksonJsonSchemaProducer`](jacksonjsonschemaproducer.md) | Produces JSON schemas compatible with OpenAI's function calling API |
