package com.paragon.telemetry.langfuse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.telemetry.events.*;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for LangfuseProcessor builder and configuration.
 *
 * <p>Note: Integration tests that actually send telemetry require a Langfuse account and are not
 * included here. These tests focus on builder configuration and accessors.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LangfuseProcessor Tests")
class LangfuseProcessorTest {

  private static final String DEFAULT_ENDPOINT =
      "https://cloud.langfuse.com/api/public/otel/v1/traces";

  // ═══════════════════════════════════════════════════════════════════════════
  // BUILDER
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Builder")
  class BuilderTests {

    @Test
    @DisplayName("builder creates processor with required fields")
    void builderCreatesProcessor() {
      LangfuseProcessor processor =
          LangfuseProcessor.builder().publicKey("pk-test").secretKey("sk-test").build();

      assertNotNull(processor);
    }

    @Test
    @DisplayName("builder sets httpClient")
    void builderSetsHttpClient() {
      OkHttpClient client = new OkHttpClient.Builder().build();

      LangfuseProcessor processor =
          LangfuseProcessor.builder()
              .httpClient(client)
              .publicKey("pk-test")
              .secretKey("sk-test")
              .build();

      assertNotNull(processor);
    }

    @Test
    @DisplayName("builder sets objectMapper")
    void builderSetsObjectMapper() {
      ObjectMapper mapper = new ObjectMapper();

      LangfuseProcessor processor =
          LangfuseProcessor.builder()
              .objectMapper(mapper)
              .publicKey("pk-test")
              .secretKey("sk-test")
              .build();

      assertNotNull(processor);
    }

    @Test
    @DisplayName("builder sets custom endpoint")
    void builderSetsCustomEndpoint() {
      LangfuseProcessor processor =
          LangfuseProcessor.builder()
              .endpoint("https://self-hosted.example.com/otel/traces")
              .publicKey("pk-test")
              .secretKey("sk-test")
              .build();

      assertNotNull(processor);
    }

    @Test
    @DisplayName("fromEnv method exists")
    void fromEnvMethodExists() {
      LangfuseProcessor.Builder builder = LangfuseProcessor.builder().fromEnv();
      assertNotNull(builder);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // VALIDATION
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Validation")
  class Validation {

    @Test
    @DisplayName("throws on missing publicKey")
    void throwsOnMissingPublicKey() {
      assertThrows(
          IllegalStateException.class,
          () -> LangfuseProcessor.builder().secretKey("sk-test").build());
    }

    @Test
    @DisplayName("throws on missing secretKey")
    void throwsOnMissingSecretKey() {
      assertThrows(
          IllegalStateException.class,
          () -> LangfuseProcessor.builder().publicKey("pk-test").build());
    }
  }

}
