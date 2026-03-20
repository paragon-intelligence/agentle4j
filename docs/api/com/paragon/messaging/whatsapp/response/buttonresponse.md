# :material-database: ButtonResponse

`com.paragon.messaging.whatsapp.response.ButtonResponse` &nbsp;·&nbsp; **Record**

---

Button-based response for structured AI output.

Represents an interactive message with quick reply buttons (max 3).

### Usage Example

```java
// Simple button response
ButtonResponse response = ButtonResponse.builder()
    .body("How would you like to proceed?")
    .addButton("confirm", "Confirm")
    .addButton("cancel", "Cancel")
    .build();
// With header, footer, and context
ButtonResponse response = ButtonResponse.builder()
    .header("Order Confirmation")
    .body("Your order is ready. Confirm to proceed with payment.")
    .footer("Tap a button to continue")
    .addButton("pay", "Pay Now")
    .addButton("later", "Pay Later")
    .addButton("cancel", "Cancel Order")
    .replyTo("wamid.xyz123")
    .build();
```

*Since: 2.1*

## Methods

### `builder`

```java
public static Builder builder()
```

Creates a builder for ButtonResponse.

**Returns**

new builder

---

### `body`

```java
public Builder body(@NonNull String body)
```

Sets the message body text.

**Parameters**

| Name | Description |
|------|-------------|
| `body` | the body text |

**Returns**

this builder

---

### `header`

```java
public Builder header(@Nullable String header)
```

Sets the optional header text.

**Parameters**

| Name | Description |
|------|-------------|
| `header` | the header text |

**Returns**

this builder

---

### `footer`

```java
public Builder footer(@Nullable String footer)
```

Sets the optional footer text.

**Parameters**

| Name | Description |
|------|-------------|
| `footer` | the footer text |

**Returns**

this builder

---

### `addButton`

```java
public Builder addButton(@NonNull String id, @NonNull String title)
```

Adds a button to the response.

**Parameters**

| Name | Description |
|------|-------------|
| `id` | unique button identifier |
| `title` | button display text |

**Returns**

this builder

---

### `addButton`

```java
public Builder addButton(@NonNull Button button)
```

Adds a button to the response.

**Parameters**

| Name | Description |
|------|-------------|
| `button` | the button to add |

**Returns**

this builder

---

### `replyTo`

```java
public Builder replyTo(@NonNull String messageId)
```

Sets the message ID to reply to.

**Parameters**

| Name | Description |
|------|-------------|
| `messageId` | the message ID |

**Returns**

this builder

---

### `reactTo`

```java
public Builder reactTo(@NonNull String messageId, @NonNull String emoji)
```

Sets the reaction for this response.

**Parameters**

| Name | Description |
|------|-------------|
| `messageId` | the message ID to react to |
| `emoji` | the reaction emoji |

**Returns**

this builder

---

### `build`

```java
public ButtonResponse build()
```

Builds the ButtonResponse.

**Returns**

the built response

