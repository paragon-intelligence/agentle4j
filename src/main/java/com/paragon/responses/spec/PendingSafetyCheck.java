package com.paragon.responses.spec;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The pending safety checks for the computer call.
 *
 * @param id The ID of the pending safety check.
 * @param code The type of the pending safety check.
 * @param message Details about the pending safety check.
 */
public record PendingSafetyCheck(
    @NonNull String id, @Nullable String code, @Nullable String message) {
  @Override
  public @NonNull String toString() {
    return String.format(
        """
        <pending_safety_check>
            <id>%s</id>
            <code>%s</code>
            <message>%s</message>
        </pending_safety_check>
        """,
        id, code != null ? code : "null", message != null ? message : "null");
  }
}
