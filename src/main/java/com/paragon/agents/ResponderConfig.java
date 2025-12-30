package com.paragon.agents;

import com.paragon.responses.OpenRouterCustomPayload;
import com.paragon.responses.spec.*;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public record ResponderConfig(
    @Nullable List<OutputDataInclude> include,
    @Nullable String instructions,
    @Nullable Integer maxOutputTokens,
    @Nullable Integer maxToolCalls,
    @Nullable Map<String, String> metadata,
    @Nullable String model,
    @Nullable Boolean parallelToolCalls,
    @Nullable String promptCacheKey,
    @Nullable String promptCacheRetention,
    @Nullable ReasoningConfig reasoning,
    @Nullable String safetyIdentifier,
    @Nullable ServiceTierType serviceTier,
    @Nullable Boolean store,
    @Nullable StreamOptions streamOptions,
    @Nullable Double temperature,
    @Nullable TextConfigurationOptions text,
    @Nullable ToolChoice toolChoice,
    @Nullable List<Tool> tools,
    @Nullable Integer topLogprobs,
    @Nullable Number topP,
    @Nullable Truncation truncation,
    @Nullable OpenRouterCustomPayload openRouterCustomPayload) {}
