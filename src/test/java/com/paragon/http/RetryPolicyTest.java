package com.paragon.http;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for {@link RetryPolicy}.
 *
 * <p>Tests cover: factory methods, builder, delay calculation, and retryability checks.
 */
@DisplayName("RetryPolicy Tests")
class RetryPolicyTest {

  // ═══════════════════════════════════════════════════════════════════════════
  // FACTORY METHODS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Factory Methods")
  class FactoryMethodTests {

    @Test
    @DisplayName("defaults() returns sensible defaults")
    void defaultsReturnsSensibleDefaults() {
      RetryPolicy policy = RetryPolicy.defaults();

      assertEquals(3, policy.maxRetries());
      assertEquals(Duration.ofSeconds(1), policy.initialDelay());
      assertEquals(Duration.ofSeconds(30), policy.maxDelay());
      assertEquals(2.0, policy.multiplier());
      assertTrue(policy.retryableStatusCodes().contains(429));
      assertTrue(policy.retryableStatusCodes().contains(500));
      assertTrue(policy.retryableStatusCodes().contains(502));
      assertTrue(policy.retryableStatusCodes().contains(503));
      assertTrue(policy.retryableStatusCodes().contains(504));
      assertTrue(policy.retryableStatusCodes().contains(529));
    }

    @Test
    @DisplayName("disabled() returns policy with no retries")
    void disabledReturnsNoRetriesPolicy() {
      RetryPolicy policy = RetryPolicy.disabled();

      assertEquals(0, policy.maxRetries());
    }

    @Test
    @DisplayName("none() is alias for disabled()")
    void noneIsAliasForDisabled() {
      RetryPolicy none = RetryPolicy.none();
      RetryPolicy disabled = RetryPolicy.disabled();

      assertEquals(none.maxRetries(), disabled.maxRetries());
    }

    @Test
    @DisplayName("builder() returns new builder")
    void builderReturnsNewBuilder() {
      RetryPolicy.Builder builder = RetryPolicy.builder();
      assertNotNull(builder);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // BUILDER
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Builder")
  class BuilderTests {

    @Test
    @DisplayName("builds policy with custom maxRetries")
    void buildsWithCustomMaxRetries() {
      RetryPolicy policy = RetryPolicy.builder().maxRetries(5).build();
      assertEquals(5, policy.maxRetries());
    }

    @Test
    @DisplayName("builds policy with custom initialDelay")
    void buildsWithCustomInitialDelay() {
      RetryPolicy policy = RetryPolicy.builder().initialDelay(Duration.ofMillis(500)).build();
      assertEquals(Duration.ofMillis(500), policy.initialDelay());
    }

    @Test
    @DisplayName("builds policy with custom maxDelay")
    void buildsWithCustomMaxDelay() {
      RetryPolicy policy = RetryPolicy.builder().maxDelay(Duration.ofMinutes(1)).build();
      assertEquals(Duration.ofMinutes(1), policy.maxDelay());
    }

    @Test
    @DisplayName("builds policy with custom multiplier")
    void buildsWithCustomMultiplier() {
      RetryPolicy policy = RetryPolicy.builder().multiplier(3.0).build();
      assertEquals(3.0, policy.multiplier());
    }

    @Test
    @DisplayName("builds policy with custom retryableStatusCodes")
    void buildsWithCustomRetryableStatusCodes() {
      Set<Integer> codes = Set.of(429, 503);
      RetryPolicy policy = RetryPolicy.builder().retryableStatusCodes(codes).build();
      assertEquals(codes, policy.retryableStatusCodes());
    }

    @Test
    @DisplayName("builder chains fluently")
    void builderChainsFluently() {
      RetryPolicy policy =
          RetryPolicy.builder()
              .maxRetries(10)
              .initialDelay(Duration.ofMillis(100))
              .maxDelay(Duration.ofSeconds(60))
              .multiplier(1.5)
              .retryableStatusCodes(Set.of(429, 500))
              .build();

      assertEquals(10, policy.maxRetries());
      assertEquals(Duration.ofMillis(100), policy.initialDelay());
      assertEquals(Duration.ofSeconds(60), policy.maxDelay());
      assertEquals(1.5, policy.multiplier());
      assertEquals(Set.of(429, 500), policy.retryableStatusCodes());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // VALIDATION
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Validation")
  class ValidationTests {

    @Test
    @DisplayName("rejects negative maxRetries")
    void rejectsNegativeMaxRetries() {
      assertThrows(
          IllegalArgumentException.class, () -> RetryPolicy.builder().maxRetries(-1).build());
    }

    @Test
    @DisplayName("accepts zero maxRetries")
    void acceptsZeroMaxRetries() {
      RetryPolicy policy = RetryPolicy.builder().maxRetries(0).build();
      assertEquals(0, policy.maxRetries());
    }

    @Test
    @DisplayName("rejects multiplier less than 1.0")
    void rejectsMultiplierLessThan1() {
      assertThrows(
          IllegalArgumentException.class, () -> RetryPolicy.builder().multiplier(0.5).build());
    }

    @Test
    @DisplayName("accepts multiplier of exactly 1.0")
    void acceptsMultiplierOf1() {
      RetryPolicy policy = RetryPolicy.builder().multiplier(1.0).build();
      assertEquals(1.0, policy.multiplier());
    }

    @Test
    @DisplayName("rejects null initialDelay")
    void rejectsNullInitialDelay() {
      assertThrows(
          NullPointerException.class, () -> RetryPolicy.builder().initialDelay(null).build());
    }

    @Test
    @DisplayName("rejects null maxDelay")
    void rejectsNullMaxDelay() {
      assertThrows(NullPointerException.class, () -> RetryPolicy.builder().maxDelay(null).build());
    }

    @Test
    @DisplayName("rejects null retryableStatusCodes")
    void rejectsNullRetryableStatusCodes() {
      assertThrows(
          NullPointerException.class,
          () -> RetryPolicy.builder().retryableStatusCodes(null).build());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // DELAY CALCULATION
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Delay Calculation")
  class DelayCalculationTests {

    @Test
    @DisplayName("getDelayForAttempt returns initialDelay for attempt 1")
    void getDelayForAttempt1ReturnsInitialDelay() {
      RetryPolicy policy = RetryPolicy.builder().initialDelay(Duration.ofMillis(100)).build();

      assertEquals(Duration.ofMillis(100), policy.getDelayForAttempt(1));
    }

    @Test
    @DisplayName("getDelayForAttempt applies exponential backoff")
    void getDelayForAttemptAppliesExponentialBackoff() {
      RetryPolicy policy =
          RetryPolicy.builder()
              .initialDelay(Duration.ofMillis(100))
              .multiplier(2.0)
              .maxDelay(Duration.ofSeconds(10))
              .build();

      assertEquals(Duration.ofMillis(100), policy.getDelayForAttempt(1)); // 100 * 2^0
      assertEquals(Duration.ofMillis(200), policy.getDelayForAttempt(2)); // 100 * 2^1
      assertEquals(Duration.ofMillis(400), policy.getDelayForAttempt(3)); // 100 * 2^2
      assertEquals(Duration.ofMillis(800), policy.getDelayForAttempt(4)); // 100 * 2^3
    }

    @Test
    @DisplayName("getDelayForAttempt caps at maxDelay")
    void getDelayForAttemptCapsAtMaxDelay() {
      RetryPolicy policy =
          RetryPolicy.builder()
              .initialDelay(Duration.ofMillis(100))
              .multiplier(2.0)
              .maxDelay(Duration.ofMillis(500))
              .build();

      assertEquals(Duration.ofMillis(500), policy.getDelayForAttempt(10));
    }

    @Test
    @DisplayName("getDelayForAttempt handles attempt < 1")
    void getDelayForAttemptHandlesAttemptLessThan1() {
      RetryPolicy policy = RetryPolicy.builder().initialDelay(Duration.ofMillis(100)).build();

      assertEquals(Duration.ofMillis(100), policy.getDelayForAttempt(0));
      assertEquals(Duration.ofMillis(100), policy.getDelayForAttempt(-1));
    }

    @Test
    @DisplayName("delayForAttempt returns milliseconds")
    void delayForAttemptReturnsMillis() {
      RetryPolicy policy = RetryPolicy.builder().initialDelay(Duration.ofMillis(250)).build();

      assertEquals(250L, policy.delayForAttempt(1));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // RETRYABILITY
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Retryability")
  class RetryabilityTests {

    @Test
    @DisplayName("isRetryable returns true for retryable status codes")
    void isRetryableReturnsTrueForRetryableCodes() {
      RetryPolicy policy = RetryPolicy.defaults();

      assertTrue(policy.isRetryable(429));
      assertTrue(policy.isRetryable(500));
      assertTrue(policy.isRetryable(502));
      assertTrue(policy.isRetryable(503));
      assertTrue(policy.isRetryable(504));
      assertTrue(policy.isRetryable(529));
    }

    @Test
    @DisplayName("isRetryable returns false for non-retryable status codes")
    void isRetryableReturnsFalseForNonRetryableCodes() {
      RetryPolicy policy = RetryPolicy.defaults();

      assertFalse(policy.isRetryable(200));
      assertFalse(policy.isRetryable(201));
      assertFalse(policy.isRetryable(400));
      assertFalse(policy.isRetryable(401));
      assertFalse(policy.isRetryable(403));
      assertFalse(policy.isRetryable(404));
    }

    @Test
    @DisplayName("shouldRetry returns false when attempt >= maxRetries")
    void shouldRetryReturnsFalseWhenMaxRetriesExceeded() {
      RetryPolicy policy = RetryPolicy.builder().maxRetries(3).build();
      HttpException error = HttpException.fromResponse(429, "Rate limited", "GET", "/api/test");

      assertFalse(policy.shouldRetry(error, 3)); // 3 >= 3
      assertFalse(policy.shouldRetry(error, 4)); // 4 >= 3
    }

    @Test
    @DisplayName("shouldRetry returns true for network errors (status -1)")
    void shouldRetryReturnsTrueForNetworkErrors() {
      RetryPolicy policy = RetryPolicy.builder().maxRetries(3).build();
      HttpException error =
          HttpException.networkFailure(
              "Network error", new java.io.IOException("Connection refused"));

      assertTrue(policy.shouldRetry(error, 1));
      assertTrue(policy.shouldRetry(error, 2));
    }

    @Test
    @DisplayName("shouldRetry returns true for retryable status codes")
    void shouldRetryReturnsTrueForRetryableStatusCodes() {
      RetryPolicy policy = RetryPolicy.builder().maxRetries(3).build();
      HttpException error = HttpException.fromResponse(429, "Rate limited", "GET", "/api/test");

      assertTrue(policy.shouldRetry(error, 1));
      assertTrue(policy.shouldRetry(error, 2));
    }

    @Test
    @DisplayName("shouldRetry returns false for non-retryable status codes")
    void shouldRetryReturnsFalseForNonRetryableStatusCodes() {
      RetryPolicy policy = RetryPolicy.builder().maxRetries(3).build();
      HttpException error = HttpException.fromResponse(400, "Bad request", "GET", "/api/test");

      assertFalse(policy.shouldRetry(error, 1));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // RECORD FUNCTIONALITY
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Record Functionality")
  class RecordFunctionalityTests {

    @Test
    @DisplayName("equals and hashCode work correctly")
    void equalsAndHashCodeWork() {
      RetryPolicy policy1 = RetryPolicy.defaults();
      RetryPolicy policy2 = RetryPolicy.defaults();

      assertEquals(policy1, policy2);
      assertEquals(policy1.hashCode(), policy2.hashCode());
    }

    @Test
    @DisplayName("toString returns meaningful representation")
    void toStringReturnsMeaningfulRepresentation() {
      RetryPolicy policy = RetryPolicy.defaults();
      String str = policy.toString();

      assertTrue(str.contains("maxRetries"));
      assertTrue(str.contains("3"));
    }

    @Test
    @DisplayName("different policies are not equal")
    void differentPoliciesAreNotEqual() {
      RetryPolicy policy1 = RetryPolicy.builder().maxRetries(3).build();
      RetryPolicy policy2 = RetryPolicy.builder().maxRetries(5).build();

      assertNotEquals(policy1, policy2);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // DEFAULT STATUS CODES
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Default Status Codes")
  class DefaultStatusCodesTests {

    @Test
    @DisplayName("DEFAULT_RETRYABLE_STATUS_CODES contains expected values")
    void defaultRetryableStatusCodesContainsExpectedValues() {
      Set<Integer> expected = Set.of(429, 500, 502, 503, 504, 529);
      assertEquals(expected, RetryPolicy.DEFAULT_RETRYABLE_STATUS_CODES);
    }

    @Test
    @DisplayName("DEFAULT_RETRYABLE_STATUS_CODES is immutable")
    void defaultRetryableStatusCodesIsImmutable() {
      assertThrows(
          UnsupportedOperationException.class,
          () -> RetryPolicy.DEFAULT_RETRYABLE_STATUS_CODES.add(418));
    }
  }
}
