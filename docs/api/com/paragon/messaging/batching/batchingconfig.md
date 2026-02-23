# :material-database: BatchingConfig

> This docs was updated at: 2026-02-23

`com.paragon.messaging.batching.BatchingConfig` &nbsp;·&nbsp; **Record**

---

Main configuration for `MessageBatchingService`.

Aggregates all configurations for batching, rate limiting, backpressure, error handling, TTS,
security, and persistence.

### Usage Example

```java
BatchingConfig config = BatchingConfig.builder()
    .adaptiveTimeout(Duration.ofSeconds(5))
    .silenceThreshold(Duration.ofSeconds(2))
    .maxBufferSize(50)
    .rateLimitConfig(RateLimitConfig.lenient())
    .backpressureStrategy(BackpressureStrategy.DROP_OLDEST)
    .errorHandlingStrategy(ErrorHandlingStrategy.defaults())
    .messageStore(RedisMessageStore.create(redisClient))
    .ttsConfig(TTSConfig.builder()
        .provider(elevenLabsProvider)
        .speechChance(0.3)
        .build())
    .securityConfig(SecurityConfig.strict("verify-token", "app-secret"))
    .build();
```

*Since: 2.1*

## Fields

### `BatchingConfig`

```java
public BatchingConfig
```

Canonical constructor with validation.

## Methods

### `defaults`

```java
public static BatchingConfig defaults()
```

Default configuration.

  
- Adaptive timeout: 5 seconds
- Silence: 1 second
- Buffer: 50 messages
- Rate limit: Lenient
- Backpressure: DROP_OLDEST
- Error handling: 3 retries with exponential backoff
- No TTS, no persistence, no security

**Returns**

default config

---

### `builder`

```java
public static Builder builder()
```

Creates a new builder for BatchingConfig.

**Returns**

new builder

---

### `hasMessageStore`

```java
public boolean hasMessageStore()
```

Checks if a message store is configured.

**Returns**

true if message store is present

---

### `hasSecurity`

```java
public boolean hasSecurity()
```

Checks if security configuration is present.

**Returns**

true if security config is present

---

### `hasTTS`

```java
public boolean hasTTS()
```

Checks if TTS is enabled.

**Returns**

true if TTS is configured and enabled

---

### `adaptiveTimeout`

```java
public Builder adaptiveTimeout(@NonNull Duration timeout)
```

Sets the maximum wait time before processing.

Even if messages continue arriving, after this time the buffer is processed.

**Parameters**

| Name | Description |
|------|-------------|
| `timeout` | maximum timeout |

**Returns**

this builder

---

### `silenceThreshold`

```java
public Builder silenceThreshold(@NonNull Duration threshold)
```

Sets the silence threshold before processing.

If the user stops sending for this duration, the buffer is processed immediately (doesn't
wait for full timeout).

**Parameters**

| Name | Description |
|------|-------------|
| `threshold` | silence duration |

**Returns**

this builder

---

### `maxBufferSize`

```java
public Builder maxBufferSize(int size)
```

Sets the maximum buffer size per user.

When reached, backpressureStrategy is applied.

**Parameters**

| Name | Description |
|------|-------------|
| `size` | maximum size |

**Returns**

this builder

---

### `rateLimitConfig`

```java
public Builder rateLimitConfig(@NonNull RateLimitConfig config)
```

Sets the rate limiting configuration.

**Parameters**

| Name | Description |
|------|-------------|
| `config` | rate limit config |

**Returns**

this builder

---

### `backpressureStrategy`

```java
public Builder backpressureStrategy(@NonNull BackpressureStrategy strategy)
```

Sets the backpressure strategy.

**Parameters**

| Name | Description |
|------|-------------|
| `strategy` | the strategy |

**Returns**

this builder

---

### `errorHandlingStrategy`

```java
public Builder errorHandlingStrategy(@NonNull ErrorHandlingStrategy strategy)
```

Sets the error handling strategy.

**Parameters**

| Name | Description |
|------|-------------|
| `strategy` | the strategy |

**Returns**

this builder

---

### `messageStore`

```java
public Builder messageStore(@Nullable MessageStore store)
```

Sets the message store for persistence and deduplication.

**Parameters**

| Name | Description |
|------|-------------|
| `store` | the store (can be null) |

**Returns**

this builder

---

### `ttsConfig`

```java
public Builder ttsConfig(@NonNull TTSConfig config)
```

Sets the TTS configuration.

**Parameters**

| Name | Description |
|------|-------------|
| `config` | TTS config |

**Returns**

this builder

---

### `securityConfig`

```java
public Builder securityConfig(@Nullable SecurityConfig config)
```

Sets the security configuration.

**Parameters**

| Name | Description |
|------|-------------|
| `config` | security config (can be null) |

**Returns**

this builder

---

### `build`

```java
public BatchingConfig build()
```

Builds the BatchingConfig.

**Returns**

the built configuration

