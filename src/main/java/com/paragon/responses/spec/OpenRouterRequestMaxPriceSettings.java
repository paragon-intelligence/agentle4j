package com.paragon.responses.spec;

import org.jspecify.annotations.Nullable;

public record OpenRouterRequestMaxPriceSettings(
    @Nullable Number prompt,
    @Nullable Number completion,
    @Nullable Number image,
    @Nullable Number audio,
    @Nullable Number request) {}
