# :material-code-braces: OutputMessage

`com.paragon.responses.spec.OutputMessage` &nbsp;·&nbsp; **Class**

Extends `AssistantMessage` &nbsp;·&nbsp; Implements `Item`, `ResponseOutput`

---

Represents an assistant output message with optional structured output support.

OutputMessage extends `AssistantMessage` to represent API-returned assistant responses
with additional metadata. While similar to AssistantMessage in content, OutputMessage includes a
unique identifier and optional structured output that can be extracted when the model is
configured to generate responses following a specific schema.

The parsed field contains structured output when the API is configured to generate responses
in a specific format (JSON schema, function calls, etc.). When structured output is not used, the
parsed field will be `null`, and the generic type parameter is typically `Void`.

**See Also**

- `AssistantMessage`
- `MessageContent`
- `InputMessageStatus`

## Methods

### `OutputMessage`

```java
public OutputMessage(
      @JsonProperty("content") @NonNull List<MessageContent> content,
      @JsonProperty("id") @NonNull String id,
      @JsonProperty("status") @NonNull InputMessageStatus status,
      @JsonProperty("parsed") @Nullable T parsed)
```

Constructs an OutputMessage with all fields.

This constructor is used by Jackson for deserialization.

**Parameters**

| Name | Description |
|------|-------------|
| `content` | the raw message content; must not be null |
| `id` | the unique identifier for this message; must not be null |
| `status` | the processing status of the message; must not be null |
| `parsed` | the structured output extracted from the response, or `null` when structured output is not configured |

---

### `id`

```java
public String id()
```

Returns the unique identifier for this message.

The ID is assigned by the API and can be used to reference this specific message in
subsequent operations or for tracking purposes.

**Returns**

the message identifier

---

### `parsed`

```java
public @Nullable T parsed()
```

Returns the structured output extracted from this message.

When the API is configured to generate responses following a specific schema (such as JSON
schema or function definitions), this field contains the parsed structured output. If
structured output is not configured or not applicable, this method returns `null`.

Common use cases include:

  
- Extracting structured data from model responses
- Getting function call results in a typed format
- Parsing JSON schema-validated output

**Returns**

the structured output, or `null` if not available

