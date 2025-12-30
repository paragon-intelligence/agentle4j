package com.paragon.responses.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests for {@link ApiException} and its subclasses. */
@DisplayName("ApiException")
class ApiExceptionTest {

  @Nested
  @DisplayName("fromStatusCode factory")
  class FromStatusCode {

    @Test
    @DisplayName("should return RateLimitException for 429")
    void shouldReturnRateLimitFor429() {
      ApiException e = ApiException.fromStatusCode(429, "Too many requests", "req-123", "{error}");

      assertInstanceOf(RateLimitException.class, e);
      assertEquals(429, e.statusCode());
      assertEquals("req-123", e.requestId());
      assertEquals("{error}", e.responseBody());
      assertTrue(e.isRetryable());
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403})
    @DisplayName("should return AuthenticationException for 401/403")
    void shouldReturnAuthExceptionFor401And403(int statusCode) {
      ApiException e = ApiException.fromStatusCode(statusCode, "Unauthorized", null, null);

      assertInstanceOf(AuthenticationException.class, e);
      assertEquals(statusCode, e.statusCode());
      assertFalse(e.isRetryable());
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 404, 405, 422})
    @DisplayName("should return InvalidRequestException for 4xx client errors")
    void shouldReturnInvalidRequestFor4xx(int statusCode) {
      ApiException e = ApiException.fromStatusCode(statusCode, "Bad request", null, null);

      assertInstanceOf(InvalidRequestException.class, e);
      assertEquals(statusCode, e.statusCode());
      assertFalse(e.isRetryable());
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 502, 503, 504})
    @DisplayName("should return ServerException for 5xx errors")
    void shouldReturnServerExceptionFor5xx(int statusCode) {
      ApiException e = ApiException.fromStatusCode(statusCode, "Server error", null, null);

      assertInstanceOf(ServerException.class, e);
      assertEquals(statusCode, e.statusCode());
      assertTrue(e.isRetryable());
    }

    @Test
    @DisplayName("should return generic ApiException for unknown status codes")
    void shouldReturnGenericForUnknown() {
      ApiException e = ApiException.fromStatusCode(418, "I'm a teapot", null, null);

      assertEquals(ApiException.class, e.getClass());
      assertEquals(418, e.statusCode());
      assertFalse(e.isRetryable());
    }
  }

  @Nested
  @DisplayName("RateLimitException")
  class RateLimitTests {

    @Test
    @DisplayName("should have retryAfter duration")
    void shouldHaveRetryAfter() {
      java.time.Duration retryAfter = java.time.Duration.ofSeconds(30);
      RateLimitException e = new RateLimitException("Rate limited", "req-1", null, retryAfter);

      assertEquals(retryAfter, e.retryAfter());
      assertTrue(e.isRetryable());
      assertNotNull(e.suggestion());
      assertTrue(e.suggestion().contains("30"));
    }

    @Test
    @DisplayName("should handle null retryAfter")
    void shouldHandleNullRetryAfter() {
      RateLimitException e = new RateLimitException("Rate limited", null, null, null);

      assertNull(e.retryAfter());
      assertTrue(e.suggestion().contains("moment"));
    }
  }

  @Nested
  @DisplayName("AuthenticationException")
  class AuthenticationTests {

    @Test
    @DisplayName("should use AUTHENTICATION_FAILED code for 401")
    void shouldUseAuthFailedFor401() {
      AuthenticationException e = new AuthenticationException(401, "Unauthorized", null, null);

      assertEquals(AgentleException.ErrorCode.AUTHENTICATION_FAILED, e.code());
      assertTrue(e.suggestion().contains("valid"));
    }

    @Test
    @DisplayName("should use AUTHORIZATION_FAILED code for 403")
    void shouldUseAuthorizationFailedFor403() {
      AuthenticationException e = new AuthenticationException(403, "Forbidden", null, null);

      assertEquals(AgentleException.ErrorCode.AUTHORIZATION_FAILED, e.code());
      assertTrue(e.suggestion().contains("permissions"));
    }
  }

  @Nested
  @DisplayName("ServerException")
  class ServerTests {

    @Test
    @DisplayName("should use SERVICE_UNAVAILABLE code for 503")
    void shouldUseServiceUnavailableFor503() {
      ServerException e = new ServerException(503, "Service unavailable", null, null);

      assertEquals(AgentleException.ErrorCode.SERVICE_UNAVAILABLE, e.code());
      assertTrue(e.isRetryable());
    }

    @Test
    @DisplayName("should use SERVER_ERROR code for other 5xx")
    void shouldUseServerErrorForOther5xx() {
      ServerException e = new ServerException(500, "Internal error", null, null);

      assertEquals(AgentleException.ErrorCode.SERVER_ERROR, e.code());
      assertTrue(e.isRetryable());
    }
  }

  @Nested
  @DisplayName("InvalidRequestException")
  class InvalidRequestTests {

    @Test
    @DisplayName("should create with status code")
    void shouldCreateWithStatusCode() {
      InvalidRequestException e = new InvalidRequestException(400, "Bad request", "req-1", null);

      assertEquals(400, e.statusCode());
      assertEquals(AgentleException.ErrorCode.INVALID_REQUEST, e.code());
      assertFalse(e.isRetryable());
    }

    @Test
    @DisplayName("should create with cause")
    void shouldCreateWithCause() {
      Exception cause = new IllegalArgumentException("Invalid param");
      InvalidRequestException e = new InvalidRequestException("Bad input", cause);

      assertEquals(cause, e.getCause());
      assertEquals(400, e.statusCode());
    }
  }

  @Nested
  @DisplayName("Inheritance")
  class InheritanceTests {

    @Test
    @DisplayName("all subclasses should extend ApiException")
    void allSubclassesShouldExtendApiException() {
      assertInstanceOf(ApiException.class, new RateLimitException("", null, null, null));
      assertInstanceOf(ApiException.class, new AuthenticationException(401, "", null, null));
      assertInstanceOf(ApiException.class, new ServerException(500, "", null, null));
      assertInstanceOf(ApiException.class, new InvalidRequestException(400, "", null, null));
    }

    @Test
    @DisplayName("ApiException should extend AgentleException")
    void apiExceptionShouldExtendAgentleException() {
      ApiException e = ApiException.fromStatusCode(500, "Error", null, null);
      assertInstanceOf(AgentleException.class, e);
    }
  }
}
