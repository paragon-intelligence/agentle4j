package com.paragon.responses.spec;

import org.jspecify.annotations.NonNull;

/**
 * Represents a summary of the reasoning output from the model so far.
 *
 * @param text A summary of the reasoning output from the model so far.
 */
public record ReasoningSummaryText(@NonNull String text) implements ReasoningSummary {}
