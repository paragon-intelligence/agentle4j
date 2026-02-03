package com.paragon.messaging.whatsapp.history;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.responses.spec.Message;
import com.paragon.responses.spec.ResponseInputItem;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Redis-backed implementation of {@link ConversationHistoryStore}.
 *
 * <p>Provides persistent conversation history storage using Redis sorted sets.
 * Each user's history is stored in a separate sorted set with message timestamps
 * as scores for efficient time-based queries.</p>
 *
 * <h2>Redis Data Structure</h2>
 * <ul>
 *   <li><b>Key:</b> {@code {keyPrefix}:history:{userId}}</li>
 *   <li><b>Type:</b> Sorted Set (ZSET)</li>
 *   <li><b>Score:</b> Message timestamp (epoch milliseconds)</li>
 *   <li><b>Value:</b> JSON-serialized message</li>
 * </ul>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Persistent storage across application restarts</li>
 *   <li>Automatic TTL-based expiration</li>
 *   <li>Efficient time-range queries using sorted set scores</li>
 *   <li>Configurable key prefix for multi-tenant deployments</li>
 *   <li>Thread-safe (Redis operations are atomic)</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Using Lettuce Redis client (included in Spring Data Redis)
 * RedisClient redisClient = RedisClient.create("redis://localhost:6379");
 * StatefulRedisConnection<String, String> connection = redisClient.connect();
 * RedisCommands<String, String> commands = connection.sync();
 *
 * // Create store with Lettuce commands wrapper
 * RedisConversationHistoryStore store = RedisConversationHistoryStore.builder()
 *     .redisOperations(new LettuceRedisOperations(commands))
 *     .keyPrefix("whatsapp")
 *     .defaultTtl(Duration.ofDays(7))
 *     .maxMessagesPerUser(100)
 *     .build();
 *
 * // Or use Spring Data Redis
 * RedisConversationHistoryStore store = RedisConversationHistoryStore.builder()
 *     .redisOperations(new SpringDataRedisOperations(stringRedisTemplate))
 *     .build();
 * }</pre>
 *
 * <h2>Dependencies</h2>
 * <p>This class requires a Redis client. Supported options:</p>
 * <ul>
 *   <li>Lettuce (recommended, included in Spring Boot)</li>
 *   <li>Spring Data Redis (RedisTemplate)</li>
 *   <li>Jedis</li>
 * </ul>
 *
 * @author Agentle Team
 * @since 2.1
 * @see ConversationHistoryStore
 * @see RedisOperations
 */
public final class RedisConversationHistoryStore implements ConversationHistoryStore {

    private static final String DEFAULT_KEY_PREFIX = "conversation";
    private static final Duration DEFAULT_TTL = Duration.ofDays(7);
    private static final int DEFAULT_MAX_MESSAGES_PER_USER = 100;

    private final RedisOperations redisOperations;
    private final ObjectMapper objectMapper;
    private final String keyPrefix;
    private final Duration defaultTtl;
    private final int maxMessagesPerUser;

    private RedisConversationHistoryStore(
            @NonNull RedisOperations redisOperations,
            @NonNull ObjectMapper objectMapper,
            @NonNull String keyPrefix,
            @NonNull Duration defaultTtl,
            int maxMessagesPerUser) {
        this.redisOperations = Objects.requireNonNull(redisOperations, "redisOperations cannot be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper cannot be null");
        this.keyPrefix = Objects.requireNonNull(keyPrefix, "keyPrefix cannot be null");
        this.defaultTtl = Objects.requireNonNull(defaultTtl, "defaultTtl cannot be null");
        if (maxMessagesPerUser <= 0) {
            throw new IllegalArgumentException("maxMessagesPerUser must be positive");
        }
        this.maxMessagesPerUser = maxMessagesPerUser;
    }

    /**
     * Creates a builder for customizing the Redis store.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void addMessage(@NonNull String userId, @NonNull Message message) {
        Objects.requireNonNull(userId, "userId cannot be null");
        Objects.requireNonNull(message, "message cannot be null");

        String key = buildKey(userId);
        long timestamp = Instant.now().toEpochMilli();

        try {
            String json = objectMapper.writeValueAsString(new StoredMessage(message, timestamp));

            // Add to sorted set with timestamp as score
            redisOperations.zadd(key, timestamp, json);

            // Trim to max size (keep most recent)
            long count = redisOperations.zcard(key);
            if (count > maxMessagesPerUser) {
                redisOperations.zremrangeByRank(key, 0, count - maxMessagesPerUser - 1);
            }

            // Refresh TTL
            redisOperations.expire(key, defaultTtl);

        } catch (JsonProcessingException e) {
            throw new ConversationHistoryException("Failed to serialize message", e);
        }
    }

    @Override
    public @NonNull List<ResponseInputItem> getHistory(
            @NonNull String userId,
            int maxMessages,
            @NonNull Duration maxAge) {
        Objects.requireNonNull(userId, "userId cannot be null");
        Objects.requireNonNull(maxAge, "maxAge cannot be null");

        if (maxMessages <= 0) {
            return List.of();
        }

        String key = buildKey(userId);
        long minScore = Instant.now().minus(maxAge).toEpochMilli();

        // Get messages within time range, ordered by score (timestamp)
        List<String> jsonMessages = redisOperations.zrangeByScore(key, minScore, Long.MAX_VALUE);

        if (jsonMessages.isEmpty()) {
            return List.of();
        }

        // Deserialize and convert to ResponseInputItem
        List<ResponseInputItem> result = new ArrayList<>();
        for (String json : jsonMessages) {
            try {
                StoredMessage stored = objectMapper.readValue(json, StoredMessage.class);
                result.add(stored.message());
            } catch (JsonProcessingException e) {
                // Log and skip corrupted entries
                // In production, consider logging this
            }
        }

        // Return only most recent maxMessages
        if (result.size() <= maxMessages) {
            return result;
        }

        return result.subList(result.size() - maxMessages, result.size());
    }

    @Override
    public int getMessageCount(@NonNull String userId) {
        Objects.requireNonNull(userId, "userId cannot be null");
        return (int) redisOperations.zcard(buildKey(userId));
    }

    @Override
    public void clearHistory(@NonNull String userId) {
        Objects.requireNonNull(userId, "userId cannot be null");
        redisOperations.del(buildKey(userId));
    }

    @Override
    public void clearAll() {
        // Get all keys matching the pattern
        Set<String> keys = redisOperations.keys(keyPrefix + ":history:*");
        if (!keys.isEmpty()) {
            redisOperations.del(keys.toArray(new String[0]));
        }
    }

    @Override
    public int cleanupExpired(@NonNull Duration maxAge) {
        Objects.requireNonNull(maxAge, "maxAge cannot be null");

        long maxScore = Instant.now().minus(maxAge).toEpochMilli();
        int totalRemoved = 0;

        // Get all user keys
        Set<String> keys = redisOperations.keys(keyPrefix + ":history:*");
        for (String key : keys) {
            long removed = redisOperations.zremrangeByScore(key, 0, maxScore);
            totalRemoved += (int) removed;

            // Remove empty keys
            if (redisOperations.zcard(key) == 0) {
                redisOperations.del(key);
            }
        }

        return totalRemoved;
    }

    private String buildKey(String userId) {
        return keyPrefix + ":history:" + userId;
    }

    /**
     * Wrapper record for storing messages with their timestamp.
     */
    private record StoredMessage(Message message, long timestamp) {}

    /**
     * Exception thrown when conversation history operations fail.
     */
    public static class ConversationHistoryException extends RuntimeException {
        public ConversationHistoryException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Abstraction over Redis client operations.
     *
     * <p>Implement this interface to adapt your preferred Redis client:</p>
     * <ul>
     *   <li>Lettuce (recommended)</li>
     *   <li>Spring Data Redis (RedisTemplate)</li>
     *   <li>Jedis</li>
     * </ul>
     *
     * <h2>Example Implementation (Lettuce)</h2>
     * <pre>{@code
     * public class LettuceRedisOperations implements RedisOperations {
     *     private final RedisCommands<String, String> commands;
     *
     *     public LettuceRedisOperations(RedisCommands<String, String> commands) {
     *         this.commands = commands;
     *     }
     *
     *     @Override
     *     public void zadd(String key, double score, String member) {
     *         commands.zadd(key, score, member);
     *     }
     *
     *     // ... implement other methods
     * }
     * }</pre>
     */
    public interface RedisOperations {
        /**
         * Adds a member to a sorted set with the given score.
         */
        void zadd(String key, double score, String member);

        /**
         * Gets the number of members in a sorted set.
         */
        long zcard(String key);

        /**
         * Gets members within a score range, ordered by score ascending.
         */
        List<String> zrangeByScore(String key, double min, double max);

        /**
         * Removes members with scores in the given range.
         */
        long zremrangeByScore(String key, double min, double max);

        /**
         * Removes members by rank range.
         */
        long zremrangeByRank(String key, long start, long stop);

        /**
         * Sets the expiration time for a key.
         */
        void expire(String key, Duration ttl);

        /**
         * Deletes one or more keys.
         */
        void del(String... keys);

        /**
         * Finds all keys matching the given pattern.
         */
        Set<String> keys(String pattern);
    }

    /**
     * Builder for RedisConversationHistoryStore.
     */
    public static final class Builder {
        private RedisOperations redisOperations;
        private ObjectMapper objectMapper;
        private String keyPrefix = DEFAULT_KEY_PREFIX;
        private Duration defaultTtl = DEFAULT_TTL;
        private int maxMessagesPerUser = DEFAULT_MAX_MESSAGES_PER_USER;

        private Builder() {}

        /**
         * Sets the Redis operations implementation.
         *
         * <p>This is required. Implement {@link RedisOperations} to adapt
         * your preferred Redis client.</p>
         *
         * @param operations the Redis operations
         * @return this builder
         */
        public Builder redisOperations(@NonNull RedisOperations operations) {
            this.redisOperations = operations;
            return this;
        }

        /**
         * Sets the ObjectMapper for JSON serialization.
         *
         * <p>If not set, a default ObjectMapper with Message type handling
         * will be created.</p>
         *
         * @param objectMapper the object mapper
         * @return this builder
         */
        public Builder objectMapper(@Nullable ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return this;
        }

        /**
         * Sets the key prefix for Redis keys.
         *
         * <p>Default is "conversation". Keys will be formatted as:
         * {@code {prefix}:history:{userId}}</p>
         *
         * @param prefix the key prefix
         * @return this builder
         */
        public Builder keyPrefix(@NonNull String prefix) {
            this.keyPrefix = prefix;
            return this;
        }

        /**
         * Sets the default TTL for conversation history.
         *
         * <p>Default is 7 days. This TTL is refreshed each time a message
         * is added to a user's history.</p>
         *
         * @param ttl the time-to-live duration
         * @return this builder
         */
        public Builder defaultTtl(@NonNull Duration ttl) {
            this.defaultTtl = ttl;
            return this;
        }

        /**
         * Sets the maximum messages to store per user.
         *
         * <p>Default is 100. When exceeded, oldest messages are removed.</p>
         *
         * @param max the maximum messages per user
         * @return this builder
         */
        public Builder maxMessagesPerUser(int max) {
            this.maxMessagesPerUser = max;
            return this;
        }

        /**
         * Builds the configured RedisConversationHistoryStore.
         *
         * @return the store instance
         * @throws IllegalStateException if redisOperations is not set
         */
        public RedisConversationHistoryStore build() {
            if (redisOperations == null) {
                throw new IllegalStateException("redisOperations must be set");
            }

            ObjectMapper mapper = objectMapper;
            if (mapper == null) {
                mapper = createDefaultObjectMapper();
            }

            return new RedisConversationHistoryStore(
                    redisOperations,
                    mapper,
                    keyPrefix,
                    defaultTtl,
                    maxMessagesPerUser
            );
        }

        private ObjectMapper createDefaultObjectMapper() {
            // Create ObjectMapper configured for Message polymorphism
            // The Message class already has Jackson annotations
            return new ObjectMapper()
                    .findAndRegisterModules();
        }
    }
}
