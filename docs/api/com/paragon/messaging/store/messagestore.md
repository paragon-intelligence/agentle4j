# :material-approximately-equal: MessageStore

> This docs was updated at: 2026-02-23

`com.paragon.messaging.store.MessageStore` &nbsp;·&nbsp; **Interface**

---

Interface para persistência e deduplicação de mensagens.

Implementações podem usar:

  
- In-memory (LRU cache)
- Redis
- Database SQL
- NoSQL

*Since: 1.0*

## Methods

### `noOp`

```java
static MessageStore noOp()
```

Store no-op.

**Returns**

store vazio

---

### `store`

```java
void store(String userId, OutboundMessage message)
```

Armazena mensagem no buffer do usuário.

**Parameters**

| Name | Description |
|------|-------------|
| `userId` | ID do usuário |
| `message` | mensagem |

---

### `retrieve`

```java
List<OutboundMessage> retrieve(String userId)
```

Recupera todas mensagens do usuário.

**Parameters**

| Name | Description |
|------|-------------|
| `userId` | ID do usuário |

**Returns**

lista de mensagens ordenadas

---

### `remove`

```java
void remove(String userId)
```

Remove todas mensagens do usuário.

**Parameters**

| Name | Description |
|------|-------------|
| `userId` | ID do usuário |

---

### `hasProcessed`

```java
boolean hasProcessed(String userId, String messageId)
```

Verifica se mensagem já foi processada.

**Parameters**

| Name | Description |
|------|-------------|
| `userId` | ID do usuário |
| `messageId` | ID da mensagem |

**Returns**

true se já processada

---

### `markProcessed`

```java
void markProcessed(String userId, String messageId)
```

Marca mensagem como processada.

**Parameters**

| Name | Description |
|------|-------------|
| `userId` | ID do usuário |
| `messageId` | ID da mensagem |

