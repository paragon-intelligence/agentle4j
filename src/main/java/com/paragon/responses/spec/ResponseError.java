package com.paragon.responses.spec;

import org.jspecify.annotations.Nullable;

/**
 * An error object returned when the model fails to generate a Response.
 *
 * @param code The error code for the response.
 * @param message A human-readable description of the error.
 */
public record ResponseError(@Nullable ErrorCode code, @Nullable String message) {}
