package com.paragon.agents;

import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.responses.annotations.FunctionMetadata;
import com.paragon.responses.spec.FunctionTool;
import com.paragon.responses.spec.FunctionToolCallOutput;

/**
 * A handoff defines when and to which agent control should be transferred.
 *
 * <p>Handoffs enable multi-agent collaboration by allowing one agent to delegate to another when a
 * task falls outside its expertise. Handoffs are exposed to the model as tools - when the model
 * calls a handoff tool, control is automatically transferred to the target agent.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * Agent supportAgent = Agent.builder()
 *     .name("CustomerSupport")
 *     .instructions("Handle customer support issues")
 *     .build();
 *
 * Agent salesAgent = Agent.builder()
 *     .name("Sales")
 *     .instructions("Handle sales inquiries")
 *     .addHandoff(Handoff.to(supportAgent)
 *         .withDescription("Transfer to support for technical issues"))
 *     .build();
 *
 * // When salesAgent.interact() detects a handoff tool call,
 * // it automatically invokes supportAgent.interact()
 * }</pre>
 *
 * @see Agent
 * @since 1.0
 */
public final class Handoff {

  private final @NonNull String name;
  private final @NonNull String description;
  private final @NonNull Agent targetAgent;

  private Handoff(Builder builder) {
    this.name = Objects.requireNonNull(builder.name, "name cannot be null");
    this.description = Objects.requireNonNull(builder.description, "description cannot be null");
    this.targetAgent = Objects.requireNonNull(builder.targetAgent, "targetAgent cannot be null");
  }

  /**
   * Creates a handoff builder targeting the specified agent.
   *
   * <p>The handoff name defaults to "transfer_to_[agent_name]" and description defaults to the
   * agent's instructions.
   *
   * @param targetAgent the agent to transfer control to
   * @return a builder for configuring the handoff
   */
  public static @NonNull Builder to(@NonNull Agent targetAgent) {
    return new Builder(targetAgent);
  }

  /**
   * Returns the name of this handoff (used as the tool name).
   *
   * @return the handoff name
   */
  public @NonNull String name() {
    return name;
  }

  /**
   * Returns the description explaining when to use this handoff.
   *
   * @return the handoff description
   */
  public @NonNull String description() {
    return description;
  }

  /**
   * Returns the target agent that will receive control.
   *
   * @return the target agent
   */
  public @NonNull Agent targetAgent() {
    return targetAgent;
  }

  /**
   * Converts this handoff to a FunctionTool that can be passed to the LLM.
   *
   * @return a FunctionTool representing this handoff
   */
  public @NonNull FunctionTool<HandoffParams> asTool() {
    return new HandoffTool(name, description);
  }

  /**
   * Parameters for the handoff tool - just an optional message.
   */
  public record HandoffParams(@Nullable String message) {}

  /**
   * Internal tool implementation for handoffs.
   */
  @FunctionMetadata(name = "", description = "")
  private static final class HandoffTool extends FunctionTool<HandoffParams> {
    private final String toolName;
    private final String toolDescription;

    HandoffTool(String name, String description) {
      super(Map.of(
          "type", "object",
          "properties", Map.of(
              "message", Map.of(
                  "type", "string",
                  "description", "Optional message to pass to the next agent"
              )
          ),
          "required", java.util.List.of(),
          "additionalProperties", false
      ), true);
      this.toolName = name;
      this.toolDescription = description;
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
    public @Nullable FunctionToolCallOutput call(@Nullable HandoffParams params) {
      // The actual handoff is handled by the Agent's run loop
      // This is just a marker that a handoff was requested
      String message = params != null && params.message() != null ? params.message() : "";
      return FunctionToolCallOutput.success("Handoff initiated: " + message);
    }
  }

  /**
   * Builder for creating Handoff instances.
   */
  public static final class Builder {
    private final @NonNull Agent targetAgent;
    private @Nullable String name;
    private @Nullable String description;

    private Builder(@NonNull Agent targetAgent) {
      this.targetAgent = Objects.requireNonNull(targetAgent, "targetAgent cannot be null");
    }

    /**
     * Sets a custom name for the handoff tool.
     *
     * <p>Defaults to "transfer_to_[agent_name]".
     *
     * @param name the handoff name
     * @return this builder
     */
    public @NonNull Builder withName(@NonNull String name) {
      this.name = Objects.requireNonNull(name);
      return this;
    }

    /**
     * Sets a custom description explaining when to use this handoff.
     *
     * <p>Defaults to the target agent's instructions.
     *
     * @param description the handoff description
     * @return this builder
     */
    public @NonNull Builder withDescription(@NonNull String description) {
      this.description = Objects.requireNonNull(description);
      return this;
    }

    /**
     * Builds the Handoff instance.
     *
     * @return the configured handoff
     */
    public @NonNull Handoff build() {
      if (name == null) {
        name = "transfer_to_" + toSnakeCase(targetAgent.name());
      }
      if (description == null) {
        description = "Transfer to " + targetAgent.name() + ": " + targetAgent.instructions();
      }
      return new Handoff(this);
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
  }
}
