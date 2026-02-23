# :material-approximately-equal: TokenCounter

> This docs was updated at: 2026-02-23

`com.paragon.agents.context.TokenCounter` &nbsp;·&nbsp; **Interface**

---

Interface for counting tokens in conversation content.

Token counting is used by `ContextWindowStrategy` implementations to determine when
context exceeds the maximum token limit and needs to be managed.

Implementations should handle all content types that may appear in conversation history,
including text, images, and tool call results.

### Usage Example

```java
TokenCounter counter = new SimpleTokenCounter();
int textTokens = counter.countText("Hello, how can I help you?");
int imageTokens = counter.countImage(Image.fromUrl("https://example.com/image.jpg"));
List history = context.getHistory();
int totalTokens = counter.countTokens(history);
```

**See Also**

- `SimpleTokenCounter`
- `ContextWindowStrategy`

*Since: 1.0*

## Methods

### `countTokens`

```java
int countTokens(@NonNull ResponseInputItem item)
```

Counts tokens for a single response input item.

**Parameters**

| Name | Description |
|------|-------------|
| `item` | the input item to count tokens for |

**Returns**

the estimated token count

---

### `countTokens`

```java
default int countTokens(@NonNull List<ResponseInputItem> items)
```

Counts tokens for a list of response input items.

**Parameters**

| Name | Description |
|------|-------------|
| `items` | the input items to count tokens for |

**Returns**

the total estimated token count

---

### `countText`

```java
int countText(@NonNull String text)
```

Counts tokens for a text string.

**Parameters**

| Name | Description |
|------|-------------|
| `text` | the text to count tokens for |

**Returns**

the estimated token count

---

### `countImage`

```java
int countImage(@NonNull Image image)
```

Counts tokens for an image.

Image token costs vary based on detail level and resolution.

**Parameters**

| Name | Description |
|------|-------------|
| `image` | the image to count tokens for |

**Returns**

the estimated token count

