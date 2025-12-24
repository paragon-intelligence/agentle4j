package com.paragon.responses.spec;

import org.jspecify.annotations.Nullable;

public record ReasoningConfig(
    @Nullable ReasoningEffort effort, @Nullable ReasoningSummaryKind summary) {}
