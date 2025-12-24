package com.paragon.responses;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.responses.spec.*;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests for OpenRouterCustomPayload serialization and deserialization. Validates that:
 *
 * <ul>
 *   <li>Field names are snake_case (e.g., provider_config, session_id)
 *   <li>Enum values are lowercase (e.g., fallback, sort)
 *   <li>Round-trip serialization preserves data
 * </ul>
 */
class OpenRouterCustomPayloadSerializationTest {

  private final ObjectMapper objectMapper = ResponsesApiObjectMapper.create();

  // ===== Snake Case Field Naming Tests =====

  @Test
  void openRouterCustomPayload_fieldNames_areSnakeCase() throws Exception {
    OpenRouterCustomPayload payload =
        OpenRouterCustomPayload.builder()
            .route(OpenRouterRouteStrategy.FALLBACK)
            .user("user-123")
            .sessionId("session-456")
            .build();

    String json = objectMapper.writeValueAsString(payload);
    JsonNode node = objectMapper.readTree(json);

    // Verify snake_case field names
    assertTrue(node.has("session_id"), "Field should be 'session_id', not 'sessionId'");
    assertTrue(node.has("route"), "Field should be present");
    assertTrue(node.has("user"), "Field should be present");

    // Verify camelCase is NOT used
    assertFalse(node.has("sessionId"), "Should not have camelCase 'sessionId'");
    assertFalse(node.has("providerConfig"), "Should not have camelCase 'providerConfig'");
  }

  @Test
  void openRouterProviderConfig_fieldNames_areSnakeCase() throws Exception {
    OpenRouterProviderConfig config =
        new OpenRouterProviderConfig(
            true, // allowFallbacks
            true, // requireParameters
            DataCollectionSetting.DENY, // dataCollection
            false, // zdr
            true, // enforceDistillableText
            null, // order
            null, // only
            null, // ignore
            List.of(Quantization.FP8, Quantization.INT8), // quantizations
            OpenRouterProviderSortingStrategy.PRICE, // sort
            null // maxPrice
            );

    String json = objectMapper.writeValueAsString(config);
    JsonNode node = objectMapper.readTree(json);

    // Verify snake_case field names
    assertTrue(node.has("allow_fallbacks"), "Should have 'allow_fallbacks'");
    assertTrue(node.has("require_parameters"), "Should have 'require_parameters'");
    assertTrue(node.has("data_collection"), "Should have 'data_collection'");
    assertTrue(node.has("enforce_distillable_text"), "Should have 'enforce_distillable_text'");
    assertTrue(node.has("quantizations"), "Should have 'quantizations'");
    assertTrue(node.has("sort"), "Should have 'sort'");

    // Verify camelCase is NOT used
    assertFalse(node.has("allowFallbacks"), "Should not have camelCase 'allowFallbacks'");
    assertFalse(node.has("requireParameters"), "Should not have camelCase 'requireParameters'");
    assertFalse(node.has("dataCollection"), "Should not have camelCase 'dataCollection'");
  }

  // ===== Enum Serialization Tests =====

  @Test
  void openRouterRouteStrategy_serializes_asLowercase() throws Exception {
    OpenRouterCustomPayload payload =
        OpenRouterCustomPayload.builder().route(OpenRouterRouteStrategy.FALLBACK).build();

    String json = objectMapper.writeValueAsString(payload);

    // Verify lowercase enum value
    assertTrue(
        json.contains("\"route\":\"fallback\""), "Should serialize as 'fallback', not 'FALLBACK'");
    assertFalse(json.contains("FALLBACK"), "Should not contain uppercase 'FALLBACK'");
  }

  @Test
  void openRouterRouteStrategy_serializes_sort_asLowercase() throws Exception {
    OpenRouterCustomPayload payload =
        OpenRouterCustomPayload.builder().route(OpenRouterRouteStrategy.SORT).build();

    String json = objectMapper.writeValueAsString(payload);

    assertTrue(json.contains("\"route\":\"sort\""), "Should serialize as 'sort', not 'SORT'");
  }

  @Test
  void dataCollectionSetting_serializes_asLowercase() throws Exception {
    OpenRouterProviderConfig config =
        new OpenRouterProviderConfig(
            null, null, DataCollectionSetting.DENY, null, null, null, null, null, null, null, null);

    String json = objectMapper.writeValueAsString(config);

    assertTrue(
        json.contains("\"data_collection\":\"deny\""), "Should serialize as 'deny', not 'DENY'");
  }

  @Test
  void quantization_serializes_asLowercase() throws Exception {
    OpenRouterProviderConfig config =
        new OpenRouterProviderConfig(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            List.of(Quantization.FP8, Quantization.BF16),
            null,
            null);

    String json = objectMapper.writeValueAsString(config);

    assertTrue(json.contains("\"fp8\""), "Should serialize as 'fp8', not 'FP8'");
    assertTrue(json.contains("\"bf16\""), "Should serialize as 'bf16', not 'BF16'");
  }

  @Test
  void openRouterProviderSortingStrategy_serializes_asLowercase() throws Exception {
    OpenRouterProviderConfig config =
        new OpenRouterProviderConfig(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            OpenRouterProviderSortingStrategy.THROUGHPUT,
            null);

    String json = objectMapper.writeValueAsString(config);

    assertTrue(
        json.contains("\"sort\":\"throughput\""),
        "Should serialize as 'throughput', not 'THROUGHPUT'");
  }

  // ===== Round-trip Tests =====

  @Test
  void openRouterCustomPayload_roundTrip_preservesData() throws Exception {
    OpenRouterCustomPayload original =
        OpenRouterCustomPayload.builder()
            .route(OpenRouterRouteStrategy.FALLBACK)
            .user("test-user")
            .sessionId("test-session-123")
            .build();

    String json = objectMapper.writeValueAsString(original);
    OpenRouterCustomPayload deserialized =
        objectMapper.readValue(json, OpenRouterCustomPayload.class);

    assertEquals(original, deserialized, "Round-trip should preserve data");
    assertEquals(OpenRouterRouteStrategy.FALLBACK, deserialized.route());
    assertEquals("test-user", deserialized.user());
    assertEquals("test-session-123", deserialized.sessionId());
  }

  @Test
  void openRouterProviderConfig_roundTrip_preservesData() throws Exception {
    OpenRouterProviderConfig original =
        new OpenRouterProviderConfig(
            true,
            false,
            DataCollectionSetting.ALLOW,
            true,
            false,
            null,
            null,
            null,
            List.of(Quantization.INT4, Quantization.FP32),
            OpenRouterProviderSortingStrategy.LATENCY,
            null);

    String json = objectMapper.writeValueAsString(original);
    OpenRouterProviderConfig deserialized =
        objectMapper.readValue(json, OpenRouterProviderConfig.class);

    assertEquals(original, deserialized, "Round-trip should preserve data");
  }

  @Test
  void openRouterRouteStrategy_deserialize_fromLowercase() throws Exception {
    String json = "{\"route\":\"fallback\",\"user\":\"test\"}";

    OpenRouterCustomPayload payload = objectMapper.readValue(json, OpenRouterCustomPayload.class);

    assertEquals(OpenRouterRouteStrategy.FALLBACK, payload.route());
    assertEquals("test", payload.user());
  }

  @Test
  void openRouterRouteStrategy_deserialize_fromSort() throws Exception {
    String json = "{\"route\":\"sort\"}";

    OpenRouterCustomPayload payload = objectMapper.readValue(json, OpenRouterCustomPayload.class);

    assertEquals(OpenRouterRouteStrategy.SORT, payload.route());
  }

  // ===== Full Payload Unwrapping Test =====

  @Test
  void createResponsePayload_unwraps_openRouterFields() throws Exception {
    CreateResponsePayload payload =
        new CreateResponsePayload(
            null,
            null,
            null,
            null,
            "test",
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
            OpenRouterCustomPayload.builder()
                .route(OpenRouterRouteStrategy.FALLBACK)
                .user("test-user")
                .sessionId("test-session")
                .build());

    String json = objectMapper.writeValueAsString(payload);
    JsonNode node = objectMapper.readTree(json);

    // OpenRouter fields should be unwrapped to root level
    assertTrue(node.has("route"), "OpenRouter 'route' field should be at root level");
    assertTrue(node.has("user"), "OpenRouter 'user' field should be at root level");
    assertTrue(
        node.has("session_id"),
        "OpenRouter 'session_id' field should be at root level (snake_case)");

    // There should NOT be a nested openRouterCustomPayload object
    assertFalse(
        node.has("openRouterCustomPayload"), "Should not have nested 'openRouterCustomPayload'");
    assertFalse(
        node.has("open_router_custom_payload"),
        "Should not have nested 'open_router_custom_payload'");

    // Values should be correct
    assertEquals("fallback", node.get("route").asText());
    assertEquals("test-user", node.get("user").asText());
  }
}
