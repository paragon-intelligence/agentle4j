package com.paragon.responses.spec;

import org.jspecify.annotations.NonNull;

public record ItemReference(@NonNull String id) implements ResponseInputItem {}
