package com.paragon.responses.openrouter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.jspecify.annotations.NonNull;

/**
 * Response from the OpenRouter /api/v1/models endpoint.
 *
 * @param data list of available models with pricing
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenRouterModelsResponse(@JsonProperty("data") @NonNull List<OpenRouterModel> data) {

  /** Returns an empty response. */
  public static @NonNull OpenRouterModelsResponse empty() {
    return new OpenRouterModelsResponse(List.of());
  }
}
