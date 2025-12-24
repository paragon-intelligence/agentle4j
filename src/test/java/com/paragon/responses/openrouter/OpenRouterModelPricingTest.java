package com.paragon.responses.openrouter;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link OpenRouterModelPricing}. */
@DisplayName("OpenRouterModelPricing")
class OpenRouterModelPricingTest {

  @Nested
  @DisplayName("calculateCost()")
  class CalculateCost {

    @Test
    @DisplayName("should calculate cost correctly for standard tokens")
    void calculateStandardCost() {
      // GPT-4o pricing: $2.50/1M input, $10.00/1M output
      var pricing =
          new OpenRouterModelPricing(
              "0.0000025", // $2.50 per million = 0.0000025 per token
              "0.00001", // $10.00 per million = 0.00001 per token
              null,
              null,
              null,
              null,
              null,
              null);

      BigDecimal cost = pricing.calculateCost(1000, 500);

      // Expected: 1000 * 0.0000025 + 500 * 0.00001 = 0.0025 + 0.005 = 0.0075
      assertNotNull(cost);
      assertEquals(new BigDecimal("0.0075000000"), cost);
    }

    @Test
    @DisplayName("should calculate cost with zero tokens")
    void calculateZeroTokens() {
      var pricing =
          new OpenRouterModelPricing("0.0000025", "0.00001", null, null, null, null, null, null);

      BigDecimal cost = pricing.calculateCost(0, 0);

      assertNotNull(cost);
      assertEquals(BigDecimal.ZERO.setScale(10), cost);
    }

    @Test
    @DisplayName("should apply discount when present")
    void calculateCostWithDiscount() {
      var pricing =
          new OpenRouterModelPricing(
              "0.0000025", "0.00001", null, null, null, null, null, 0.1 // 10% discount
              );

      BigDecimal cost = pricing.calculateCost(1000, 500);

      // Expected: 0.0075 * 0.9 = 0.00675
      assertNotNull(cost);
      assertEquals(new BigDecimal("0.0067500000"), cost);
    }

    @Test
    @DisplayName("should return null for invalid pricing format")
    void returnNullForInvalidPricing() {
      var pricing =
          new OpenRouterModelPricing("invalid", "0.00001", null, null, null, null, null, null);

      BigDecimal cost = pricing.calculateCost(1000, 500);

      assertNull(cost);
    }
  }

  @Nested
  @DisplayName("promptAsBigDecimal()")
  class PromptAsBigDecimal {

    @Test
    @DisplayName("should parse valid prompt cost")
    void parseValidPromptCost() {
      var pricing =
          new OpenRouterModelPricing("0.0000025", "0.00001", null, null, null, null, null, null);

      BigDecimal result = pricing.promptAsBigDecimal();

      assertNotNull(result);
      assertEquals(new BigDecimal("0.0000025"), result);
    }

    @Test
    @DisplayName("should return null for invalid format")
    void returnNullForInvalidFormat() {
      var pricing =
          new OpenRouterModelPricing("not-a-number", "0.00001", null, null, null, null, null, null);

      BigDecimal result = pricing.promptAsBigDecimal();

      assertNull(result);
    }
  }

  @Nested
  @DisplayName("completionAsBigDecimal()")
  class CompletionAsBigDecimal {

    @Test
    @DisplayName("should parse valid completion cost")
    void parseValidCompletionCost() {
      var pricing =
          new OpenRouterModelPricing("0.0000025", "0.00001", null, null, null, null, null, null);

      BigDecimal result = pricing.completionAsBigDecimal();

      assertNotNull(result);
      assertEquals(new BigDecimal("0.00001"), result);
    }
  }
}
