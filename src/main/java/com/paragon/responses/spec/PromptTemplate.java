package com.paragon.responses.spec;

import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record PromptTemplate(
    @NonNull String id, @Nullable Map<String, String> variables, @Nullable String version) {}
