# :material-code-braces: DefaultMessageConverter

`com.paragon.messaging.conversion.DefaultMessageConverter` &nbsp;·&nbsp; **Class**

Implements `MessageConverter`

---

Default implementation of `MessageConverter`.

Provides straightforward conversion between WhatsApp message types and the framework's
UserMessage/AssistantMessage types for AI context.

### Conversion Behavior

  
- **Text messages:** Body text extracted directly
- **Media messages:** Caption if present, otherwise type placeholder (e.g., "[Image]",
      "[Video: my_file.mp4]")
- **Interactive messages:** Button or list selection text
- **Location messages:** Coordinates description
- **System/Order messages:** Type placeholders

### Usage Example

```java
MessageConverter converter = DefaultMessageConverter.create();
// Single message conversion
InboundMessage inbound = ...;
UserMessage user = converter.toUserMessage(inbound);
// Batch conversion (combines into single message)
List batch = ...;
UserMessage combined = converter.toUserMessage(batch);
// With custom message separator
MessageConverter custom = DefaultMessageConverter.builder()
    .batchSeparator(" | ")
    .build();
```

**See Also**

- `MessageConverter`

*Since: 2.1*

## Methods

### `create`

```java
public static DefaultMessageConverter create()
```

Creates a new DefaultMessageConverter with default settings.

Uses newline as the batch separator when combining multiple messages.

**Returns**

a new converter instance

---

### `builder`

```java
public static Builder builder()
```

Creates a builder for customizing the converter.

**Returns**

a new builder

---

### `batchSeparator`

```java
public Builder batchSeparator(@NonNull String separator)
```

Sets the separator used when combining multiple messages into one.

Default is newline (`"\n"`).

**Parameters**

| Name | Description |
|------|-------------|
| `separator` | the batch separator |

**Returns**

this builder

---

### `build`

```java
public DefaultMessageConverter build()
```

Builds the configured DefaultMessageConverter.

**Returns**

the converter instance

