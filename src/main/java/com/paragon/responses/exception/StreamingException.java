package com.paragon.responses.exception;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Exception thrown when an error occurs during streaming.
 *
 * <p>Provides streaming-specific context:
 *
 * <ul>
 *   <li>{@link #partialOutput()} - Any content received before the failure
 *   <li>{@link #bytesReceived()} - Total bytes received before failure
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * responder.respond(streamingPayload)
 *     .onError(error -> {
 *         if (error instanceof StreamingException se && se.partialOutput() != null) {
 *             savePartialOutput(se.partialOutput());
 *         }
 *     })
 *     .start();
 * }</pre>
 */
public class StreamingException extends AgentleException {

  private final @Nullable String partialOutput;
  private final long bytesReceived;

  /**
   * Creates a new StreamingException.
   *
   * @param code the error code
   * @param message the error message
   * @param partialOutput any output received before failure
   * @param bytesReceived total bytes received
   * @param retryable whether the error is retryable
   */
  public StreamingException(
      @NonNull ErrorCode code,
      @NonNull String message,
      @Nullable String partialOutput,
      long bytesReceived,
      boolean retryable) {
    super(code, message, null, retryable);
    this.partialOutput = partialOutput;
    this.bytesReceived = bytesReceived;
  }

  /**
   * Creates a new StreamingException with a cause.
   *
   * @param code the error code
   * @param message the error message
   * @param cause the underlying cause
   * @param partialOutput any output received before failure
   * @param bytesReceived total bytes received
   * @param retryable whether the error is retryable
   */
  public StreamingException(
      @NonNull ErrorCode code,
      @NonNull String message,
      @NonNull Throwable cause,
      @Nullable String partialOutput,
      long bytesReceived,
      boolean retryable) {
    super(code, message, cause, null, retryable);
    this.partialOutput = partialOutput;
    this.bytesReceived = bytesReceived;
  }

  /**
   * Creates a connection dropped exception.
   *
   * @param cause the underlying cause
   * @param partialOutput any output received before failure
   * @param bytesReceived total bytes received
   * @return a new StreamingException
   */
  public static StreamingException connectionDropped(
      @NonNull Throwable cause, @Nullable String partialOutput, long bytesReceived) {
    return new StreamingException(
        ErrorCode.CONNECTION_DROPPED,
        "Connection dropped during streaming: " + cause.getMessage(),
        cause,
        partialOutput,
        bytesReceived,
        true);
  }

  /**
   * Creates a stream timeout exception.
   *
   * @param partialOutput any output received before timeout
   * @param bytesReceived total bytes received
   * @return a new StreamingException
   */
  public static StreamingException timeout(@Nullable String partialOutput, long bytesReceived) {
    return new StreamingException(
        ErrorCode.STREAM_TIMEOUT,
        "Stream timed out waiting for data",
        partialOutput,
        bytesReceived,
        true);
  }

  /**
   * Returns any output received before the failure.
   *
   * <p>This allows recovery of partial content for UI display or caching.
   *
   * @return the partial output, or null if nothing was received
   */
  public @Nullable String partialOutput() {
    return partialOutput;
  }

  /**
   * Returns the total bytes received before failure.
   *
   * @return bytes received
   */
  public long bytesReceived() {
    return bytesReceived;
  }
}
