package com.paragon.messaging.hooks;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Exception thrown by a {@link ProcessingHook} to interrupt message processing.
 *
 * <p>This exception is used by pre-hooks to intentionally stop the processing pipeline
 * (e.g., for content moderation, rate limiting, or validation failures). It is NOT
 * considered a processing error and will NOT trigger retries.</p>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * ProcessingHook moderationHook = context -> {
 *     if (containsInappropriateContent(context.messages())) {
 *         throw new HookInterruptedException(
 *             "Inappropriate content detected",
 *             "CONTENT_MODERATION"
 *         );
 *     }
 * };
 * }</pre>
 *
 * @author Agentle Team
 * @see ProcessingHook
 * @since 2.1
 */
public class HookInterruptedException extends Exception {

    private final String reasonCode;

    /**
     * Creates a new HookInterruptedException with a reason message.
     *
     * @param reason human-readable reason for interruption
     */
    public HookInterruptedException(@NonNull String reason) {
        this(reason, null);
    }

    /**
     * Creates a new HookInterruptedException with reason and code.
     *
     * @param reason     human-readable reason for interruption
     * @param reasonCode machine-readable code (e.g., "CONTENT_MODERATION", "RATE_LIMIT")
     */
    public HookInterruptedException(@NonNull String reason, @Nullable String reasonCode) {
        super(reason);
        this.reasonCode = reasonCode;
    }

    /**
     * Returns the machine-readable reason code.
     *
     * @return reason code or null if not set
     */
    public @Nullable String getReasonCode() {
        return reasonCode;
    }

    /**
     * Returns the human-readable reason.
     *
     * @return reason message
     */
    public @NonNull String getReason() {
        return getMessage();
    }
}
