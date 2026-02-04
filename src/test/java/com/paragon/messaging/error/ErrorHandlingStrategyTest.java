package com.paragon.messaging.error;

import com.paragon.messaging.whatsapp.payload.InboundMessage;
import com.paragon.messaging.testutil.MockMessageFactory;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ErrorHandlingStrategy}.
 */
@DisplayName("ErrorHandlingStrategy")
class ErrorHandlingStrategyTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("defaults() creates strategy with default values")
        void defaults_createsDefaultStrategy() {
            ErrorHandlingStrategy strategy = ErrorHandlingStrategy.defaults();

            assertNotNull(strategy);
            assertEquals(3, strategy.maxRetries());
            assertEquals(Duration.ofSeconds(2), strategy.retryDelay());
            assertTrue(strategy.exponentialBackoff());
            assertTrue(strategy.notifyUserOnFailure());
        }

        @Test
        @DisplayName("noRetry() creates strategy without retries")
        void noRetry_createsNoRetryStrategy() {
            ErrorHandlingStrategy strategy = ErrorHandlingStrategy.noRetry();

            assertEquals(0, strategy.maxRetries());
            assertTrue(strategy.notifyUserOnFailure());
        }

        @Test
        @DisplayName("builder() allows custom configuration")
        void builder_allowsCustomConfiguration() {
            ErrorHandlingStrategy strategy = ErrorHandlingStrategy.builder()
                    .maxRetries(5)
                    .retryDelay(Duration.ofSeconds(3))
                    .exponentialBackoff(false)
                    .notifyUserOnFailure(false)
                    .userNotificationMessage("Custom error message")
                    .build();

            assertEquals(5, strategy.maxRetries());
            assertEquals(Duration.ofSeconds(3), strategy.retryDelay());
            assertFalse(strategy.exponentialBackoff());
            assertFalse(strategy.notifyUserOnFailure());
            assertEquals("Custom error message", strategy.getNotificationMessage());
        }

        @Test
        @DisplayName("builder() can set dead letter handler")
        void builder_setsDLQHandler() {
            AtomicBoolean handlerCalled = new AtomicBoolean(false);

            ErrorHandlingStrategy strategy = ErrorHandlingStrategy.builder()
                    .deadLetterHandler((userId, messages) -> handlerCalled.set(true))
                    .build();

            assertTrue(strategy.hasDLQHandler());
            
            // Test handler invocation
            strategy.deadLetterHandler().ifPresent(handler -> 
                    handler.accept("user123", List.of()));
            
            assertTrue(handlerCalled.get());
        }
    }

    @Nested
    @DisplayName("Validation")
    class ValidationTests {

        @Test
        @DisplayName("valid strategy passes validation")
        void validStrategy_passesValidation() {
            ErrorHandlingStrategy strategy = ErrorHandlingStrategy.defaults();

            Set<ConstraintViolation<ErrorHandlingStrategy>> violations = 
                    validator.validate(strategy);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("negative maxRetries fails validation")
        void negativeMaxRetries_failsValidation() {
            assertThrows(IllegalArgumentException.class, () -> 
                    new ErrorHandlingStrategy(
                            -1,
                            Duration.ofSeconds(2),
                            true,
                            true,
                            null,
                            null
                    ));
        }

        @Test
        @DisplayName("maxRetries exceeding maximum fails validation")
        void maxRetriesExceedingMax_failsValidation() {
            ErrorHandlingStrategy strategy = ErrorHandlingStrategy.builder()
                    .maxRetries(11) // Exceeds max of 10
                    .build();

            Set<ConstraintViolation<ErrorHandlingStrategy>> violations = 
                    validator.validate(strategy);

            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("maxRetries")));
        }

        @Test
        @DisplayName("null retryDelay fails construction")
        void nullRetryDelay_failsConstruction() {
            assertThrows(IllegalArgumentException.class, () -> 
                    new ErrorHandlingStrategy(
                            3,
                            null,
                            true,
                            true,
                            null,
                            null
                    ));
        }

        @Test
        @DisplayName("negative retryDelay fails construction")
        void negativeRetryDelay_failsConstruction() {
            assertThrows(IllegalArgumentException.class, () -> 
                    new ErrorHandlingStrategy(
                            3,
                            Duration.ofSeconds(-1),
                            true,
                            true,
                            null,
                            null
                    ));
        }
    }

    @Nested
    @DisplayName("Retry Delay Calculation")
    class RetryDelayCalculationTests {

        @Test
        @DisplayName("calculateDelay() with exponential backoff increases exponentially")
        void calculateDelay_exponentialBackoff_increases() {
            ErrorHandlingStrategy strategy = ErrorHandlingStrategy.builder()
                    .retryDelay(Duration.ofSeconds(2))
                    .exponentialBackoff(true)
                    .build();

            assertEquals(Duration.ofSeconds(2), strategy.calculateDelay(1));  // 2^0 * 2 = 2
            assertEquals(Duration.ofSeconds(4), strategy.calculateDelay(2));  // 2^1 * 2 = 4
            assertEquals(Duration.ofSeconds(8), strategy.calculateDelay(3));  // 2^2 * 2 = 8
            assertEquals(Duration.ofSeconds(16), strategy.calculateDelay(4)); // 2^3 * 2 = 16
        }

        @Test
        @DisplayName("calculateDelay() without exponential backoff is constant")
        void calculateDelay_noExponential_constant() {
            ErrorHandlingStrategy strategy = ErrorHandlingStrategy.builder()
                    .retryDelay(Duration.ofSeconds(3))
                    .exponentialBackoff(false)
                    .build();

            assertEquals(Duration.ofSeconds(3), strategy.calculateDelay(1));
            assertEquals(Duration.ofSeconds(3), strategy.calculateDelay(2));
            assertEquals(Duration.ofSeconds(3), strategy.calculateDelay(3));
            assertEquals(Duration.ofSeconds(3), strategy.calculateDelay(4));
        }

        @Test
        @DisplayName("calculateDelay() with attempt 0 or less returns zero")
        void calculateDelay_nonPositiveAttempt_returnsZero() {
            ErrorHandlingStrategy strategy = ErrorHandlingStrategy.defaults();

            assertEquals(Duration.ZERO, strategy.calculateDelay(0));
            assertEquals(Duration.ZERO, strategy.calculateDelay(-1));
        }

        @Test
        @DisplayName("calculateDelay() with large attempt number doesn't overflow")
        void calculateDelay_largeAttempt_noOverflow() {
            ErrorHandlingStrategy strategy = ErrorHandlingStrategy.builder()
                    .retryDelay(Duration.ofMillis(100))
                    .exponentialBackoff(true)
                    .build();

            // Should not throw
            assertDoesNotThrow(() -> strategy.calculateDelay(10));
        }
    }

    @Nested
    @DisplayName("Notification Message")
    class NotificationMessageTests {

        @Test
        @DisplayName("getNotificationMessage() returns custom message when set")
        void getNotificationMessage_customMessage_returns() {
            String customMessage = "Please try again later";
            ErrorHandlingStrategy strategy = ErrorHandlingStrategy.builder()
                    .userNotificationMessage(customMessage)
                    .build();

            assertEquals(customMessage, strategy.getNotificationMessage());
        }

        @Test
        @DisplayName("getNotificationMessage() returns default when not set")
        void getNotificationMessage_noCustom_returnsDefault() {
            ErrorHandlingStrategy strategy = ErrorHandlingStrategy.defaults();

            String message = strategy.getNotificationMessage();
            
            assertNotNull(message);
            assertFalse(message.isBlank());
            assertTrue(message.contains("Desculpe") || message.contains("tente novamente"));
        }

        @Test
        @DisplayName("getNotificationMessage() returns non-empty default")
        void getNotificationMessage_defaultNotEmpty() {
            ErrorHandlingStrategy strategy = ErrorHandlingStrategy.builder()
                    .build();

            String message = strategy.getNotificationMessage();
            
            assertFalse(message.isBlank());
        }
    }

    @Nested
    @DisplayName("Dead Letter Queue")
    class DeadLetterQueueTests {

        @Test
        @DisplayName("hasDLQHandler() returns true when handler is set")
        void hasDLQHandler_whenSet_returnsTrue() {
            ErrorHandlingStrategy strategy = ErrorHandlingStrategy.builder()
                    .deadLetterHandler((userId, messages) -> {})
                    .build();

            assertTrue(strategy.hasDLQHandler());
        }

        @Test
        @DisplayName("hasDLQHandler() returns false when handler is not set")
        void hasDLQHandler_whenNotSet_returnsFalse() {
            ErrorHandlingStrategy strategy = ErrorHandlingStrategy.defaults();

            assertFalse(strategy.hasDLQHandler());
        }

        @Test
        @DisplayName("deadLetterHandler is invokable when present")
        void deadLetterHandler_invokable() {
            AtomicReference<String> capturedUserId = new AtomicReference<>();
            AtomicReference<List<InboundMessage>> capturedMessages = new AtomicReference<>();

            ErrorHandlingStrategy strategy = ErrorHandlingStrategy.builder()
                    .deadLetterHandler((userId, messages) -> {
                        capturedUserId.set(userId);
                        capturedMessages.set(messages);
                    })
                    .build();

            List<InboundMessage> testMessages = List.of(
                    MockMessageFactory.createTextMessage("user123", "Test")
            );

            strategy.deadLetterHandler().ifPresent(handler -> 
                    handler.accept("user123", testMessages));

            assertEquals("user123", capturedUserId.get());
            assertEquals(testMessages, capturedMessages.get());
        }

        @Test
        @DisplayName("deadLetterHandler is empty when not set")
        void deadLetterHandler_notSet_isEmpty() {
            ErrorHandlingStrategy strategy = ErrorHandlingStrategy.defaults();

            assertTrue(strategy.deadLetterHandler().isEmpty());
        }
    }

    @Nested
    @DisplayName("Accessors")
    class AccessorTests {

        @Test
        @DisplayName("maxRetries() returns configured value")
        void maxRetries_returnsValue() {
            ErrorHandlingStrategy strategy = ErrorHandlingStrategy.builder()
                    .maxRetries(7)
                    .build();

            assertEquals(7, strategy.maxRetries());
        }

        @Test
        @DisplayName("retryDelay() returns configured value")
        void retryDelay_returnsValue() {
            Duration delay = Duration.ofSeconds(5);
            ErrorHandlingStrategy strategy = ErrorHandlingStrategy.builder()
                    .retryDelay(delay)
                    .build();

            assertEquals(delay, strategy.retryDelay());
        }

        @Test
        @DisplayName("exponentialBackoff() returns configured value")
        void exponentialBackoff_returnsValue() {
            ErrorHandlingStrategy strategy = ErrorHandlingStrategy.builder()
                    .exponentialBackoff(false)
                    .build();

            assertFalse(strategy.exponentialBackoff());
        }

        @Test
        @DisplayName("notifyUserOnFailure() returns configured value")
        void notifyUserOnFailure_returnsValue() {
            ErrorHandlingStrategy strategy = ErrorHandlingStrategy.builder()
                    .notifyUserOnFailure(false)
                    .build();

            assertFalse(strategy.notifyUserOnFailure());
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("zero retries is valid")
        void zeroRetries_isValid() {
            ErrorHandlingStrategy strategy = ErrorHandlingStrategy.builder()
                    .maxRetries(0)
                    .build();

            Set<ConstraintViolation<ErrorHandlingStrategy>> violations = 
                    validator.validate(strategy);

            assertTrue(violations.isEmpty());
            assertEquals(0, strategy.maxRetries());
        }

        @Test
        @DisplayName("maximum retries (10) is valid")
        void maxRetries_isValid() {
            ErrorHandlingStrategy strategy = ErrorHandlingStrategy.builder()
                    .maxRetries(10)
                    .build();

            Set<ConstraintViolation<ErrorHandlingStrategy>> violations = 
                    validator.validate(strategy);

            assertTrue(violations.isEmpty());
            assertEquals(10, strategy.maxRetries());
        }

        @Test
        @DisplayName("zero delay is valid")
        void zeroDelay_isValid() {
            ErrorHandlingStrategy strategy = ErrorHandlingStrategy.builder()
                    .retryDelay(Duration.ZERO)
                    .build();

            assertDoesNotThrow(() -> validator.validate(strategy));
            assertEquals(Duration.ZERO, strategy.retryDelay());
        }
    }

    @Nested
    @DisplayName("Real-World Scenarios")
    class RealWorldScenariosTests {

        @Test
        @DisplayName("create strategy for critical messages")
        void criticalMessages_strategy() {
            ErrorHandlingStrategy strategy = ErrorHandlingStrategy.builder()
                    .maxRetries(5)
                    .retryDelay(Duration.ofSeconds(1))
                    .exponentialBackoff(true)
                    .notifyUserOnFailure(true)
                    .deadLetterHandler((userId, messages) -> {
                        // Log to DLQ
                    })
                    .build();

            assertEquals(5, strategy.maxRetries());
            assertTrue(strategy.exponentialBackoff());
            assertTrue(strategy.hasDLQHandler());
        }

        @Test
        @DisplayName("create strategy for non-critical messages")
        void nonCriticalMessages_strategy() {
            ErrorHandlingStrategy strategy = ErrorHandlingStrategy.builder()
                    .maxRetries(1)
                    .retryDelay(Duration.ofSeconds(1))
                    .exponentialBackoff(false)
                    .notifyUserOnFailure(false)
                    .build();

            assertEquals(1, strategy.maxRetries());
            assertFalse(strategy.exponentialBackoff());
            assertFalse(strategy.notifyUserOnFailure());
        }

        @Test
        @DisplayName("exponential backoff delays grow reasonably")
        void exponentialBackoff_reasonableGrowth() {
            ErrorHandlingStrategy strategy = ErrorHandlingStrategy.builder()
                    .retryDelay(Duration.ofSeconds(1))
                    .exponentialBackoff(true)
                    .maxRetries(5)
                    .build();

            // Verify delays: 1s, 2s, 4s, 8s, 16s
            Duration delay1 = strategy.calculateDelay(1);
            Duration delay2 = strategy.calculateDelay(2);
            Duration delay3 = strategy.calculateDelay(3);
            Duration delay4 = strategy.calculateDelay(4);
            Duration delay5 = strategy.calculateDelay(5);

            assertTrue(delay2.toSeconds() == delay1.toSeconds() * 2);
            assertTrue(delay3.toSeconds() == delay2.toSeconds() * 2);
            assertTrue(delay4.toSeconds() == delay3.toSeconds() * 2);
            assertTrue(delay5.toSeconds() == delay4.toSeconds() * 2);
        }
    }
}
