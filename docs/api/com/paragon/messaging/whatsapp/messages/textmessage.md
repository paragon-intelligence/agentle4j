# :material-database: TextMessage

`com.paragon.messaging.whatsapp.messages.TextMessage` &nbsp;·&nbsp; **Record**

---

Represents a simple text message for outbound delivery.

Uses Bean Validation (Hibernate Validator) for declarative field validation, ensuring the
message meets API requirements before sending.

### Usage Example

```java
// Simple text message
TextMessage message = new TextMessage("Hello, World!");
// With URL preview enabled
TextMessage message = new TextMessage("Check out https://example.com", true);
// As a reply to another message
TextMessage reply = TextMessage.builder()
    .body("Thanks for your message!")
    .replyTo("wamid.xyz123")
    .build();
```

*Since: 2.0*

## Fields

### `MAX_BODY_LENGTH`

```java
public static final int MAX_BODY_LENGTH = 4096
```

Maximum allowed length for message body.

## Methods

### `TextMessage`

```java
public TextMessage(String body)
```

Convenience constructor without URL preview or reply context.

**Parameters**

| Name | Description |
|------|-------------|
| `body` | message content |

---

### `TextMessage`

```java
public TextMessage(String body, boolean previewUrl)
```

Convenience constructor with URL preview but no reply context.

**Parameters**

| Name | Description |
|------|-------------|
| `body` | message content |
| `previewUrl` | whether to generate URL previews |

---

### `builder`

```java
public static Builder builder()
```

Creates a builder for TextMessage.

**Returns**

new builder

---

### `body`

```java
public Builder body(String body)
```

Sets the message body text.

**Parameters**

| Name | Description |
|------|-------------|
| `body` | the message content |

**Returns**

this builder

---

### `previewUrl`

```java
public Builder previewUrl(boolean previewUrl)
```

Sets whether to generate URL previews.

**Parameters**

| Name | Description |
|------|-------------|
| `previewUrl` | true to enable URL previews |

**Returns**

this builder

---

### `enablePreviewUrl`

```java
public Builder enablePreviewUrl()
```

Enables URL preview generation.

**Returns**

this builder

---

### `disablePreviewUrl`

```java
public Builder disablePreviewUrl()
```

Disables URL preview generation.

**Returns**

this builder

---

### `replyTo`

```java
public Builder replyTo(@Nullable String messageId)
```

Sets the message ID to reply to.

When set, the message will appear as a reply/quote in WhatsApp.

**Parameters**

| Name | Description |
|------|-------------|
| `messageId` | the WhatsApp message ID to reply to |

**Returns**

this builder

---

### `build`

```java
public TextMessage build()
```

Builds the TextMessage.

Note: Validation will be executed when the object is passed to MessagingProvider via
the @Valid annotation.

**Returns**

the built TextMessage

