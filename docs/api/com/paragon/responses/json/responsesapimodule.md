# :material-code-braces: ResponsesApiModule

`com.paragon.responses.json.ResponsesApiModule` &nbsp;·&nbsp; **Class**

Extends `SimpleModule`

---

Jackson module that registers all custom serializers and deserializers for the Responses API
specification classes.

This module includes:

  
- LowercaseEnumSerializer/Deserializer for enum handling
- Custom deserializers for action classes with @JsonUnwrapped Coordinate fields
- CreateResponsePayloadDeserializer for @JsonUnwrapped OpenRouterCustomPayload
- MessageDeserializer for role-based Message subclass selection
