# :material-database: MessageResponse

> This docs was updated at: 2026-02-23

`com.paragon.messaging.core.MessageResponse` &nbsp;·&nbsp; **Record**

---

Resposta do envio de uma mensagem.

## Methods

### `MessageResponse`

```java
public MessageResponse(String messageId, MessageStatus status, Instant timestamp)
```

Constructor without error or conversationId (successful response).

---

### `MessageResponse`

```java
public MessageResponse(String messageId, MessageStatus status, Instant timestamp, String error)
```

Constructor without conversationId.

---

### `MessageResponse`

```java
public MessageResponse(
      String messageId, MessageStatus status, Instant timestamp, Optional<String> conversationId)
```

Constructor without error (successful response with conversationId).

---

### `success`

```java
public boolean success()
```

Returns true if this response represents a successful send.

**Returns**

true if error is null

---

### `accepted`

```java
public static MessageResponse accepted(String messageId)
```

Creates an accepted response with current timestamp.

**Parameters**

| Name | Description |
|------|-------------|
| `messageId` | the message ID |

**Returns**

accepted response

---

### `failed`

```java
public static MessageResponse failed(String error)
```

Creates a failed response with current timestamp.

**Parameters**

| Name | Description |
|------|-------------|
| `error` | the error description |

**Returns**

failed response

