package com.paragon.responses;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.http.RetryPolicy;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link RetryPolicy}. */
class RetryPolicyTest {

  @Test
  void defaults_returnsExpectedValues() {
    RetryPolicy policy = RetryPolicy.defaults();

    assertEquals(3, policy.maxRetries());
    assertEquals(Duration.ofSeconds(1), policy.initialDelay());
    assertEquals(Duration.ofSeconds(30), policy.maxDelay());
    assertEquals(2.0, policy.multiplier());
    assertEquals(Set.of(429, 500, 502, 503, 504, 529), policy.retryableStatusCodes());
  }

  @Test
  void disabled_returnsZeroMaxRetries() {
    RetryPolicy policy = RetryPolicy.disabled();

    assertEquals(0, policy.maxRetries());
  }

  @Test
  void builder_createsCustomPolicy() {
    RetryPolicy policy =
        RetryPolicy.builder()
            .maxRetries(5)
            .initialDelay(Duration.ofMillis(500))
            .maxDelay(Duration.ofSeconds(60))
            .multiplier(3.0)
            .retryableStatusCodes(Set.of(429, 503))
            .build();

    assertEquals(5, policy.maxRetries());
    assertEquals(Duration.ofMillis(500), policy.initialDelay());
    assertEquals(Duration.ofSeconds(60), policy.maxDelay());
    assertEquals(3.0, policy.multiplier());
    assertEquals(Set.of(429, 503), policy.retryableStatusCodes());
  }

  @Test
  void constructor_throwsOnNegativeMaxRetries() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new RetryPolicy(
                -1,
                Duration.ofSeconds(1),
                Duration.ofSeconds(30),
                2.0,
                RetryPolicy.DEFAULT_RETRYABLE_STATUS_CODES));
  }

  @Test
  void constructor_throwsOnMultiplierLessThanOne() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new RetryPolicy(
                3,
                Duration.ofSeconds(1),
                Duration.ofSeconds(30),
                0.5,
                RetryPolicy.DEFAULT_RETRYABLE_STATUS_CODES));
  }

  @Test
  void constructor_throwsOnNullInitialDelay() {
    assertThrows(
        NullPointerException.class,
        () ->
            new RetryPolicy(
                3, null, Duration.ofSeconds(30), 2.0, RetryPolicy.DEFAULT_RETRYABLE_STATUS_CODES));
  }

  @Test
  void constructor_throwsOnNullMaxDelay() {
    assertThrows(
        NullPointerException.class,
        () ->
            new RetryPolicy(
                3, Duration.ofSeconds(1), null, 2.0, RetryPolicy.DEFAULT_RETRYABLE_STATUS_CODES));
  }

  @Test
  void constructor_throwsOnNullStatusCodes() {
    assertThrows(
        NullPointerException.class,
        () -> new RetryPolicy(3, Duration.ofSeconds(1), Duration.ofSeconds(30), 2.0, null));
  }

  @Test
  void getDelayForAttempt_calculatesExponentialBackoff() {
    RetryPolicy policy =
        RetryPolicy.builder()
            .initialDelay(Duration.ofMillis(100))
            .maxDelay(Duration.ofSeconds(10))
            .multiplier(2.0)
            .build();

    // Attempt 1: 100ms
    assertEquals(Duration.ofMillis(100), policy.getDelayForAttempt(1));
    // Attempt 2: 200ms
    assertEquals(Duration.ofMillis(200), policy.getDelayForAttempt(2));
    // Attempt 3: 400ms
    assertEquals(Duration.ofMillis(400), policy.getDelayForAttempt(3));
    // Attempt 4: 800ms
    assertEquals(Duration.ofMillis(800), policy.getDelayForAttempt(4));
  }

  @Test
  void getDelayForAttempt_capsAtMaxDelay() {
    RetryPolicy policy =
        RetryPolicy.builder()
            .initialDelay(Duration.ofSeconds(1))
            .maxDelay(Duration.ofSeconds(5))
            .multiplier(2.0)
            .build();

    // Attempt 1: 1s
    assertEquals(Duration.ofSeconds(1), policy.getDelayForAttempt(1));
    // Attempt 2: 2s
    assertEquals(Duration.ofSeconds(2), policy.getDelayForAttempt(2));
    // Attempt 3: 4s
    assertEquals(Duration.ofSeconds(4), policy.getDelayForAttempt(3));
    // Attempt 4: would be 8s, but capped at 5s
    assertEquals(Duration.ofSeconds(5), policy.getDelayForAttempt(4));
    // Attempt 5: still capped at 5s
    assertEquals(Duration.ofSeconds(5), policy.getDelayForAttempt(5));
  }

  @Test
  void getDelayForAttempt_returnsInitialDelayForZeroOrNegative() {
    RetryPolicy policy = RetryPolicy.defaults();

    assertEquals(policy.initialDelay(), policy.getDelayForAttempt(0));
    assertEquals(policy.initialDelay(), policy.getDelayForAttempt(-1));
  }

  @Test
  void isRetryable_returnsTrueForConfiguredStatusCodes() {
    RetryPolicy policy = RetryPolicy.defaults();

    assertTrue(policy.isRetryable(429));
    assertTrue(policy.isRetryable(500));
    assertTrue(policy.isRetryable(502));
    assertTrue(policy.isRetryable(503));
    assertTrue(policy.isRetryable(504));
  }

  @Test
  void isRetryable_returnsFalseForNonRetryableStatusCodes() {
    RetryPolicy policy = RetryPolicy.defaults();

    assertFalse(policy.isRetryable(400));
    assertFalse(policy.isRetryable(401));
    assertFalse(policy.isRetryable(403));
    assertFalse(policy.isRetryable(404));
    assertFalse(policy.isRetryable(200));
  }

  @Test
  void isRetryable_usesCustomStatusCodes() {
    RetryPolicy policy = RetryPolicy.builder().retryableStatusCodes(Set.of(418, 503)).build();

    assertTrue(policy.isRetryable(418));
    assertTrue(policy.isRetryable(503));
    assertFalse(policy.isRetryable(429));
    assertFalse(policy.isRetryable(500));
  }
}
