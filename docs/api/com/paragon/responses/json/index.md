# Package `com.paragon.responses.json`

> This docs was updated at: 2026-03-21

---

## :material-code-braces: Classs

| Name | Description |
|------|-------------|
| [`ApplyPatchToolCallSerializer`](applypatchtoolcallserializer.md) | Custom serializer for `ApplyPatchToolCall` that ensures the `type` discriminator is always presen… |
| [`ClickActionDeserializer`](clickactiondeserializer.md) | Custom deserializer for ClickAction to handle @JsonUnwrapped Coordinate |
| [`CodeInterpreterToolCallSerializer`](codeinterpretertoolcallserializer.md) | Serializer that enforces the correct wire format for `CodeInterpreterToolCall` |
| [`ComputerToolCallSerializer`](computertoolcallserializer.md) | Serializer that enforces the correct wire format for `ComputerToolCall` |
| [`ConcreteMessageDeserializer`](concretemessagedeserializer.md) | Deserializer for concrete Message subclasses that delegates to MessageDeserializer and casts to t… |
| [`CreateResponsePayloadDeserializer`](createresponsepayloaddeserializer.md) | Custom deserializer for CreateResponsePayload to handle @JsonUnwrapped OpenRouterCustomPayload |
| [`CustomToolCallSerializer`](customtoolcallserializer.md) | Serializer that enforces the correct wire format for `CustomToolCall` |
| [`DoubleClickActionDeserializer`](doubleclickactiondeserializer.md) | Custom deserializer for DoubleClickAction to handle @JsonUnwrapped Coordinate |
| [`FileSearchToolCallSerializer`](filesearchtoolcallserializer.md) | Serializer that enforces the correct wire format for `FileSearchToolCall` |
| [`FunctionShellToolCallSerializer`](functionshelltoolcallserializer.md) | Serializer that enforces the correct wire format for `FunctionShellToolCall` |
| [`FunctionToolCallOutputDeserializer`](functiontoolcalloutputdeserializer.md) | Custom deserializer for `FunctionToolCallOutput` |
| [`FunctionToolCallOutputSerializer`](functiontoolcalloutputserializer.md) | Custom Jackson serializer for `FunctionToolCallOutput` |
| [`FunctionToolCallSerializer`](functiontoolcallserializer.md) | Custom Jackson serializer for `FunctionToolCall` |
| [`ImageGenerationCallSerializer`](imagegenerationcallserializer.md) | Serializer that enforces the correct wire format for `ImageGenerationCall` |
| [`LocalShellCallSerializer`](localshellcallserializer.md) | Serializer that enforces the correct wire format for `LocalShellCall` |
| [`McpToolCallSerializer`](mcptoolcallserializer.md) | Serializer that enforces the correct wire format for `McpToolCall` |
| [`MessageContentDeserializer`](messagecontentdeserializer.md) | Custom deserializer for `MessageContent` that is tolerant of plain string values |
| [`MessageDeserializer`](messagedeserializer.md) | Custom deserializer for Message that uses the 'role' field to determine which concrete subclass t… |
| [`MessageSerializer`](messageserializer.md) | Custom Jackson serializer for `Message` and its subclasses |
| [`MoveActionDeserializer`](moveactiondeserializer.md) | Custom deserializer for MoveAction to handle @JsonUnwrapped Coordinate |
| [`ResponsesApiModule`](responsesapimodule.md) | Jackson module that registers all custom serializers and deserializers for the Responses API spec… |
| [`ScrollActionDeserializer`](scrollactiondeserializer.md) | Custom deserializer for ScrollAction to handle @JsonUnwrapped Coordinate |
| [`WebSearchToolCallSerializer`](websearchtoolcallserializer.md) | Serializer that enforces the correct wire format for `WebSearchToolCall` |

## :material-database: Records

| Name | Description |
|------|-------------|
| [`JacksonJsonSchemaProducer`](jacksonjsonschemaproducer.md) | Produces JSON schemas compatible with OpenAI's function calling API |
