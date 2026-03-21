# :material-code-braces: ImageGenerationCall

> This docs was updated at: 2026-03-21

`com.paragon.responses.spec.ImageGenerationCall` &nbsp;·&nbsp; **Class**

Extends `ToolCall` &nbsp;·&nbsp; Implements `Item`, `ResponseOutput`

---

An image generation request made by the model.

## Methods

### `ImageGenerationCall`

```java
public ImageGenerationCall(@NonNull String id, @NonNull String result, @NonNull String status)
```

**Parameters**

| Name | Description |
|------|-------------|
| `id` | The unique ID of the image generation call. |
| `result` | The generated image encoded in base64. |
| `status` | The status of the image generation call. |

