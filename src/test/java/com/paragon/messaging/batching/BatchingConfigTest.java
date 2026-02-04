package com.paragon.messaging.batching;

import com.paragon.messaging.error.ErrorHandlingStrategy;
import com.paragon.messaging.ratelimit.RateLimitConfig;
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
 * Tests for {@link BatchingConfig}.
 */
@DisplayName("BatchingConfig")
class BatchingConfigTest {

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
            BatchingConfig config = BatchingConfig.defaults();

            assertNotNull(config);
            assertEquals(Duration.ofSeconds(5), config.adaptiveTimeout());
            assertEquals(Duration.ofSeconds(1), config.silenceThreshold());
            assertEquals(50, config.maxBufferSize());
            assertEquals(BackpressureStrategy.FLUSH_AND_ACCEPT, config.backpressureStrategy());
            assertNotNull(config.rateLimitConfig());
        }

        @Test
        @DisplayName("builder() allows custom configuration")
        void builder_allowsCustomConfiguration() {
            BatchingConfig config = BatchingConfig.builder()
                    .adaptiveTimeout(Duration.ofSeconds(3))
                    .silenceThreshold(Duration.ofMillis(800))
                    .maxBufferSize(10)
                    .backpressureStrategy(BackpressureStrategy.DROP_NEW)
                    .build();

            assertEquals(Duration.ofSeconds(3), config.adaptiveTimeout());
            assertEquals(Duration.ofMillis(800), config.silenceThreshold());
            assertEquals(10, config.maxBufferSize());
            assertEquals(BackpressureStrategy.DROP_NEW, config.backpressureStrategy());
        }

        @Test
        @DisplayName("builder() can set rate limit config")
        void builder_setsRateLimitConfig() {
            RateLimitConfig rateLimitConfig = RateLimitConfig.builder()
                    .tokensPerMinute(20)
                    .build();

            BatchingConfig config = BatchingConfig.builder()
                    .rateLimitConfig(rateLimitConfig)
                    .build();

            assertEquals(rateLimitConfig, config.rateLimitConfig());
        }

        @Test
        @DisplayName("builder() can set error handling strategy")
        void builder_setsErrorHandlingStrategy() {
            ErrorHandlingStrategy errorStrategy = ErrorHandlingStrategy.builder()
                    .maxRetries(5)
                    .build();

            BatchingConfig config = BatchingConfig.builder()
                    .errorHandlingStrategy(errorStrategy)
                    .build();

            assertEquals(errorStrategy, config.errorHandlingStrategy());
        }
    }

    @Nested
    @DisplayName("Validation")
    class ValidationTests {

        @Test
        @DisplayName("valid config passes validation")
        void validConfig_passesValidation() {
            BatchingConfig config = BatchingConfig.defaults();

            Set<ConstraintViolation<BatchingConfig>> violations = validator.validate(config);

            assertTrue(violations.isEmpty(), "Valid config should have no violations");
        }

        @Test
        @DisplayName("maxBufferSize exceeding maximum fails validation")
        void maxBufferSize_exceedingMax_failsValidation() {
            BatchingConfig config = BatchingConfig.builder()
                    .maxBufferSize(15000) // Exceeds max of 10000
                    .build();

            Set<ConstraintViolation<BatchingConfig>> violations = validator.validate(config);

            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("maxBufferSize")));
        }

        @Test
        @DisplayName("maxBufferSize below minimum fails validation")
        void maxBufferSize_belowMin_failsValidation() {
            BatchingConfig config = BatchingConfig.builder()
                    .maxBufferSize(0) // Below min of 1
                    .build();

            Set<ConstraintViolation<BatchingConfig>> violations = validator.validate(config);

            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("maxBufferSize")));
        }

        @Test
        @DisplayName("nested rate limit config is validated")
        void nestedRateLimitConfig_isValidated() {
            RateLimitConfig invalidRateLimitConfig = RateLimitConfig.builder()
                    .tokensPerMinute(20000) // Exceeds max
                    .build();

            BatchingConfig config = BatchingConfig.builder()
                    .rateLimitConfig(invalidRateLimitConfig)
                    .build();

            Set<ConstraintViolation<BatchingConfig>> violations = validator.validate(config);

            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().contains("rateLimitConfig")));
        }

        @Test
        @DisplayName("nested error handling strategy is validated")
        void nestedErrorHandlingStrategy_isValidated() {
            ErrorHandlingStrategy invalidStrategy = ErrorHandlingStrategy.builder()
                    .maxRetries(15) // Exceeds max of 10
                    .build();

            BatchingConfig config = BatchingConfig.builder()
                    .errorHandlingStrategy(invalidStrategy)
                    .build();

            Set<ConstraintViolation<BatchingConfig>> violations = validator.validate(config);

            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().contains("errorHandlingStrategy")));
        }
    }

    @Nested
    @DisplayName("Accessors")
    class AccessorTests {

        @Test
        @DisplayName("adaptiveTimeout() returns configured value")
        void adaptiveTimeout_returnsValue() {
            Duration timeout = Duration.ofSeconds(3);
            BatchingConfig config = BatchingConfig.builder()
                    .adaptiveTimeout(timeout)
                    .build();

            assertEquals(timeout, config.adaptiveTimeout());
        }

        @Test
        @DisplayName("silenceThreshold() returns configured value")
        void silenceThreshold_returnsValue() {
            Duration threshold = Duration.ofMillis(500);
            BatchingConfig config = BatchingConfig.builder()
                    .silenceThreshold(threshold)
                    .build();

            assertEquals(threshold, config.silenceThreshold());
        }

        @Test
        @DisplayName("maxBufferSize() returns configured value")
        void maxBufferSize_returnsValue() {
            int size = 25;
            BatchingConfig config = BatchingConfig.builder()
                    .maxBufferSize(size)
                    .build();

            assertEquals(size, config.maxBufferSize());
        }

        @Test
        @DisplayName("backpressureStrategy() returns configured value")
        void backpressureStrategy_returnsValue() {
            BackpressureStrategy strategy = BackpressureStrategy.DROP_OLDEST;
            BatchingConfig config = BatchingConfig.builder()
                    .backpressureStrategy(strategy)
                    .build();

            assertEquals(strategy, config.backpressureStrategy());
        }
    }
}
