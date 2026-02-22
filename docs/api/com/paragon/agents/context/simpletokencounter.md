# :material-code-braces: SimpleTokenCounter

`com.paragon.agents.context.SimpleTokenCounter` &nbsp;·&nbsp; **Class**

Implements `TokenCounter`

---

A simple token counter that uses character-based estimation.

This implementation provides approximate token counts using the following heuristics:

  
- **Text:** `text.length() / 4` (approximately 4 characters per token for English)
- **Images:** Fixed costs based on `ImageDetail` level:
- `HIGH`: 765 tokens
- `LOW`: 85 tokens
- `AUTO`: 170 tokens (estimated average)
      

This counter is thread-safe and stateless.

### Usage Example

```java
TokenCounter counter = new SimpleTokenCounter();
// Count text tokens
int tokens = counter.countText("Hello, world!"); // ~3 tokens
// Count with custom chars-per-token ratio
TokenCounter customCounter = new SimpleTokenCounter(3); // 3 chars per token
```

**See Also**

- `TokenCounter`

*Since: 1.0*

## Fields

### `DEFAULT_CHARS_PER_TOKEN`

```java
public static final int DEFAULT_CHARS_PER_TOKEN = 4
```

Default characters per token ratio (approximately 4 for English).

---

### `HIGH_DETAIL_IMAGE_TOKENS`

```java
public static final int HIGH_DETAIL_IMAGE_TOKENS = 765
```

Token cost for high-detail images.

---

### `LOW_DETAIL_IMAGE_TOKENS`

```java
public static final int LOW_DETAIL_IMAGE_TOKENS = 85
```

Token cost for low-detail images.

---

### `AUTO_DETAIL_IMAGE_TOKENS`

```java
public static final int AUTO_DETAIL_IMAGE_TOKENS = 170
```

Token cost for auto-detail images (estimated average).

## Methods

### `SimpleTokenCounter`

```java
public SimpleTokenCounter()
```

Creates a SimpleTokenCounter with default settings.

---

### `SimpleTokenCounter`

```java
public SimpleTokenCounter(int charsPerToken)
```

Creates a SimpleTokenCounter with a custom characters-per-token ratio.

**Parameters**

| Name | Description |
|------|-------------|
| `charsPerToken` | the number of characters to consider as one token |

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalArgumentException` | if charsPerToken is less than 1 |

---

### `countMessage`

```java
private int countMessage(Message message)
```

Counts tokens in a message, including all content items.

---

### `countContent`

```java
private int countContent(MessageContent content)
```

Counts tokens for a single content item.

---

### `countToolOutput`

```java
private int countToolOutput(FunctionToolCallOutput output)
```

Counts tokens in a tool call output.

---

### `charsPerToken`

```java
public int charsPerToken()
```

Returns the characters-per-token ratio used by this counter.

**Returns**

the chars per token ratio

