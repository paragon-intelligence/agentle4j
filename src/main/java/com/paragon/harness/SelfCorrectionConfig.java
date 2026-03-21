package com.paragon.harness;

import com.paragon.agents.AgentResult;
import java.util.Objects;
import java.util.function.Predicate;
import org.jspecify.annotations.NonNull;

/**
 * Configuration for the self-correction loop in {@link SelfCorrectingInteractable}.
 *
 * <p>Self-correction automatically retries a failed agent run by injecting the error back as user
 * context, giving the agent a chance to fix its own mistakes.
 *
 * <p>Example:
 *
 * <pre>{@code
 * SelfCorrectionConfig config = SelfCorrectionConfig.builder()
 *     .maxRetries(3)
 *     .retryOn(result -> result.isError() || result.isGuardrailFailed())
 *     .feedbackTemplate("Your previous response failed with: {error}. Please try again.")
 *     .build();
 * }</pre>
 *
 * @see SelfCorrectingInteractable
 * @since 1.0
 */
public final class SelfCorrectionConfig {

  /** Default feedback template injected on failure. */
  public static final String DEFAULT_FEEDBACK_TEMPLATE =
      "Your previous response failed with the following error:\n\n"
          + "{error}\n\n"
          + "Please review your approach and try again, addressing the issue above.";

  private final int maxRetries;
  private final Predicate<AgentResult> retryOn;
  private final String feedbackTemplate;

  private SelfCorrectionConfig(Builder builder) {
    this.maxRetries = builder.maxRetries;
    this.retryOn = builder.retryOn;
    this.feedbackTemplate = builder.feedbackTemplate;
  }

  /**
   * Returns the maximum number of retry attempts.
   *
   * @return max retries
   */
  public int maxRetries() {
    return maxRetries;
  }

  /**
   * Returns the predicate that decides whether a result should trigger a retry.
   *
   * @return retry predicate
   */
  public @NonNull Predicate<AgentResult> retryOn() {
    return retryOn;
  }

  /**
   * Returns the feedback template. Use {@code {error}} as a placeholder for the error message.
   *
   * @return feedback template
   */
  public @NonNull String feedbackTemplate() {
    return feedbackTemplate;
  }

  /**
   * Formats the error message into the feedback template.
   *
   * @param errorMessage the error message to inject
   * @return the formatted feedback string
   */
  public @NonNull String formatFeedback(@NonNull String errorMessage) {
    return feedbackTemplate.replace("{error}", errorMessage);
  }

  /**
   * Returns a new builder with sensible defaults:
   *
   * <ul>
   *   <li>maxRetries = 3
   *   <li>retryOn = any error result
   *   <li>feedbackTemplate = {@link #DEFAULT_FEEDBACK_TEMPLATE}
   * </ul>
   *
   * @return a new builder
   */
  public static @NonNull Builder builder() {
    return new Builder();
  }

  /** Builder for {@link SelfCorrectionConfig}. */
  public static final class Builder {
    private int maxRetries = 3;
    private Predicate<AgentResult> retryOn = AgentResult::isError;
    private String feedbackTemplate = DEFAULT_FEEDBACK_TEMPLATE;

    private Builder() {}

    /**
     * Sets the maximum number of self-correction retries.
     *
     * @param maxRetries must be positive
     * @return this builder
     */
    public @NonNull Builder maxRetries(int maxRetries) {
      if (maxRetries <= 0) throw new IllegalArgumentException("maxRetries must be positive");
      this.maxRetries = maxRetries;
      return this;
    }

    /**
     * Sets the predicate that decides whether to retry.
     *
     * @param retryOn predicate returning true when a retry should be attempted
     * @return this builder
     */
    public @NonNull Builder retryOn(@NonNull Predicate<AgentResult> retryOn) {
      this.retryOn = Objects.requireNonNull(retryOn, "retryOn cannot be null");
      return this;
    }

    /**
     * Sets the feedback template. Use {@code {error}} where you want the error message injected.
     *
     * @param feedbackTemplate the template string
     * @return this builder
     */
    public @NonNull Builder feedbackTemplate(@NonNull String feedbackTemplate) {
      Objects.requireNonNull(feedbackTemplate, "feedbackTemplate cannot be null");
      if (!feedbackTemplate.contains("{error}")) {
        throw new IllegalArgumentException("feedbackTemplate must contain {error} placeholder");
      }
      this.feedbackTemplate = feedbackTemplate;
      return this;
    }

    /** Builds the configuration. */
    public @NonNull SelfCorrectionConfig build() {
      return new SelfCorrectionConfig(this);
    }
  }
}
