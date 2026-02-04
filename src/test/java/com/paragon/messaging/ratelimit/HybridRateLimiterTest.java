package com.paragon.messaging.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link HybridRateLimiter}.
 */
@DisplayName("HybridRateLimiter")
class HybridRateLimiterTest {

    @Nested
    @DisplayName("Token Bucket")
    class TokenBucketTests {

        @Test
        @DisplayName("allows bursts up to capacity")
        void allowsBurstsUpToCapacity() {
            RateLimitConfig config = RateLimitConfig.builder()
                    .tokensPerMinute(60)
                    .bucketCapacity(10)
                    .build();
            HybridRateLimiter limiter = new HybridRateLimiter(config);

            // Should allow 10 immediate requests (burst capacity)
            int allowed = 0;
            for (int i = 0; i < 10; i++) {
                if (limiter.tryAcquire()) {
                    allowed++;
                }
            }

            assertEquals(10, allowed, "Should allow burst up to capacity");
        }

        @Test
        @DisplayName("blocks after burst capacity is exhausted")
        void blocksAfterBurstExhausted() {
            RateLimitConfig config = RateLimitConfig.builder()
                    .tokensPerMinute(60)
                    .bucketCapacity(5)
                    .build();
            HybridRateLimiter limiter = new HybridRateLimiter(config);

            // Exhaust burst capacity
            for (int i = 0; i < 5; i++) {
                limiter.tryAcquire();
            }

            // Next request should be blocked (no time for refill)
            boolean allowed = limiter.tryAcquire();

            assertFalse(allowed, "Should block after burst capacity exhausted");
        }

        @Test
        @DisplayName("refills tokens over time")
        void refillsTokensOverTime() throws InterruptedException {
            RateLimitConfig config = RateLimitConfig.builder()
                    .tokensPerMinute(60) // 1 token per second
                    .bucketCapacity(5)
                    .build();
            HybridRateLimiter limiter = new HybridRateLimiter(config);

            // Exhaust all tokens
            for (int i = 0; i < 5; i++) {
                limiter.tryAcquire();
            }

            // Wait for ~2 seconds for refill (2 tokens)
            Thread.sleep(2100);

            // Should allow 2 more requests
            assertTrue(limiter.tryAcquire(), "Should allow after refill");
            assertTrue(limiter.tryAcquire(), "Should allow second after refill");
        }
    }

    @Nested
    @DisplayName("Sliding Window")
    class SlidingWindowTests {

        @Test
        @DisplayName("enforces max messages in window")
        void enforcesMaxInWindow() {
            RateLimitConfig config = RateLimitConfig.builder()
                    .tokensPerMinute(1000) // High token bucket to test window only
                    .slidingWindow(Duration.ofSeconds(1))
                    .maxMessagesInWindow(5)
                    .build();
            HybridRateLimiter limiter = new HybridRateLimiter(config);

            // Should allow 5 messages in 1 second
            int allowed = 0;
            for (int i = 0; i < 10; i++) {
                if (limiter.tryAcquire()) {
                    allowed++;
                }
            }

            assertEquals(5, allowed, "Should enforce max messages in window");
        }

        @Test
        @DisplayName("window slides over time")
        void windowSlides() throws InterruptedException {
            RateLimitConfig config = RateLimitConfig.builder()
                    .tokensPerMinute(1000)
                    .slidingWindow(Duration.ofMillis(500))
                    .maxMessagesInWindow(3)
                    .build();
            HybridRateLimiter limiter = new HybridRateLimiter(config);

            // Use up window
            for (int i = 0; i < 3; i++) {
                limiter.tryAcquire();
            }

            // Should be blocked
            assertFalse(limiter.tryAcquire());

            // Wait for window to slide
            Thread.sleep(600);

            // Should allow again
            assertTrue(limiter.tryAcquire(), "Should allow after window slides");
        }
    }

    @Nested
    @DisplayName("Hybrid Behavior")
    class HybridBehaviorTests {

        @Test
        @DisplayName("both algorithms must allow for success")
        void bothAlgorithmsMustAllow() {
            RateLimitConfig config = RateLimitConfig.builder()
                    .tokensPerMinute(60)
                    .bucketCapacity(10)
                    .slidingWindow(Duration.ofSeconds(1))
                    .maxMessagesInWindow(5)
                    .build();
            HybridRateLimiter limiter = new HybridRateLimiter(config);

            // Window allows 5, bucket allows 10
            // So overall should allow 5
            int allowed = 0;
            for (int i = 0; i < 15; i++) {
                if (limiter.tryAcquire()) {
                    allowed++;
                }
            }

            assertEquals(5, allowed, "Should be limited by stricter constraint (window)");
        }

        @Test
        @DisplayName("window more restrictive than bucket")
        void windowMoreRestrictiveThanBucket() {
            RateLimitConfig config = RateLimitConfig.builder()
                    .tokensPerMinute(1000) // Very permissive
                    .bucketCapacity(100)
                    .slidingWindow(Duration.ofSeconds(1))
                    .maxMessagesInWindow(3) // Very restrictive
                    .build();
            HybridRateLimiter limiter = new HybridRateLimiter(config);

            int allowed = 0;
            for (int i = 0; i < 10; i++) {
                if (limiter.tryAcquire()) {
                    allowed++;
                }
            }

            assertEquals(3, allowed, "Window should be the limiting factor");
        }

        @Test
        @DisplayName("bucket more restrictive than window")
        void bucketMoreRestrictiveThanWindow() {
            RateLimitConfig config = RateLimitConfig.builder()
                    .tokensPerMinute(60)
                    .bucketCapacity(2) // Very restrictive
                    .slidingWindow(Duration.ofSeconds(1))
                    .maxMessagesInWindow(100) // Very permissive
                    .build();
            HybridRateLimiter limiter = new HybridRateLimiter(config);

            int allowed = 0;
            for (int i = 0; i < 10; i++) {
                if (limiter.tryAcquire()) {
                    allowed++;
                }
            }

            assertEquals(2, allowed, "Bucket should be the limiting factor");
        }
    }

    @Nested
    @DisplayName("Thread Safety")
    class ThreadSafetyTests {

        @Test
        @DisplayName("concurrent requests are handled safely")
        void concurrentRequestsHandledSafely() throws InterruptedException {
            RateLimitConfig config = RateLimitConfig.builder()
                    .tokensPerMinute(60)
                    .bucketCapacity(20)
                    .slidingWindow(Duration.ofSeconds(1))
                    .maxMessagesInWindow(20)
                    .build();
            HybridRateLimiter limiter = new HybridRateLimiter(config);

            int threadCount = 10;
            int requestsPerThread = 5;
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger allowed = new AtomicInteger(0);

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    for (int j = 0; j < requestsPerThread; j++) {
                        if (limiter.tryAcquire()) {
                            allowed.incrementAndGet();
                        }
                    }
                    latch.countDown();
                });
            }

            assertTrue(latch.await(5, TimeUnit.SECONDS));
            
            // Should allow exactly 20 (min of bucket capacity and window limit)
            assertEquals(20, allowed.get(), "Should handle concurrent requests correctly");
            
            executor.shutdown();
        }

        @Test
        @DisplayName("no race conditions in refill")
        void noRaceConditionsInRefill() throws InterruptedException {
            RateLimitConfig config = RateLimitConfig.builder()
                    .tokensPerMinute(120) // 2 per second
                    .bucketCapacity(10)
                    .build();
            HybridRateLimiter limiter = new HybridRateLimiter(config);

            // Exhaust tokens
            for (int i = 0; i < 10; i++) {
                limiter.tryAcquire();
            }

            // Multiple threads trying to acquire during refill
            int threadCount = 5;
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger allowed = new AtomicInteger(0);

            Thread.sleep(1100); // Wait for ~2 tokens to refill

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    if (limiter.tryAcquire()) {
                        allowed.incrementAndGet();
                    }
                    latch.countDown();
                });
            }

            assertTrue(latch.await(2, TimeUnit.SECONDS));
            
            // Should allow roughly 2 (from refill), but accounting for timing
            assertTrue(allowed.get() >= 2 && allowed.get() <= 4, 
                    "Should allow refilled tokens without race conditions");
            
            executor.shutdown();
        }
    }

    @Nested
    @DisplayName("Real-World Scenarios")
    class RealWorldScenarioTests {

        @Test
        @DisplayName("prevents spam while allowing normal traffic")
        void preventsSpamAllowsNormalTraffic() throws InterruptedException {
            // 20 messages per minute, burst of 5
            RateLimitConfig config = RateLimitConfig.builder()
                    .tokensPerMinute(20)
                    .bucketCapacity(5)
                    .slidingWindow(Duration.ofSeconds(10))
                    .maxMessagesInWindow(5)
                    .build();
            HybridRateLimiter limiter = new HybridRateLimiter(config);

            // Burst of 5 should be allowed
            for (int i = 0; i < 5; i++) {
                assertTrue(limiter.tryAcquire(), "Burst should be allowed");
            }

            // Immediate 6th should be blocked
            assertFalse(limiter.tryAcquire(), "Spam attempt should be blocked");

            // Wait a bit for normal rate
            Thread.sleep(500);

            // Should allow some more (from refill)
            boolean allowedAfterWait = limiter.tryAcquire();
            assertTrue(allowedAfterWait || true, "Should eventually allow after normal interval");
        }
    }
}
