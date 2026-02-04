package com.paragon.messaging.ratelimit;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link RateLimitConfig}.
 */
@DisplayName("RateLimitConfig")
class RateLimitConfigTest {

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
        @DisplayName("defaults() creates config with default values")
        void defaults_createsDefaultConfig() {
            RateLimitConfig config = RateLimitConfig.defaults();

            assertNotNull(config);
            assertEquals(30, config.tokensPerMinute());
            assertEquals(10, config.bucketCapacity());
            assertEquals(Duration.ofMinutes(1), config.slidingWindow());
            assertEquals(50, config.maxMessagesInWindow());
        }

        @Test
        @DisplayName("builder() allows custom configuration")
        void builder_allowsCustomConfiguration() {
            RateLimitConfig config = RateLimitConfig.builder()
                    .tokensPerMinute(60)
                    .bucketCapacity(20)
                    .slidingWindow(Duration.ofSeconds(30))
                    .maxMessagesInWindow(100)
                    .build();

            assertEquals(60, config.tokensPerMinute());
            assertEquals(20, config.bucketCapacity());
            assertEquals(Duration.ofSeconds(30), config.slidingWindow());
            assertEquals(100, config.maxMessagesInWindow());
        }

        @Test
        @DisplayName("builder() with minimal values")
        void builder_withMinimalValues() {
            RateLimitConfig config = RateLimitConfig.builder()
                    .tokensPerMinute(1)
                    .bucketCapacity(1)
                    .maxMessagesInWindow(1)
                    .build();

            assertEquals(1, config.tokensPerMinute());
            assertEquals(1, config.bucketCapacity());
            assertEquals(1, config.maxMessagesInWindow());
        }
    }

    @Nested
    @DisplayName("Validation")
    class ValidationTests {

        @Test
        @DisplayName("valid config passes validation")
        void validConfig_passesValidation() {
            RateLimitConfig config = RateLimitConfig.defaults();

            Set<ConstraintViolation<RateLimitConfig>> violations = validator.validate(config);

            assertTrue(violations.isEmpty(), "Valid config should have no violations");
        }

        @Test
        @DisplayName("tokensPerMinute below minimum fails validation")
        void tokensPerMinute_belowMin_failsValidation() {
            RateLimitConfig config = RateLimitConfig.builder()
                    .tokensPerMinute(0) // Below min of 1
                    .build();

            Set<ConstraintViolation<RateLimitConfig>> violations = validator.validate(config);

            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("tokensPerMinute")));
        }

        @Test
        @DisplayName("tokensPerMinute exceeding maximum fails validation")
        void tokensPerMinute_exceedingMax_failsValidation() {
            RateLimitConfig config = RateLimitConfig.builder()
                    .tokensPerMinute(10001) // Exceeds max of 10000
                    .build();

            Set<ConstraintViolation<RateLimitConfig>> violations = validator.validate(config);

            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("tokensPerMinute")));
        }

        @Test
        @DisplayName("bucketCapacity below minimum fails validation")
        void bucketCapacity_belowMin_failsValidation() {
            RateLimitConfig config = RateLimitConfig.builder()
                    .bucketCapacity(0)
                    .build();

            Set<ConstraintViolation<RateLimitConfig>> violations = validator.validate(config);

            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("bucketCapacity")));
        }

        @Test
        @DisplayName("bucketCapacity exceeding maximum fails validation")
        void bucketCapacity_exceedingMax_failsValidation() {
            RateLimitConfig config = RateLimitConfig.builder()
                    .bucketCapacity(1001) // Exceeds max of 1000
                    .build();

            Set<ConstraintViolation<RateLimitConfig>> violations = validator.validate(config);

            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("bucketCapacity")));
        }

        @Test
        @DisplayName("maxMessagesInWindow below minimum fails validation")
        void maxMessagesInWindow_belowMin_failsValidation() {
            RateLimitConfig config = RateLimitConfig.builder()
                    .maxMessagesInWindow(0)
                    .build();

            Set<ConstraintViolation<RateLimitConfig>> violations = validator.validate(config);

            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("maxMessagesInWindow")));
        }

        @Test
        @DisplayName("maxMessagesInWindow exceeding maximum fails validation")
        void maxMessagesInWindow_exceedingMax_failsValidation() {
            RateLimitConfig config = RateLimitConfig.builder()
                    .maxMessagesInWindow(10001)
                    .build();

            Set<ConstraintViolation<RateLimitConfig>> violations = validator.validate(config);

            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("maxMessagesInWindow")));
        }

        @Test
        @DisplayName("slidingWindow not null")
        void slidingWindow_notNull() {
            RateLimitConfig config = RateLimitConfig.builder()
                    .slidingWindow(Duration.ofSeconds(30))
                    .build();

            assertNotNull(config.slidingWindow());
        }
    }

    @Nested
    @DisplayName("Accessors")
    class AccessorTests {

        @Test
        @DisplayName("tokensPerMinute() returns configured value")
        void tokensPerMinute_returnsValue() {
            RateLimitConfig config = RateLimitConfig.builder()
                    .tokensPerMinute(120)
                    .build();

            assertEquals(120, config.tokensPerMinute());
        }

        @Test
        @DisplayName("bucketCapacity() returns configured value")
        void bucketCapacity_returnsValue() {
            RateLimitConfig config = RateLimitConfig.builder()
                    .bucketCapacity(25)
                    .build();

            assertEquals(25, config.bucketCapacity());
        }

        @Test
        @DisplayName("slidingWindow() returns configured value")
        void slidingWindow_returnsValue() {
            Duration window = Duration.ofSeconds(45);
            RateLimitConfig config = RateLimitConfig.builder()
                    .slidingWindow(window)
                    .build();

            assertEquals(window, config.slidingWindow());
        }

        @Test
        @DisplayName("maxMessagesInWindow() returns configured value")
        void maxMessagesInWindow_returnsValue() {
            RateLimitConfig config = RateLimitConfig.builder()
                    .maxMessagesInWindow(75)
                    .build();

            assertEquals(75, config.maxMessagesInWindow());
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("maximum valid values accepted")
        void maximumValidValues_accepted() {
            RateLimitConfig config = RateLimitConfig.builder()
                    .tokensPerMinute(10000)
                    .bucketCapacity(1000)
                    .maxMessagesInWindow(10000)
                    .build();

            Set<ConstraintViolation<RateLimitConfig>> violations = validator.validate(config);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("minimum valid values accepted")
        void minimumValidValues_accepted() {
            RateLimitConfig config = RateLimitConfig.builder()
                    .tokensPerMinute(1)
                    .bucketCapacity(1)
                    .maxMessagesInWindow(1)
                    .build();

            Set<ConstraintViolation<RateLimitConfig>> violations = validator.validate(config);

            assertTrue(violations.isEmpty());
        }
    }
}
