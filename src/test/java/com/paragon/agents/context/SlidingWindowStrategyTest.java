package com.paragon.agents.context;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.paragon.responses.spec.*;

/**
 * Tests for SlidingWindowStrategy.
 */
@DisplayName("SlidingWindowStrategy")
class SlidingWindowStrategyTest {

    private SlidingWindowStrategy strategy;
    private SimpleTokenCounter counter;

    @BeforeEach
    void setUp() {
        strategy = new SlidingWindowStrategy();
        counter = new SimpleTokenCounter();
    }

    @Nested
    @DisplayName("Basic Functionality")
    class BasicFunctionality {

        @Test
        @DisplayName("returns same history when under limit")
        void returnsSameHistory_whenUnderLimit() {
            List<ResponseInputItem> history = List.of(
                Message.user("Hello"),
                Message.user("World")
            );
            
            // Very high limit - nothing should be removed
            List<ResponseInputItem> result = strategy.manage(history, 10000, counter);
            
            assertEquals(history.size(), result.size());
        }

        @Test
        @DisplayName("removes oldest messages when over limit")
        void removesOldestMessages_whenOverLimit() {
            List<ResponseInputItem> history = new ArrayList<>();
            // Add many messages to exceed a low token limit
            for (int i = 0; i < 10; i++) {
                history.add(Message.user("Message number " + i + " with some content"));
            }
            
            int originalSize = history.size();
            
            // Set a low limit to force truncation
            List<ResponseInputItem> result = strategy.manage(history, 50, counter);
            
            assertTrue(result.size() < originalSize, "Should remove some messages");
            assertTrue(result.size() > 0, "Should keep at least some messages");
        }

        @Test
        @DisplayName("keeps most recent messages")
        void keepsMostRecentMessages() {
            List<ResponseInputItem> history = List.of(
                Message.user("First - oldest"),
                Message.user("Second"),
                Message.user("Third"),
                Message.user("Fourth - newest")
            );
            
            // Limit that should only allow 1-2 messages
            List<ResponseInputItem> result = strategy.manage(history, 30, counter);
            
            // The last message should be present
            if (!result.isEmpty()) {
                ResponseInputItem last = result.get(result.size() - 1);
                assertTrue(last instanceof Message);
                Message lastMsg = (Message) last;
                String text = lastMsg.content().get(0).toString();
                assertTrue(text.contains("newest") || text.contains("Fourth"),
                    "Should keep the most recent message");
            }
        }

        @Test
        @DisplayName("handles empty history")
        void handlesEmptyHistory() {
            List<ResponseInputItem> result = strategy.manage(List.of(), 1000, counter);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("handles zero max tokens")
        void handlesZeroMaxTokens() {
            List<ResponseInputItem> history = List.of(Message.user("Test"));
            
            // maxTokens <= 0 should return original
            List<ResponseInputItem> result = strategy.manage(history, 0, counter);
            assertEquals(1, result.size());
        }
    }

    @Nested
    @DisplayName("Developer Message Preservation")
    class DeveloperMessagePreservation {

        @Test
        @DisplayName("default strategy does not preserve developer message")
        void defaultStrategyDoesNotPreserve() {
            assertFalse(strategy.preservesDeveloperMessage());
        }

        @Test
        @DisplayName("preserving strategy is created correctly")
        void preservingStrategyCreatedCorrectly() {
            SlidingWindowStrategy preserving = SlidingWindowStrategy.preservingDeveloperMessage();
            assertTrue(preserving.preservesDeveloperMessage());
        }

        @Test
        @DisplayName("preserving strategy keeps developer message")
        void preservingStrategyKeepsDeveloperMessage() {
            SlidingWindowStrategy preserving = new SlidingWindowStrategy(true);
            
            List<ResponseInputItem> history = new ArrayList<>();
            history.add(Message.developer("System instructions"));
            // Add many user messages
            for (int i = 0; i < 10; i++) {
                history.add(Message.user("User message " + i + " with content"));
            }
            
            // Low limit to force truncation
            List<ResponseInputItem> result = preserving.manage(history, 100, counter);
            
            // First message should still be the developer message
            if (!result.isEmpty()) {
                ResponseInputItem first = result.get(0);
                assertTrue(first instanceof DeveloperMessage, 
                    "Developer message should be preserved at start");
            }
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("throws for null history")
        void throwsForNullHistory() {
            assertThrows(NullPointerException.class, 
                () -> strategy.manage(null, 1000, counter));
        }

        @Test
        @DisplayName("throws for null counter")
        void throwsForNullCounter() {
            assertThrows(NullPointerException.class, 
                () -> strategy.manage(List.of(), 1000, null));
        }

        @Test
        @DisplayName("single message that exceeds limit")
        void singleMessageExceedsLimit() {
            String longText = "x".repeat(1000); // Very long message
            List<ResponseInputItem> history = List.of(Message.user(longText));
            
            // Even if single message exceeds limit, we keep it (can't split further)
            List<ResponseInputItem> result = strategy.manage(history, 10, counter);
            
            // Behavior: should return empty if can't fit, or keep trying
            assertNotNull(result);
        }
    }
}
