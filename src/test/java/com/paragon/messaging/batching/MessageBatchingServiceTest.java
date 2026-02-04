package com.paragon.messaging.batching;

import com.paragon.messaging.core.MessageProcessor;
import com.paragon.messaging.error.ErrorHandlingStrategy;
import com.paragon.messaging.hooks.HookContext;
import com.paragon.messaging.hooks.HookInterruptedException;
import com.paragon.messaging.hooks.ProcessingHook;
import com.paragon.messaging.ratelimit.RateLimitConfig;
import com.paragon.messaging.store.MessageStore;
import com.paragon.messaging.testutil.MockMessageFactory;
import com.paragon.messaging.whatsapp.payload.InboundMessage;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link MessageBatchingService}.
 */
@DisplayName("MessageBatchingService")
class MessageBatchingServiceTest {

    private BatchingConfig config;
    private MessageProcessor mockProcessor;
    private MessageStore mockStore;
    private MessageBatchingService service;

    @BeforeEach
    void setUp() {
        mockProcessor = mock(MessageProcessor.class);
        mockStore = mock(MessageStore.class);

        config = BatchingConfig.builder()
                .maxBufferSize(10)
                .adaptiveTimeout(Duration.ofMillis(500))
                .silenceThreshold(Duration.ofMillis(200))
                .rateLimitConfig(RateLimitConfig.builder()
                        .tokensPerMinute(60)
                        .build())
                .backpressureStrategy(BackpressureStrategy.DROP_NEW)
                .errorHandlingStrategy(ErrorHandlingStrategy.defaults())
                .messageStore(mockStore)
                .build();

        service = MessageBatchingService.builder()
                .config(config)
                .processor(mockProcessor)
                .build();
    }

    @AfterEach
    void tearDown() {
        if (service != null) {
            service.shutdown();
        }
    }

    @Nested
    @DisplayName("Message Reception")
    class MessageReceptionTests {

        @Test
        @DisplayName("receiveMessage() accepts valid message")
        void receiveMessage_validMessage_accepts() throws InterruptedException {
            InboundMessage msg = MockMessageFactory.createTextMessage("user123", "Hello");
            
            when(mockStore.hasProcessed(anyString(), anyString())).thenReturn(false);

            assertDoesNotThrow(() -> service.receiveMessage("user123", msg));
            
            // Give scheduling time to process
            Thread.sleep(100);
        }

        @Test
        @DisplayName("receiveMessage() rejects null userId")
        void receiveMessage_nullUserId_throws() {
            InboundMessage msg = MockMessageFactory.createTextMessage("user123", "Hello");

            assertThrows(NullPointerException.class,
                    () -> service.receiveMessage(null, msg));
        }

        @Test
        @DisplayName("receiveMessage() rejects null message")
        void receiveMessage_nullMessage_throws() {
            assertThrows(NullPointerException.class,
                    () -> service.receiveMessage("user123", null));
        }

        @Test
        @DisplayName("receiveMessage() ignores duplicate messages")
        void receiveMessage_duplicate_ignores() throws Exception {
            InboundMessage msg = MockMessageFactory.createTextMessage("user123", "msg1", "Hello");

            when(mockStore.hasProcessed("user123", "msg1")).thenReturn(true);

            service.receiveMessage("user123", msg);
            
            Thread.sleep(700); // Wait beyond timeout

            verify(mockProcessor, never()).process(anyString(), anyList(), any());
        }

        @Test
        @DisplayName("receiveMessage() stores message in buffer")
        void receiveMessage_storesInBuffer() {
            InboundMessage msg = MockMessageFactory.createTextMessage("user123", "Hello");
            
            when(mockStore.hasProcessed(anyString(), anyString())).thenReturn(false);

            service.receiveMessage("user123", msg);

            MessageBatchingService.ServiceStats stats = service.getStats();
            assertEquals(1, stats.activeUsers());
            assertTrue(stats.pendingMessages() > 0);
        }
    }

    @Nested
    @DisplayName("Batching Behavior")
    class BatchingBehaviorTests {

        @Test
        @DisplayName("processes batch after adaptive timeout")
        void processesBatch_afterAdaptiveTimeout() throws Exception {
            CountDownLatch latch = new CountDownLatch(1);
            doAnswer(inv -> {
                latch.countDown();
                return null;
            }).when(mockProcessor).process(anyString(), anyList(), any());

            when(mockStore.hasProcessed(anyString(), anyString())).thenReturn(false);

            InboundMessage msg1 = MockMessageFactory.createTextMessage("user123", "msg1", "Hello");
            InboundMessage msg2 = MockMessageFactory.createTextMessage("user123", "msg2", "World");

            service.receiveMessage("user123", msg1);
            service.receiveMessage("user123", msg2);

            // Wait for adaptive timeout (500ms)
            assertTrue(latch.await(1, TimeUnit.SECONDS));

            verify(mockProcessor).process(eq("user123"), argThat(msgs -> msgs.size() == 2), any());
        }

        @Test
        @DisplayName("processes batch after silence threshold")
        void processesBatch_afterSilenceThreshold() throws Exception {
            CountDownLatch latch = new CountDownLatch(1);
            doAnswer(inv -> {
                latch.countDown();
                return null;
            }).when(mockProcessor).process(anyString(), anyList(), any());

            when(mockStore.hasProcessed(anyString(), anyString())).thenReturn(false);

            InboundMessage msg = MockMessageFactory.createTextMessage("user123", "Hello");
            service.receiveMessage("user123", msg);

            // Wait for silence threshold (200ms)
            assertTrue(latch.await(500, TimeUnit.MILLISECONDS));

            verify(mockProcessor).process(eq("user123"), anyList(), any());
        }

        @Test
        @DisplayName("batches multiple messages from same user")
        void batchesMultiple_sameUser() throws Exception {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicInteger messageCount = new AtomicInteger();

            doAnswer(inv -> {
                List<?> msgs = inv.getArgument(1);
                messageCount.set(msgs.size());
                latch.countDown();
                return null;
            }).when(mockProcessor).process(anyString(), anyList(), any());

            when(mockStore.hasProcessed(anyString(), anyString())).thenReturn(false);

            service.receiveMessage("user123", MockMessageFactory.createTextMessage("user123", "msg1", "One"));
            service.receiveMessage("user123", MockMessageFactory.createTextMessage("user123", "msg2", "Two"));
            service.receiveMessage("user123", MockMessageFactory.createTextMessage("user123", "msg3", "Three"));

            assertTrue(latch.await(1, TimeUnit.SECONDS));
            assertEquals(3, messageCount.get());
        }

        @Test
        @DisplayName("keeps messages from different users separate")
        void separateBuffers_perUser() throws Exception {
            CountDownLatch latch = new CountDownLatch(2);

            doAnswer(inv -> {
                latch.countDown();
                return null;
            }).when(mockProcessor).process(anyString(), anyList(), any());

            when(mockStore.hasProcessed(anyString(), anyString())).thenReturn(false);

            service.receiveMessage("user1", MockMessageFactory.createTextMessage("user1", "A"));
            service.receiveMessage("user2", MockMessageFactory.createTextMessage("user2", "B"));

            assertTrue(latch.await(1, TimeUnit.SECONDS));

            verify(mockProcessor).process(eq("user1"), anyList(), any());
            verify(mockProcessor).process(eq("user2"), anyList(), any());
        }
    }

    @Nested
    @DisplayName("Rate Limiting")
    class RateLimitingTests {

        @Test
        @DisplayName("rate limiting enforced per user")
        void rateLimiting_perUser() throws InterruptedException {
            // Use strict rate limit
            BatchingConfig strictConfig = BatchingConfig.builder()
                    .rateLimitConfig(RateLimitConfig.builder()
                            .tokensPerMinute(60)
                            .bucketCapacity(3) // Only 3 messages allowed in burst
                            .build())
                    .messageStore(mockStore)
                    .build();

            service.shutdown();
            service = MessageBatchingService.builder()
                    .config(strictConfig)
                    .processor(mockProcessor)
                    .build();

            when(mockStore.hasProcessed(anyString(), anyString())).thenReturn(false);

            // Send more than capacity
            for (int i = 0; i < 10; i++) {
                service.receiveMessage("user123",
                        MockMessageFactory.createTextMessage("user123", "msg" + i, "Message " + i));
            }

            Thread.sleep(100);

            // Should have buffered only first 3
            MessageBatchingService.ServiceStats stats = service.getStats();
            assertTrue(stats.pendingMessages() <= 3);
        }
    }

    @Nested
    @DisplayName("Backpressure Handling")
    class BackpressureHandlingTests {

        @Test
        @DisplayName("DROP_NEW strategy drops new messages when full")
        void dropNew_whenBufferFull() {
            BatchingConfig config = BatchingConfig.builder()
                    .maxBufferSize(2)
                    .backpressureStrategy(BackpressureStrategy.DROP_NEW)
                    .messageStore(mockStore)
                    .build();

            service.shutdown();
            service = MessageBatchingService.builder()
                    .config(config)
                    .processor(mockProcessor)
                    .build();

            when(mockStore.hasProcessed(anyString(), anyString())).thenReturn(false);

            service.receiveMessage("user123", MockMessageFactory.createTextMessage("user123", "1"));
            service.receiveMessage("user123", MockMessageFactory.createTextMessage("user123", "2"));
            service.receiveMessage("user123", MockMessageFactory.createTextMessage("user123", "3")); // Should be dropped

            MessageBatchingService.ServiceStats stats = service.getStats();
            assertEquals(2, stats.pendingMessages());
        }

        @Test
        @DisplayName("FLUSH_AND_ACCEPT strategy processes and accepts new")
        void flushAndAccept_whenBufferFull() throws Exception {
            CountDownLatch latch = new CountDownLatch(1);
            doAnswer(inv -> {
                latch.countDown();
                return null;
            }).when(mockProcessor).process(anyString(), anyList(), any());

            BatchingConfig config = BatchingConfig.builder()
                    .maxBufferSize(2)
                    .backpressureStrategy(BackpressureStrategy.FLUSH_AND_ACCEPT)
                    .messageStore(mockStore)
                    .build();

            service.shutdown();
            service = MessageBatchingService.builder()
                    .config(config)
                    .processor(mockProcessor)
                    .build();

            when(mockStore.hasProcessed(anyString(), anyString())).thenReturn(false);

            service.receiveMessage("user123", MockMessageFactory.createTextMessage("user123", "1"));
            service.receiveMessage("user123", MockMessageFactory.createTextMessage("user123", "2"));
            service.receiveMessage("user123", MockMessageFactory.createTextMessage("user123", "3")); // Triggers flush

            assertTrue(latch.await(500, TimeUnit.MILLISECONDS));
            verify(mockProcessor, atLeastOnce()).process(eq("user123"), anyList(), any());
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("retries on processing failure")
        void retries_onFailure() throws Exception {
            AtomicInteger attempts = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(2);

            doAnswer(inv -> {
                int attempt = attempts.incrementAndGet();
                latch.countDown();
                if (attempt == 1) {
                    throw new RuntimeException("Simulated failure");
                }
                return null;
            }).when(mockProcessor).process(anyString(), anyList(), any());

            BatchingConfig config = BatchingConfig.builder()
                    .errorHandlingStrategy(ErrorHandlingStrategy.builder()
                            .maxRetries(2)
                            .retryDelay(Duration.ofMillis(100))
                            .build())
                    .messageStore(mockStore)
                    .build();

            service.shutdown();
            service = MessageBatchingService.builder()
                    .config(config)
                    .processor(mockProcessor)
                    .build();

            when(mockStore.hasProcessed(anyString(), anyString())).thenReturn(false);

            service.receiveMessage("user123", MockMessageFactory.createTextMessage("user123", "Test"));

            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertTrue(attempts.get() >= 2, "Should have retried");
        }

        @Test
        @DisplayName("calls dead letter queue after max retries")
        void callsDLQ_afterMaxRetries() throws Exception {
            List<InboundMessage> dlqMessages = new ArrayList<>();
            CountDownLatch dlqLatch = new CountDownLatch(1);

            doThrow(new RuntimeException("Always fails"))
                    .when(mockProcessor).process(anyString(), anyList(), any());

            BatchingConfig config = BatchingConfig.builder()
                    .errorHandlingStrategy(ErrorHandlingStrategy.builder()
                            .maxRetries(1)
                            .retryDelay(Duration.ofMillis(50))
                            .deadLetterHandler((userId, msgs) -> {
                                dlqMessages.addAll(msgs);
                                dlqLatch.countDown();
                            })
                            .build())
                    .messageStore(mockStore)
                    .build();

            service.shutdown();
            service = MessageBatchingService.builder()
                    .config(config)
                    .processor(mockProcessor)
                    .build();

            when(mockStore.hasProcessed(anyString(), anyString())).thenReturn(false);

            service.receiveMessage("user123", MockMessageFactory.createTextMessage("user123", "msg1", "Fail"));

            assertTrue(dlqLatch.await(2, TimeUnit.SECONDS));
            assertFalse(dlqMessages.isEmpty());
        }
    }

    @Nested
    @DisplayName("Hooks")
    class HooksTests {

        @Test
        @DisplayName("executes pre-hooks before processing")
        void executesPreHooks_beforeProcessing() throws Exception {
            List<String> executionOrder = new ArrayList<>();
            CountDownLatch latch = new CountDownLatch(1);

            ProcessingHook preHook = context -> {
                executionOrder.add("PRE");
            };

            doAnswer(inv -> {
                executionOrder.add("PROCESS");
                latch.countDown();
                return null;
            }).when(mockProcessor).process(anyString(), anyList(), any());

            service.shutdown();
            service = MessageBatchingService.builder()
                    .config(config)
                    .processor(mockProcessor)
                    .addPreHook(preHook)
                    .build();

            when(mockStore.hasProcessed(anyString(), anyString())).thenReturn(false);

            service.receiveMessage("user123", MockMessageFactory.createTextMessage("user123", "Test"));

            assertTrue(latch.await(1, TimeUnit.SECONDS));
            assertEquals(List.of("PRE", "PROCESS"), executionOrder);
        }

        @Test
        @DisplayName("executes post-hooks after processing")
        void executesPostHooks_afterProcessing() throws Exception {
            List<String> executionOrder = new ArrayList<>();
            CountDownLatch latch = new CountDownLatch(1);

            ProcessingHook postHook = context -> {
                executionOrder.add("POST");
                latch.countDown();
            };

            doAnswer(inv -> {
                executionOrder.add("PROCESS");
                return null;
            }).when(mockProcessor).process(anyString(), anyList(), any());

            service.shutdown();
            service = MessageBatchingService.builder()
                    .config(config)
                    .processor(mockProcessor)
                    .addPostHook(postHook)
                    .build();

            when(mockStore.hasProcessed(anyString(), anyString())).thenReturn(false);

            service.receiveMessage("user123", MockMessageFactory.createTextMessage("user123", "Test"));

            assertTrue(latch.await(1, TimeUnit.SECONDS));
            assertEquals(List.of("PROCESS", "POST"), executionOrder);
        }

        @Test
        @DisplayName("hook interruption stops processing")
        void hookInterruption_stopsProcessing() throws Exception {
            CountDownLatch hookLatch = new CountDownLatch(1);

            ProcessingHook interruptingHook = context -> {
                hookLatch.countDown();
                throw new HookInterruptedException("Content rejected", "MODERATION");
            };

            service.shutdown();
            service = MessageBatchingService.builder()
                    .config(config)
                    .processor(mockProcessor)
                    .addPreHook(interruptingHook)
                    .build();

            when(mockStore.hasProcessed(anyString(), anyString())).thenReturn(false);

            service.receiveMessage("user123", MockMessageFactory.createTextMessage("user123", "Spam"));

            assertTrue(hookLatch.await(1, TimeUnit.SECONDS));
            
            Thread.sleep(100); // Give time to ensure processor not called
            verify(mockProcessor, never()).process(anyString(), anyList(), any());
        }

        @Test
        @DisplayName("hooks receive correct context")
        void hooksReceiveCorrectContext() throws Exception {
            AtomicReference<HookContext> capturedContext = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);

            ProcessingHook capturingHook = context -> {
                capturedContext.set(context);
                latch.countDown();
            };

            service.shutdown();
            service = MessageBatchingService.builder()
                    .config(config)
                    .processor(mockProcessor)
                    .addPreHook(capturingHook)
                    .build();

            when(mockStore.hasProcessed(anyString(), anyString())).thenReturn(false);

            service.receiveMessage("user123", MockMessageFactory.createTextMessage("user123", "msg1", "Test"));

            assertTrue(latch.await(1, TimeUnit.SECONDS));

            HookContext ctx = capturedContext.get();
            assertNotNull(ctx);
            assertEquals("user123", ctx.userId());
            assertEquals(1, ctx.batchSize());
            assertFalse(ctx.isRetry());
        }
    }

    @Nested
    @DisplayName("Service Management")
    class ServiceManagementTests {

        @Test
        @DisplayName("getStats() returns service statistics")
        void getStats_returnsStats() {
            when(mockStore.hasProcessed(anyString(), anyString())).thenReturn(false);

            service.receiveMessage("user1", MockMessageFactory.createTextMessage("user1", "A"));
            service.receiveMessage("user2", MockMessageFactory.createTextMessage("user2", "B"));

            MessageBatchingService.ServiceStats stats = service.getStats();

            assertEquals(2, stats.activeUsers());
            assertTrue(stats.pendingMessages() > 0);
        }

        @Test
        @DisplayName("shutdown() stops service gracefully")
        void shutdown_stopsGracefully() {
            assertDoesNotThrow(() -> service.shutdown());
        }

        @Test
        @DisplayName("builder requires config")
        void builder_requiresConfig() {
            assertThrows(NullPointerException.class, () ->
                    MessageBatchingService.builder()
                            .processor(mockProcessor)
                            .build());
        }

        @Test
        @DisplayName("builder requires processor")
        void builder_requiresProcessor() {
            assertThrows(NullPointerException.class, () ->
                    MessageBatchingService.builder()
                            .config(config)
                            .build());
        }
    }

    @Nested
    @DisplayName("Deduplication")
    class DeduplicationTests {

        @Test
        @DisplayName("marks messages as processed after successful processing")
        void marksAsProcessed_afterSuccess() throws Exception {
            CountDownLatch latch = new CountDownLatch(1);
            doAnswer(inv -> {
                latch.countDown();
                return null;
            }).when(mockProcessor).process(anyString(), anyList(), any());

            when(mockStore.hasProcessed(anyString(), anyString())).thenReturn(false);

            InboundMessage msg = MockMessageFactory.createTextMessage("user123", "msg1", "Test");
            service.receiveMessage("user123", msg);

            assertTrue(latch.await(1, TimeUnit.SECONDS));

            verify(mockStore).markProcessed("user123", "msg1");
        }

        @Test
        @DisplayName("does not mark as processed on failure")
        void doesNotMarkAsProcessed_onFailure() throws Exception {
            doThrow(new RuntimeException("Fail"))
                    .when(mockProcessor).process(anyString(), anyList(), any());

            when(mockStore.hasProcessed(anyString(), anyString())).thenReturn(false);

            service.receiveMessage("user123", MockMessageFactory.createTextMessage("user123", "msg1", "Test"));

            Thread.sleep(300);

            // Should not mark as processed on first attempt
            verify(mockStore, never()).markProcessed(eq("user123"), eq("msg1"));
        }
    }

    @Nested
    @DisplayName("Thread Safety")
    class ThreadSafetyTests {

        @Test
        @DisplayName("concurrent message reception is thread-safe")
        void concurrentReception_isThreadSafe() throws InterruptedException {
            when(mockStore.hasProcessed(anyString(), anyString())).thenReturn(false);

            int threadCount = 10;
            int messagesPerThread = 5;
            CountDownLatch latch = new CountDownLatch(threadCount);

            for (int t = 0; t < threadCount; t++) {
                final String userId = "user" + t;
                Thread.startVirtualThread(() -> {
                    for (int m = 0; m < messagesPerThread; m++) {
                        service.receiveMessage(userId,
                                MockMessageFactory.createTextMessage(userId, "msg" + m, "Message " + m));
                    }
                    latch.countDown();
                });
            }

            assertTrue(latch.await(2, TimeUnit.SECONDS));

            MessageBatchingService.ServiceStats stats = service.getStats();
            assertEquals(threadCount, stats.activeUsers());
        }
    }
}
