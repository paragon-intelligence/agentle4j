# :material-code-braces: ImageGenerationCall

`com.paragon.responses.spec.ImageGenerationCall` &nbsp;·&nbsp; **Class**

Extends `ToolCall` &nbsp;·&nbsp; Implements `Item`, `ResponseOutput`

---

An image generation request made by the model.

## Methods

### `ImageGenerationCall`

```java
public ImageGenerationCall(@NonNull String id, @NonNull String result, @NonNull String status)
```

@param id The unique ID of the image generation call.

**Parameters**

| Name | Description |
|------|-------------|
| `result` | The generated image encoded in base64. |
| `status` | The status of the image generation call. |

