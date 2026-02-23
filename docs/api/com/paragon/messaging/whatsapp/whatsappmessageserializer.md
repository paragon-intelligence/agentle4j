# :material-code-braces: WhatsAppMessageSerializer

> This docs was updated at: 2026-02-23

`com.paragon.messaging.whatsapp.WhatsAppMessageSerializer` &nbsp;·&nbsp; **Class**

---

Serializador que converte mensagens genéricas para o formato JSON da API WhatsApp.

Esta implementação demonstra o padrão de como transformar os modelos genéricos em payloads
específicos do provedor.

*Since: 1.0*

## Methods

### `serialize`

```java
public String serialize(Recipient recipient, OutboundMessage message)
```

Serializa uma mensagem para JSON no formato esperado pela API WhatsApp.

**Parameters**

| Name | Description |
|------|-------------|
| `recipient` | destinatário |
| `message` | mensagem a ser serializada |

**Returns**

JSON string

---

### `serializeTextMessage`

```java
private String serializeTextMessage(Recipient recipient, TextMessage message)
```

Serializa mensagem de texto.

---

### `serializeMediaMessage`

```java
private String serializeMediaMessage(Recipient recipient, MediaMessage media)
```

Serializa mensagem de mídia.

---

### `serializeMediaSource`

```java
private String serializeMediaSource(MediaMessage.MediaSource source)
```

Serializa a fonte de mídia (URL ou ID).

---

### `serializeTemplateMessage`

```java
private String serializeTemplateMessage(Recipient recipient, TemplateMessage template)
```

Serializa mensagem de template.

---

### `serializeInteractiveMessage`

```java
private String serializeInteractiveMessage(Recipient recipient, InteractiveMessage interactive)
```

Serializa mensagem interativa.

---

### `serializeLocationMessage`

```java
private String serializeLocationMessage(Recipient recipient, LocationMessage location)
```

Serializa mensagem de localização.

---

### `serializeContactMessage`

```java
private String serializeContactMessage(Recipient recipient, ContactMessage contact)
```

Serializa mensagem de contato.

---

### `serializeReactionMessage`

```java
private String serializeReactionMessage(Recipient recipient, ReactionMessage reaction)
```

Serializa mensagem de reação.

---

### `escapeJson`

```java
private String escapeJson(String value)
```

Escapa caracteres especiais para JSON.
