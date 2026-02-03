package com.paragon.messaging.store.history;

import com.paragon.responses.spec.Message;
import com.paragon.responses.spec.ResponseInputItem;
import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.util.List;

/**
 * Interface for storing and retrieving conversation history.
 *
 * <p>Implementations provide persistence for conversation messages, enabling
 * AI agents to maintain context across multiple user interactions. Messages
 * are stored per user ID (typically the WhatsApp phone number).</p>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Per-user message storage and retrieval</li>
 *   <li>Configurable history limits (count and age)</li>
 *   <li>Support for building AI context from history</li>
 *   <li>Automatic cleanup of expired messages</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create in-memory store
 * ConversationHistoryStore store = InMemoryConversationHistoryStore.create(100);
 *
 * // Or create Redis-backed store
 * ConversationHistoryStore store = RedisConversationHistoryStore.create(redisClient);
 *
 * // Add messages to history
 * store.addMessage(userId, Message.user("Hello!"));
 * store.addMessage(userId, Message.assistant("Hi! How can I help?"));
 *
 * // Get recent history for AI context
 * List<ResponseInputItem> history = store.getHistory(userId, 20, Duration.ofHours(24));
 *
 * // Clear user's history
 * store.clearHistory(userId);
 * }</pre>
 *
 * @author Agentle Team
 * @since 2.1
 * @see InMemoryConversationHistoryStore
 * @see RedisConversationHistoryStore
 */
public interface ConversationHistoryStore {

    /**
     * Adds a message to the user's conversation history.
     *
     * <p>Messages are stored with a timestamp for age-based filtering.
     * If the store has a maximum capacity, older messages may be evicted.</p>
     *
     * @param userId  the user's unique identifier (e.g., WhatsApp phone number)
     * @param message the message to store (UserMessage or AssistantMessage)
     */
    void addMessage(@NonNull String userId, @NonNull Message message);

    /**
     * Adds multiple messages to the user's conversation history.
     *
     * <p>Messages are added in order (first message is oldest).</p>
     *
     * @param userId   the user's unique identifier
     * @param messages the messages to store
     */
    default void addMessages(@NonNull String userId, @NonNull List<? extends Message> messages) {
        for (Message message : messages) {
            addMessage(userId, message);
        }
    }

    /**
     * Retrieves the user's conversation history.
     *
     * <p>Returns messages in chronological order (oldest first), filtered by
     * both count and age limits. Messages older than {@code maxAge} are excluded,
     * then the most recent {@code maxMessages} are returned.</p>
     *
     * @param userId      the user's unique identifier
     * @param maxMessages maximum number of messages to return
     * @param maxAge      maximum age of messages to include
     * @return list of messages as ResponseInputItem (for AgentContext)
     */
    @NonNull List<ResponseInputItem> getHistory(@NonNull String userId, int maxMessages, @NonNull Duration maxAge);

    /**
     * Retrieves the user's conversation history with default age limit (24 hours).
     *
     * @param userId      the user's unique identifier
     * @param maxMessages maximum number of messages to return
     * @return list of messages as ResponseInputItem
     */
    default @NonNull List<ResponseInputItem> getHistory(@NonNull String userId, int maxMessages) {
        return getHistory(userId, maxMessages, Duration.ofHours(24));
    }

    /**
     * Gets the number of messages stored for a user.
     *
     * @param userId the user's unique identifier
     * @return the message count
     */
    int getMessageCount(@NonNull String userId);

    /**
     * Clears all conversation history for a specific user.
     *
     * @param userId the user's unique identifier
     */
    void clearHistory(@NonNull String userId);

    /**
     * Clears all conversation history for all users.
     *
     * <p>Use with caution - this removes all stored data.</p>
     */
    void clearAll();

    /**
     * Removes messages older than the specified age from all users.
     *
     * <p>This method can be called periodically to clean up stale data.
     * Some implementations may perform this automatically.</p>
     *
     * @param maxAge maximum age of messages to retain
     * @return the number of messages removed
     */
    int cleanupExpired(@NonNull Duration maxAge);

    /**
     * Checks if any history exists for the specified user.
     *
     * @param userId the user's unique identifier
     * @return true if the user has stored messages
     */
    default boolean hasHistory(@NonNull String userId) {
        return getMessageCount(userId) > 0;
    }
}
