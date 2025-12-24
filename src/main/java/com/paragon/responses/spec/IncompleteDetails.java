package com.paragon.responses.spec;

import org.jspecify.annotations.Nullable;

/**
 * Details about why the response is incomplete.
 *
 * @param reason The reason why the response is incomplete.
 */
public record IncompleteDetails(@Nullable String reason) {}
