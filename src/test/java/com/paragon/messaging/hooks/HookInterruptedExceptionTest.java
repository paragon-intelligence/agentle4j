package com.paragon.messaging.hooks;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link HookInterruptedException}.
 */
@DisplayName("HookInterruptedException")
class HookInterruptedExceptionTest {

    @Nested
    @DisplayName("Construction")
    class ConstructionTests {

        @Test
        @DisplayName("constructor with reason only")
        void constructor_reasonOnly() {
            HookInterruptedException exception = new HookInterruptedException("Test reason");

            assertEquals("Test reason", exception.getMessage());
            assertEquals("Test reason", exception.getReason());
            assertNull(exception.getReasonCode());
        }

        @Test
        @DisplayName("constructor with reason and code")
        void constructor_reasonAndCode() {
            HookInterruptedException exception = new HookInterruptedException(
                    "Inappropriate content",
                    "CONTENT_MODERATION"
            );

            assertEquals("Inappropriate content", exception.getMessage());
            assertEquals("Inappropriate content", exception.getReason());
            assertEquals("CONTENT_MODERATION", exception.getReasonCode());
        }

        @Test
        @DisplayName("constructor with null reason code")
        void constructor_nullReasonCode() {
            HookInterruptedException exception = new HookInterruptedException("Reason", null);

            assertEquals("Reason", exception.getReason());
            assertNull(exception.getReasonCode());
        }
    }

    @Nested
    @DisplayName("Accessors")
    class AccessorTests {

        @Test
        @DisplayName("getReason() returns message")
        void getReason_returnsMessage() {
            HookInterruptedException exception = new HookInterruptedException("My reason");

            assertEquals("My reason", exception.getReason());
        }

        @Test
        @DisplayName("getMessage() returns reason")
        void getMessage_returnsReason() {
            HookInterruptedException exception = new HookInterruptedException("My reason");

            assertEquals("My reason", exception.getMessage());
        }

        @Test
        @DisplayName("getReasonCode() returns code when set")
        void getReasonCode_whenSet_returnsCode() {
            HookInterruptedException exception = new HookInterruptedException(
                    "Reason",
                    "ERROR_CODE"
            );

            assertEquals("ERROR_CODE", exception.getReasonCode());
        }

        @Test
        @DisplayName("getReasonCode() returns null when not set")
        void getReasonCode_whenNotSet_returnsNull() {
            HookInterruptedException exception = new HookInterruptedException("Reason");

            assertNull(exception.getReasonCode());
        }
    }

    @Nested
    @DisplayName("Exception Behavior")
    class ExceptionBehaviorTests {

        @Test
        @DisplayName("is throwable")
        void isThrowable() {
            assertThrows(HookInterruptedException.class, () -> {
                throw new HookInterruptedException("Test");
            });
        }

        @Test
        @DisplayName("is instance of Exception")
        void isException() {
            HookInterruptedException exception = new HookInterruptedException("Test");

            assertTrue(exception instanceof Exception);
        }

        @Test
        @DisplayName("can be caught as Exception")
        void canBeCaughtAsException() {
            try {
                throw new HookInterruptedException("Test");
            } catch (Exception e) {
                assertTrue(e instanceof HookInterruptedException);
                assertEquals("Test", e.getMessage());
            }
        }

        @Test
        @DisplayName("preserves message in stack trace")
        void preservesMessageInStackTrace() {
            HookInterruptedException exception = new HookInterruptedException(
                    "Important message",
                    "CODE"
            );

            String stackTrace = exception.toString();
            assertTrue(stackTrace.contains("Important message"));
        }
    }

    @Nested
    @DisplayName("Real-World Use Cases")
    class RealWorldUseCasesTests {

        @Test
        @DisplayName("content moderation scenario")
        void contentModerationScenario() {
            HookInterruptedException exception = new HookInterruptedException(
                    "Spam content detected",
                    "CONTENT_MODERATION"
            );

            assertEquals("Spam content detected", exception.getReason());
            assertEquals("CONTENT_MODERATION", exception.getReasonCode());
        }

        @Test
        @DisplayName("rate limiting scenario")
        void rateLimitingScenario() {
            HookInterruptedException exception = new HookInterruptedException(
                    "User exceeded rate limit",
                    "RATE_LIMIT"
            );

            assertEquals("User exceeded rate limit", exception.getReason());
            assertEquals("RATE_LIMIT", exception.getReasonCode());
        }

        @Test
        @DisplayName("validation failure scenario")
        void validationFailureScenario() {
            HookInterruptedException exception = new HookInterruptedException(
                    "Invalid message format",
                    "VALIDATION_ERROR"
            );

            assertEquals("Invalid message format", exception.getReason());
            assertEquals("VALIDATION_ERROR", exception.getReasonCode());
        }

        @Test
        @DisplayName("business rule violation scenario")
        void businessRuleViolationScenario() {
            HookInterruptedException exception = new HookInterruptedException(
                    "User not allowed in this channel",
                    "ACCESS_DENIED"
            );

            assertEquals("User not allowed in this channel", exception.getReason());
            assertEquals("ACCESS_DENIED", exception.getReasonCode());
        }
    }

    @Nested
    @DisplayName("Reason Code Patterns")
    class ReasonCodePatternsTests {

        @Test
        @DisplayName("uppercase snake_case reason codes")
        void uppercaseSnakeCaseReasonCodes() {
            HookInterruptedException exception = new HookInterruptedException(
                    "Error",
                    "CONTENT_MODERATION_SPAM"
            );

            assertEquals("CONTENT_MODERATION_SPAM", exception.getReasonCode());
        }

        @Test
        @DisplayName("reason codes for categorization")
        void reasonCodesForCategorization() {
            HookInterruptedException mod = new HookInterruptedException("msg", "MODERATION");
            HookInterruptedException limit = new HookInterruptedException("msg", "RATE_LIMIT");
            HookInterruptedException val = new HookInterruptedException("msg", "VALIDATION");

            assertNotEquals(mod.getReasonCode(), limit.getReasonCode());
            assertNotEquals(limit.getReasonCode(), val.getReasonCode());
        }
    }

    @Nested
    @DisplayName("Integration with Hook Processing")
    class IntegrationTests {

        @Test
        @DisplayName("thrown from ProcessingHook")
        void thrownFromHook() {
            ProcessingHook hook = context -> {
                throw new HookInterruptedException("Stop processing", "TEST");
            };

            HookContext context = HookContext.create("user123", List.of());

            HookInterruptedException exception = assertThrows(
                    HookInterruptedException.class,
                    () -> hook.execute(context)
            );

            assertEquals("Stop processing", exception.getReason());
            assertEquals("TEST", exception.getReasonCode());
        }

        @Test
        @DisplayName("thrown conditionally from hook")
        void thrownConditionally() {
            ProcessingHook hook = context -> {
                if (context.batchSize() > 10) {
                    throw new HookInterruptedException("Too many messages", "LIMIT");
                }
            };

            HookContext smallBatch = HookContext.create("user123", List.of());
            assertDoesNotThrow(() -> hook.execute(smallBatch));
        }
    }
}
