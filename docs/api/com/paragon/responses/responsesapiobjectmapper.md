# :material-code-braces: ResponsesApiObjectMapper

> This docs was updated at: 2026-02-23

`com.paragon.responses.ResponsesApiObjectMapper` &nbsp;·&nbsp; **Class**

Extends `ObjectMapper`

---

Factory class for creating a centrally configured Jackson ObjectMapper for the Responses API
specification classes. This ObjectMapper is configured with:

  
- Snake case naming strategy for JSON field names
- Null value exclusion from JSON output
- Unknown property tolerance during deserialization
- Custom serializers and deserializers for special types

## Methods

### `create`

```java
public static ObjectMapper create()
```

Creates and configures a new ObjectMapper instance with all necessary settings for Responses
API serialization and deserialization.

**Returns**

A fully configured ObjectMapper instance

