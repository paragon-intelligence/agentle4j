# :material-code-braces: MessageBatchingService

> This docs was updated at: 2026-02-23

`com.paragon.messaging.batching.MessageBatchingService` &nbsp;·&nbsp; **Class**

---

Serviço principal de batching e rate limiting de mensagens.

Funcionalidades:

  
- Batching adaptativo com timeout e silence threshold
- Rate limiting híbrido (Token Bucket + Sliding Window)
- Deduplicação via MessageStore
- Backpressure handling configurável
- Error handling com retry exponencial
- Pre/post hooks extensíveis
- Processamento assíncrono com virtual threads

**Exemplo de uso:**

```java
MessageBatchingService service = MessageBatchingService.builder()
    .config(batchingConfig)
    .processor(messageProcessor)
    .addPreHook(loggingHook)
    .addPostHook(metricsHook)
    .build();
// No webhook do WhatsApp
service.receiveMessage(userId, messageId, content, Instant.now());
```

*Since: 1.0*

## Methods

### `builder`

```java
public static Builder builder()
```

Builder para MessageBatchingService.

---

### `receiveMessage`

```java
public void receiveMessage(String userId, InboundMessage message)
```

Recebe mensagem nova do usuário para batching.

**Thread Safety:** Este método é thread-safe.

Fluxo:

  
- Verifica deduplicação (se MessageStore configurado)
- Aplica rate limiting
- Adiciona ao buffer do usuário
- Agenda processamento adaptativo

**Parameters**

| Name | Description |
|------|-------------|
| `userId` | ID do usuário (ex: número WhatsApp) |
| `message` | mensagem completa recebida |

---

### `isDuplicate`

```java
private boolean isDuplicate(String userId, String messageId)
```

Verifica se mensagem é duplicata.

---

### `isRateLimited`

```java
private boolean isRateLimited(String userId)
```

Verifica se usuário excedeu rate limit.

---

### `scheduleAdaptiveProcessing`

```java
private void scheduleAdaptiveProcessing(String userId, UserMessageBuffer buffer)
```

Agenda processamento adaptativo do buffer.

Usa silence threshold: se usuário para de enviar por X segundos, processa imediatamente (não
espera timeout completo).

---

### `checkAndProcessIfSilent`

```java
private void checkAndProcessIfSilent(String userId, UserMessageBuffer buffer)
```

Verifica se houve silêncio e processa se sim.

---

### `processIfPending`

```java
private void processIfPending(String userId, UserMessageBuffer buffer)
```

Processa buffer se ainda houver mensagens pendentes (timeout atingido).

---

### `processBatch`

```java
private void processBatch(String userId, UserMessageBuffer buffer)
```

Processa batch de mensagens em virtual thread.

---

### `processInVirtualThread`

```java
private void processInVirtualThread(
      String userId, List<InboundMessage> messages, int retryCount)
```

Processamento real em virtual thread (com retry).

---

### `createHookContext`

```java
private HookContext createHookContext(
      String userId, List<InboundMessage> messages, int retryCount)
```

Cria HookContext para hooks.

---

### `executeHooks`

```java
private void executeHooks(List<ProcessingHook> hooks, HookContext context) throws Exception
```

Executa lista de hooks.

---

### `markAsProcessed`

```java
private void markAsProcessed(String userId, List<InboundMessage> messages)
```

Marca mensagens como processadas (deduplicação).

---

### `handleProcessingError`

```java
private void handleProcessingError(
      String userId,
      List<InboundMessage> messages,
      int retryCount,
      Exception error,
      HookContext context)
```

Trata erro de processamento (retry com exponential backoff).

---

### `handleBackpressure`

```java
private void handleBackpressure(
      String userId, UserMessageBuffer buffer, InboundMessage newMessage)
```

Trata backpressure quando buffer cheio.

---

### `handleRateLimitExceeded`

```java
private void handleRateLimitExceeded(String userId)
```

Trata rate limit excedido.

---

### `notifyUser`

```java
private void notifyUser(String userId, String message)
```

Notifica usuário (deve ser implementado pelo consumidor da API).

---

### `getOrCreateBuffer`

```java
private UserMessageBuffer getOrCreateBuffer(String userId)
```

Obtém ou cria buffer para usuário.

---

### `getRateLimiter`

```java
private HybridRateLimiter getRateLimiter(String userId)
```

Obtém ou cria rate limiter para usuário.

---

### `shutdown`

```java
public void shutdown()
```

Shutdown graceful do serviço.

---

### `getStats`

```java
public ServiceStats getStats()
```

Retorna estatísticas do serviço (útil para monitoramento).
