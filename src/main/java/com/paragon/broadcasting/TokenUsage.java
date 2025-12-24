package com.paragon.broadcasting;

import org.jspecify.annotations.NonNull;

public record TokenUsage(
    @NonNull Integer inputTokens, @NonNull Integer outputTokens, @NonNull Integer totalTokens) {}
