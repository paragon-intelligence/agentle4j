package com.paragon.messaging.store.history;

import com.paragon.responses.spec.Message;
import com.paragon.responses.spec.ResponseInputItem;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.jspecify.annotations.NonNull;

/**
 * In-memory implementation of {@link ConversationHistoryStore}.
 *
 * <p>Stores conversation history in memory with configurable per-user limits and LRU eviction.
 * Suitable for development, testing, and single-instance deployments where persistence across
 * restarts is not required.
 *
 * <h2>Features</h2>
 *
 * <ul>
 *   <li>Thread-safe with fine-grained locking per user
 *   <li>LRU eviction when per-user limit is reached
 *   <li>Timestamped messages for age-based filtering
 *   <li>Automatic cleanup of expired messages
 * </ul>
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * // Create with default settings (100 messages per user)
 * ConversationHistoryStore store = InMemoryConversationHistoryStore.create();
 *
 * // Create with custom per-user limit
 * ConversationHistoryStore store = InMemoryConversationHistoryStore.create(50);
 *
 * // Create with builder for full customization
 * ConversationHistoryStore store = InMemoryConversationHistoryStore.builder()
 *     .maxMessagesPerUser(200)
 *     .build();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 *
 * <p>This implementation is thread-safe. Operations on different users can proceed concurrently.
 * Operations on the same user are serialized using per-user read-write locks for optimal concurrent
 * read performance.
 *
 * @author Agentle Team
 * @since 2.1
 * @see ConversationHistoryStore
 */
public final class InMemoryConversationHistoryStore implements ConversationHistoryStore {

  private static final int DEFAULT_MAX_MESSAGES_PER_USER = 100;

  private final int maxMessagesPerUser;
  private final ConcurrentHashMap<String, UserHistory> histories = new ConcurrentHashMap<>();

  private InMemoryConversationHistoryStore(int maxMessagesPerUser) {
    if (maxMessagesPerUser <= 0) {
      throw new IllegalArgumentException("maxMessagesPerUser must be positive");
    }
    this.maxMessagesPerUser = maxMessagesPerUser;
  }

  /**
   * Creates a new store with default settings (100 messages per user).
   *
   * @return a new InMemoryConversationHistoryStore
   */
  public static InMemoryConversationHistoryStore create() {
    return new InMemoryConversationHistoryStore(DEFAULT_MAX_MESSAGES_PER_USER);
  }

  /**
   * Creates a new store with the specified per-user message limit.
   *
   * @param maxMessagesPerUser maximum messages to store per user
   * @return a new InMemoryConversationHistoryStore
   */
  public static InMemoryConversationHistoryStore create(int maxMessagesPerUser) {
    return new InMemoryConversationHistoryStore(maxMessagesPerUser);
  }

  /**
   * Creates a builder for customizing the store.
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

    UserHistory history =
        histories.computeIfAbsent(userId, k -> new UserHistory(maxMessagesPerUser));
    history.add(message);
  }

  @Override
  public @NonNull List<ResponseInputItem> getHistory(
      @NonNull String userId, int maxMessages, @NonNull Duration maxAge) {
    Objects.requireNonNull(userId, "userId cannot be null");
    Objects.requireNonNull(maxAge, "maxAge cannot be null");

    if (maxMessages <= 0) {
      return List.of();
    }

    UserHistory history = histories.get(userId);
    if (history == null) {
      return List.of();
    }

    return history.getRecent(maxMessages, maxAge);
  }

  @Override
  public int getMessageCount(@NonNull String userId) {
    Objects.requireNonNull(userId, "userId cannot be null");
    UserHistory history = histories.get(userId);
    return history != null ? history.size() : 0;
  }

  @Override
  public void clearHistory(@NonNull String userId) {
    Objects.requireNonNull(userId, "userId cannot be null");
    histories.remove(userId);
  }

  @Override
  public void clearAll() {
    histories.clear();
  }

  @Override
  public int cleanupExpired(@NonNull Duration maxAge) {
    Objects.requireNonNull(maxAge, "maxAge cannot be null");

    int totalRemoved = 0;
    Iterator<Map.Entry<String, UserHistory>> iterator = histories.entrySet().iterator();

    while (iterator.hasNext()) {
      Map.Entry<String, UserHistory> entry = iterator.next();
      int removed = entry.getValue().removeOlderThan(maxAge);
      totalRemoved += removed;

      // Remove user entry if all messages were cleaned up
      if (entry.getValue().isEmpty()) {
        iterator.remove();
      }
    }

    return totalRemoved;
  }

  /**
   * Returns the current number of users with stored history.
   *
   * @return the number of users
   */
  public int getUserCount() {
    return histories.size();
  }

  /**
   * Returns the maximum messages stored per user.
   *
   * @return the per-user limit
   */
  public int getMaxMessagesPerUser() {
    return maxMessagesPerUser;
  }

  /** Thread-safe per-user message history with LRU eviction. */
  private static final class UserHistory {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final LinkedList<TimestampedMessage> messages = new LinkedList<>();
    private final int maxSize;

    UserHistory(int maxSize) {
      this.maxSize = maxSize;
    }

    void add(Message message) {
      lock.writeLock().lock();
      try {
        messages.addLast(new TimestampedMessage(message, Instant.now()));

        // LRU eviction
        while (messages.size() > maxSize) {
          messages.removeFirst();
        }
      } finally {
        lock.writeLock().unlock();
      }
    }

    List<ResponseInputItem> getRecent(int maxMessages, Duration maxAge) {
      lock.readLock().lock();
      try {
        Instant cutoff = Instant.now().minus(maxAge);

        // Filter by age and get most recent
        List<ResponseInputItem> result =
            messages.stream()
                .filter(tm -> tm.timestamp.isAfter(cutoff))
                .map(tm -> (ResponseInputItem) tm.message)
                .toList();

        // Return only the most recent maxMessages
        if (result.size() <= maxMessages) {
          return result;
        }

        return result.subList(result.size() - maxMessages, result.size());
      } finally {
        lock.readLock().unlock();
      }
    }

    int size() {
      lock.readLock().lock();
      try {
        return messages.size();
      } finally {
        lock.readLock().unlock();
      }
    }

    boolean isEmpty() {
      return size() == 0;
    }

    int removeOlderThan(Duration maxAge) {
      lock.writeLock().lock();
      try {
        Instant cutoff = Instant.now().minus(maxAge);
        int initialSize = messages.size();

        messages.removeIf(tm -> tm.timestamp.isBefore(cutoff));

        return initialSize - messages.size();
      } finally {
        lock.writeLock().unlock();
      }
    }
  }

  /** Wrapper for storing messages with their creation timestamp. */
  private record TimestampedMessage(Message message, Instant timestamp) {}

  /** Builder for InMemoryConversationHistoryStore. */
  public static final class Builder {
    private int maxMessagesPerUser = DEFAULT_MAX_MESSAGES_PER_USER;

    private Builder() {}

    /**
     * Sets the maximum number of messages to store per user.
     *
     * <p>When this limit is reached, older messages are evicted (LRU).
     *
     * @param max the maximum messages per user
     * @return this builder
     */
    public Builder maxMessagesPerUser(int max) {
      this.maxMessagesPerUser = max;
      return this;
    }

    /**
     * Builds the configured InMemoryConversationHistoryStore.
     *
     * @return the store instance
     */
    public InMemoryConversationHistoryStore build() {
      return new InMemoryConversationHistoryStore(maxMessagesPerUser);
    }
  }
}
