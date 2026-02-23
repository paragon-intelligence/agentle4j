# :material-database: ErrorHandlingStrategy

> This docs was updated at: 2026-02-23

`com.paragon.messaging.error.ErrorHandlingStrategy` &nbsp;·&nbsp; **Record**

---

Configuração para tratamento de erros durante processamento.

Controla retry, backoff exponencial, dead letter queue e notificações ao usuário quando
processamento falha.

**Exemplo com Retry:**

```java
ErrorHandlingStrategy strategy = ErrorHandlingStrategy.builder()
    .maxRetries(3)
    .retryDelay(Duration.ofSeconds(2))
    .exponentialBackoff(true)
    .notifyUserOnFailure(true)
    .build();
```

**Exemplo com DLQ:**

```java
ErrorHandlingStrategy strategy = ErrorHandlingStrategy.builder()
    .maxRetries(0)
    .deadLetterHandler((userId, messages) -> {
        dlqService.store(userId, messages);
    })
    .notifyUserOnFailure(true)
    .build();
```

*Since: 1.0*

## Methods

### `defaults`

```java
public static ErrorHandlingStrategy defaults()
```

Estratégia padrão.

  
- 3 retries com exponential backoff
- 2 segundos de delay inicial
- Notifica usuário em falha permanente

**Returns**

estratégia padrão

---

### `noRetry`

```java
public static ErrorHandlingStrategy noRetry()
```

Sem retry (fail fast).

**Returns**

estratégia sem retry

---

### `calculateDelay`

```java
public Duration calculateDelay(int attemptNumber)
```

Calcula delay para tentativa específica.

**Parameters**

| Name | Description |
|------|-------------|
| `attemptNumber` | número da tentativa (1-based) |

**Returns**

duração do delay

---

### `getNotificationMessage`

```java
public String getNotificationMessage()
```

Retorna mensagem de notificação ou padrão.

**Returns**

mensagem de notificação

---

### `hasDLQHandler`

```java
public boolean hasDLQHandler()
```

Verifica se tem handler de dead letter queue configurado.

**Returns**

true se DLQ handler está presente

