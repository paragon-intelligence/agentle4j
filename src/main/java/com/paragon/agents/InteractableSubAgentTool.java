package com.paragon.agents;

import com.paragon.responses.annotations.FunctionMetadata;
import com.paragon.responses.spec.FunctionTool;
import com.paragon.responses.spec.FunctionToolCallOutput;
import com.paragon.responses.spec.Message;
import com.paragon.telemetry.processors.TraceIdGenerator;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Wraps any Interactable as a FunctionTool, enabling composition of multi-agent patterns.
 *
 * <p>This extends the capabilities of {@link SubAgentTool} to support not just Agents,
 * but any Interactable implementation including RouterAgent, ParallelAgents, AgentNetwork,
 * and SupervisorAgent.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * // Create a router that handles different domains
 * RouterAgent router = RouterAgent.builder()
 *     .addRoute(billingAgent, "billing questions")
 *     .addRoute(techAgent, "technical support")
 *     .build();
 *
 * // Use the router as a tool in a supervisor
 * InteractableSubAgentTool tool = new InteractableSubAgentTool(router, "Route to specialists");
 * }</pre>
 *
 * @see SubAgentTool
 * @see Interactable
 * @since 1.0
 */
@FunctionMetadata(name = "", description = "")
public final class InteractableSubAgentTool extends FunctionTool<InteractableSubAgentTool.InteractableParams> {

  // ScopedValue for context propagation - virtual thread optimized
  private static final ScopedValue<AgenticContext> CURRENT_CONTEXT = ScopedValue.newInstance();
  private final @NonNull Interactable target;
  private final @NonNull String toolName;
  private final @NonNull String toolDescription;
  private final boolean shareState;
  private final boolean shareHistory;

  /**
   * Creates an InteractableSubAgentTool with a description.
   *
   * @param target      the interactable to invoke as a tool
   * @param description description of when to use this interactable
   */
  public InteractableSubAgentTool(@NonNull Interactable target, @NonNull String description) {
    this(target, Config.builder().description(description).build());
  }

  /**
   * Creates an InteractableSubAgentTool with the specified configuration.
   *
   * @param target the interactable to invoke as a tool
   * @param config configuration for context sharing and description
   */
  public InteractableSubAgentTool(@NonNull Interactable target, @NonNull Config config) {
    super(
            Map.of(
                    "type", "object",
                    "properties", Map.of(
                            "request", Map.of(
                                    "type", "string",
                                    "description", "The message/request to send to the interactable")),
                    "required", List.of("request"),
                    "additionalProperties", false),
            true);

    this.target = Objects.requireNonNull(target, "target cannot be null");
    this.toolName = "invoke_" + toSnakeCase(target.name());
    this.toolDescription = config.description != null
            ? config.description
            : "Invoke " + target.name();
    this.shareState = config.shareState;
    this.shareHistory = config.shareHistory;
  }

  /**
   * Runs a task with the current context set for sub-agent execution.
   *
   * <p>Uses ScopedValue for virtual-thread-safe context propagation.
   *
   * @param context the parent context to propagate
   * @param task    the task to run with the context
   */
  static void runWithContext(@Nullable AgenticContext context, Runnable task) {
    if (context != null) {
      ScopedValue.where(CURRENT_CONTEXT, context).run(task);
    } else {
      task.run();
    }
  }

  private static String toSnakeCase(String input) {
    if (input == null || input.isEmpty()) {
      return input;
    }
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      if (Character.isUpperCase(c)) {
        if (i > 0) {
          result.append('_');
        }
        result.append(Character.toLowerCase(c));
      } else if (Character.isWhitespace(c)) {
        result.append('_');
      } else {
        result.append(c);
      }
    }
    return result.toString();
  }

  @Override
  public @NonNull String getName() {
    return toolName;
  }

  @Override
  public @Nullable String getDescription() {
    return toolDescription;
  }

  @Override
  public @NonNull FunctionToolCallOutput call(@Nullable InteractableParams params) {
    if (params == null || params.request() == null || params.request().isEmpty()) {
      return FunctionToolCallOutput.error("Request cannot be empty");
    }

    try {
      // Build child context based on configuration
      AgenticContext childContext = buildChildContext(params.request());

      // Invoke the interactable (blocking - cheap in virtual threads)
      AgentResult result = target.interact(childContext);

      if (result.isError()) {
        return FunctionToolCallOutput.error(
                "'" + target.name() + "' failed: " + result.error().getMessage());
      }

      return FunctionToolCallOutput.success(result.output());
    } catch (Exception e) {
      return FunctionToolCallOutput.error(
              "'" + target.name() + "' error: " + e.getMessage());
    }
  }

  /**
   * Builds the child context based on configuration.
   *
   * @param request the request message to send
   * @return configured child context
   */
  private AgenticContext buildChildContext(String request) {
    // Safely check if CURRENT_CONTEXT is bound (only set when called from Agent)
    AgenticContext parentContext = CURRENT_CONTEXT.isBound() ? CURRENT_CONTEXT.get() : null;
    AgenticContext childContext;

    if (parentContext != null && shareHistory) {
      // Fork full context including history
      String childSpanId = TraceIdGenerator.generateSpanId();
      childContext = parentContext.fork(childSpanId);
      childContext.addInput(Message.user(request));
    } else if (parentContext != null && shareState) {
      // Fresh history but copy state
      childContext = AgenticContext.create();

      // Copy trace context for observability
      if (parentContext.hasTraceContext()) {
        String childSpanId = TraceIdGenerator.generateSpanId();
        childContext.withTraceContext(parentContext.parentTraceId().orElseThrow(), childSpanId);
      }
      parentContext.requestId().ifPresent(childContext::withRequestId);

      // Copy custom state
      for (Map.Entry<String, Object> entry : parentContext.getAllState().entrySet()) {
        childContext.setState(entry.getKey(), entry.getValue());
      }

      childContext.addInput(Message.user(request));
    } else {
      // Completely isolated
      childContext = AgenticContext.create();
      childContext.addInput(Message.user(request));
    }

    return childContext;
  }

  /**
   * Returns the target interactable.
   *
   * @return the wrapped interactable
   */
  public @NonNull Interactable target() {
    return target;
  }

  /**
   * Returns whether state is shared.
   *
   * @return true if state is shared
   */
  public boolean sharesState() {
    return shareState;
  }

  /**
   * Returns whether history is shared.
   *
   * @return true if history is shared
   */
  public boolean sharesHistory() {
    return shareHistory;
  }

  /**
   * Parameters for the interactable invocation.
   *
   * @param request The message/request to send to the interactable
   */
  public record InteractableParams(@NonNull String request) {
  }

  /**
   * Configuration for interactable sub-agent behavior.
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

    /**
     * Creates a new Config builder.
     */
    public static @NonNull Builder builder() {
      return new Builder();
    }

    /**
     * Returns the description.
     */
    public @Nullable String description() {
      return description;
    }

    /**
     * Returns whether state is shared.
     */
    public boolean shareState() {
      return shareState;
    }

    /**
     * Returns whether history is shared.
     */
    public boolean shareHistory() {
      return shareHistory;
    }

    /**
     * Builder for Config.
     */
    public static final class Builder {
      private @Nullable String description;
      private boolean shareState = true;
      private boolean shareHistory = false;

      private Builder() {
      }

      /**
       * Sets the description for when to use this interactable.
       *
       * @param description the description
       * @return this builder
       */
      public @NonNull Builder description(@NonNull String description) {
        this.description = Objects.requireNonNull(description);
        return this;
      }

      /**
       * Sets whether to share custom state with the interactable.
       *
       * <p>Default: true
       *
       * @param shareState true to share state
       * @return this builder
       */
      public @NonNull Builder shareState(boolean shareState) {
        this.shareState = shareState;
        return this;
      }

      /**
       * Sets whether to share conversation history.
       *
       * <p>Default: false
       *
       * @param shareHistory true to share history
       * @return this builder
       */
      public @NonNull Builder shareHistory(boolean shareHistory) {
        this.shareHistory = shareHistory;
        return this;
      }

      /**
       * Builds the Config.
       */
      public @NonNull Config build() {
        return new Config(this);
      }
    }
  }
}
