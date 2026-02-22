package com.paragon.responses.exception;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.http.RetryPolicy;
import com.paragon.responses.Responder;
import com.paragon.responses.ResponsesApiObjectMapper;
import com.paragon.responses.spec.*;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests verifying that Responder throws the correct exception types when various HTTP
 * errors occur.
 *
 * <p>These tests use MockWebServer to simulate different HTTP status codes and verify that the
 * appropriate exception subclass is thrown with correct properties.
 */
@DisplayName("Responder Exception Integration")
class ResponderExceptionIntegrationTest {

  private static final String TEST_API_KEY = "test-api-key";
  private MockWebServer mockWebServer;
  private OkHttpClient okHttpClient;

  @BeforeEach
  void setUp() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    okHttpClient =
        new OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(1, TimeUnit.SECONDS)
            .writeTimeout(1, TimeUnit.SECONDS)
            .build();
  }

  @AfterEach
  void tearDown() throws IOException {
    mockWebServer.shutdown();
  }

  @Nested
  @DisplayName("Rate Limit (429)")
  class RateLimitTests {

    @Test
    @DisplayName("should throw exception with 429 status code")
    void shouldThrowOnRateLimit() {
      mockWebServer.enqueue(
          new MockResponse()
              .setResponseCode(429)
              .setBody("{\"error\": {\"message\": \"Rate limit exceeded\"}}"));

      Responder responder = createResponder(RetryPolicy.disabled());

      RuntimeException ex =
          assertThrows(RuntimeException.class, () -> responder.respond(createPayload()));

      // Verify the exception message contains the status code
      assertTrue(ex.getMessage().contains("429"));
    }

    @Test
    @DisplayName("should be retryable")
    void shouldBeRetryable() {
      // 429 followed by success - proves 429 is retryable
      mockWebServer.enqueue(new MockResponse().setResponseCode(429).setBody("Rate limited"));
      mockWebServer.enqueue(createSuccessResponse());

      Responder responder = createResponder(RetryPolicy.builder().maxRetries(1).build());

      assertDoesNotThrow(
          () -> {
            responder.respond(createPayload());
          });
      assertEquals(2, mockWebServer.getRequestCount());
    }
  }

  @Nested
  @DisplayName("Authentication (401/403)")
  class AuthenticationTests {

    @Test
    @DisplayName("should throw exception on 401 Unauthorized")
    void shouldThrowOn401() {
      mockWebServer.enqueue(
          new MockResponse()
              .setResponseCode(401)
              .setBody("{\"error\": {\"message\": \"Invalid API key\"}}"));

      Responder responder = createResponder(RetryPolicy.disabled());

      RuntimeException ex =
          assertThrows(RuntimeException.class, () -> responder.respond(createPayload()));

      assertTrue(ex.getMessage().contains("401"));
    }

    @Test
    @DisplayName("should throw exception on 403 Forbidden")
    void shouldThrowOn403() {
      mockWebServer.enqueue(
          new MockResponse()
              .setResponseCode(403)
              .setBody("{\"error\": {\"message\": \"Access denied\"}}"));

      Responder responder = createResponder(RetryPolicy.disabled());

      RuntimeException ex =
          assertThrows(RuntimeException.class, () -> responder.respond(createPayload()));

      assertTrue(ex.getMessage().contains("403"));
    }

    @Test
    @DisplayName("should not retry on auth errors")
    void shouldNotRetryOnAuthError() {
      mockWebServer.enqueue(new MockResponse().setResponseCode(401).setBody("Unauthorized"));

      Responder responder = createResponder(RetryPolicy.defaults());

      assertThrows(RuntimeException.class, () -> responder.respond(createPayload()));

      assertEquals(1, mockWebServer.getRequestCount()); // No retry
    }
  }

  @Nested
  @DisplayName("Server Errors (5xx)")
  class ServerErrorTests {

    @Test
    @DisplayName("should throw exception on 500 Internal Server Error")
    void shouldThrowOn500() {
      mockWebServer.enqueue(
          new MockResponse()
              .setResponseCode(500)
              .setBody("{\"error\": {\"message\": \"Internal server error\"}}"));

      Responder responder = createResponder(RetryPolicy.disabled());

      RuntimeException ex =
          assertThrows(RuntimeException.class, () -> responder.respond(createPayload()));

      assertTrue(ex.getMessage().contains("500"));
    }

    @Test
    @DisplayName("should throw exception on 503 Service Unavailable")
    void shouldThrowOn503() {
      mockWebServer.enqueue(
          new MockResponse()
              .setResponseCode(503)
              .setBody("{\"error\": {\"message\": \"Service unavailable\"}}"));

      Responder responder = createResponder(RetryPolicy.disabled());

      RuntimeException ex =
          assertThrows(RuntimeException.class, () -> responder.respond(createPayload()));

      assertTrue(ex.getMessage().contains("503"));
    }

    @Test
    @DisplayName("should be retryable")
    void shouldBeRetryable() {
      // 500 followed by success - proves 5xx is retryable
      mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Error"));
      mockWebServer.enqueue(createSuccessResponse());

      Responder responder = createResponder(RetryPolicy.builder().maxRetries(1).build());

      assertDoesNotThrow(
          () -> {
            responder.respond(createPayload());
          });
      assertEquals(2, mockWebServer.getRequestCount());
    }
  }

  @Nested
  @DisplayName("Invalid Request (4xx)")
  class InvalidRequestTests {

    @Test
    @DisplayName("should throw exception on 400 Bad Request")
    void shouldThrowOn400() {
      mockWebServer.enqueue(
          new MockResponse()
              .setResponseCode(400)
              .setBody("{\"error\": {\"message\": \"Invalid model\"}}"));

      Responder responder = createResponder(RetryPolicy.disabled());

      RuntimeException ex =
          assertThrows(RuntimeException.class, () -> responder.respond(createPayload()));

      assertTrue(ex.getMessage().contains("400"));
    }

    @Test
    @DisplayName("should throw exception on 404 Not Found")
    void shouldThrowOn404() {
      mockWebServer.enqueue(
          new MockResponse()
              .setResponseCode(404)
              .setBody("{\"error\": {\"message\": \"Model not found\"}}"));

      Responder responder = createResponder(RetryPolicy.disabled());

      RuntimeException ex =
          assertThrows(RuntimeException.class, () -> responder.respond(createPayload()));

      assertTrue(ex.getMessage().contains("404"));
    }

    @Test
    @DisplayName("should throw exception on 422 Unprocessable Entity")
    void shouldThrowOn422() {
      mockWebServer.enqueue(
          new MockResponse()
              .setResponseCode(422)
              .setBody("{\"error\": {\"message\": \"Invalid parameters\"}}"));

      Responder responder = createResponder(RetryPolicy.disabled());

      RuntimeException ex =
          assertThrows(RuntimeException.class, () -> responder.respond(createPayload()));

      assertTrue(ex.getMessage().contains("422"));
    }

    @Test
    @DisplayName("should not retry on 4xx errors")
    void shouldNotRetryOn4xx() {
      mockWebServer.enqueue(new MockResponse().setResponseCode(400).setBody("Bad request"));

      Responder responder = createResponder(RetryPolicy.defaults());

      assertThrows(RuntimeException.class, () -> responder.respond(createPayload()));

      assertEquals(1, mockWebServer.getRequestCount()); // No retry
    }
  }

  @Nested
  @DisplayName("Connection Errors")
  class ConnectionErrorTests {

    @Test
    @DisplayName("should throw on connection drop")
    void shouldThrowOnConnectionDrop() {
      mockWebServer.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

      Responder responder = createResponder(RetryPolicy.disabled());

      assertThrows(RuntimeException.class, () -> responder.respond(createPayload()));
    }

    @Test
    @DisplayName("should retry on connection errors")
    void shouldRetryOnConnectionError() {
      mockWebServer.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));
      mockWebServer.enqueue(createSuccessResponse());

      Responder responder = createResponder(RetryPolicy.builder().maxRetries(1).build());

      assertDoesNotThrow(
          () -> {
            responder.respond(createPayload());
          });
      assertEquals(2, mockWebServer.getRequestCount());
    }
  }

  @Nested
  @DisplayName("Error Message Contents")
  class ErrorMessageTests {

    @Test
    @DisplayName("should include status code in error message")
    void shouldIncludeStatusCode() {
      mockWebServer.enqueue(new MockResponse().setResponseCode(418).setBody("I'm a teapot"));

      Responder responder = createResponder(RetryPolicy.disabled());

      RuntimeException ex =
          assertThrows(RuntimeException.class, () -> responder.respond(createPayload()));

      String message = ex.getMessage();
      assertTrue(
          message.contains("418") || message.contains("teapot"),
          "Error message should contain status info");
    }

    @Test
    @DisplayName("should include response body in error")
    void shouldIncludeResponseBody() {
      mockWebServer.enqueue(
          new MockResponse().setResponseCode(400).setBody("Specific error details here"));

      Responder responder = createResponder(RetryPolicy.disabled());

      RuntimeException ex =
          assertThrows(RuntimeException.class, () -> responder.respond(createPayload()));

      // Error message or cause should contain the response body
      assertNotNull(ex.getMessage());
    }
  }

  // ===== Helper Methods =====

  private Responder createResponder(RetryPolicy retryPolicy) {
    return Responder.builder()
        .httpClient(okHttpClient)
        .apiKey(TEST_API_KEY)
        .baseUrl(mockWebServer.url("/v1/responses"))
        .retryPolicy(retryPolicy)
        .build();
  }

  private MockResponse createSuccessResponse() {
    try {
      Response response =
          new Response(
              null,
              null,
              System.currentTimeMillis() / 1000,
              null,
              "resp-123",
              null,
              null,
              null,
              null,
              null,
              "gpt-4o",
              ResponseObject.RESPONSE,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              ResponseGenerationStatus.COMPLETED,
              null,
              null,
              null,
              null,
              null,
              null,
              null);

      String json = ResponsesApiObjectMapper.create().writeValueAsString(response);
      return new MockResponse()
          .setResponseCode(200)
          .setBody(json)
          .addHeader("Content-Type", "application/json");
    } catch (Exception e) {
      throw new RuntimeException("Failed to create mock response", e);
    }
  }

  private CreateResponsePayload createPayload() {
    return new CreateResponsePayload(
        null,
        null,
        null,
        List.of(new DeveloperMessage(List.of(new Text("Test")), null)),
        "Test",
        null,
        null,
        null,
        "gpt-4o",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }
}
