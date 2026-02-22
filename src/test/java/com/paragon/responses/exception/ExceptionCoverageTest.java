package com.paragon.responses.exception;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Coverage tests for exception types that need additional test coverage. */
@DisplayName("Exception Coverage Tests")
class ExceptionCoverageTest {

  // =========================================================================
  // AuthenticationException Tests
  // =========================================================================
  @Nested
  @DisplayName("AuthenticationException")
  class AuthenticationExceptionTest {

    @Test
    @DisplayName("should create 401 authentication exception")
    void shouldCreate401AuthenticationException() {
      AuthenticationException exception =
          new AuthenticationException(401, "Invalid API key", "req-123", "{\"error\":\"invalid\"}");

      assertEquals(401, exception.statusCode());
      assertEquals("Invalid API key", exception.getMessage());
      assertEquals("req-123", exception.requestId());
      assertEquals("{\"error\":\"invalid\"}", exception.responseBody());
      assertFalse(exception.isRetryable());
      assertTrue(exception.suggestion().contains("API key is valid"));
    }

    @Test
    @DisplayName("should create 403 authorization exception")
    void shouldCreate403AuthorizationException() {
      AuthenticationException exception =
          new AuthenticationException(403, "Insufficient permissions", null, null);

      assertEquals(403, exception.statusCode());
      assertEquals("Insufficient permissions", exception.getMessage());
      assertNull(exception.requestId());
      assertNull(exception.responseBody());
      assertFalse(exception.isRetryable());
      assertTrue(exception.suggestion().contains("permissions"));
    }

    @Test
    @DisplayName("should extend ApiException")
    void shouldExtendApiException() {
      AuthenticationException exception =
          new AuthenticationException(401, "Auth error", null, null);

      assertInstanceOf(ApiException.class, exception);
    }
  }

  // =========================================================================
  // InvalidRequestException Tests
  // =========================================================================
  @Nested
  @DisplayName("InvalidRequestException")
  class InvalidRequestExceptionTest {

    @Test
    @DisplayName("should create invalid request exception with status code")
    void shouldCreateInvalidRequestExceptionWithStatusCode() {
      InvalidRequestException exception =
          new InvalidRequestException(400, "Invalid JSON", "req-456", "{\"error\":\"parse\"}");

      assertEquals(400, exception.statusCode());
      assertEquals("Invalid JSON", exception.getMessage());
      assertEquals("req-456", exception.requestId());
      assertEquals("{\"error\":\"parse\"}", exception.responseBody());
      assertFalse(exception.isRetryable());
      assertTrue(exception.suggestion().contains("payload"));
    }

    @Test
    @DisplayName("should create invalid request exception with cause")
    void shouldCreateInvalidRequestExceptionWithCause() {
      Throwable cause = new RuntimeException("JSON parsing failed");
      InvalidRequestException exception = new InvalidRequestException("Bad request", cause);

      assertEquals("Bad request", exception.getMessage());
      assertEquals(cause, exception.getCause());
      assertEquals(400, exception.statusCode());
      assertFalse(exception.isRetryable());
    }

    @Test
    @DisplayName("should handle other 4xx status codes")
    void shouldHandleOther4xxStatusCodes() {
      InvalidRequestException exception =
          new InvalidRequestException(404, "Model not found", null, null);

      assertEquals(404, exception.statusCode());
      assertEquals("Model not found", exception.getMessage());
    }

    @Test
    @DisplayName("should extend ApiException")
    void shouldExtendApiException() {
      InvalidRequestException exception = new InvalidRequestException(400, "Invalid", null, null);

      assertInstanceOf(ApiException.class, exception);
    }
  }

  // =========================================================================
  // RateLimitException Tests
  // =========================================================================
  @Nested
  @DisplayName("RateLimitException")
  class RateLimitExceptionTest {

    @Test
    @DisplayName("should create rate limit exception with retry-after")
    void shouldCreateRateLimitExceptionWithRetryAfter() {
      Duration retryAfter = Duration.ofSeconds(30);
      RateLimitException exception =
          new RateLimitException("Rate limit exceeded", "req-789", null, retryAfter);

      assertEquals(429, exception.statusCode());
      assertEquals("Rate limit exceeded", exception.getMessage());
      assertEquals("req-789", exception.requestId());
      assertEquals(retryAfter, exception.retryAfter());
      assertTrue(exception.isRetryable());
      assertTrue(exception.suggestion().contains("30 seconds"));
    }

    @Test
    @DisplayName("should create rate limit exception without retry-after")
    void shouldCreateRateLimitExceptionWithoutRetryAfter() {
      RateLimitException exception = new RateLimitException("Too many requests", null, null, null);

      assertEquals(429, exception.statusCode());
      assertNull(exception.retryAfter());
      assertTrue(exception.isRetryable());
      assertTrue(exception.suggestion().contains("moment"));
    }

    @Test
    @DisplayName("should handle various retry-after durations")
    void shouldHandleVariousRetryAfterDurations() {
      RateLimitException shortWait =
          new RateLimitException("Rate limited", null, null, Duration.ofSeconds(5));
      RateLimitException longWait =
          new RateLimitException("Rate limited", null, null, Duration.ofMinutes(5));

      assertEquals(5, shortWait.retryAfter().toSeconds());
      assertEquals(300, longWait.retryAfter().toSeconds());
    }

    @Test
    @DisplayName("should extend ApiException")
    void shouldExtendApiException() {
      RateLimitException exception = new RateLimitException("Rate limited", null, null, null);

      assertInstanceOf(ApiException.class, exception);
    }
  }

  // =========================================================================
  // ServerException Tests
  // =========================================================================
  @Nested
  @DisplayName("ServerException")
  class ServerExceptionTest {

    @Test
    @DisplayName("should create 500 server exception")
    void shouldCreate500ServerException() {
      ServerException exception =
          new ServerException(500, "Internal server error", "req-001", "{\"error\":\"internal\"}");

      assertEquals(500, exception.statusCode());
      assertEquals("Internal server error", exception.getMessage());
      assertEquals("req-001", exception.requestId());
      assertEquals("{\"error\":\"internal\"}", exception.responseBody());
      assertTrue(exception.isRetryable());
      assertTrue(exception.suggestion().contains("temporary"));
    }

    @Test
    @DisplayName("should create 502 bad gateway exception")
    void shouldCreate502BadGatewayException() {
      ServerException exception = new ServerException(502, "Bad Gateway", null, null);

      assertEquals(502, exception.statusCode());
      assertTrue(exception.isRetryable());
    }

    @Test
    @DisplayName("should create 503 service unavailable exception")
    void shouldCreate503ServiceUnavailableException() {
      ServerException exception =
          new ServerException(503, "Service temporarily unavailable", null, null);

      assertEquals(503, exception.statusCode());
      assertEquals("Service temporarily unavailable", exception.getMessage());
      assertTrue(exception.isRetryable());
    }

    @Test
    @DisplayName("should create 504 gateway timeout exception")
    void shouldCreate504GatewayTimeoutException() {
      ServerException exception = new ServerException(504, "Gateway Timeout", null, null);

      assertEquals(504, exception.statusCode());
      assertTrue(exception.isRetryable());
    }

    @Test
    @DisplayName("should extend ApiException")
    void shouldExtendApiException() {
      ServerException exception = new ServerException(500, "Error", null, null);

      assertInstanceOf(ApiException.class, exception);
    }
  }

  // =========================================================================
  // Edge Cases
  // =========================================================================
  @Nested
  @DisplayName("Edge Cases")
  class EdgeCasesTest {

    @Test
    @DisplayName("should handle null request id and response body")
    void shouldHandleNullRequestIdAndResponseBody() {
      AuthenticationException auth = new AuthenticationException(401, "Auth failed", null, null);
      InvalidRequestException invalid = new InvalidRequestException(400, "Invalid", null, null);
      RateLimitException rate = new RateLimitException("Rate limit", null, null, null);
      ServerException server = new ServerException(500, "Server error", null, null);

      assertNull(auth.requestId());
      assertNull(invalid.responseBody());
      assertNull(rate.retryAfter());
      assertNull(server.requestId());
    }

    @Test
    @DisplayName("should handle empty message")
    void shouldHandleEmptyMessage() {
      ServerException exception = new ServerException(500, "", null, null);
      assertEquals("", exception.getMessage());
    }

    @Test
    @DisplayName("should handle very long response body")
    void shouldHandleVeryLongResponseBody() {
      String longBody = "x".repeat(10000);
      ServerException exception = new ServerException(500, "Error", null, longBody);
      assertEquals(longBody, exception.responseBody());
    }

    @Test
    @DisplayName("should handle minimum retry-after duration")
    void shouldHandleMinimumRetryAfterDuration() {
      RateLimitException exception =
          new RateLimitException("Rate limited", null, null, Duration.ofSeconds(0));
      assertEquals(0, exception.retryAfter().toSeconds());
    }
  }
}
