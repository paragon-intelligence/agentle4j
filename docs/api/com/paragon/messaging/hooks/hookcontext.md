# :material-database: HookContext

> This docs was updated at: 2026-02-23

`com.paragon.messaging.hooks.HookContext` &nbsp;·&nbsp; **Record**

---

Contexto passado para hooks durante processamento de lote de mensagens.

Contém informações sobre o lote sendo processado e um mapa mutável para hooks compartilharem
dados entre si.

**Exemplo de uso entre hooks:**

```java
// Pre-hook armazena timestamp
ProcessingHook preHook = context -> {
    context.putMetadata("startTime", Instant.now());
    context.putMetadata("userTier", getUserTier(context.userId()));
};
// Post-hook calcula duração
ProcessingHook postHook = context -> {
    Instant start = context.getMetadata("startTime", Instant.class).orElseThrow();
    Duration elapsed = Duration.between(start, Instant.now());
    metrics.record(context.userId(), elapsed);
};
```

*Since: 1.0*

## Methods

### `create`

```java
public static HookContext create(String userId, List<InboundMessage> messages)
```

Cria contexto para primeira tentativa.

**Parameters**

| Name | Description |
|------|-------------|
| `userId` | ID do usuário |
| `messages` | mensagens a processar |

**Returns**

novo HookContext

---

### `forRetry`

```java
public static HookContext forRetry(HookContext original, int retryCount)
```

Cria contexto para retry.

**Parameters**

| Name | Description |
|------|-------------|
| `original` | contexto original |
| `retryCount` | número da tentativa |

**Returns**

novo HookContext para retry

---

### `elapsedTime`

```java
public Duration elapsedTime()
```

Tempo decorrido desde início do processamento.

**Returns**

duração desde batchStartTime

---

### `firstMessage`

```java
public InboundMessage firstMessage()
```

Primeira mensagem do lote.

**Returns**

primeira mensagem

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalStateException` | se lote vazio |

---

### `lastMessage`

```java
public InboundMessage lastMessage()
```

Última mensagem do lote.

**Returns**

última mensagem

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalStateException` | se lote vazio |

---

### `putMetadata`

```java
public void putMetadata(String key, Object value)
```

Armazena valor em metadata (thread-safe).

**Parameters**

| Name | Description |
|------|-------------|
| `key` | chave |
| `value` | valor |

---

### `getMetadata`

```java
public Optional<Object> getMetadata(String key)
```

Recupera valor de metadata.

**Parameters**

| Name | Description |
|------|-------------|
| `key` | chave |

**Returns**

Optional com valor ou empty

---

### `getMetadata`

```java
public <T> Optional<T> getMetadata(String key, Class<T> type)
```

Recupera valor tipado de metadata.

**Parameters**

| Name | Description |
|------|-------------|
| `key` | chave |
| `type` | tipo esperado |
| `<T>` | tipo genérico |

**Returns**

Optional com valor tipado ou empty

---

### `hasMetadata`

```java
public boolean hasMetadata(String key)
```

Verifica se metadata contém chave.

**Parameters**

| Name | Description |
|------|-------------|
| `key` | chave |

**Returns**

true se existe

---

### `isFirstAttempt`

```java
public boolean isFirstAttempt()
```

Verifica se é primeira tentativa (não é retry).

**Returns**

true se retryCount == 0

