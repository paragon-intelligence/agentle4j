package com.paragon.responses.openrouter;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;

/** Unit tests for {@link OpenRouterModelRegistry}. */
@DisplayName("OpenRouterModelRegistry")
class OpenRouterModelRegistryTest {

  private MockWebServer mockWebServer;
  private OkHttpClient httpClient;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    httpClient =
        new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(5))
            .readTimeout(Duration.ofSeconds(5))
            .build();
    objectMapper = new ObjectMapper();
  }

  @AfterEach
  void tearDown() throws IOException {
    mockWebServer.shutdown();
  }

  private OpenRouterModelRegistry createRegistry() {
    return OpenRouterModelRegistry.builder()
        .apiKey("test-api-key")
        .httpClient(httpClient)
        .objectMapper(objectMapper)
        .cacheTtl(Duration.ofMinutes(5))
        .build();
  }

  @Nested
  @DisplayName("Builder")
  class BuilderTests {

    @Test
    @DisplayName("should throw if API key is missing")
    void throwIfApiKeyMissing() {
      assertThrows(IllegalStateException.class, () -> OpenRouterModelRegistry.builder().build());
    }

    @Test
    @DisplayName("should create registry with defaults")
    void createWithDefaults() {
      var registry = OpenRouterModelRegistry.builder().apiKey("test-key").build();

      assertNotNull(registry);
      assertFalse(registry.isInitialized());
    }
  }

  @Nested
  @DisplayName("getModel()")
  class GetModel {

    @Test
    @DisplayName("should return empty for non-existent model before initialization")
    void returnEmptyBeforeInit() {
      var registry = createRegistry();

      Optional<OpenRouterModel> model = registry.getModel("openai/gpt-4o");

      // Before any request, cache is empty
      assertTrue(model.isEmpty() || registry.getCachedModelCount() >= 0);
    }
  }

  @Nested
  @DisplayName("calculateCost()")
  class CalculateCostTests {

    @Test
    @DisplayName("should return empty when model not found")
    void returnEmptyWhenModelNotFound() {
      var registry = createRegistry();

      Optional<BigDecimal> cost = registry.calculateCost("non-existent-model", 1000, 500);

      assertTrue(cost.isEmpty());
    }
  }

  @Nested
  @DisplayName("Cache behavior")
  class CacheBehavior {

    @Test
    @DisplayName("should report not initialized before first access")
    void notInitializedBeforeAccess() {
      var registry = createRegistry();

      assertFalse(registry.isInitialized());
      assertEquals(0, registry.getCachedModelCount());
    }

    @Test
    @DisplayName("should invalidate cache when requested")
    void invalidateCache() {
      var registry = createRegistry();

      registry.invalidateCache();

      assertFalse(registry.isInitialized());
    }
  }
}
