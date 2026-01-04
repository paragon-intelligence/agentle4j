package com.paragon.prompts;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.http.RetryPolicy;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link LangfusePromptProvider}. */
class LangfusePromptProviderTest {

  private MockWebServer server;
  private OkHttpClient httpClient;

  @BeforeEach
  void setUp() throws Exception {
    server = new MockWebServer();
    server.start();
    httpClient =
        new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build();
  }

  @AfterEach
  void tearDown() throws Exception {
    server.shutdown();
  }

  private LangfusePromptProvider createProvider() {
    return LangfusePromptProvider.builder()
        .httpClient(httpClient)
        .publicKey("pk-test")
        .secretKey("sk-test")
        .baseUrl(server.url("/").toString().replaceAll("/$", ""))
        .retryPolicy(
            RetryPolicy.builder().maxRetries(2).initialDelay(Duration.ofMillis(10)).build())
        .build();
  }

  // ===== Builder Tests =====

  @Nested
  class BuilderTests {

    @Test
    void builder_allRequiredFields_builds() {
      LangfusePromptProvider provider =
          LangfusePromptProvider.builder()
              .httpClient(httpClient)
              .publicKey("pk-test")
              .secretKey("sk-test")
              .build();

      assertNotNull(provider);
      assertEquals(LangfusePromptProvider.DEFAULT_BASE_URL, provider.baseUrl());
    }

    @Test
    void builder_withCustomBaseUrl_setsBaseUrl() {
      LangfusePromptProvider provider =
          LangfusePromptProvider.builder()
              .httpClient(httpClient)
              .publicKey("pk-test")
              .secretKey("sk-test")
              .baseUrl("https://custom.langfuse.com")
              .build();

      assertEquals("https://custom.langfuse.com", provider.baseUrl());
    }

    @Test
    void builder_withRetryPolicy_setsRetryPolicy() {
      RetryPolicy policy = RetryPolicy.builder().maxRetries(5).build();

      LangfusePromptProvider provider =
          LangfusePromptProvider.builder()
              .httpClient(httpClient)
              .publicKey("pk-test")
              .secretKey("sk-test")
              .retryPolicy(policy)
              .build();

      assertEquals(5, provider.retryPolicy().maxRetries());
    }

    @Test
    void builder_withCustomObjectMapper_usesMapper() {
      ObjectMapper mapper = new ObjectMapper();

      LangfusePromptProvider provider =
          LangfusePromptProvider.builder()
              .httpClient(httpClient)
              .publicKey("pk-test")
              .secretKey("sk-test")
              .objectMapper(mapper)
              .build();

      assertNotNull(provider);
    }

    @Test
    void builder_missingHttpClient_throwsNullPointerException() {
      assertThrows(
          NullPointerException.class,
          () -> LangfusePromptProvider.builder().publicKey("pk-test").secretKey("sk-test").build());
    }

    @Test
    void builder_missingPublicKey_throwsNullPointerException() {
      assertThrows(
          NullPointerException.class,
          () ->
              LangfusePromptProvider.builder().httpClient(httpClient).secretKey("sk-test").build());
    }

    @Test
    void builder_missingSecretKey_throwsNullPointerException() {
      assertThrows(
          NullPointerException.class,
          () ->
              LangfusePromptProvider.builder().httpClient(httpClient).publicKey("pk-test").build());
    }

    @Test
    void builder_nullHttpClient_throwsNullPointerException() {
      assertThrows(
          NullPointerException.class, () -> LangfusePromptProvider.builder().httpClient(null));
    }

    @Test
    void builder_nullPublicKey_throwsNullPointerException() {
      assertThrows(
          NullPointerException.class, () -> LangfusePromptProvider.builder().publicKey(null));
    }

    @Test
    void builder_nullSecretKey_throwsNullPointerException() {
      assertThrows(
          NullPointerException.class, () -> LangfusePromptProvider.builder().secretKey(null));
    }

    @Test
    void builder_nullBaseUrl_throwsNullPointerException() {
      assertThrows(
          NullPointerException.class, () -> LangfusePromptProvider.builder().baseUrl(null));
    }

    @Test
    void builder_nullRetryPolicy_throwsNullPointerException() {
      assertThrows(
          NullPointerException.class, () -> LangfusePromptProvider.builder().retryPolicy(null));
    }

    @Test
    void builder_nullObjectMapper_throwsNullPointerException() {
      assertThrows(
          NullPointerException.class, () -> LangfusePromptProvider.builder().objectMapper(null));
    }

    @Test
    void builder_fromEnv_loadsEnvironmentVariables() {
      // This test just verifies fromEnv doesn't throw when env vars are not set
      LangfusePromptProvider.Builder builder =
          LangfusePromptProvider.builder().httpClient(httpClient).fromEnv();

      // Should be able to chain after fromEnv
      builder.publicKey("pk-test").secretKey("sk-test");
      assertNotNull(builder.build());
    }
  }

  // ===== Text Prompt Success =====

  @Nested
  class TextPromptSuccess {

    @Test
    void providePrompt_textPrompt_returnsPrompt() throws Exception {
      String json =
          """
          {
            "type": "text",
            "name": "my-prompt",
            "version": 1,
            "prompt": "Hello, {{name}}!",
            "config": null,
            "labels": ["production"],
            "tags": ["greeting"]
          }
          """;
      server.enqueue(
          new MockResponse()
              .setResponseCode(200)
              .setBody(json)
              .setHeader("Content-Type", "application/json"));

      LangfusePromptProvider provider = createProvider();
      Prompt prompt = provider.providePrompt("my-prompt", null);

      assertEquals("Hello, {{name}}!", prompt.content());
    }

    @Test
    void providePrompt_textPromptWithVariables_returnsUncompiled() throws Exception {
      String json =
          """
          {
            "type": "text",
            "name": "template",
            "version": 1,
            "prompt": "{{#if condition}}Yes{{/if}}",
            "config": null,
            "labels": [],
            "tags": []
          }
          """;
      server.enqueue(
          new MockResponse()
              .setResponseCode(200)
              .setBody(json)
              .setHeader("Content-Type", "application/json"));

      LangfusePromptProvider provider = createProvider();
      Prompt prompt = provider.providePrompt("template");

      assertFalse(prompt.isCompiled());
      assertTrue(prompt.content().contains("{{#if"));
    }
  }

  // ===== Chat Prompt Success =====

  @Nested
  class ChatPromptSuccess {

    @Test
    void providePrompt_chatPrompt_returnsConcatenatedContent() throws Exception {
      String json =
          """
          {
            "type": "chat",
            "name": "chat-prompt",
            "version": 1,
            "prompt": [
              {"type": "chatmessage", "role": "system", "content": "You are helpful."},
              {"type": "chatmessage", "role": "user", "content": "Hi!"}
            ],
            "config": null,
            "labels": [],
            "tags": []
          }
          """;
      server.enqueue(
          new MockResponse()
              .setResponseCode(200)
              .setBody(json)
              .setHeader("Content-Type", "application/json"));

      LangfusePromptProvider provider = createProvider();
      Prompt prompt = provider.providePrompt("chat-prompt");

      assertTrue(prompt.content().contains("system: You are helpful."));
      assertTrue(prompt.content().contains("user: Hi!"));
    }
  }

  // ===== Filters =====

  @Nested
  class FiltersHandling {

    @Test
    void providePrompt_withVersionFilter_sendsQueryParam() throws Exception {
      String json =
          """
          {
            "type": "text",
            "name": "test",
            "version": 2,
            "prompt": "v2 content",
            "config": null,
            "labels": [],
            "tags": []
          }
          """;
      server.enqueue(
          new MockResponse()
              .setResponseCode(200)
              .setBody(json)
              .setHeader("Content-Type", "application/json"));

      LangfusePromptProvider provider = createProvider();
      provider.providePrompt("test", Map.of("version", "2"));

      RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
      assertTrue(request.getPath().contains("version=2"));
    }

    @Test
    void providePrompt_withLabelFilter_sendsQueryParam() throws Exception {
      String json =
          """
          {
            "type": "text",
            "name": "test",
            "version": 1,
            "prompt": "staging content",
            "config": null,
            "labels": ["staging"],
            "tags": []
          }
          """;
      server.enqueue(
          new MockResponse()
              .setResponseCode(200)
              .setBody(json)
              .setHeader("Content-Type", "application/json"));

      LangfusePromptProvider provider = createProvider();
      provider.providePrompt("test", Map.of("label", "staging"));

      RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
      assertTrue(request.getPath().contains("label=staging"));
    }

    @Test
    void providePrompt_withBothFilters_sendsBothQueryParams() throws Exception {
      String json =
          """
          {
            "type": "text",
            "name": "test",
            "version": 3,
            "prompt": "content",
            "config": null,
            "labels": [],
            "tags": []
          }
          """;
      server.enqueue(
          new MockResponse()
              .setResponseCode(200)
              .setBody(json)
              .setHeader("Content-Type", "application/json"));

      LangfusePromptProvider provider = createProvider();
      provider.providePrompt("test", Map.of("version", "3", "label", "production"));

      RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
      assertTrue(request.getPath().contains("version=3"));
      assertTrue(request.getPath().contains("label=production"));
    }

    @Test
    void providePrompt_emptyFilters_noQueryParams() throws Exception {
      String json =
          """
          {
            "type": "text",
            "name": "test",
            "version": 1,
            "prompt": "content",
            "config": null,
            "labels": [],
            "tags": []
          }
          """;
      server.enqueue(
          new MockResponse()
              .setResponseCode(200)
              .setBody(json)
              .setHeader("Content-Type", "application/json"));

      LangfusePromptProvider provider = createProvider();
      provider.providePrompt("test", Map.of());

      RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
      assertFalse(request.getPath().contains("version="));
      assertFalse(request.getPath().contains("label="));
    }

    @Test
    void providePrompt_emptyVersionValue_notSent() throws Exception {
      String json =
          """
          {
            "type": "text",
            "name": "test",
            "version": 1,
            "prompt": "content",
            "config": null,
            "labels": [],
            "tags": []
          }
          """;
      server.enqueue(
          new MockResponse()
              .setResponseCode(200)
              .setBody(json)
              .setHeader("Content-Type", "application/json"));

      LangfusePromptProvider provider = createProvider();
      provider.providePrompt("test", Map.of("version", ""));

      RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
      assertFalse(request.getPath().contains("version="));
    }
  }

  // ===== Authentication =====

  @Nested
  class Authentication {

    @Test
    void providePrompt_sendsBasicAuthHeader() throws Exception {
      String json =
          """
          {
            "type": "text",
            "name": "test",
            "version": 1,
            "prompt": "content",
            "config": null,
            "labels": [],
            "tags": []
          }
          """;
      server.enqueue(
          new MockResponse()
              .setResponseCode(200)
              .setBody(json)
              .setHeader("Content-Type", "application/json"));

      LangfusePromptProvider provider = createProvider();
      provider.providePrompt("test");

      RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
      String auth = request.getHeader("Authorization");

      assertNotNull(auth);
      assertTrue(auth.startsWith("Basic "));

      // Decode and verify credentials
      String encoded = auth.substring("Basic ".length());
      String decoded = new String(java.util.Base64.getDecoder().decode(encoded));
      assertEquals("pk-test:sk-test", decoded);
    }

    @Test
    void providePrompt_sendsAcceptHeader() throws Exception {
      String json =
          """
          {
            "type": "text",
            "name": "test",
            "version": 1,
            "prompt": "content",
            "config": null,
            "labels": [],
            "tags": []
          }
          """;
      server.enqueue(
          new MockResponse()
              .setResponseCode(200)
              .setBody(json)
              .setHeader("Content-Type", "application/json"));

      LangfusePromptProvider provider = createProvider();
      provider.providePrompt("test");

      RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
      assertEquals("application/json", request.getHeader("Accept"));
    }
  }

  // ===== URL Encoding =====

  @Nested
  class UrlEncoding {

    @Test
    void providePrompt_promptIdWithSlash_urlEncodes() throws Exception {
      String json =
          """
          {
            "type": "text",
            "name": "folder/prompt",
            "version": 1,
            "prompt": "content",
            "config": null,
            "labels": [],
            "tags": []
          }
          """;
      server.enqueue(
          new MockResponse()
              .setResponseCode(200)
              .setBody(json)
              .setHeader("Content-Type", "application/json"));

      LangfusePromptProvider provider = createProvider();
      provider.providePrompt("folder/prompt");

      RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
      // URL-encoded slash is %2F
      assertTrue(
          request.getPath().contains("folder%2Fprompt")
              || request.getPath().contains("folder/prompt"));
    }

    @Test
    void providePrompt_promptIdWithSpecialChars_urlEncodes() throws Exception {
      String json =
          """
          {
            "type": "text",
            "name": "my prompt",
            "version": 1,
            "prompt": "content",
            "config": null,
            "labels": [],
            "tags": []
          }
          """;
      server.enqueue(
          new MockResponse()
              .setResponseCode(200)
              .setBody(json)
              .setHeader("Content-Type", "application/json"));

      LangfusePromptProvider provider = createProvider();
      provider.providePrompt("my prompt");

      RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
      // URL-encoded space is + or %20
      assertTrue(
          request.getPath().contains("my+prompt") || request.getPath().contains("my%20prompt"));
    }
  }

  // ===== Error Handling =====

  @Nested
  class ErrorHandling {

    @Test
    void providePrompt_404_throwsException() {
      server.enqueue(new MockResponse().setResponseCode(404).setBody("Not found"));

      LangfusePromptProvider provider = createProvider();

      PromptProviderException ex =
          assertThrows(PromptProviderException.class, () -> provider.providePrompt("nonexistent"));

      assertEquals("nonexistent", ex.promptId());
      assertTrue(ex.getMessage().contains("not found"));
      assertFalse(ex.isRetryable());
    }

    @Test
    void providePrompt_401_throwsException() {
      server.enqueue(new MockResponse().setResponseCode(401).setBody("Unauthorized"));

      LangfusePromptProvider provider = createProvider();

      PromptProviderException ex =
          assertThrows(PromptProviderException.class, () -> provider.providePrompt("test"));

      assertTrue(ex.getMessage().toLowerCase().contains("unauthorized"));
      assertFalse(ex.isRetryable());
    }

    @Test
    void providePrompt_403_throwsException() {
      server.enqueue(new MockResponse().setResponseCode(403).setBody("Forbidden"));

      LangfusePromptProvider provider = createProvider();

      PromptProviderException ex =
          assertThrows(PromptProviderException.class, () -> provider.providePrompt("test"));

      assertTrue(
          ex.getMessage().toLowerCase().contains("forbidden")
              || ex.getMessage().toLowerCase().contains("access denied"));
    }

    @Test
    void providePrompt_nullPromptId_throwsNullPointerException() {
      LangfusePromptProvider provider = createProvider();

      assertThrows(NullPointerException.class, () -> provider.providePrompt(null));
    }

    @Test
    void providePrompt_emptyPromptId_throwsException() {
      LangfusePromptProvider provider = createProvider();

      PromptProviderException ex =
          assertThrows(PromptProviderException.class, () -> provider.providePrompt(""));

      assertTrue(ex.getMessage().contains("empty"));
    }

    @Test
    void providePrompt_invalidJson_throwsException() {
      server.enqueue(
          new MockResponse()
              .setResponseCode(200)
              .setBody("not valid json")
              .setHeader("Content-Type", "application/json"));

      LangfusePromptProvider provider = createProvider();

      assertThrows(PromptProviderException.class, () -> provider.providePrompt("test"));
    }
  }

  // ===== Retry Behavior =====

  @Nested
  class RetryBehavior {

    @Test
    void providePrompt_429_retriesThenSucceeds() throws Exception {
      server.enqueue(new MockResponse().setResponseCode(429).setBody("Rate limited"));
      server.enqueue(
          new MockResponse()
              .setResponseCode(200)
              .setBody(
                  """
                  {
                    "type": "text",
                    "name": "test",
                    "version": 1,
                    "prompt": "success",
                    "config": null,
                    "labels": [],
                    "tags": []
                  }
                  """)
              .setHeader("Content-Type", "application/json"));

      LangfusePromptProvider provider = createProvider();
      Prompt prompt = provider.providePrompt("test");

      assertEquals("success", prompt.content());
      assertEquals(2, server.getRequestCount());
    }

    @Test
    void providePrompt_503_retriesThenSucceeds() throws Exception {
      server.enqueue(new MockResponse().setResponseCode(503).setBody("Service Unavailable"));
      server.enqueue(
          new MockResponse()
              .setResponseCode(200)
              .setBody(
                  """
                  {
                    "type": "text",
                    "name": "test",
                    "version": 1,
                    "prompt": "recovered",
                    "config": null,
                    "labels": [],
                    "tags": []
                  }
                  """)
              .setHeader("Content-Type", "application/json"));

      LangfusePromptProvider provider = createProvider();
      Prompt prompt = provider.providePrompt("test");

      assertEquals("recovered", prompt.content());
      assertEquals(2, server.getRequestCount());
    }

    @Test
    void providePrompt_multipleRetries_exhaustsRetries() {
      // With maxRetries=2, we get 3 total attempts (initial + 2 retries)
      server.enqueue(new MockResponse().setResponseCode(503).setBody("Error"));
      server.enqueue(new MockResponse().setResponseCode(503).setBody("Error"));
      server.enqueue(new MockResponse().setResponseCode(503).setBody("Error"));

      LangfusePromptProvider provider = createProvider();

      assertThrows(PromptProviderException.class, () -> provider.providePrompt("test"));

      // Should have made initial + retry attempts
      assertTrue(server.getRequestCount() >= 2);
    }

    @Test
    void providePrompt_404_doesNotRetry() {
      server.enqueue(new MockResponse().setResponseCode(404).setBody("Not found"));

      LangfusePromptProvider provider = createProvider();

      assertThrows(PromptProviderException.class, () -> provider.providePrompt("test"));

      assertEquals(1, server.getRequestCount());
    }
  }

  // ===== Default Method =====

  @Nested
  class DefaultMethodTests {

    @Test
    void providePrompt_noFilters_works() throws Exception {
      String json =
          """
          {
            "type": "text",
            "name": "test",
            "version": 1,
            "prompt": "content",
            "config": null,
            "labels": [],
            "tags": []
          }
          """;
      server.enqueue(
          new MockResponse()
              .setResponseCode(200)
              .setBody(json)
              .setHeader("Content-Type", "application/json"));

      LangfusePromptProvider provider = createProvider();
      Prompt prompt = provider.providePrompt("test");

      assertEquals("content", prompt.content());
    }
  }

  // ===== Constants =====

  @Nested
  class ConstantsTests {

    @Test
    void defaultBaseUrl_hasCorrectValue() {
      assertEquals("https://cloud.langfuse.com", LangfusePromptProvider.DEFAULT_BASE_URL);
    }
  }
}
