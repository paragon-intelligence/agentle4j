# :material-approximately-equal: WebhookEvent

`com.paragon.messaging.whatsapp.WebhookEvent` &nbsp;·&nbsp; **Interface**

---

Interface selada para eventos de webhook.

*Since: 1.0*

## Methods

### `type`

```java
WebhookEventType type()
```

Retorna o tipo do evento.

---

### `timestamp`

```java
Instant timestamp()
```

Retorna o timestamp do evento.

---

### `isDelivered`

```java
public boolean isDelivered()
```

Verifica se a mensagem foi entregue com sucesso.

---

### `isRead`

```java
public boolean isRead()
```

Verifica se a mensagem foi lida.

---

### `isFailed`

```java
public boolean isFailed()
```

Verifica se houve falha.
