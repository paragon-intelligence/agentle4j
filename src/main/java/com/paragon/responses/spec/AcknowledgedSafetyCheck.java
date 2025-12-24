package com.paragon.responses.spec;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The safety checks reported by the API that have been acknowledged by the developer.
 *
 * @param id The ID of the pending safety check.
 * @param code The type of the pending safety check.
 * @param message Details about the pending safety check.
 */
public record AcknowledgedSafetyCheck(
    @NonNull String id, @Nullable String code, @Nullable String message) {}
