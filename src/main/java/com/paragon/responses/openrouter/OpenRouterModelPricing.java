package com.paragon.responses.openrouter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Pricing information for an OpenRouter model.
 *
 * <p>Prices are in USD per token. The OpenRouter API returns these as strings to handle arbitrary
 * precision.
 *
 * @param prompt cost per input/prompt token (USD)
 * @param completion cost per output/completion token (USD)
 * @param image optional cost per image (USD)
 * @param audio optional cost per audio (USD)
 * @param request optional fixed cost per request (USD)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenRouterModelPricing(
    @JsonProperty("prompt") @NonNull String prompt,
    @JsonProperty("completion") @NonNull String completion,
    @JsonProperty("image") @Nullable String image,
    @JsonProperty("audio") @Nullable String audio,
    @JsonProperty("request") @Nullable String request,
    @JsonProperty("input_cache_read") @Nullable String inputCacheRead,
    @JsonProperty("input_cache_write") @Nullable String inputCacheWrite,
    @JsonProperty("discount") @Nullable Double discount) {

  /**
   * Calculates the total cost for a request given token counts.
   *
   * @param inputTokens number of input/prompt tokens
   * @param outputTokens number of output/completion tokens
   * @return total cost in USD, or null if pricing data is invalid
   */
  public @Nullable BigDecimal calculateCost(int inputTokens, int outputTokens) {
    try {
      BigDecimal promptCost = new BigDecimal(prompt);
      BigDecimal completionCost = new BigDecimal(completion);

      BigDecimal inputCost = promptCost.multiply(BigDecimal.valueOf(inputTokens));
      BigDecimal outputCost = completionCost.multiply(BigDecimal.valueOf(outputTokens));
      BigDecimal total = inputCost.add(outputCost);

      // Apply discount if present
      if (discount != null && discount > 0) {
        BigDecimal discountMultiplier = BigDecimal.ONE.subtract(BigDecimal.valueOf(discount));
        total = total.multiply(discountMultiplier);
      }

      return total.setScale(10, RoundingMode.HALF_UP);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /** Returns the prompt cost as a BigDecimal. */
  public @Nullable BigDecimal promptAsBigDecimal() {
    try {
      return new BigDecimal(prompt);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /** Returns the completion cost as a BigDecimal. */
  public @Nullable BigDecimal completionAsBigDecimal() {
    try {
      return new BigDecimal(completion);
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
