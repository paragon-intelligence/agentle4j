package com.paragon.messaging.config;

import com.paragon.messaging.batching.BackpressureStrategy;
import com.paragon.messaging.batching.BatchingConfig;
import com.paragon.messaging.error.ErrorHandlingStrategy;
import com.paragon.messaging.ratelimit.RateLimitConfig;
import com.paragon.messaging.security.SecurityConfig;
import com.paragon.messaging.whatsapp.config.TTSConfig;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test validation annotations for configuration classes.
 */
class ConfigurationValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void testBatchingConfigValidation() {
        // Test with invalid max buffer size (> 10000)
        BatchingConfig invalidConfig = BatchingConfig.builder()
                .maxBufferSize(15000)  // Exceeds max
                .build();

        Set<ConstraintViolation<BatchingConfig>> violations = validator.validate(invalidConfig);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("maxBufferSize")));
    }

    @Test
    void testRateLimitConfigValidation() {
        // Test with invalid tokensPerMinute (> 10000)
        RateLimitConfig invalidConfig = RateLimitConfig.builder()
                .tokensPerMinute(20000)  // Exceeds max
                .build();

        Set<ConstraintViolation<RateLimitConfig>> violations = validator.validate(invalidConfig);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("tokensPerMinute")));
    }

    @Test
    void testSecurityConfigValidation() {
        // Test with token too short (< 10 characters)
        SecurityConfig invalidConfig = SecurityConfig.builder()
                .webhookVerifyToken("short")  // Too short
                .build();

        Set<ConstraintViolation<SecurityConfig>> violations = validator.validate(invalidConfig);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("webhookVerifyToken")));
    }

    @Test
    void testErrorHandlingStrategyValidation() {
        // Test with maxRetries > 10
        ErrorHandlingStrategy invalidStrategy = ErrorHandlingStrategy.builder()
                .maxRetries(15)  // Exceeds max
                .build();

        Set<ConstraintViolation<ErrorHandlingStrategy>> violations = validator.validate(invalidStrategy);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("maxRetries")));
    }

    @Test
    void testTTSConfigValidation() {
        // Test with invalid speechChance (> 1.0)
        TTSConfig invalidConfig = TTSConfig.builder()
                .speechChance(1.5)  // Exceeds max
                .languageCode("pt-BR")
                .build();

        Set<ConstraintViolation<TTSConfig>> violations = validator.validate(invalidConfig);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("speechChance")));
    }

    @Test
    void testTTSConfigInvalidLanguageCode() {
        // Test with invalid language code format
        TTSConfig invalidConfig = TTSConfig.builder()
                .languageCode("PT-BR")  // Wrong case
                .build();

        Set<ConstraintViolation<TTSConfig>> violations = validator.validate(invalidConfig);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("languageCode")));
    }

    @Test
    void testValidConfigurationsPass() {
        // Test that valid configurations pass validation
        BatchingConfig validConfig = BatchingConfig.defaults();
        Set<ConstraintViolation<BatchingConfig>> violations = validator.validate(validConfig);
        assertTrue(violations.isEmpty(), "Valid config should have no violations");
    }

    @Test
    void testNestedValidation() {
        // Test that @Valid cascades to nested objects
        BatchingConfig configWithInvalidNested = BatchingConfig.builder()
                .rateLimitConfig(RateLimitConfig.builder()
                        .tokensPerMinute(20000)  // Invalid
                        .build())
                .build();

        Set<ConstraintViolation<BatchingConfig>> violations = validator.validate(configWithInvalidNested);
        assertFalse(violations.isEmpty());
        // Should have a violation in the nested rateLimitConfig
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().contains("rateLimitConfig")));
    }
}
