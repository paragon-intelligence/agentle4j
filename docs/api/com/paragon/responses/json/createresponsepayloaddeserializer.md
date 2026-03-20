# :material-code-braces: CreateResponsePayloadDeserializer

`com.paragon.responses.json.CreateResponsePayloadDeserializer` &nbsp;·&nbsp; **Class**

Extends `ValueDeserializer<CreateResponsePayload>`

---

Custom deserializer for CreateResponsePayload to handle @JsonUnwrapped OpenRouterCustomPayload.

Jackson doesn't support @JsonUnwrapped with constructor parameters, so we need a custom
deserializer to read the unwrapped OpenRouter fields directly from the JSON.
