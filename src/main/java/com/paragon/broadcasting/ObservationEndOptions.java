package com.paragon.broadcasting;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record ObservationEndOptions(
    @Nullable Object output, @Nullable String statusMessage, @NonNull ObservationStatus status) {}
