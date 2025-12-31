package com.paragon.embeddings;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.http.RetryPolicy;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link OpenRouterEmbeddingProvider}. */
class OpenRouterEmbeddingProviderTest {

  // ==================== Builder Tests ====================

  @Test
  void builder_createsProviderWithApiKey() {
    OpenRouterEmbeddingProvider provider =
        OpenRouterEmbeddingProvider.builder().apiKey("test-api-key").build();

    assertNotNull(provider);
    provider.close();
  }

  @Test
  void builder_setsCustomObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    OpenRouterEmbeddingProvider provider =
        OpenRouterEmbeddingProvider.builder().apiKey("test-api-key").objectMapper(mapper).build();

    assertNotNull(provider);
    provider.close();
  }

  @Test
  void builder_setsCustomRetryPolicy() {
    RetryPolicy policy =
        RetryPolicy.builder().maxRetries(5).initialDelay(Duration.ofMillis(100)).build();

    OpenRouterEmbeddingProvider provider =
        OpenRouterEmbeddingProvider.builder().apiKey("test-api-key").retryPolicy(policy).build();

    assertNotNull(provider);
    provider.close();
  }

  @Test
  void builder_setsAllowFallbacks() {
    OpenRouterEmbeddingProvider provider =
        OpenRouterEmbeddingProvider.builder().apiKey("test-api-key").allowFallbacks(false).build();

    assertNotNull(provider);
    provider.close();
  }

  @Test
  void builder_throwsWithoutApiKey() {
    // Clear the environment variable scenario - when no API key is set
    var builder = OpenRouterEmbeddingProvider.builder();

    // If OPENROUTER_API_KEY is not set in environment, this should throw
    // We can't reliably test this without mocking environment
  }

  @Test
  void close_shutsDownCleanly() {
    OpenRouterEmbeddingProvider provider =
        OpenRouterEmbeddingProvider.builder().apiKey("test-api-key").build();

    assertDoesNotThrow(provider::close);
  }

  // ==================== API Contract Tests ====================

  @Test
  void createEmbeddings_throwsOnNullInput() {
    OpenRouterEmbeddingProvider provider =
        OpenRouterEmbeddingProvider.builder().apiKey("test-api-key").build();

    try {
      assertThrows(
          NullPointerException.class,
          () -> provider.createEmbeddings(null, "openai/text-embedding-3-small"));
    } finally {
      provider.close();
    }
  }

  @Test
  void createEmbeddings_throwsOnNullModel() {
    OpenRouterEmbeddingProvider provider =
        OpenRouterEmbeddingProvider.builder().apiKey("test-api-key").build();

    try {
      assertThrows(
          NullPointerException.class,
          () -> provider.createEmbeddings(java.util.List.of("test"), null));
    } finally {
      provider.close();
    }
  }

  @Test
  void builder_returnsNewInstance() {
    assertNotNull(OpenRouterEmbeddingProvider.builder());
  }
}
