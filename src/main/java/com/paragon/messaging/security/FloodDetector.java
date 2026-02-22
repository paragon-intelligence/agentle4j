package com.paragon.messaging.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;
import org.jspecify.annotations.NonNull;

/**
 * Detects message flooding from individual users.
 *
 * <p>Tracks message timestamps per user and blocks users who send too many messages within a
 * configured time window.
 *
 * <h2>Features</h2>
 *
 * <ul>
 *   <li>Per-user message rate tracking
 *   <li>Configurable window and threshold
 *   <li>Thread-safe for concurrent access
 *   <li>Automatic cleanup of expired data
 * </ul>
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * // Create from security config
 * FloodDetector detector = FloodDetector.create(securityConfig);
 *
 * // Check before processing
 * if (detector.isFlooding(userId)) {
 *     log.warn("Flood detected from user: {}", userId);
 *     return; // Reject message
 * }
 *
 * // Record message (after processing or for blocking next time)
 * detector.recordMessage(userId);
 *
 * // Periodic cleanup (e.g., scheduled task)
 * detector.cleanup();
 * }</pre>
 *
 * @author Agentle Team
 * @since 2.1
 * @see SecurityConfig
 */
public final class FloodDetector {

  private final Duration window;
  private final int maxMessages;
  private final boolean enabled;
  private final ConcurrentHashMap<String, UserMessageHistory> userHistories;

  private FloodDetector(Duration window, int maxMessages, boolean enabled) {
    this.window = window;
    this.maxMessages = maxMessages;
    this.enabled = enabled;
    this.userHistories = new ConcurrentHashMap<>();
  }

  /**
   * Creates a flood detector from a security configuration.
   *
   * @param config the security configuration
   * @return a new flood detector
   */
  public static FloodDetector create(@NonNull SecurityConfig config) {
    Objects.requireNonNull(config, "config cannot be null");
    return new FloodDetector(config.floodPreventionWindow(), config.maxMessagesPerWindow(), true);
  }

  /**
   * Creates a flood detector with custom settings.
   *
   * @param window the time window for counting messages
   * @param maxMessages maximum messages allowed per window
   * @return a new flood detector
   */
  public static FloodDetector create(@NonNull Duration window, int maxMessages) {
    Objects.requireNonNull(window, "window cannot be null");
    if (window.isNegative() || window.isZero()) {
      throw new IllegalArgumentException("window must be positive");
    }
    if (maxMessages <= 0) {
      throw new IllegalArgumentException("maxMessages must be positive");
    }
    return new FloodDetector(window, maxMessages, true);
  }

  /**
   * Creates a disabled flood detector that never detects flooding.
   *
   * @return a disabled detector
   */
  public static FloodDetector disabled() {
    return new FloodDetector(Duration.ZERO, Integer.MAX_VALUE, false);
  }

  /**
   * Checks if a user is currently flooding (exceeding rate limit).
   *
   * <p>This method does NOT record a new message. Use {@link #recordMessage(String)} after
   * processing to track the message.
   *
   * @param userId the user identifier
   * @return true if the user has exceeded the rate limit
   */
  public boolean isFlooding(@NonNull String userId) {
    if (!enabled) {
      return false;
    }

    Objects.requireNonNull(userId, "userId cannot be null");

    UserMessageHistory history = userHistories.get(userId);
    if (history == null) {
      return false;
    }

    return history.getRecentCount(window) >= maxMessages;
  }

  /**
   * Records a message from a user.
   *
   * <p>Call this after processing a message to track it for rate limiting.
   *
   * @param userId the user identifier
   */
  public void recordMessage(@NonNull String userId) {
    if (!enabled) {
      return;
    }

    Objects.requireNonNull(userId, "userId cannot be null");

    userHistories.computeIfAbsent(userId, k -> new UserMessageHistory()).record();
  }

  /**
   * Checks if flooding and records a message atomically.
   *
   * <p>Returns true if the user WAS flooding before this message. The message is recorded
   * regardless.
   *
   * @param userId the user identifier
   * @return true if the user was already flooding
   */
  public boolean checkAndRecord(@NonNull String userId) {
    if (!enabled) {
      return false;
    }

    Objects.requireNonNull(userId, "userId cannot be null");

    UserMessageHistory history =
        userHistories.computeIfAbsent(userId, k -> new UserMessageHistory());

    boolean wasFlooding = history.getRecentCount(window) >= maxMessages;
    history.record();
    return wasFlooding;
  }

  /**
   * Returns the current message count for a user within the window.
   *
   * @param userId the user identifier
   * @return message count in current window
   */
  public int getMessageCount(@NonNull String userId) {
    Objects.requireNonNull(userId, "userId cannot be null");

    UserMessageHistory history = userHistories.get(userId);
    if (history == null) {
      return 0;
    }

    return history.getRecentCount(window);
  }

  /**
   * Returns the remaining messages allowed for a user.
   *
   * @param userId the user identifier
   * @return remaining messages before rate limit
   */
  public int getRemainingAllowance(@NonNull String userId) {
    int count = getMessageCount(userId);
    return Math.max(0, maxMessages - count);
  }

  /**
   * Clears all history for a specific user.
   *
   * @param userId the user identifier
   */
  public void clearUser(@NonNull String userId) {
    Objects.requireNonNull(userId, "userId cannot be null");
    userHistories.remove(userId);
  }

  /** Clears all user histories. */
  public void clearAll() {
    userHistories.clear();
  }

  /**
   * Removes expired entries from all user histories.
   *
   * <p>Call this periodically (e.g., every minute) to prevent memory growth.
   *
   * @return number of users cleaned up (had all entries removed)
   */
  public int cleanup() {
    int cleaned = 0;
    Iterator<Map.Entry<String, UserMessageHistory>> iterator = userHistories.entrySet().iterator();

    while (iterator.hasNext()) {
      Map.Entry<String, UserMessageHistory> entry = iterator.next();
      entry.getValue().removeExpired(window);

      if (entry.getValue().isEmpty()) {
        iterator.remove();
        cleaned++;
      }
    }

    return cleaned;
  }

  /**
   * Returns the number of users being tracked.
   *
   * @return tracked user count
   */
  public int getTrackedUserCount() {
    return userHistories.size();
  }

  /**
   * Returns the configured window duration.
   *
   * @return window duration
   */
  public Duration getWindow() {
    return window;
  }

  /**
   * Returns the maximum messages allowed per window.
   *
   * @return max messages
   */
  public int getMaxMessages() {
    return maxMessages;
  }

  /**
   * Checks if flood detection is enabled.
   *
   * @return true if enabled
   */
  public boolean isEnabled() {
    return enabled;
  }

  /** Thread-safe message history for a single user. */
  private static final class UserMessageHistory {
    private final Queue<Instant> timestamps = new ConcurrentLinkedQueue<>();
    private final ReentrantLock cleanupLock = new ReentrantLock();

    void record() {
      timestamps.add(Instant.now());
    }

    int getRecentCount(Duration window) {
      Instant cutoff = Instant.now().minus(window);
      int count = 0;

      for (Instant timestamp : timestamps) {
        if (timestamp.isAfter(cutoff)) {
          count++;
        }
      }

      return count;
    }

    void removeExpired(Duration window) {
      if (!cleanupLock.tryLock()) {
        return; // Skip if another thread is cleaning
      }

      try {
        Instant cutoff = Instant.now().minus(window);

        // Remove from head while expired
        Instant head;
        while ((head = timestamps.peek()) != null && head.isBefore(cutoff)) {
          timestamps.poll();
        }
      } finally {
        cleanupLock.unlock();
      }
    }

    boolean isEmpty() {
      return timestamps.isEmpty();
    }
  }
}
