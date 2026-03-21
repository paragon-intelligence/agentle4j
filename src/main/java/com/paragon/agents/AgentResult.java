package com.paragon.agents;

import com.paragon.responses.json.StructuredOutputDefinition;
import com.paragon.responses.spec.FunctionToolCall;
import com.paragon.responses.spec.FunctionToolCallOutput;
import com.paragon.responses.spec.Message;
import com.paragon.responses.spec.MessageRole;
import com.paragon.responses.spec.Response;
import com.paragon.responses.spec.ResponseInputItem;
import com.paragon.responses.spec.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

/**
 * The result of an agent interaction, containing the final output and execution metadata.
 *
 * <p>AgentResult captures everything that happened during an agent run:
 *
 * <ul>
 *   <li>The final text output
 *   <li>The last API response from the LLM
 *   <li>Complete conversation history
 *   <li>All tool executions that occurred
 *   <li>Number of LLM turns used
 *   <li>Any handoff that was triggered
 *   <li>Any error that occurred
 *   <li>Related results from parallel/composite execution
 * </ul>
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * AgentResult result = agent.interact("What's the status of order #12345?");
 *
 * if (result.isSuccess()) {
 *     System.out.println(result.output());
 *     System.out.println("Messages returned: " + result.messages().size());
 * } else if (result.isHandoff()) {
 *     // Handoff was auto-executed, result contains final output
 *     System.out.println("Handled by: " + result.handoffAgent().name());
 *     System.out.println(result.output());
 * } else {
 *     System.err.println("Error: " + result.error().getMessage());
 * }
 * }</pre>
 *
 * @see Agent
 * @since 1.0
 */
public class AgentResult {

  private final @Nullable String output;
  private final @Nullable Response finalResponse;
  private final @NonNull List<ResponseInputItem> history;
  private final @NonNull List<ToolExecution> toolExecutions;
  private final int turnsUsed;
  private final @Nullable Agent handoffAgent;
  private final @Nullable Throwable error;
  private final @Nullable Object parsed;
  private final @Nullable AgentRunState pausedState;
  private final @NonNull List<AgentResult> relatedResults;
  private final @Nullable FunctionToolCall clientSideToolCall;
  private final @NonNull OutputOrigin outputOrigin;
  private final @Nullable String outputProducerName;
  private final @NonNull List<String> delegationPath;

  protected AgentResult(Builder builder) {
    this.output = builder.output;
    this.finalResponse = builder.finalResponse;
    this.history = builder.history != null ? List.copyOf(builder.history) : List.of();
    this.toolExecutions =
        builder.toolExecutions != null ? List.copyOf(builder.toolExecutions) : List.of();
    this.turnsUsed = builder.turnsUsed;
    this.handoffAgent = builder.handoffAgent;
    this.error = builder.error;
    this.parsed = builder.parsed;
    this.pausedState = builder.pausedState;
    this.relatedResults =
        builder.relatedResults != null ? List.copyOf(builder.relatedResults) : List.of();
    this.clientSideToolCall = builder.clientSideToolCall;
    this.outputOrigin = builder.outputOrigin != null ? builder.outputOrigin : OutputOrigin.LOCAL;
    this.outputProducerName = builder.outputProducerName;
    this.delegationPath =
        builder.delegationPath != null ? List.copyOf(builder.delegationPath) : List.of();
  }

  // ===== Factory Methods =====

  /**
   * Creates a successful result.
   *
   * @param output the final text output
   * @param response the final API response
   * @param context the agent context with history
   * @param toolExecutions all tool executions
   * @param turnsUsed number of LLM turns
   * @return a success result
   */
  public static @NonNull AgentResult success(
      @NonNull String output,
      @Nullable Response response,
      @NonNull AgenticContext context,
      @NonNull List<ToolExecution> toolExecutions,
      int turnsUsed) {
    return success(output, response, context, toolExecutions, turnsUsed, null);
  }

  public static @NonNull AgentResult success(
      @NonNull String output,
      @Nullable Response response,
      @NonNull AgenticContext context,
      @NonNull List<ToolExecution> toolExecutions,
      int turnsUsed,
      @Nullable String outputProducerName) {
    Builder builder =
        new Builder()
            .output(output)
            .finalResponse(response)
            .history(context.getHistory())
            .toolExecutions(toolExecutions)
            .turnsUsed(turnsUsed)
            .outputOrigin(OutputOrigin.LOCAL);
    if (outputProducerName != null) {
      builder.outputProducerName(outputProducerName).delegationPath(List.of(outputProducerName));
    }
    return builder.build();
  }

  /**
   * Convenience method for creating a simple successful result (for testing).
   *
   * @param output the final text output
   * @return a minimal success result
   */
  public static @NonNull AgentResult success(@NonNull String output) {
    return new Builder().output(output).outputOrigin(OutputOrigin.LOCAL).build();
  }

  /**
   * Creates a successful result with parsed structured output.
   *
   * @param output the final text output (JSON)
   * @param parsed the parsed structured object
   * @param response the final API response
   * @param context the agent context with history
   * @param toolExecutions all tool executions
   * @param turnsUsed number of LLM turns
   * @param <T> the parsed type
   * @return a success result with parsed output
   */
  public static <T> @NonNull AgentResult successWithParsed(
      @NonNull String output,
      @NonNull T parsed,
      @NonNull Response response,
      @NonNull AgenticContext context,
      @NonNull List<ToolExecution> toolExecutions,
      int turnsUsed) {
    return successWithParsed(output, parsed, response, context, toolExecutions, turnsUsed, null);
  }

  public static <T> @NonNull AgentResult successWithParsed(
      @NonNull String output,
      @NonNull T parsed,
      @NonNull Response response,
      @NonNull AgenticContext context,
      @NonNull List<ToolExecution> toolExecutions,
      int turnsUsed,
      @Nullable String outputProducerName) {
    Builder builder =
        new Builder()
            .output(output)
            .parsed(parsed)
            .finalResponse(response)
            .history(context.getHistory())
            .toolExecutions(toolExecutions)
            .turnsUsed(turnsUsed)
            .outputOrigin(OutputOrigin.LOCAL);
    if (outputProducerName != null) {
      builder.outputProducerName(outputProducerName).delegationPath(List.of(outputProducerName));
    }
    return builder.build();
  }

  /**
   * Creates a handoff result (after auto-executing the target agent).
   *
   * @param handoffAgent the agent that was handed off to
   * @param innerResult the result from the handoff agent
   * @param context the original context with combined history
   * @return a handoff result
   */
  public static @NonNull AgentResult handoff(
      @NonNull String delegatorName,
      @NonNull Handoff handoff,
      @NonNull AgentResult innerResult,
      @NonNull AgenticContext context) {
    Builder builder =
        Builder.from(innerResult)
            .history(context.getHistory())
            .handoffAgent(handoff.targetAgent())
            .outputOrigin(OutputOrigin.DELEGATED)
            .delegationPath(prependDelegationPath(delegatorName, innerResult.delegationPath()));

    String producerName = innerResult.outputProducerName();
    if (producerName == null || producerName.isBlank()) {
      producerName = handoff.targetAgent().name();
    }
    builder.outputProducerName(producerName);

    return builder.build();
  }

  public static @NonNull AgentResult delegated(
      @NonNull String delegatorName,
      @NonNull String fallbackProducerName,
      @NonNull AgentResult innerResult) {
    Builder builder =
        Builder.from(innerResult)
            .outputOrigin(OutputOrigin.DELEGATED)
            .delegationPath(prependDelegationPath(delegatorName, innerResult.delegationPath()));

    String producerName = innerResult.outputProducerName();
    if (producerName == null || producerName.isBlank()) {
      producerName = fallbackProducerName;
    }
    builder.outputProducerName(producerName);

    return builder.build();
  }

  /**
   * Creates an error result.
   *
   * @param error the error that occurred
   * @param context the agent context at time of error
   * @param turnsUsed number of LLM turns before error
   * @return an error result
   */
  public static @NonNull AgentResult error(
      @NonNull Throwable error, @NonNull AgenticContext context, int turnsUsed) {
    return new Builder()
        .error(error)
        .history(context.getHistory())
        .turnsUsed(turnsUsed)
        .outputOrigin(OutputOrigin.LOCAL)
        .build();
  }

  /**
   * Convenience method for creating a simple error result (for testing).
   *
   * @param error the error that occurred
   * @return a minimal error result
   */
  public static @NonNull AgentResult error(@NonNull Throwable error) {
    return new Builder().error(error).outputOrigin(OutputOrigin.LOCAL).build();
  }

  /**
   * Creates an error result from a guardrail failure.
   *
   * @param reason the guardrail failure reason
   * @param context the agent context
   * @return an error result
   */
  public static @NonNull AgentResult guardrailFailed(
      @NonNull String reason, @NonNull AgenticContext context) {
    return new Builder()
        .error(new GuardrailException(reason))
        .history(context.getHistory())
        .turnsUsed(0)
        .outputOrigin(OutputOrigin.LOCAL)
        .build();
  }

  /**
   * Creates a paused result for human-in-the-loop tool approval.
   *
   * <p>The run can be resumed later with {@code Agent.resume(state)}.
   *
   * @param state the serializable run state
   * @param context the agent context
   * @return a paused result
   */
  public static @NonNull AgentResult paused(
      @NonNull AgentRunState state, @NonNull AgenticContext context) {
    return new Builder()
        .pausedState(state)
        .history(context.getHistory())
        .turnsUsed(context.getTurnCount())
        .outputOrigin(OutputOrigin.LOCAL)
        .build();
  }

  /**
   * Creates a client-side tool result when a {@code stopsLoop = true} tool is called.
   *
   * <p>The call is NOT persisted to history and the tool's {@code call()} method is NOT invoked.
   * This is a clean, non-error exit from the agentic loop.
   *
   * @param call the function tool call that triggered the exit
   * @param context the agent context at time of exit
   * @param turnsUsed number of LLM turns used
   * @return a client-side tool result
   */
  public static @NonNull AgentResult clientSideTool(
      @NonNull FunctionToolCall call, @NonNull AgenticContext context, int turnsUsed) {
    return new Builder()
        .clientSideToolCall(call)
        .history(context.getHistory())
        .turnsUsed(turnsUsed)
        .outputOrigin(OutputOrigin.LOCAL)
        .build();
  }

  /**
   * Creates a composite result containing a primary result and related results.
   *
   * <p>This is useful for patterns like ParallelAgents where multiple agents run concurrently and
   * one result is selected as primary while the others are preserved as related results.
   *
   * @param primary the primary result (e.g., first to complete)
   * @param related the other related results
   * @return a result containing both primary and related results
   */
  public static @NonNull AgentResult composite(
      @NonNull AgentResult primary, @NonNull List<AgentResult> related) {
    return Builder.from(primary).relatedResults(related).build();
  }

  // ===== Accessors =====

  /**
   * Returns the final text output from the agent.
   *
   * @return the output text, or null if the run failed before producing output
   */
  public @Nullable String output() {
    return output;
  }

  /**
   * Returns whether this result has output text available.
   *
   * @return true if output text is present
   */
  public boolean hasOutput() {
    return output != null;
  }

  /**
   * Returns the output text, or an empty string when unavailable.
   *
   * @return the output text, or an empty string
   */
  public @NonNull String outputOrEmpty() {
    return outputOr("");
  }

  /**
   * Returns the output text, or the provided fallback when unavailable.
   *
   * @param fallback the fallback text to use when no output is available
   * @return the output text, or the fallback
   */
  public @NonNull String outputOr(@NonNull String fallback) {
    return output != null ? output : Objects.requireNonNull(fallback, "fallback cannot be null");
  }

  /**
   * Returns the final API response.
   *
   * @return the last Response from the LLM, or null if not available
   */
  public @Nullable Response finalResponse() {
    return finalResponse;
  }

  /**
   * Returns whether a final API response is available.
   *
   * @return true if a final response is present
   */
  public boolean hasFinalResponse() {
    return finalResponse != null;
  }

  /**
   * Returns the complete conversation history.
   *
   * @return an unmodifiable list of all messages
   */
  public @NonNull List<ResponseInputItem> history() {
    return history;
  }

  /**
   * Returns all history items assignable to the requested type.
   *
   * @param type the desired item type
   * @param <T> the desired item type
   * @return an immutable list of matching items in original order
   */
  public <T> @NonNull List<T> historyItems(@NonNull Class<T> type) {
    Objects.requireNonNull(type, "type cannot be null");
    return history.stream().filter(type::isInstance).map(type::cast).toList();
  }

  /**
   * Returns all messages present in the result history.
   *
   * @return an immutable list of messages in original order
   */
  public @NonNull List<Message> messages() {
    return historyItems(Message.class);
  }

  /**
   * Returns all messages with the requested role.
   *
   * @param role the role to filter by
   * @return an immutable list of matching messages in original order
   */
  public @NonNull List<Message> messages(@NonNull MessageRole role) {
    Objects.requireNonNull(role, "role cannot be null");
    return messages().stream().filter(message -> message.role() == role).toList();
  }

  /**
   * Returns all user messages present in the result history.
   *
   * @return an immutable list of user messages
   */
  public @NonNull List<Message> userMessages() {
    return messages(MessageRole.USER);
  }

  /**
   * Returns all assistant messages present in the result history.
   *
   * @return an immutable list of assistant messages
   */
  public @NonNull List<Message> assistantMessages() {
    return messages(MessageRole.ASSISTANT);
  }

  /**
   * Returns all developer messages present in the result history.
   *
   * @return an immutable list of developer messages
   */
  public @NonNull List<Message> developerMessages() {
    return messages(MessageRole.DEVELOPER);
  }

  /**
   * Returns all function tool calls present in the result history.
   *
   * @return an immutable list of function tool calls
   */
  public @NonNull List<FunctionToolCall> toolCalls() {
    return historyItems(FunctionToolCall.class);
  }

  /**
   * Returns all function tool outputs present in the result history.
   *
   * @return an immutable list of function tool outputs
   */
  public @NonNull List<FunctionToolCallOutput> toolOutputs() {
    return historyItems(FunctionToolCallOutput.class);
  }

  /**
   * Returns the most recent message from the result history.
   *
   * @return an Optional containing the last message, or empty if none exist
   */
  public @NonNull Optional<Message> lastMessage() {
    return lastMessageMatching(null);
  }

  /**
   * Returns the most recent message with the requested role.
   *
   * @param role the role to filter by
   * @return an Optional containing the last matching message, or empty if none exist
   */
  public @NonNull Optional<Message> lastMessage(@NonNull MessageRole role) {
    Objects.requireNonNull(role, "role cannot be null");
    return lastMessageMatching(role);
  }

  /**
   * Returns the most recent user message from the result history.
   *
   * @return an Optional containing the last user message, or empty if none exist
   */
  public @NonNull Optional<Message> lastUserMessage() {
    return lastMessage(MessageRole.USER);
  }

  /**
   * Returns the most recent assistant message from the result history.
   *
   * @return an Optional containing the last assistant message, or empty if none exist
   */
  public @NonNull Optional<Message> lastAssistantMessage() {
    return lastMessage(MessageRole.ASSISTANT);
  }

  /**
   * Returns the most recent developer message from the result history.
   *
   * @return an Optional containing the last developer message, or empty if none exist
   */
  public @NonNull Optional<Message> lastDeveloperMessage() {
    return lastMessage(MessageRole.DEVELOPER);
  }

  /**
   * Returns the first text segment from the most recent user message in the result history.
   *
   * @return an Optional containing the last user text, or empty if none found
   */
  public @NonNull Optional<String> lastUserMessageText() {
    return lastUserMessage().flatMap(AgentResult::firstTextContent);
  }

  /**
   * Returns the first text segment from the most recent user message, or a fallback value.
   *
   * @param fallback the fallback value when no user message text is found
   * @return the last user message text, or the fallback
   */
  public @NonNull String lastUserMessageText(@NonNull String fallback) {
    return lastUserMessageText()
        .orElse(Objects.requireNonNull(fallback, "fallback cannot be null"));
  }

  /**
   * Returns all tool executions that occurred during the run.
   *
   * @return an unmodifiable list of tool executions
   */
  public @NonNull List<ToolExecution> toolExecutions() {
    return toolExecutions;
  }

  /**
   * Returns whether any tool executions occurred during the run.
   *
   * @return true if tool executions are present
   */
  public boolean hasToolExecutions() {
    return !toolExecutions.isEmpty();
  }

  /**
   * Returns all tool executions for the requested tool name.
   *
   * @param toolName the tool name to filter by
   * @return an immutable list of matching tool executions
   */
  public @NonNull List<ToolExecution> toolExecutions(@NonNull String toolName) {
    Objects.requireNonNull(toolName, "toolName cannot be null");
    return toolExecutions.stream().filter(exec -> exec.toolName().equals(toolName)).toList();
  }

  /**
   * Returns the most recent tool execution, if any.
   *
   * @return an Optional containing the last tool execution, or empty if none occurred
   */
  public @NonNull Optional<ToolExecution> lastToolExecution() {
    if (toolExecutions.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(toolExecutions.get(toolExecutions.size() - 1));
  }

  /**
   * Returns the successful tool executions from this run.
   *
   * @return an immutable list of successful tool executions
   */
  public @NonNull List<ToolExecution> successfulToolExecutions() {
    return toolExecutions.stream().filter(ToolExecution::isSuccess).toList();
  }

  /**
   * Returns the failed or incomplete tool executions from this run.
   *
   * @return an immutable list of failed tool executions
   */
  public @NonNull List<ToolExecution> failedToolExecutions() {
    return toolExecutions.stream().filter(exec -> !exec.isSuccess()).toList();
  }

  /**
   * Returns the number of LLM turns used.
   *
   * @return the turn count
   */
  public int turnsUsed() {
    return turnsUsed;
  }

  /**
   * Returns the agent that was handed off to, if a handoff occurred.
   *
   * @return the handoff target agent, or null if no handoff
   */
  public @Nullable Agent handoffAgent() {
    return handoffAgent;
  }

  /**
   * Returns the error if the run failed.
   *
   * @return the error, or null if successful
   */
  public @Nullable Throwable error() {
    return error;
  }

  /**
   * Returns the error message if one occurred.
   *
   * @return the error message, or null if no error is present
   */
  public @Nullable String errorMessage() {
    return error != null ? error.getMessage() : null;
  }

  /**
   * Returns the parsed structured output if applicable.
   *
   * @param <T> the expected type
   * @return the parsed object, or null if not a structured output run
   */
  @SuppressWarnings("unchecked")
  public <T> @Nullable T parsed() {
    return (T) parsed;
  }

  /**
   * Returns the parsed structured output, if available.
   *
   * @return an Optional containing the parsed output
   */
  public @NonNull Optional<?> parsedOptional() {
    return Optional.ofNullable(parsed);
  }

  /**
   * Returns the parsed structured output when it matches the requested type.
   *
   * @param type the requested parsed output type
   * @param <T> the requested parsed output type
   * @return an Optional containing the typed parsed output, or empty if unavailable/incompatible
   */
  public <T> @NonNull Optional<T> parsedOptional(@NonNull Class<T> type) {
    Objects.requireNonNull(type, "type cannot be null");
    return type.isInstance(parsed) ? Optional.of(type.cast(parsed)) : Optional.empty();
  }

  // ===== Status Checks =====

  /**
   * Checks if this is a successful result.
   *
   * @return true if no error and no handoff occurred
   */
  public boolean isSuccess() {
    return error == null;
  }

  /**
   * Checks if a handoff occurred (to another agent).
   *
   * @return true if control was transferred to another agent
   */
  public boolean isHandoff() {
    return handoffAgent != null;
  }

  /**
   * Checks if an error occurred.
   *
   * @return true if an error occurred
   */
  public boolean isError() {
    return error != null;
  }

  /**
   * Checks if this result contains parsed structured output.
   *
   * @return true if parsed output is available
   */
  public boolean hasParsed() {
    return parsed != null;
  }

  /**
   * Checks if this run is paused waiting for approval.
   *
   * @return true if paused
   */
  public boolean isPaused() {
    return pausedState != null;
  }

  /**
   * Returns the paused state if this run is paused.
   *
   * @return the paused state, or null if not paused
   */
  public @Nullable AgentRunState pausedState() {
    return pausedState;
  }

  /**
   * Checks if the loop was stopped by a client-side tool ({@code stopsLoop = true}).
   *
   * @return true if a stopsLoop tool triggered the exit
   */
  public boolean isClientSideTool() {
    return clientSideToolCall != null;
  }

  /**
   * Returns the tool call that stopped the loop, if applicable.
   *
   * @return the client-side tool call, or null if not a client-side tool exit
   */
  public @Nullable FunctionToolCall clientSideToolCall() {
    return clientSideToolCall;
  }

  /**
   * Returns whether the output was produced locally or via delegation.
   *
   * @return the output origin
   */
  public @NonNull OutputOrigin outputOrigin() {
    return outputOrigin;
  }

  /**
   * Returns the name of the interactable that produced the final output, when known.
   *
   * @return the producer name, or null if unavailable
   */
  public @Nullable String outputProducerName() {
    return outputProducerName;
  }

  /**
   * Returns the delegation chain that led to the final output.
   *
   * @return an immutable delegation path
   */
  public @NonNull List<String> delegationPath() {
    return delegationPath;
  }

  /**
   * Returns related results from parallel or composite execution.
   *
   * <p>When using patterns like parallel execution, this contains the results from other agents
   * that ran alongside the primary result.
   *
   * @return an unmodifiable list of related results (empty if none)
   */
  public @NonNull List<AgentResult> relatedResults() {
    return relatedResults;
  }

  /**
   * Checks if this result has related results from parallel execution.
   *
   * @return true if related results are present
   */
  public boolean hasRelatedResults() {
    return !relatedResults.isEmpty();
  }

  // ===== Conversion =====

  /**
   * Parses this result's output text into a typed {@link StructuredAgentResult}.
   *
   * <p>If this result is an error, the error is propagated. Otherwise the output JSON is
   * deserialized to the given type.
   *
   * @param outputType the target class to deserialize into
   * @param objectMapper the Jackson mapper for JSON parsing
   * @param <T> the output type
   * @return a structured result with parsed output, or an error result if parsing fails
   */
  public <T> @NonNull StructuredAgentResult<T> toStructured(
      @NonNull Class<T> outputType, @NonNull ObjectMapper objectMapper) {
    return toStructured(StructuredOutputDefinition.create(outputType), objectMapper);
  }

  public <T> @NonNull StructuredAgentResult<T> toStructured(
      @NonNull TypeReference<T> outputType, @NonNull ObjectMapper objectMapper) {
    return toStructured(StructuredOutputDefinition.create(outputType), objectMapper);
  }

  public <T> @NonNull StructuredAgentResult<T> toStructured(
      @NonNull JavaType outputType, @NonNull ObjectMapper objectMapper) {
    return toStructured(StructuredOutputDefinition.create(outputType), objectMapper);
  }

  public <T> @NonNull StructuredAgentResult<T> toStructured(
      @NonNull StructuredOutputDefinition<T> structuredOutputDefinition,
      @NonNull ObjectMapper objectMapper) {
    if (isError()) {
      return StructuredAgentResult.error(this.error, this);
    }

    if (StructuredOutputSupport.isCompatible(structuredOutputDefinition, parsed)) {
      @SuppressWarnings("unchecked")
      T cast = (T) parsed;
      return StructuredAgentResult.success(cast, this);
    }

    try {
      T parsedValue =
          StructuredOutputSupport.parse(structuredOutputDefinition, output, objectMapper);
      return StructuredAgentResult.success(
          parsedValue, Builder.from(this).parsed(parsedValue).build());
    } catch (JacksonException e) {
      return StructuredAgentResult.error(
          new RuntimeException("Failed to parse structured output: " + e.getMessage(), e), this);
    }
  }

  private static @NonNull List<String> prependDelegationPath(
      @NonNull String delegatorName, @NonNull List<String> existingPath) {
    List<String> path = new ArrayList<>(existingPath.size() + 1);
    path.add(delegatorName);
    if (existingPath.isEmpty()) {
      return path;
    }
    path.addAll(existingPath);
    return path;
  }

  private @NonNull Optional<Message> lastMessageMatching(@Nullable MessageRole role) {
    for (int i = history.size() - 1; i >= 0; i--) {
      ResponseInputItem item = history.get(i);
      if (item instanceof Message message && (role == null || message.role() == role)) {
        return Optional.of(message);
      }
    }
    return Optional.empty();
  }

  private static @NonNull Optional<String> firstTextContent(@NonNull Message message) {
    for (var content : message.content()) {
      if (content instanceof Text text) {
        return Optional.of(text.text());
      }
    }
    return Optional.empty();
  }

  // ===== Builder =====

  static final class Builder {
    private @Nullable String output;
    private @Nullable Response finalResponse;
    private @Nullable List<ResponseInputItem> history;
    private @Nullable List<ToolExecution> toolExecutions;
    private int turnsUsed;
    private @Nullable Agent handoffAgent;
    private @Nullable Throwable error;
    private @Nullable Object parsed;
    private @Nullable AgentRunState pausedState;
    private @Nullable List<AgentResult> relatedResults;
    private @Nullable FunctionToolCall clientSideToolCall;
    private @Nullable OutputOrigin outputOrigin;
    private @Nullable String outputProducerName;
    private @Nullable List<String> delegationPath;

    static @NonNull Builder from(@NonNull AgentResult result) {
      return new Builder()
          .output(result.output)
          .finalResponse(result.finalResponse)
          .history(result.history)
          .toolExecutions(result.toolExecutions)
          .turnsUsed(result.turnsUsed)
          .handoffAgent(result.handoffAgent)
          .error(result.error)
          .parsed(result.parsed)
          .pausedState(result.pausedState)
          .relatedResults(result.relatedResults)
          .clientSideToolCall(result.clientSideToolCall)
          .outputOrigin(result.outputOrigin)
          .outputProducerName(result.outputProducerName)
          .delegationPath(result.delegationPath);
    }

    Builder output(String output) {
      this.output = output;
      return this;
    }

    Builder finalResponse(Response response) {
      this.finalResponse = response;
      return this;
    }

    Builder history(List<ResponseInputItem> history) {
      this.history = history;
      return this;
    }

    Builder toolExecutions(List<ToolExecution> toolExecutions) {
      this.toolExecutions = toolExecutions;
      return this;
    }

    Builder turnsUsed(int turnsUsed) {
      this.turnsUsed = turnsUsed;
      return this;
    }

    Builder handoffAgent(Agent agent) {
      this.handoffAgent = agent;
      return this;
    }

    Builder error(Throwable error) {
      this.error = error;
      return this;
    }

    Builder parsed(Object parsed) {
      this.parsed = parsed;
      return this;
    }

    Builder pausedState(AgentRunState state) {
      this.pausedState = state;
      return this;
    }

    Builder relatedResults(List<AgentResult> relatedResults) {
      this.relatedResults = relatedResults;
      return this;
    }

    Builder clientSideToolCall(FunctionToolCall call) {
      this.clientSideToolCall = call;
      return this;
    }

    Builder outputOrigin(OutputOrigin outputOrigin) {
      this.outputOrigin = outputOrigin;
      return this;
    }

    Builder outputProducerName(String outputProducerName) {
      this.outputProducerName = outputProducerName;
      return this;
    }

    Builder delegationPath(List<String> delegationPath) {
      this.delegationPath = delegationPath;
      return this;
    }

    AgentResult build() {
      return new AgentResult(this);
    }
  }

  /** Exception thrown when a guardrail validation fails. */
  public static final class GuardrailException extends RuntimeException {
    public GuardrailException(String message) {
      super(message);
    }
  }
}
