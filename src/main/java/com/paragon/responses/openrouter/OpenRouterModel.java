package com.paragon.responses.openrouter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Represents a model from the OpenRouter API.
 *
 * <p>Only includes fields relevant for pricing and identification. Other fields from the API are
 * ignored.
 *
 * @param id unique model identifier (e.g., "openai/gpt-4o")
 * @param name display name of the model
 * @param contextLength maximum context length in tokens
 * @param pricing pricing information for the model
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenRouterModel(
    @JsonProperty("id") @NonNull String id,
    @JsonProperty("name") @NonNull String name,
    @JsonProperty("context_length") @Nullable Integer contextLength,
    @JsonProperty("pricing") @NonNull OpenRouterModelPricing pricing,
    @JsonProperty("description") @Nullable String description) {

  /**
   * Calculates the cost for a request with this model.
   *
   * @param inputTokens number of input tokens
   * @param outputTokens number of output tokens
   * @return cost in USD, or null if pricing is invalid
   */
  public @Nullable BigDecimal calculateCost(int inputTokens, int outputTokens) {
    return pricing.calculateCost(inputTokens, outputTokens);
  }

  /** Returns the cost per 1000 input tokens for display purposes. */
  public @Nullable BigDecimal costPer1kInputTokens() {
    BigDecimal perToken = pricing.promptAsBigDecimal();
    return perToken != null ? perToken.multiply(BigDecimal.valueOf(1000)) : null;
  }

  /** Returns the cost per 1000 output tokens for display purposes. */
  public @Nullable BigDecimal costPer1kOutputTokens() {
    BigDecimal perToken = pricing.completionAsBigDecimal();
    return perToken != null ? perToken.multiply(BigDecimal.valueOf(1000)) : null;
  }
}
