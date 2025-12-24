package com.paragon.responses.spec;

import com.paragon.LlmProvider;
import org.jspecify.annotations.Nullable;

import java.util.List;

public record OpenRouterProviderConfig(
        @Nullable Boolean allowFallbacks,
        @Nullable Boolean requireParameters,
        @Nullable DataCollectionSetting dataCollection,
        @Nullable Boolean zdr,
        @Nullable Boolean enforceDistillableText,
        @Nullable List<LlmProvider> order,
        @Nullable List<LlmProvider> only,
        @Nullable List<LlmProvider> ignore,
        @Nullable List<Quantization> quantizations,
        @Nullable OpenRouterProviderSortingStrategy sort,
        @Nullable OpenRouterRequestMaxPriceSettings maxPrice) {

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    @Nullable Boolean allowFallbacks = null;
    @Nullable Boolean requireParameters = null;
    @Nullable DataCollectionSetting dataCollection = null;
    @Nullable Boolean zdr = null;
    @Nullable Boolean enforceDistillableText = null;
    @Nullable List<LlmProvider> order = null;
    @Nullable List<LlmProvider> only = null;
    @Nullable List<LlmProvider> ignore = null;
    @Nullable List<Quantization> quantizations = null;
    @Nullable OpenRouterProviderSortingStrategy sort = null;
    @Nullable OpenRouterRequestMaxPriceSettings maxPrice = null;

    public OpenRouterProviderConfig build() {
      return new OpenRouterProviderConfig(
              allowFallbacks,
              requireParameters,
              dataCollection,
              zdr,
              enforceDistillableText,
              order,
              only,
              ignore,
              quantizations,
              sort, maxPrice);
    }
  }
}
