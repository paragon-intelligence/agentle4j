package com.paragon.skills;

import com.paragon.agents.Agent;
import com.paragon.agents.AgentContext;
import com.paragon.agents.AgentResult;
import com.paragon.prompts.Prompt;
import com.paragon.responses.Responder;
import com.paragon.responses.annotations.FunctionMetadata;
import com.paragon.responses.spec.FunctionTool;
import com.paragon.responses.spec.FunctionToolCallOutput;
import com.paragon.responses.spec.Message;
import com.paragon.telemetry.processors.TraceIdGenerator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Wraps a Skill as a FunctionTool for on-demand invocation.
 *
 * <p>When the LLM calls this tool, the skill's instructions and tools are loaded,
 * a sub-agent is created, and the request is processed. This follows the progressive
 * disclosure pattern where skill content is only loaded when actually needed.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * Skill pdfSkill = Skill.builder()
 *     .name("pdf-processor")
 *     .description("Process PDF files")
 *     .instructions("You are a PDF expert...")
 *     .build();
 *
 * SkillTool tool = new SkillTool(pdfSkill, responder, "gpt-4o");
 *
 * // Add to agent
 * Agent agent = Agent.builder()
 *     .name("Assistant")
 *     .addTool(tool)
 *     .build();
 * }</pre>
 *
 * <h2>Context Sharing</h2>
 *
 * <p>By default, skills execute in isolation for security. Use {@link Config} to enable
 * context sharing for trusted skills:
 *
 * <pre>{@code
 * SkillTool tool = new SkillTool(skill, responder, model, Config.builder()
 *     .shareState(true)   // Share userId, sessionId, etc.
 *     .shareHistory(true) // Include conversation history
 *     .build());
 * }</pre>
 *
 * @see Skill
 * @see Config
 * @since 1.0
 */
@FunctionMetadata(name = "", description = "")
public final class SkillTool extends FunctionTool<SkillTool.SkillParams> {

  /**
   * Parameters for skill invocation.
   *
   * @param request The message/request to send to the skill
   */
  public record SkillParams(@NonNull String request) {}

  private final @NonNull Skill skill;
  private final @NonNull Responder responder;
  private final @NonNull String model;
  private final @NonNull Config config;
  private final @NonNull String toolName;

  // ScopedValue for context propagation - virtual thread optimized
  private static final ScopedValue<AgentContext> CURRENT_CONTEXT = ScopedValue.newInstance();

  /**
   * Creates a SkillTool with default configuration.
   *
   * @param skill the skill to wrap
   * @param responder the responder for sub-agent API calls
   * @param model the model to use for the sub-agent
   */
  public SkillTool(@NonNull Skill skill, @NonNull Responder responder, @NonNull String model) {
    this(skill, responder, model, Config.defaults());
  }

  /**
   * Creates a SkillTool with custom configuration.
   *
   * @param skill the skill to wrap
   * @param responder the responder for sub-agent API calls
   * @param model the model to use for the sub-agent
   * @param config configuration for context sharing
   */
  public SkillTool(
      @NonNull Skill skill,
      @NonNull Responder responder,
      @NonNull String model,
      @NonNull Config config) {
    super(
        Map.of(
            "type", "object",
            "properties", Map.of(
                "request", Map.of(
                    "type", "string",
                    "description", "The request to send to the skill")),
            "required", List.of("request"),
            "additionalProperties", false),
        true);

    this.skill = Objects.requireNonNull(skill, "skill cannot be null");
    this.responder = Objects.requireNonNull(responder, "responder cannot be null");
    this.model = Objects.requireNonNull(model, "model cannot be null");
    this.config = Objects.requireNonNull(config, "config cannot be null");
    this.toolName = "skill_" + skill.name().replace("-", "_");
  }

  @Override
  public @NonNull String getName() {
    return toolName;
  }

  @Override
  public @Nullable String getDescription() {
    return skill.description();
  }

  @Override
  public @NonNull FunctionToolCallOutput call(@Nullable SkillParams params) {
    if (params == null || params.request() == null || params.request().isEmpty()) {
      return FunctionToolCallOutput.error("Skill request cannot be empty");
    }

    try {
      // Build child context based on configuration
      AgentContext childContext = buildChildContext(params.request());

      // Build skill instructions with resources if available
      Prompt skillInstructions = buildSkillInstructions();

      // Create sub-agent with skill instructions and tools
      Agent.Builder subAgentBuilder = Agent.builder()
          .name(skill.name() + "-skill-agent")
          .instructions(skillInstructions)
          .model(model)
          .responder(responder)
          .maxTurns(config.maxTurns);

      // Add skill's bundled tools
      for (FunctionTool<?> tool : skill.tools()) {
        subAgentBuilder.addTool(tool);
      }

      Agent subAgent = subAgentBuilder.build();

      // Invoke the sub-agent (blocking - cheap in virtual threads)
      AgentResult result = subAgent.interact(childContext);

      if (result.isError()) {
        return FunctionToolCallOutput.error(
            "Skill '" + skill.name() + "' failed: " + result.error().getMessage());
      }

      return FunctionToolCallOutput.success(result.output());
    } catch (Exception e) {
      return FunctionToolCallOutput.error(
          "Skill '" + skill.name() + "' error: " + e.getMessage());
    }
  }

  /**
   * Builds the complete skill instructions, including any resources.
   *
   * @return the compiled instructions
   */
  private Prompt buildSkillInstructions() {
    if (!skill.hasResources()) {
      return skill.instructions();
    }

    // Append resources to instructions
    StringBuilder sb = new StringBuilder();
    sb.append(skill.instructions().text());

    for (Map.Entry<String, Prompt> resource : skill.resources().entrySet()) {
      sb.append("\n\n## ").append(resource.getKey()).append("\n\n");
      sb.append(resource.getValue().text());
    }

    return Prompt.of(sb.toString());
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

    if (parentContext != null && config.shareHistory) {
      // Fork full context including history
      String childSpanId = TraceIdGenerator.generateSpanId();
      childContext = parentContext.fork(childSpanId);
      childContext.addInput(Message.user(request));
    } else if (parentContext != null && config.shareState) {
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
      // Completely isolated (default for safety)
      childContext = AgentContext.create();
      childContext.addInput(Message.user(request));
    }

    return childContext;
  }

  /**
   * Runs a task with the current context set for skill execution.
   *
   * <p>Uses ScopedValue for virtual-thread-safe context propagation.
   *
   * @param context the parent context to propagate
   * @param task the task to run with the context
   */
  static void runWithContext(@Nullable AgentContext context, Runnable task) {
    if (context != null) {
      ScopedValue.where(CURRENT_CONTEXT, context).run(task);
    } else {
      task.run();
    }
  }

  /**
   * Returns the wrapped skill.
   *
   * @return the skill
   */
  public @NonNull Skill skill() {
    return skill;
  }

  /**
   * Returns whether state is shared with the skill.
   *
   * @return true if state is shared
   */
  public boolean sharesState() {
    return config.shareState;
  }

  /**
   * Returns whether history is shared with the skill.
   *
   * @return true if history is shared
   */
  public boolean sharesHistory() {
    return config.shareHistory;
  }

  /**
   * Configuration for skill execution behavior.
   *
   * <p>Example:
   *
   * <pre>{@code
   * SkillTool.Config config = SkillTool.Config.builder()
   *     .shareState(true)   // Share userId, sessionId, etc.
   *     .shareHistory(true) // Include conversation history
   *     .maxTurns(5)        // Limit skill execution turns
   *     .build();
   * }</pre>
   */
  public static final class Config {
    private final boolean shareState;
    private final boolean shareHistory;
    private final int maxTurns;

    private Config(Builder builder) {
      this.shareState = builder.shareState;
      this.shareHistory = builder.shareHistory;
      this.maxTurns = builder.maxTurns;
    }

    /**
     * Returns the default configuration (isolated execution).
     *
     * @return default config
     */
    public static @NonNull Config defaults() {
      return new Builder().build();
    }

    /**
     * Creates a new Config builder.
     *
     * @return a new builder
     */
    public static @NonNull Builder builder() {
      return new Builder();
    }

    /** Returns whether state is shared. */
    public boolean shareState() {
      return shareState;
    }

    /** Returns whether history is shared. */
    public boolean shareHistory() {
      return shareHistory;
    }

    /** Returns the max turns for skill execution. */
    public int maxTurns() {
      return maxTurns;
    }

    /** Builder for Config. */
    public static final class Builder {
      private boolean shareState = false;   // Default: isolated
      private boolean shareHistory = false; // Default: isolated
      private int maxTurns = 5;             // Default: 5 turns for skills

      private Builder() {}

      /**
       * Sets whether to share custom state with the skill.
       *
       * <p>When true, the skill can access userId, sessionId, and other
       * state from the parent context.
       *
       * <p>Default: false (isolated)
       *
       * @param shareState true to share state
       * @return this builder
       */
      public @NonNull Builder shareState(boolean shareState) {
        this.shareState = shareState;
        return this;
      }

      /**
       * Sets whether to share conversation history with the skill.
       *
       * <p>When true, the skill receives the full conversation context.
       * When false (default), the skill starts with a fresh conversation.
       *
       * <p>Default: false (isolated)
       *
       * @param shareHistory true to share history
       * @return this builder
       */
      public @NonNull Builder shareHistory(boolean shareHistory) {
        this.shareHistory = shareHistory;
        return this;
      }

      /**
       * Sets the maximum number of turns for skill execution.
       *
       * <p>Default: 5
       *
       * @param maxTurns maximum turns
       * @return this builder
       */
      public @NonNull Builder maxTurns(int maxTurns) {
        if (maxTurns <= 0) {
          throw new IllegalArgumentException("maxTurns must be positive");
        }
        this.maxTurns = maxTurns;
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
