package com.paragon.messaging.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link FloodDetector}.
 */
@DisplayName("FloodDetector")
class FloodDetectorTest {

    private FloodDetector detector;

    @BeforeEach
    void setUp() {
        detector = FloodDetector.create(Duration.ofSeconds(1), 3);
    }

    @Nested
    @DisplayName("Construction")
    class ConstructionTests {

        @Test
        @DisplayName("create(Duration, int) creates detector")
        void create_withParameters_createsDetector() {
            FloodDetector detector = FloodDetector.create(Duration.ofSeconds(5), 10);

            assertNotNull(detector);
            assertTrue(detector.isEnabled());
            assertEquals(Duration.ofSeconds(5), detector.getWindow());
            assertEquals(10, detector.getMaxMessages());
        }

        @Test
        @DisplayName("create(SecurityConfig) creates from config")
        void create_withConfig_createsDetector() {
            SecurityConfig config = SecurityConfig.builder()
                    .webhookVerifyToken("test-token-12345")
                    .floodPreventionWindow(Duration.ofMinutes(1))
                    .maxMessagesPerWindow(15)
                    .build();

            FloodDetector detector = FloodDetector.create(config);

            assertTrue(detector.isEnabled());
            assertEquals(Duration.ofMinutes(1), detector.getWindow());
            assertEquals(15, detector.getMaxMessages());
        }

        @Test
        @DisplayName("disabled() creates disabled detector")
        void disabled_createsDisabledDetector() {
            FloodDetector detector = FloodDetector.disabled();

            assertFalse(detector.isEnabled());
        }
    }

    @Nested
    @DisplayName("Flood Detection")
    class FloodDetectionTests {

        @Test
        @DisplayName("isFlooding() returns false for normal traffic")
        void isFlooding_normalTraffic_returnsFalse() {
            detector.recordMessage("user1");
            detector.recordMessage("user1");

            assertFalse(detector.isFlooding("user1")); // 2 < 3, not flooding
        }

        @Test
        @DisplayName("isFlooding() returns true when limit exceeded")
        void isFlooding_limitExceeded_returnsTrue() {
            detector.recordMessage("user1");
            detector.recordMessage("user1");
            detector.recordMessage("user1");

            assertTrue(detector.isFlooding("user1")); // 3 >= 3, flooding
        }

        @Test
        @DisplayName("isFlooding() returns false for new user")
        void isFlooding_newUser_returnsFalse() {
            assertFalse(detector.isFlooding("newuser"));
        }

        @Test
        @DisplayName("isFlooding() does not record message")
        void isFlooding_doesNotRecord() {
            boolean flooding = detector.isFlooding("user1");
            
            assertFalse(flooding);
            assertEquals(0, detector.getMessageCount("user1"));
        }
    }

    @Nested
    @DisplayName("Message Recording")
    class MessageRecordingTests {

        @Test
        @DisplayName("recordMessage() increments count")
        void recordMessage_incrementsCount() {
            detector.recordMessage("user1");
            assertEquals(1, detector.getMessageCount("user1"));

            detector.recordMessage("user1");
            assertEquals(2, detector.getMessageCount("user1"));
        }

        @Test
        @DisplayName("recordMessage() for different users are isolated")
        void recordMessage_isolatedPerUser() {
            detector.recordMessage("user1");
            detector.recordMessage("user2");

            assertEquals(1, detector.getMessageCount("user1"));
            assertEquals(1, detector.getMessageCount("user2"));
        }

        @Test
        @DisplayName("checkAndRecord() combines check and record")
        void checkAndRecord_combinesOperations() {
            detector.recordMessage("user1");
            detector.recordMessage("user1");

            boolean wasFlooding = detector.checkAndRecord("user1");

            assertFalse(wasFlooding); // Was not flooding before (2 < 3)
            assertEquals(3, detector.getMessageCount("user1"));
            assertTrue(detector.isFlooding("user1")); // Now flooding
        }
    }

    @Nested
    @DisplayName("Window Sliding")
    class WindowSlidingTests {

        @Test
        @DisplayName("old messages outside window are not counted")
        void oldMessages_outsideWindow_notCounted() throws InterruptedException {
            detector.recordMessage("user1");
            detector.recordMessage("user1");
            detector.recordMessage("user1");

            assertTrue(detector.isFlooding("user1"));

            // Wait for window to slide
            Thread.sleep(1100);

            assertFalse(detector.isFlooding("user1"), "Messages should expire after window");
            assertEquals(0, detector.getMessageCount("user1"));
        }

        @Test
        @DisplayName("window slides gradually")
        void window_slidesGradually() throws InterruptedException {
            // Record 3 messages with delays
            detector.recordMessage("user1");
            Thread.sleep(400);
            detector.recordMessage("user1");
            Thread.sleep(400);
            detector.recordMessage("user1");

            assertEquals(3, detector.getMessageCount("user1"));

            // Wait for first message to expire
            Thread.sleep(300);

            assertEquals(2, detector.getMessageCount("user1"), 
                    "First message should have expired");
        }
    }

    @Nested
    @DisplayName("Message Count and Allowance")
    class MessageCountTests {

        @Test
        @DisplayName("getMessageCount() returns correct count")
        void getMessageCount_returnsCorrect() {
            detector.recordMessage("user1");
            detector.recordMessage("user1");

            assertEquals(2, detector.getMessageCount("user1"));
        }

        @Test
        @DisplayName("getMessageCount() returns 0 for unknown user")
        void getMessageCount_unknownUser_returnsZero() {
            assertEquals(0, detector.getMessageCount("unknownuser"));
        }

        @Test
        @DisplayName("getRemainingAllowance() returns correct value")
        void getRemainingAllowance_returnsCorrect() {
            detector.recordMessage("user1");

            assertEquals(2, detector.getRemainingAllowance("user1")); // 3 - 1 = 2
        }

        @Test
        @DisplayName("getRemainingAllowance() returns 0 when flooding")
        void getRemainingAllowance_flooding_returnsZero() {
            detector.recordMessage("user1");
            detector.recordMessage("user1");
            detector.recordMessage("user1");

            assertEquals(0, detector.getRemainingAllowance("user1"));
        }
    }

    @Nested
    @DisplayName("Cleanup Operations")
    class CleanupTests {

        @Test
        @DisplayName("clearUser() removes user history")
        void clearUser_removesHistory() {
            detector.recordMessage("user1");
            detector.recordMessage("user1");

            detector.clearUser("user1");

            assertEquals(0, detector.getMessageCount("user1"));
            assertFalse(detector.isFlooding("user1"));
        }

        @Test
        @DisplayName("clearAll() removes all histories")
        void clearAll_removesAllHistories() {
            detector.recordMessage("user1");
            detector.recordMessage("user2");
            detector.recordMessage("user3");

            detector.clearAll();

            assertEquals(0, detector.getTrackedUserCount());
        }

        @Test
        @DisplayName("cleanup() removes expired entries")
        void cleanup_removesExpiredEntries() throws InterruptedException {
            detector.recordMessage("user1");
            detector.recordMessage("user2");

            // Wait for expiry
            Thread.sleep(1100);

            int cleaned = detector.cleanup();

            assertTrue(cleaned >= 0);
            assertEquals(0, detector.getTrackedUserCount());
        }

        @Test
        @DisplayName("cleanup() preserves recent entries")
        void cleanup_preservesRecentEntries() {
            detector.recordMessage("user1");
            detector.recordMessage("user2");

            detector.cleanup();

            assertEquals(2, detector.getTrackedUserCount());
        }
    }

    @Nested
    @DisplayName("Statistics")
    class StatisticsTests {

        @Test
        @DisplayName("getTrackedUserCount() returns correct count")
        void getTrackedUserCount_returnsCorrect() {
            detector.recordMessage("user1");
            detector.recordMessage("user2");
            detector.recordMessage("user3");

            assertEquals(3, detector.getTrackedUserCount());
        }

        @Test
        @DisplayName("getTrackedUserCount() returns 0 for new detector")
        void getTrackedUserCount_newDetector_returnsZero() {
            FloodDetector newDetector = FloodDetector.create(Duration.ofSeconds(1), 5);

            assertEquals(0, newDetector.getTrackedUserCount());
        }
    }

    @Nested
    @DisplayName("Disabled Detector Behavior")
    class DisabledDetectorTests {

        @Test
        @DisplayName("disabled detector never detects flooding")
        void disabledDetector_neverDetectsFlooding() {
            FloodDetector disabled = FloodDetector.disabled();

            for (int i = 0; i < 100; i++) {
                disabled.recordMessage("user1");
            }

            assertFalse(disabled.isFlooding("user1"));
        }
    }

    @Nested
    @DisplayName("Thread Safety")
    class ThreadSafetyTests {

        @Test
        @DisplayName("concurrent recording is thread-safe")
        void concurrentRecording_isThreadSafe() throws InterruptedException {
            int threadCount = 10;
            int messagesPerThread = 5;
            CountDownLatch latch = new CountDownLatch(threadCount);

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    for (int j = 0; j < messagesPerThread; j++) {
                        detector.recordMessage("user1");
                    }
                    latch.countDown();
                });
            }

            assertTrue(latch.await(5, TimeUnit.SECONDS));

            assertEquals(threadCount * messagesPerThread, detector.getMessageCount("user1"));

            executor.shutdown();
        }

        @Test
        @DisplayName("concurrent operations on different users are isolated")
        void concurrentOperations_isolatedPerUser() throws InterruptedException {
            int userCount = 10;
            CountDownLatch latch = new CountDownLatch(userCount);

            ExecutorService executor = Executors.newFixedThreadPool(userCount);

            for (int i = 0; i < userCount; i++) {
                final String userId = "user" + i;
                executor.submit(() -> {
                    detector.recordMessage(userId);
                    detector.recordMessage(userId);
                    detector.isFlooding(userId);
                    latch.countDown();
                });
            }

            assertTrue(latch.await(5, TimeUnit.SECONDS));

            // Each user should have exactly 2 messages
            for (int i = 0; i < userCount; i++) {
                assertEquals(2, detector.getMessageCount("user" + i));
            }

            executor.shutdown();
        }
    }

    @Nested
    @DisplayName("Configuration")
    class ConfigurationTests {

        @Test
        @DisplayName("getWindow() returns configured window")
        void getWindow_returnsConfigured() {
            assertEquals(Duration.ofSeconds(1), detector.getWindow());
        }

        @Test
        @DisplayName("getMaxMessages() returns configured max")
        void getMaxMessages_returnsConfigured() {
            assertEquals(3, detector.getMaxMessages());
        }

        @Test
        @DisplayName("isEnabled() returns true for enabled detector")
        void isEnabled_returnsTrue() {
            assertTrue(detector.isEnabled());
        }

        @Test
        @DisplayName("isEnabled() returns false for disabled detector")
        void isEnabled_returnsFalse() {
            FloodDetector disabled = FloodDetector.disabled();

            assertFalse(disabled.isEnabled());
        }
    }
}
