package com.paragon.agents;

import com.paragon.responses.annotations.FunctionMetadata;
import com.paragon.responses.spec.FunctionTool;
import com.paragon.responses.spec.FunctionToolCallOutput;
import com.paragon.responses.spec.Message;
import com.paragon.telemetry.processors.TraceIdGenerator;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Wraps an Agent as a FunctionTool, enabling agent composition.
 *
 * <p>Unlike handoffs which transfer control permanently, sub-agents are invoked like tools: the
 * parent agent calls the sub-agent, receives its output, and continues processing.
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
 * @see Config
 * @since 1.0
 */
@FunctionMetadata(name = "", description = "")
public final class SubAgentTool extends FunctionTool<SubAgentTool.SubAgentParams> {

  /**
   * Parameters for the sub-agent invocation.
   *
   * @param request The message/request to send to the sub-agent
   */
  public record SubAgentParams(@NonNull String request) {}

  private final @NonNull Agent targetAgent;
  private final @NonNull String toolName;
  private final @NonNull String toolDescription;
  private final boolean shareState;
  private final boolean shareHistory;

  // ScopedValue for context propagation - virtual thread optimized
  private static final ScopedValue<AgentContext> CURRENT_CONTEXT = ScopedValue.newInstance();

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
   * @param config configuration for context sharing and description
   */
  public SubAgentTool(@NonNull Agent targetAgent, @NonNull Config config) {
    super(
        Map.of(
            "type",
            "object",
            "properties",
            Map.of(
                "request",
                Map.of(
                    "type",
                    "string",
                    "description",
                    "The message/request to send to the sub-agent")),
            "required",
            java.util.List.of("request"),
            "additionalProperties",
            false),
        true);

    this.targetAgent = Objects.requireNonNull(targetAgent, "targetAgent cannot be null");
    this.toolName = "invoke_" + toSnakeCase(targetAgent.name());
    this.toolDescription =
        config.description != null
            ? config.description
            : "Invoke " + targetAgent.name() + ": " + targetAgent.instructions().text();
    this.shareState = config.shareState;
    this.shareHistory = config.shareHistory;
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
  public @NonNull FunctionToolCallOutput call(@Nullable SubAgentParams params) {
    if (params == null || params.request() == null || params.request().isEmpty()) {
      return FunctionToolCallOutput.error("SubAgent request cannot be empty");
    }

    try {
      // Build child context based on configuration
      AgentContext childContext = buildChildContext(params.request());

      // Invoke the sub-agent (blocking - cheap in virtual threads)
      AgentResult result = targetAgent.interact(childContext);

      if (result.isError()) {
        return FunctionToolCallOutput.error(
            "SubAgent '" + targetAgent.name() + "' failed: " + result.error().getMessage());
      }

      return FunctionToolCallOutput.success(result.output());
    } catch (Exception e) {
      return FunctionToolCallOutput.error(
          "SubAgent '" + targetAgent.name() + "' error: " + e.getMessage());
    }
  }

  /**
   * Builds the child context based on configuration.
   *
   * @param request the request message to send
   * @return configured child context
   */
  private AgentContext buildChildContext(String request) {
    // Safely check if CURRENT_CONTEXT is bound (only set when called from Agent)
    AgentContext parentContext = CURRENT_CONTEXT.isBound() ? CURRENT_CONTEXT.get() : null;
    AgentContext childContext;

    if (parentContext != null && shareHistory) {
      // Fork full context including history
      String childSpanId = TraceIdGenerator.generateSpanId();
      childContext = parentContext.fork(childSpanId);
      childContext.addInput(Message.user(request));
    } else if (parentContext != null && shareState) {
      // Fresh history but copy state
      childContext = AgentContext.create();

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
      childContext = AgentContext.create();
      childContext.addInput(Message.user(request));
    }

    return childContext;
  }

  /**
   * Runs a task with the current context set for sub-agent execution. Package-private for Agent
   * use.
   *
   * <p>Uses ScopedValue for virtual-thread-safe context propagation.
   *
   * @param context the parent context to propagate
   * @param task the task to run with the context (returns void)
   */
  static void runWithContext(@Nullable AgentContext context, java.lang.Runnable task) {
    if (context != null) {
      ScopedValue.where(CURRENT_CONTEXT, context).run(task);
    } else {
      task.run();
    }
  }

  /**
   * Runs a task with the current context set and returns a result. Package-private for Agent use.
   *
   * <p>Uses ScopedValue for virtual-thread-safe context propagation.
   *
   * @param context the parent context to propagate
   * @param task the task to run with the context
   * @param <T> the return type
   * @return the result from the task
   */
  static <T> T callWithContext(@Nullable AgentContext context, ScopedValue.CallableOp<T, Exception> task) throws Exception {
    if (context != null) {
      return ScopedValue.where(CURRENT_CONTEXT, context).call(task);
    } else {
      return task.call();
    }
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
    return shareState;
  }

  /**
   * Returns whether history is shared with the sub-agent.
   *
   * @return true if history is shared
   */
  public boolean sharesHistory() {
    return shareHistory;
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

    /**
     * Creates a new Config builder.
     *
     * @return a new builder
     */
    public static @NonNull Builder builder() {
      return new Builder();
    }

    /** Returns the description. */
    public @Nullable String description() {
      return description;
    }

    /** Returns whether state is shared. */
    public boolean shareState() {
      return shareState;
    }

    /** Returns whether history is shared. */
    public boolean shareHistory() {
      return shareHistory;
    }

    /** Builder for Config. */
    public static final class Builder {
      private @Nullable String description;
      private boolean shareState = true; // Default: share state
      private boolean shareHistory = false; // Default: don't share history

      private Builder() {}

      /**
       * Sets the description for when to use this sub-agent.
       *
       * @param description the description
       * @return this builder
       */
      public @NonNull Builder description(@NonNull String description) {
        this.description = Objects.requireNonNull(description);
        return this;
      }

      /**
       * Sets whether to share custom state (userId, sessionId, etc.) with the sub-agent.
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
       * Sets whether to share conversation history with the sub-agent.
       *
       * <p>When true, the sub-agent receives the full conversation context via fork(). When false
       * (default), the sub-agent starts with a fresh conversation.
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
       *
       * @return the configuration
       */
      public @NonNull Config build() {
        return new Config(this);
      }
    }
  }
}
