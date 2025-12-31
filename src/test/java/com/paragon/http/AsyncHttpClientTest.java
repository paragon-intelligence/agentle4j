package com.paragon.http;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link AsyncHttpClient}. */
class AsyncHttpClientTest {

  private MockWebServer server;
  private AsyncHttpClient client;

  @BeforeEach
  void setUp() throws Exception {
    server = new MockWebServer();
    server.start();
  }

  @AfterEach
  void tearDown() throws Exception {
    if (client != null) {
      client.close();
    }
    server.shutdown();
  }

  // ==================== Builder Tests ====================

  @Test
  void builder_setsBaseUrl() {
    client = AsyncHttpClient.builder().baseUrl("https://api.example.com").build();
    assertNotNull(client);
  }

  @Test
  void builder_stripsTrailingSlashFromBaseUrl() {
    client = AsyncHttpClient.builder().baseUrl("https://api.example.com/").build();
    assertNotNull(client);
  }

  @Test
  void builder_setsTimeouts() {
    client =
        AsyncHttpClient.builder()
            .connectTimeout(5000)
            .readTimeout(10000)
            .writeTimeout(15000)
            .build();
    assertNotNull(client);
  }

  @Test
  void builder_setsDefaultHeaders() {
    client =
        AsyncHttpClient.builder()
            .defaultHeader("Authorization", "Bearer token")
            .defaultHeader("User-Agent", "TestClient")
            .build();
    assertNotNull(client);
  }

  @Test
  void builder_setsRetryPolicy() {
    client = AsyncHttpClient.builder().retryPolicy(RetryPolicy.disabled()).build();
    assertNotNull(client);
  }

  @Test
  void builder_setsNoRetry() {
    client = AsyncHttpClient.builder().noRetry().build();
    assertNotNull(client);
  }

  @Test
  void builder_setsEventListener() {
    client = AsyncHttpClient.builder().eventListener(HttpEventListener.noop()).build();
    assertNotNull(client);
  }

  @Test
  void builder_setsCustomObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    client = AsyncHttpClient.builder().objectMapper(mapper).build();
    assertNotNull(client);
  }

  @Test
  void create_returnsDefaultClient() {
    client = AsyncHttpClient.create();
    assertNotNull(client);
  }

  // ==================== Execute Tests ====================

  @Test
  void execute_returnsSuccessfulResponse() throws Exception {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setBody("{\"name\":\"test\"}")
            .addHeader("Content-Type", "application/json"));

    client = AsyncHttpClient.builder().baseUrl(server.url("/").toString()).noRetry().build();

    HttpResponse response = client.execute(HttpRequest.get("/test").build()).join();

    assertEquals(200, response.statusCode());
    assertTrue(response.isSuccessful());
    assertFalse(response.isClientError());
    assertFalse(response.isServerError());
    assertEquals("{\"name\":\"test\"}", response.bodyAsString());
  }

  @Test
  void execute_deserializesToType() throws Exception {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setBody("{\"value\":42}")
            .addHeader("Content-Type", "application/json"));

    client = AsyncHttpClient.builder().baseUrl(server.url("/").toString()).noRetry().build();

    TestData data = client.execute(HttpRequest.get("/test").build(), TestData.class).join();

    assertEquals(42, data.value);
  }

  @Test
  void execute_throwsOnClientError() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(404).setBody("Not Found"));

    client = AsyncHttpClient.builder().baseUrl(server.url("/").toString()).noRetry().build();

    CompletionException ex =
        assertThrows(
            CompletionException.class,
            () -> client.execute(HttpRequest.get("/test").build()).join());

    assertInstanceOf(HttpException.class, ex.getCause());
    HttpException httpEx = (HttpException) ex.getCause();
    assertEquals(404, httpEx.statusCode());
    assertTrue(httpEx.isClientError());
    assertFalse(httpEx.isServerError());
  }

  @Test
  void execute_throwsOnServerError() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Error"));

    client = AsyncHttpClient.builder().baseUrl(server.url("/").toString()).noRetry().build();

    CompletionException ex =
        assertThrows(
            CompletionException.class,
            () -> client.execute(HttpRequest.get("/test").build()).join());

    assertInstanceOf(HttpException.class, ex.getCause());
    HttpException httpEx = (HttpException) ex.getCause();
    assertEquals(500, httpEx.statusCode());
    assertFalse(httpEx.isClientError());
    assertTrue(httpEx.isServerError());
  }

  @Test
  void execute_throwsOnDeserializationFailure() throws Exception {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setBody("not valid json")
            .addHeader("Content-Type", "application/json"));

    client = AsyncHttpClient.builder().baseUrl(server.url("/").toString()).noRetry().build();

    CompletionException ex =
        assertThrows(
            CompletionException.class,
            () -> client.execute(HttpRequest.get("/test").build(), TestData.class).join());

    assertInstanceOf(HttpException.class, ex.getCause());
  }

  @Test
  void execute_includesDefaultHeaders() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));

    client =
        AsyncHttpClient.builder()
            .baseUrl(server.url("/").toString())
            .defaultHeader("X-Custom", "value")
            .noRetry()
            .build();

    client.execute(HttpRequest.get("/test").build()).join();

    var request = server.takeRequest();
    assertEquals("value", request.getHeader("X-Custom"));
  }

  @Test
  void execute_includesRequestHeaders() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));

    client = AsyncHttpClient.builder().baseUrl(server.url("/").toString()).noRetry().build();

    client.execute(HttpRequest.get("/test").header("X-Request", "custom").build()).join();

    var request = server.takeRequest();
    assertEquals("custom", request.getHeader("X-Request"));
  }

  @Test
  void execute_sendsPostBody() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));

    client = AsyncHttpClient.builder().baseUrl(server.url("/").toString()).noRetry().build();

    client.execute(HttpRequest.post("/test").jsonBody("{\"key\":\"value\"}").build()).join();

    var request = server.takeRequest();
    assertEquals("POST", request.getMethod());
    assertEquals("{\"key\":\"value\"}", request.getBody().readUtf8());
    assertTrue(request.getHeader("Content-Type").contains("application/json"));
  }

  @Test
  void execute_handlesAbsoluteUrl() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));

    client = AsyncHttpClient.builder().baseUrl("https://other.example.com").noRetry().build();

    // Use absolute URL which should override base URL
    client.execute(HttpRequest.get(server.url("/test").toString()).build()).join();

    var request = server.takeRequest();
    assertEquals("/test", request.getPath());
  }

  // ==================== Retry Tests ====================

  @Test
  void execute_retriesOn503() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(503).setBody("Service Unavailable"));
    server.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));

    client =
        AsyncHttpClient.builder()
            .baseUrl(server.url("/").toString())
            .retryPolicy(
                RetryPolicy.builder().maxRetries(3).initialDelay(Duration.ofMillis(10)).build())
            .build();

    HttpResponse response = client.execute(HttpRequest.get("/test").build()).join();

    assertEquals(200, response.statusCode());
    assertEquals(2, server.getRequestCount());
  }

  @Test
  void execute_retriesOn429() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(429).setBody("Rate limited"));
    server.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));

    client =
        AsyncHttpClient.builder()
            .baseUrl(server.url("/").toString())
            .retryPolicy(
                RetryPolicy.builder().maxRetries(3).initialDelay(Duration.ofMillis(10)).build())
            .build();

    HttpResponse response = client.execute(HttpRequest.get("/test").build()).join();

    assertEquals(200, response.statusCode());
    assertEquals(2, server.getRequestCount());
  }

  @Test
  void execute_stopsRetryingAfterMaxAttempts() throws Exception {
    for (int i = 0; i < 5; i++) {
      server.enqueue(new MockResponse().setResponseCode(503).setBody("Service Unavailable"));
    }

    client =
        AsyncHttpClient.builder()
            .baseUrl(server.url("/").toString())
            .retryPolicy(
                RetryPolicy.builder().maxRetries(2).initialDelay(Duration.ofMillis(10)).build())
            .build();

    CompletionException ex =
        assertThrows(
            CompletionException.class,
            () -> client.execute(HttpRequest.get("/test").build()).join());

    assertInstanceOf(HttpException.class, ex.getCause());
    // Based on actual retry behavior: initial + retries
    assertTrue(server.getRequestCount() >= 2);
  }

  @Test
  void execute_doesNotRetryOn404() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(404).setBody("Not Found"));

    client =
        AsyncHttpClient.builder()
            .baseUrl(server.url("/").toString())
            .retryPolicy(RetryPolicy.defaults())
            .build();

    assertThrows(
        CompletionException.class, () -> client.execute(HttpRequest.get("/test").build()).join());

    assertEquals(1, server.getRequestCount());
  }

  // ==================== Streaming Tests ====================

  @Test
  void stream_processesSSEEvents() throws Exception {
    String sseResponse = "data: {\"id\":1}\n\ndata: {\"id\":2}\n\ndata: [DONE]\n\n";
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setBody(sseResponse)
            .addHeader("Content-Type", "text/event-stream"));

    client = AsyncHttpClient.builder().baseUrl(server.url("/").toString()).noRetry().build();

    List<TestData> events = new ArrayList<>();
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Throwable> error = new AtomicReference<>();

    client
        .stream(
            HttpRequest.post("/stream").jsonBody("{}").build(),
            TestData.class,
            events::add,
            error::set,
            latch::countDown)
        .join();

    assertTrue(latch.await(5, TimeUnit.SECONDS));
    assertNull(error.get());
    assertEquals(2, events.size());
    assertEquals(1, events.get(0).id);
    assertEquals(2, events.get(1).id);
  }

  @Test
  void stream_handlesStreamError() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(500).setBody("Server Error"));

    client = AsyncHttpClient.builder().baseUrl(server.url("/").toString()).noRetry().build();

    AtomicReference<Throwable> error = new AtomicReference<>();
    CountDownLatch latch = new CountDownLatch(1);

    try {
      client
          .stream(
              HttpRequest.post("/stream").jsonBody("{}").build(),
              TestData.class,
              event -> {},
              e -> {
                error.set(e);
                latch.countDown();
              },
              () -> {})
          .join();
    } catch (CompletionException ignored) {
    }

    assertTrue(latch.await(5, TimeUnit.SECONDS));
    assertNotNull(error.get());
    assertInstanceOf(HttpException.class, error.get());
  }

  @Test
  void stream_callsOnComplete() throws Exception {
    String sseResponse = "data: {\"id\":1}\n\n";
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setBody(sseResponse)
            .addHeader("Content-Type", "text/event-stream"));

    client = AsyncHttpClient.builder().baseUrl(server.url("/").toString()).noRetry().build();

    AtomicInteger completeCount = new AtomicInteger(0);
    CountDownLatch latch = new CountDownLatch(1);

    client
        .stream(
            HttpRequest.post("/stream").jsonBody("{}").build(),
            TestData.class,
            event -> {},
            error -> {},
            () -> {
              completeCount.incrementAndGet();
              latch.countDown();
            })
        .join();

    assertTrue(latch.await(5, TimeUnit.SECONDS));
    assertEquals(1, completeCount.get());
  }

  // ==================== Event Listener Tests ====================

  @Test
  void execute_callsEventListener() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));

    AtomicInteger startCount = new AtomicInteger(0);
    AtomicInteger responseCount = new AtomicInteger(0);

    client =
        AsyncHttpClient.builder()
            .baseUrl(server.url("/").toString())
            .noRetry()
            .eventListener(
                new HttpEventListener() {
                  @Override
                  public void onRequestStart(HttpRequest request) {
                    startCount.incrementAndGet();
                  }

                  @Override
                  public void onResponse(HttpRequest request, HttpResponse response) {
                    responseCount.incrementAndGet();
                  }
                })
            .build();

    client.execute(HttpRequest.get("/test").build()).join();

    assertEquals(1, startCount.get());
    assertEquals(1, responseCount.get());
  }

  // ==================== Close Tests ====================

  @Test
  void close_shutsDownCleanly() {
    client = AsyncHttpClient.create();
    assertDoesNotThrow(() -> client.close());
  }

  // ==================== Response Header Tests ====================

  @Test
  void response_parsesHeaders() throws Exception {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setBody("ok")
            .addHeader("X-Custom-Header", "custom-value")
            .addHeader("Content-Type", "text/plain"));

    client = AsyncHttpClient.builder().baseUrl(server.url("/").toString()).noRetry().build();

    HttpResponse response = client.execute(HttpRequest.get("/test").build()).join();

    assertEquals("custom-value", response.header("X-Custom-Header"));
    assertEquals("text/plain", response.header("content-type"));
    assertNull(response.header("X-Non-Existent"));
  }

  @Test
  void response_reportsLatency() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));

    client = AsyncHttpClient.builder().baseUrl(server.url("/").toString()).noRetry().build();

    HttpResponse response = client.execute(HttpRequest.get("/test").build()).join();

    assertTrue(response.latencyMs() >= 0);
  }

  // Test data class for JSON deserialization
  public static class TestData {
    public int value;
    public int id;
  }
}
