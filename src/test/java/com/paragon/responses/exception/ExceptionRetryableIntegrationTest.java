package com.paragon.responses.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Integration tests verifying the isRetryable() behavior across all exception types. */
@DisplayName("Exception Retryability Integration")
class ExceptionRetryableIntegrationTest {

  @Nested
  @DisplayName("Retryable Exceptions")
  class RetryableExceptions {

    @Test
    @DisplayName("RateLimitException should be retryable")
    void rateLimitShouldBeRetryable() {
      RateLimitException e = new RateLimitException("Rate limited", null, null, null);
      assertTrue(e.isRetryable());
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 502, 503, 504})
    @DisplayName("ServerException should be retryable for all 5xx codes")
    void serverExceptionsShouldBeRetryable(int statusCode) {
      ServerException e = new ServerException(statusCode, "Server error", null, null);
      assertTrue(e.isRetryable());
    }

    @Test
    @DisplayName("StreamingException from connectionDropped should be retryable")
    void connectionDroppedShouldBeRetryable() {
      StreamingException e =
          StreamingException.connectionDropped(new java.io.IOException("Reset"), null, 0);
      assertTrue(e.isRetryable());
    }

    @Test
    @DisplayName("StreamingException from timeout should be retryable")
    void timeoutShouldBeRetryable() {
      StreamingException e = StreamingException.timeout(null, 0);
      assertTrue(e.isRetryable());
    }
  }

  @Nested
  @DisplayName("Non-Retryable Exceptions")
  class NonRetryableExceptions {

    @ParameterizedTest
    @ValueSource(ints = {401, 403})
    @DisplayName("AuthenticationException should not be retryable")
    void authenticationShouldNotBeRetryable(int statusCode) {
      AuthenticationException e = new AuthenticationException(statusCode, "Auth error", null, null);
      assertFalse(e.isRetryable());
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 404, 405, 422})
    @DisplayName("InvalidRequestException should not be retryable")
    void invalidRequestShouldNotBeRetryable(int statusCode) {
      InvalidRequestException e =
          new InvalidRequestException(statusCode, "Bad request", null, null);
      assertFalse(e.isRetryable());
    }

    @Test
    @DisplayName("GuardrailException should not be retryable")
    void guardrailShouldNotBeRetryable() {
      GuardrailException e = GuardrailException.inputViolation("Blocked");
      assertFalse(e.isRetryable());
    }

    @Test
    @DisplayName("ConfigurationException should not be retryable")
    void configurationShouldNotBeRetryable() {
      ConfigurationException e = ConfigurationException.missing("apiKey");
      assertFalse(e.isRetryable());
    }

    @Test
    @DisplayName("ToolExecutionException should not be retryable")
    void toolExecutionShouldNotBeRetryable() {
      ToolExecutionException e = new ToolExecutionException("tool", null, null, "Failed");
      assertFalse(e.isRetryable());
    }
  }

  @Nested
  @DisplayName("Exception Hierarchy")
  class ExceptionHierarchy {

    @Test
    @DisplayName("all API exceptions should extend ApiException")
    void apiExceptionHierarchy() {
      assertInstanceOf(ApiException.class, new RateLimitException("", null, null, null));
      assertInstanceOf(ApiException.class, new AuthenticationException(401, "", null, null));
      assertInstanceOf(ApiException.class, new ServerException(500, "", null, null));
      assertInstanceOf(ApiException.class, new InvalidRequestException(400, "", null, null));
    }

    @Test
    @DisplayName("all exceptions should extend AgentleException")
    void agentleExceptionHierarchy() {
      assertInstanceOf(AgentleException.class, new RateLimitException("", null, null, null));
      assertInstanceOf(AgentleException.class, new AuthenticationException(401, "", null, null));
      assertInstanceOf(AgentleException.class, new ServerException(500, "", null, null));
      assertInstanceOf(AgentleException.class, new InvalidRequestException(400, "", null, null));
      assertInstanceOf(AgentleException.class, StreamingException.timeout(null, 0));
      assertInstanceOf(AgentleException.class, GuardrailException.inputViolation("test"));
      assertInstanceOf(AgentleException.class, ConfigurationException.missing("test"));
      assertInstanceOf(AgentleException.class, new ToolExecutionException("t", null, null, "e"));
    }

    @Test
    @DisplayName("all exceptions should extend RuntimeException")
    void runtimeExceptionHierarchy() {
      assertInstanceOf(RuntimeException.class, new RateLimitException("", null, null, null));
      assertInstanceOf(
          RuntimeException.class,
          new StreamingException(AgentleException.ErrorCode.STREAM_TIMEOUT, "", null, 0, false));
      assertInstanceOf(RuntimeException.class, GuardrailException.outputViolation("test"));
    }
  }

  @Nested
  @DisplayName("fromStatusCode Factory")
  class FromStatusCodeFactory {

    @Test
    @DisplayName("should create correct exception type for each status code")
    void shouldCreateCorrectTypes() {
      assertInstanceOf(
          RateLimitException.class, ApiException.fromStatusCode(429, "Rate limit", null, null));

      assertInstanceOf(
          AuthenticationException.class,
          ApiException.fromStatusCode(401, "Unauthorized", null, null));

      assertInstanceOf(
          AuthenticationException.class, ApiException.fromStatusCode(403, "Forbidden", null, null));

      assertInstanceOf(
          InvalidRequestException.class,
          ApiException.fromStatusCode(400, "Bad request", null, null));

      assertInstanceOf(
          InvalidRequestException.class,
          ApiException.fromStatusCode(422, "Unprocessable", null, null));

      assertInstanceOf(
          ServerException.class, ApiException.fromStatusCode(500, "Internal error", null, null));

      assertInstanceOf(
          ServerException.class, ApiException.fromStatusCode(503, "Unavailable", null, null));
    }

    @Test
    @DisplayName("retryable status should create retryable exceptions")
    void retryableStatusShouldCreateRetryable() {
      assertTrue(ApiException.fromStatusCode(429, "", null, null).isRetryable());
      assertTrue(ApiException.fromStatusCode(500, "", null, null).isRetryable());
      assertTrue(ApiException.fromStatusCode(502, "", null, null).isRetryable());
      assertTrue(ApiException.fromStatusCode(503, "", null, null).isRetryable());
      assertTrue(ApiException.fromStatusCode(504, "", null, null).isRetryable());
    }

    @Test
    @DisplayName("non-retryable status should create non-retryable exceptions")
    void nonRetryableStatusShouldCreateNonRetryable() {
      assertFalse(ApiException.fromStatusCode(400, "", null, null).isRetryable());
      assertFalse(ApiException.fromStatusCode(401, "", null, null).isRetryable());
      assertFalse(ApiException.fromStatusCode(403, "", null, null).isRetryable());
      assertFalse(ApiException.fromStatusCode(404, "", null, null).isRetryable());
      assertFalse(ApiException.fromStatusCode(422, "", null, null).isRetryable());
    }
  }
}
