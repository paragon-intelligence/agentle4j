package com.paragon.responses;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.http.RetryPolicy;
import com.paragon.responses.spec.*;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for retry functionality in {@link Responder}.
 *
 * <p>Tests verify:
 *
 * <ul>
 *   <li>Retry on 429 (rate limit)
 *   <li>Retry on 5xx server errors
 *   <li>No retry on 4xx client errors (except 429)
 *   <li>Retry on network failures
 *   <li>Max retries limit
 *   <li>Disabled retries
 * </ul>
 */
class ResponderRetryTest {

  private static final String TEST_API_KEY = "test-api-key";
  private MockWebServer mockWebServer;
  private OkHttpClient okHttpClient;

  @BeforeEach
  void setUp() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    // Short timeouts for faster tests
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

  @Test
  void retry_on429_succeedsAfterRetry() throws Exception {
    // First request: 429 rate limit
    mockWebServer.enqueue(new MockResponse().setResponseCode(429).setBody("Rate limited"));
    // Second request: success
    mockWebServer.enqueue(createSuccessResponse());

    Responder responder = createResponder(RetryPolicy.builder().maxRetries(3).build());
    Response response = responder.respond(createPayload());

    assertNotNull(response);
    assertEquals("resp-123", response.id());
    assertEquals(2, mockWebServer.getRequestCount()); // 1 failed + 1 success
  }

  @Test
  void retry_on500_succeedsAfterRetry() throws Exception {
    // First request: 500 server error
    mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Internal error"));
    // Second request: success
    mockWebServer.enqueue(createSuccessResponse());

    Responder responder = createResponder(RetryPolicy.builder().maxRetries(3).build());
    Response response = responder.respond(createPayload());

    assertNotNull(response);
    assertEquals(2, mockWebServer.getRequestCount());
  }

  @Test
  void retry_on502_succeedsAfterRetry() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(502).setBody("Bad gateway"));
    mockWebServer.enqueue(createSuccessResponse());

    Responder responder = createResponder(RetryPolicy.builder().maxRetries(3).build());
    Response response = responder.respond(createPayload());

    assertNotNull(response);
    assertEquals(2, mockWebServer.getRequestCount());
  }

  @Test
  void retry_on503_succeedsAfterRetry() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(503).setBody("Service unavailable"));
    mockWebServer.enqueue(createSuccessResponse());

    Responder responder = createResponder(RetryPolicy.builder().maxRetries(3).build());
    Response response = responder.respond(createPayload());

    assertNotNull(response);
    assertEquals(2, mockWebServer.getRequestCount());
  }

  @Test
  void retry_on504_succeedsAfterRetry() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(504).setBody("Gateway timeout"));
    mockWebServer.enqueue(createSuccessResponse());

    Responder responder = createResponder(RetryPolicy.builder().maxRetries(3).build());
    Response response = responder.respond(createPayload());

    assertNotNull(response);
    assertEquals(2, mockWebServer.getRequestCount());
  }

  @Test
  void noRetry_on400_failsImmediately() {
    mockWebServer.enqueue(new MockResponse().setResponseCode(400).setBody("Bad request"));

    Responder responder = createResponder(RetryPolicy.defaults());

    RuntimeException ex =
        assertThrows(
            RuntimeException.class,
            () -> responder.respond(createPayload()));

    assertTrue(ex.getMessage().contains("400"));
    assertEquals(1, mockWebServer.getRequestCount()); // No retry
  }

  @Test
  void noRetry_on401_failsImmediately() {
    mockWebServer.enqueue(new MockResponse().setResponseCode(401).setBody("Unauthorized"));

    Responder responder = createResponder(RetryPolicy.defaults());

    RuntimeException ex =
        assertThrows(
            RuntimeException.class,
            () -> responder.respond(createPayload()));

    assertTrue(ex.getMessage().contains("401"));
    assertEquals(1, mockWebServer.getRequestCount());
  }

  @Test
  void noRetry_on404_failsImmediately() {
    mockWebServer.enqueue(new MockResponse().setResponseCode(404).setBody("Not found"));

    Responder responder = createResponder(RetryPolicy.defaults());

    RuntimeException ex =
        assertThrows(
            RuntimeException.class,
            () -> responder.respond(createPayload()));

    assertTrue(ex.getMessage().contains("404"));
    assertEquals(1, mockWebServer.getRequestCount());
  }

  @Test
  void maxRetries_exceeded_failsAfterAllAttempts() {
    // All requests fail with 500
    mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Error 1"));
    mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Error 2"));
    mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Error 3"));
    mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Error 4"));

    RetryPolicy policy =
        RetryPolicy.builder()
            .maxRetries(3)
            .initialDelay(Duration.ofMillis(10)) // Fast for tests
            .build();
    Responder responder = createResponder(policy);

    RuntimeException ex =
        assertThrows(
            RuntimeException.class,
            () -> responder.respond(createPayload()));

    assertTrue(ex.getMessage().contains("500"));
    assertEquals(4, mockWebServer.getRequestCount()); // 1 initial + 3 retries
  }

  @Test
  void retryDisabled_failsImmediatelyOn429() {
    mockWebServer.enqueue(new MockResponse().setResponseCode(429).setBody("Rate limited"));

    Responder responder = createResponder(RetryPolicy.disabled());

    RuntimeException ex =
        assertThrows(
            RuntimeException.class,
            () -> responder.respond(createPayload()));

    assertTrue(ex.getMessage().contains("429"));
    assertEquals(1, mockWebServer.getRequestCount());
  }

  @Test
  void retry_onNetworkError_succeedsAfterRetry() throws Exception {
    // First request: network failure (disconnect immediately)
    mockWebServer.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));
    // Second request: success
    mockWebServer.enqueue(createSuccessResponse());

    RetryPolicy policy =
        RetryPolicy.builder().maxRetries(3).initialDelay(Duration.ofMillis(10)).build();
    Responder responder = createResponder(policy);

    Response response = responder.respond(createPayload());

    assertNotNull(response);
    assertEquals(2, mockWebServer.getRequestCount());
  }

  @Test
  void retry_multipleFailures_eventuallySucceeds() throws Exception {
    // 3 failures followed by success
    mockWebServer.enqueue(new MockResponse().setResponseCode(429).setBody("Rate limited"));
    mockWebServer.enqueue(new MockResponse().setResponseCode(503).setBody("Unavailable"));
    mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Error"));
    mockWebServer.enqueue(createSuccessResponse());

    RetryPolicy policy =
        RetryPolicy.builder().maxRetries(5).initialDelay(Duration.ofMillis(10)).build();
    Responder responder = createResponder(policy);

    Response response = responder.respond(createPayload());

    assertNotNull(response);
    assertEquals(4, mockWebServer.getRequestCount());
  }

  @Test
  void customRetryableStatusCodes_onlyRetriesConfiguredCodes() throws Exception {
    // 418 is normally not retryable, but we configure it
    mockWebServer.enqueue(new MockResponse().setResponseCode(418).setBody("I'm a teapot"));
    mockWebServer.enqueue(createSuccessResponse());

    RetryPolicy policy =
        RetryPolicy.builder()
            .maxRetries(3)
            .retryableStatusCodes(Set.of(418))
            .initialDelay(Duration.ofMillis(10))
            .build();
    Responder responder = createResponder(policy);

    Response response = responder.respond(createPayload());

    assertNotNull(response);
    assertEquals(2, mockWebServer.getRequestCount());
  }

  @Test
  void builder_maxRetries_setsRetryPolicy() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Error"));
    mockWebServer.enqueue(createSuccessResponse());

    // Use the simple maxRetries() builder method
    Responder responder =
        Responder.builder()
            .httpClient(okHttpClient)
            .apiKey(TEST_API_KEY)
            .baseUrl(mockWebServer.url("/v1/responses"))
            .maxRetries(3)
            .build();

    Response response = responder.respond(createPayload());

    assertNotNull(response);
    assertEquals(2, mockWebServer.getRequestCount());
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

  private MockResponse createSuccessResponse() throws Exception {
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
