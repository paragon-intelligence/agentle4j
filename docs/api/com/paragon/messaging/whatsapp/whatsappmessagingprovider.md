# :material-code-braces: WhatsAppMessagingProvider

> This docs was updated at: 2026-02-23

`com.paragon.messaging.whatsapp.WhatsAppMessagingProvider` &nbsp;·&nbsp; **Class**

Implements `MessagingProvider`

---

Implementação do provedor WhatsApp Business Cloud API usando Virtual Threads.

**Virtual Thread Best Practices Aplicadas:**

  
- Código síncrono/bloqueante - sem CompletableFuture
- Rate limiting via Semaphore (limita recursos, não threads)
- OkHttpClient injetado para reutilização de connection pool
- Validação com Bean Validation

*Since: 2.0*

## Methods

### `createValidator`

```java
private static final Validator VALIDATOR = createValidator()
```

Validador compartilhado (thread-safe, criado uma vez).

---

### `WhatsAppMessagingProvider`

```java
public WhatsAppMessagingProvider(WhatsAppConfig config)
```

Constructor from WhatsAppConfig.

---

### `WhatsAppMessagingProvider`

```java
public WhatsAppMessagingProvider(
      String phoneNumberId,
      String accessToken,
      OkHttpClient httpClient,
      int maxConcurrentRequests)
```

Construtor com rate limiting customizável.

---

### `WhatsAppMessagingProvider`

```java
public WhatsAppMessagingProvider(
      String phoneNumberId, String accessToken, OkHttpClient httpClient)
```

Construtor com rate limiting padrão (80 requisições simultâneas).

---

### `createDefaultHttpClient`

```java
public static OkHttpClient createDefaultHttpClient()
```

Factory method para criar OkHttpClient com configurações recomendadas.

---

### `parseResponse`

```java
private MessageResponse parseResponse(Response response)
```

Processa a resposta HTTP.

---

### `extractMessageId`

```java
private String extractMessageId(String responseBody)
```

Extrai message ID da resposta (parsing simplificado).

---

### `getRateLimiterStats`

```java
public RateLimiterStats getRateLimiterStats()
```

Retorna estatísticas de uso do rate limiter.
