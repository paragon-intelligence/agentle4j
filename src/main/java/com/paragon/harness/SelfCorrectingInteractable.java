package com.paragon.harness;

import com.paragon.agents.AgentStream;
import com.paragon.agents.AgenticContext;
import com.paragon.agents.AgentResult;
import com.paragon.agents.Interactable;
import com.paragon.responses.TraceMetadata;
import com.paragon.responses.spec.Message;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Decorator that wraps any {@link Interactable} with a self-correction loop.
 *
 * <p>When the wrapped agent produces a result that satisfies the {@code retryOn} predicate
 * (e.g., an error or guardrail failure), this decorator:
 *
 * <ol>
 *   <li>Formats the error into the configured feedback template
 *   <li>Injects the feedback as a user message into the conversation context
 *   <li>Re-runs the agent (up to {@code maxRetries} times)
 *   <li>Returns the final result (successful or last failure)
 * </ol>
 *
 * <p>LangChain data shows this pattern gives the largest benchmark improvements of any
 * harness feature, because it closes the feedback loop within a single session.
 *
 * <p>Example:
 *
 * <pre>{@code
 * Interactable agent = Agent.builder()
 *     .name("CodeWriter")
 *     .addOutputGuardrail(syntaxChecker)
 *     .build();
 *
 * SelfCorrectionConfig config = SelfCorrectionConfig.builder()
 *     .maxRetries(3)
 *     .retryOn(result -> result.isError())
 *     .build();
 *
 * Interactable correcting = SelfCorrectingInteractable.wrap(agent, config);
 * AgentResult result = correcting.interact("Write a Python function that sorts a list");
 * }</pre>
 *
 * @see SelfCorrectionConfig
 * @since 1.0
 */
public final class SelfCorrectingInteractable implements Interactable {

  private final Interactable delegate;
  private final SelfCorrectionConfig config;

  private SelfCorrectingInteractable(Interactable delegate, SelfCorrectionConfig config) {
    this.delegate = Objects.requireNonNull(delegate, "delegate cannot be null");
    this.config = Objects.requireNonNull(config, "config cannot be null");
  }

  /**
   * Wraps an interactable with the given self-correction configuration.
   *
   * @param agent the agent to wrap
   * @param config the self-correction configuration
   * @return a self-correcting interactable
   */
  public static @NonNull SelfCorrectingInteractable wrap(
      @NonNull Interactable agent, @NonNull SelfCorrectionConfig config) {
    return new SelfCorrectingInteractable(agent, config);
  }

  /**
   * Wraps an interactable with default self-correction configuration (3 retries, retry on error).
   *
   * @param agent the agent to wrap
   * @return a self-correcting interactable
   */
  public static @NonNull SelfCorrectingInteractable wrap(@NonNull Interactable agent) {
    return new SelfCorrectingInteractable(agent, SelfCorrectionConfig.builder().build());
  }

  @Override
  public @NonNull String name() {
    return delegate.name() + "[SelfCorrecting]";
  }

  @Override
  public @NonNull AgentResult interact(
      @NonNull AgenticContext context, @Nullable TraceMetadata trace) {
    AgentResult result = delegate.interact(context, trace);

    int attempt = 0;
    while (config.retryOn().test(result) && attempt < config.maxRetries()) {
      attempt++;
      String errorMessage = extractErrorMessage(result);
      String feedback = config.formatFeedback(errorMessage);

      // Inject error feedback as a user message so the agent can correct itself
      context.addMessage(Message.user(feedback));

      result = delegate.interact(context, trace);
    }

    return result;
  }

  @Override
  public @NonNull AgentStream interactStream(
      @NonNull AgenticContext context, @Nullable TraceMetadata trace) {
    // Self-correction is a blocking concept; streaming delegates directly to the wrapped agent
    return delegate.interactStream(context, trace);
  }

  // ===== Private Helpers =====

  private String extractErrorMessage(AgentResult result) {
    if (result.error() != null) {
      String msg = result.error().getMessage();
      return msg != null ? msg : result.error().getClass().getSimpleName();
    }
    // Try to get output as fallback error description
    if (result.output() != null && !result.output().isBlank()) {
      return result.output();
    }
    return "Unknown error (no error details available)";
  }
}
