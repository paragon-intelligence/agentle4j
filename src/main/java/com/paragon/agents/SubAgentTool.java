package com.paragon.agents;

import com.paragon.responses.annotations.FunctionMetadata;
import com.paragon.responses.spec.FunctionTool;
import com.paragon.responses.spec.FunctionToolCallOutput;
import com.paragon.responses.spec.Message;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Wraps an Agent as a FunctionTool, enabling agent composition.
 *
 * <p>Unlike handoffs which transfer control permanently, sub-agents are invoked like tools: the
 * parent agent calls the sub-agent, receives its output, and continues processing.
 *
 * <p>This is a thin wrapper around {@link InteractableSubAgentTool} that provides a typed
 * {@link #targetAgent()} accessor for backward compatibility.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * Agent dataAnalyst = Agent.builder()
 *     .name("DataAnalyst")
 *     .instructions("You analyze data and return insights.")
 *     .responder(responder)
 *     .build();
 *
 * Agent orchestrator = Agent.builder()
 *     .name("Orchestrator")
 *     .instructions("Use the data analyst when you need deep analysis.")
 *     .addSubAgent(dataAnalyst, "For data analysis and statistical insights")
 *     .responder(responder)
 *     .build();
 * }</pre>
 *
 * <h2>Context Sharing</h2>
 *
 * <p>By default, sub-agents inherit the parent's custom state (userId, sessionId, etc.) but start
 * with a fresh conversation history. Use {@link Config#shareHistory()} to include the full
 * conversation context.
 *
 * @see Agent.Builder#addSubAgent(Agent, String)
 * @see InteractableSubAgentTool
 * @see Config
 * @since 1.0
 */
@FunctionMetadata(name = "", description = "")
public final class SubAgentTool extends FunctionTool<SubAgentTool.SubAgentParams> {

  private final @NonNull Agent targetAgent;
  private final @NonNull InteractableSubAgentTool delegate;

  /**
   * Creates a SubAgentTool with default configuration.
   *
   * @param targetAgent the agent to invoke as a tool
   * @param description description of when to use this sub-agent
   */
  public SubAgentTool(@NonNull Agent targetAgent, @NonNull String description) {
    this(targetAgent, Config.builder().description(description).build());
  }

  /**
   * Creates a SubAgentTool with the specified configuration.
   *
   * @param targetAgent the agent to invoke as a tool
   * @param config      configuration for context sharing and description
   */
  public SubAgentTool(@NonNull Agent targetAgent, @NonNull Config config) {
    super(
            Map.of(
                    "type", "object",
                    "properties", Map.of(
                            "request", Map.of(
                                    "type", "string",
                                    "description", "The message/request to send to the sub-agent")),
                    "required", List.of("request"),
                    "additionalProperties", false),
            true);

    this.targetAgent = Objects.requireNonNull(targetAgent, "targetAgent cannot be null");
    this.delegate = new InteractableSubAgentTool(
            targetAgent,
            InteractableSubAgentTool.Config.builder()
                    .description(config.description())
                    .shareState(config.shareState())
                    .shareHistory(config.shareHistory())
                    .build());
  }

  @Override
  public @NonNull String getName() {
    return delegate.getName();
  }

  @Override
  public @Nullable String getDescription() {
    return delegate.getDescription();
  }

  @Override
  public @NonNull FunctionToolCallOutput call(@Nullable SubAgentParams params) {
    if (params == null || params.request() == null || params.request().isEmpty()) {
      return FunctionToolCallOutput.error("SubAgent request cannot be empty");
    }
    return delegate.call(new InteractableSubAgentTool.InteractableParams(params.request()));
  }

  /**
   * Returns the target agent.
   *
   * @return the wrapped agent
   */
  public @NonNull Agent targetAgent() {
    return targetAgent;
  }

  /**
   * Returns whether state is shared with the sub-agent.
   *
   * @return true if state is shared
   */
  public boolean sharesState() {
    return delegate.sharesState();
  }

  /**
   * Returns whether history is shared with the sub-agent.
   *
   * @return true if history is shared
   */
  public boolean sharesHistory() {
    return delegate.sharesHistory();
  }

  /**
   * Parameters for the sub-agent invocation.
   *
   * @param request The message/request to send to the sub-agent
   */
  public record SubAgentParams(@NonNull String request) {
  }

  /**
   * Configuration for sub-agent behavior.
   *
   * <p>Example:
   *
   * <pre>{@code
   * SubAgentTool.Config config = SubAgentTool.Config.builder()
   *     .description("For data analysis")
   *     .shareState(true)   // Default: true - share userId, etc.
   *     .shareHistory(true) // Default: false - include conversation history
   *     .build();
   * }</pre>
   */
  public static final class Config {
    private final @Nullable String description;
    private final boolean shareState;
    private final boolean shareHistory;

    private Config(Builder builder) {
      this.description = builder.description;
      this.shareState = builder.shareState;
      this.shareHistory = builder.shareHistory;
    }

    public static @NonNull Builder builder() {
      return new Builder();
    }

    public @Nullable String description() { return description; }
    public boolean shareState() { return shareState; }
    public boolean shareHistory() { return shareHistory; }

    public static final class Builder {
      private @Nullable String description;
      private boolean shareState = true;
      private boolean shareHistory = false;

      private Builder() {}

      public @NonNull Builder description(@NonNull String description) {
        this.description = Objects.requireNonNull(description);
        return this;
      }

      public @NonNull Builder shareState(boolean shareState) {
        this.shareState = shareState;
        return this;
      }

      public @NonNull Builder shareHistory(boolean shareHistory) {
        this.shareHistory = shareHistory;
        return this;
      }

      public @NonNull Config build() {
        return new Config(this);
      }
    }
  }
}
