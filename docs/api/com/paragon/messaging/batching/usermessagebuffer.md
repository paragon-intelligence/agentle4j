# :material-code-braces: UserMessageBuffer

> This docs was updated at: 2026-03-15















`com.paragon.messaging.batching.UserMessageBuffer` &nbsp;·&nbsp; **Class**

---

Buffer de mensagens para um único usuário (thread-safe).

Usa `ConcurrentLinkedQueue` para garantir thread safety sem locks. Cada usuário tem seu
próprio buffer isolado.

**Thread Safety:** Todas operações são thread-safe.

*Since: 1.0*

## Methods

### `add`

```java
public boolean add(InboundMessage message)
```

Adiciona mensagem ao buffer.

**Parameters**

| Name | Description |
|------|-------------|
| `message` | mensagem a adicionar |

**Returns**

true se adicionada, false se buffer cheio

---

### `removeOldest`

```java
public InboundMessage removeOldest()
```

Remove mensagem mais antiga (para backpressure DROP_OLDEST).

**Returns**

mensagem removida ou null se vazio

---

### `drain`

```java
public List<InboundMessage> drain()
```

Drena todas mensagens do buffer (limpa buffer).

**Returns**

lista de mensagens (ordenadas)

---

### `size`

```java
public int size()
```

Retorna tamanho atual do buffer.

**Returns**

número de mensagens

---

### `isEmpty`

```java
public boolean isEmpty()
```

Verifica se buffer está vazio.

**Returns**

true se vazio

---

### `lastMessageTime`

```java
public Instant lastMessageTime()
```

Retorna timestamp da última mensagem recebida.

**Returns**

timestamp

---

### `setScheduledTask`

```java
public void setScheduledTask(ScheduledFuture<?> task)
```

Define tarefa agendada (cancela anterior se existir).

**Parameters**

| Name | Description |
|------|-------------|
| `task` | nova tarefa agendada |

---

### `cancelScheduledTask`

```java
public void cancelScheduledTask()
```

Cancela tarefa agendada atual.

---

### `userId`

```java
public String userId()
```

Retorna ID do usuário deste buffer.

**Returns**

user ID

