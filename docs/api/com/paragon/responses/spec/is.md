# :material-code-braces: is

> This docs was updated at: 2026-02-23

`com.paragon.responses.spec.is` &nbsp;·&nbsp; **Class**

---

## Methods

### `AssistantMessage`

```java
public AssistantMessage(
      @JsonProperty("content") @NonNull List<MessageContent> content,
      @JsonProperty("status") @Nullable InputMessageStatus status)
```

Constructs an AssistantMessage with the specified content and status.

Assistant messages are typically created by the API after response generation, with the
status reflecting the completion state of that generation process.

**Parameters**

| Name | Description |
|------|-------------|
| `content` | the model-generated response content; must not be null |
| `status` | the processing status of the response generation; indicates whether generation completed successfully, is ongoing, or failed |

---

### `role`

```java
public MessageRole role()
```

Returns the role identifier for this message type.

Assistant messages represent model-generated content from previous conversation turns and
are used to maintain conversation context.

**Returns**

the string `"assistant"`

