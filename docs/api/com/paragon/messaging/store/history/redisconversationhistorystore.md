# :material-code-braces: RedisConversationHistoryStore

> This docs was updated at: 2026-02-23

`com.paragon.messaging.store.history.RedisConversationHistoryStore` &nbsp;·&nbsp; **Class**

Implements `ConversationHistoryStore`

---

Redis-backed implementation of `ConversationHistoryStore`.

Provides persistent conversation history storage using Redis sorted sets. Each user's history
is stored in a separate sorted set with message timestamps as scores for efficient time-based
queries.

### Redis Data Structure

  
- **Key:** `{keyPrefix`:history:{userId}}
- **Type:** Sorted Set (ZSET)
- **Score:** Message timestamp (epoch milliseconds)
- **Value:** JSON-serialized message

### Features
- Persistent storage across application restarts
- Automatic TTL-based expiration
- Efficient time-range queries using sorted set scores
- Configurable key prefix for multi-tenant deployments
- Thread-safe (Redis operations are atomic)

### Usage Example

```java
// Using Lettuce Redis client (included in Spring Data Redis)
RedisClient redisClient = RedisClient.create("redis://localhost:6379");
StatefulRedisConnection connection = redisClient.connect();
RedisCommands commands = connection.sync();
// Create store with Lettuce commands wrapper
RedisConversationHistoryStore store = RedisConversationHistoryStore.builder()
    .redisOperations(new LettuceRedisOperations(commands))
    .keyPrefix("whatsapp")
    .defaultTtl(Duration.ofDays(7))
    .maxMessagesPerUser(100)
    .build();
// Or use Spring Data Redis
RedisConversationHistoryStore store = RedisConversationHistoryStore.builder()
    .redisOperations(new SpringDataRedisOperations(stringRedisTemplate))
    .build();
```

### Dependencies

This class requires a Redis client. Supported options:
- Lettuce (recommended, included in Spring Boot)
- Spring Data Redis (RedisTemplate)
- Jedis

**See Also**

- `ConversationHistoryStore`
- `RedisOperations`

*Since: 2.1*

## Methods

### `builder`

```java
public static Builder builder()
```

Creates a builder for customizing the Redis store.

**Returns**

a new builder

---

### `zadd`

```java
void zadd(String key, double score, String member)
```

Adds a member to a sorted set with the given score.

---

### `zcard`

```java
long zcard(String key)
```

Gets the number of members in a sorted set.

---

### `zrangeByScore`

```java
List<String> zrangeByScore(String key, double min, double max)
```

Gets members within a score range, ordered by score ascending.

---

### `zremrangeByScore`

```java
long zremrangeByScore(String key, double min, double max)
```

Removes members with scores in the given range.

---

### `zremrangeByRank`

```java
long zremrangeByRank(String key, long start, long stop)
```

Removes members by rank range.

---

### `expire`

```java
void expire(String key, Duration ttl)
```

Sets the expiration time for a key.

---

### `del`

```java
void del(String... keys)
```

Deletes one or more keys.

---

### `keys`

```java
Set<String> keys(String pattern)
```

Finds all keys matching the given pattern.

---

### `redisOperations`

```java
public Builder redisOperations(@NonNull RedisOperations operations)
```

Sets the Redis operations implementation.

This is required. Implement `RedisOperations` to adapt your preferred Redis client.

**Parameters**

| Name | Description |
|------|-------------|
| `operations` | the Redis operations |

**Returns**

this builder

---

### `objectMapper`

```java
public Builder objectMapper(@Nullable ObjectMapper objectMapper)
```

Sets the ObjectMapper for JSON serialization.

If not set, a default ObjectMapper with Message type handling will be created.

**Parameters**

| Name | Description |
|------|-------------|
| `objectMapper` | the object mapper |

**Returns**

this builder

---

### `keyPrefix`

```java
public Builder keyPrefix(@NonNull String prefix)
```

Sets the key prefix for Redis keys.

Default is "conversation". Keys will be formatted as: `{prefix`:history:{userId}}

**Parameters**

| Name | Description |
|------|-------------|
| `prefix` | the key prefix |

**Returns**

this builder

---

### `defaultTtl`

```java
public Builder defaultTtl(@NonNull Duration ttl)
```

Sets the default TTL for conversation history.

Default is 7 days. This TTL is refreshed each time a message is added to a user's history.

**Parameters**

| Name | Description |
|------|-------------|
| `ttl` | the time-to-live duration |

**Returns**

this builder

---

### `maxMessagesPerUser`

```java
public Builder maxMessagesPerUser(int max)
```

Sets the maximum messages to store per user.

Default is 100. When exceeded, oldest messages are removed.

**Parameters**

| Name | Description |
|------|-------------|
| `max` | the maximum messages per user |

**Returns**

this builder

---

### `build`

```java
public RedisConversationHistoryStore build()
```

Builds the configured RedisConversationHistoryStore.

**Returns**

the store instance

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalStateException` | if redisOperations is not set |

