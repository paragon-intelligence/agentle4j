package com.paragon.responses.openrouter;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link OpenRouterModel}. */
@DisplayName("OpenRouterModel")
class OpenRouterModelTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Nested
  @DisplayName("JSON Deserialization")
  class JsonDeserialization {

    @Test
    @DisplayName("should deserialize complete model JSON")
    void deserializeCompleteModel() throws Exception {
      String json =
          """
          {
            "id": "openai/gpt-4o",
            "name": "GPT-4o",
            "context_length": 128000,
            "description": "OpenAI's most capable model",
            "pricing": {
              "prompt": "0.0000025",
              "completion": "0.00001"
            }
          }
          """;

      OpenRouterModel model = objectMapper.readValue(json, OpenRouterModel.class);

      assertEquals("openai/gpt-4o", model.id());
      assertEquals("GPT-4o", model.name());
      assertEquals(128000, model.contextLength());
      assertEquals("OpenAI's most capable model", model.description());
      assertNotNull(model.pricing());
      assertEquals("0.0000025", model.pricing().prompt());
      assertEquals("0.00001", model.pricing().completion());
    }

    @Test
    @DisplayName("should handle missing optional fields")
    void handleMissingOptionalFields() throws Exception {
      String json =
          """
          {
            "id": "meta/llama-3",
            "name": "Llama 3",
            "pricing": {
              "prompt": "0.0000005",
              "completion": "0.0000005"
            }
          }
          """;

      OpenRouterModel model = objectMapper.readValue(json, OpenRouterModel.class);

      assertEquals("meta/llama-3", model.id());
      assertEquals("Llama 3", model.name());
      assertNull(model.contextLength());
      assertNull(model.description());
    }

    @Test
    @DisplayName("should ignore unknown fields")
    void ignoreUnknownFields() throws Exception {
      String json =
          """
          {
            "id": "openai/gpt-4o",
            "name": "GPT-4o",
            "unknown_field": "should be ignored",
            "pricing": {
              "prompt": "0.0000025",
              "completion": "0.00001",
              "unknown_pricing_field": "0.001"
            }
          }
          """;

      OpenRouterModel model = objectMapper.readValue(json, OpenRouterModel.class);

      assertEquals("openai/gpt-4o", model.id());
      assertNotNull(model.pricing());
    }
  }

  @Nested
  @DisplayName("calculateCost()")
  class CalculateCost {

    @Test
    @DisplayName("should delegate to pricing")
    void delegateToPricing() {
      var pricing =
          new OpenRouterModelPricing("0.0000025", "0.00001", null, null, null, null, null, null);
      var model = new OpenRouterModel("openai/gpt-4o", "GPT-4o", 128000, pricing, null);

      BigDecimal cost = model.calculateCost(1000, 500);

      assertNotNull(cost);
      assertEquals(new BigDecimal("0.0075000000"), cost);
    }
  }

  @Nested
  @DisplayName("costPer1kTokens")
  class CostPer1kTokens {

    @Test
    @DisplayName("should calculate cost per 1k input tokens")
    void costPer1kInputTokens() {
      var pricing =
          new OpenRouterModelPricing("0.0000025", "0.00001", null, null, null, null, null, null);
      var model = new OpenRouterModel("openai/gpt-4o", "GPT-4o", 128000, pricing, null);

      BigDecimal costPer1k = model.costPer1kInputTokens();

      assertNotNull(costPer1k);
      // 0.0000025 * 1000 = 0.0025
      assertEquals(new BigDecimal("0.0025000"), costPer1k);
    }

    @Test
    @DisplayName("should calculate cost per 1k output tokens")
    void costPer1kOutputTokens() {
      var pricing =
          new OpenRouterModelPricing("0.0000025", "0.00001", null, null, null, null, null, null);
      var model = new OpenRouterModel("openai/gpt-4o", "GPT-4o", 128000, pricing, null);

      BigDecimal costPer1k = model.costPer1kOutputTokens();

      assertNotNull(costPer1k);
      // 0.00001 * 1000 = 0.01
      assertEquals(new BigDecimal("0.01000"), costPer1k);
    }
  }
}
