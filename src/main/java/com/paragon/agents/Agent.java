package com.paragon.agents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.agents.context.ContextManagementConfig;
import com.paragon.responses.Responder;
import com.paragon.responses.exception.AgentExecutionException;
import com.paragon.responses.exception.GuardrailException;
import com.paragon.responses.exception.ToolExecutionException;
import com.paragon.responses.spec.*;
import com.paragon.telemetry.TelemetryContext;
import com.paragon.telemetry.events.AgentFailedEvent;
import com.paragon.telemetry.processors.ProcessorRegistry;
import com.paragon.telemetry.processors.TelemetryProcessor;
import com.paragon.telemetry.processors.TraceIdGenerator;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A stateful AI agent that can perceive, plan, and act using tools.
 *
 * <p>Unlike {@link Responder} which is stateless, Agent maintains:
 *
 * <ul>
 *   <li>Conversation history (via {@link AgentContext})
 *   <li>Tool store for function calling
 *   <li>Handoffs for multi-agent delegation
 *   <li>Input/output guardrails
 * </ul>
 *
 * <h2>Core Concepts</h2>
 *
 * <p>Agent implements an <b>agentic loop</b>: it calls the LLM, checks for tool calls, executes
 * tools, and repeats until the model produces a final answer or a handoff is triggered.
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Basic Agent</h3>
 *
 * <pre>{@code
 * Agent agent = Agent.builder()
 *     .name("CustomerSupport")
 *     .instructions("You are a helpful customer support agent.")
 *     .model("openai/gpt-4o")
 *     .responder(responder)
 *     .addTool(new GetOrderStatusTool())
 *     .addTool(new RefundOrderTool())
 *     .maxTurns(10)
 *     .build();
 *
 * AgentResult result = agent.interact("What's the status of order #12345?");
 * System.out.println(result.output());
 * }</pre>
 *
 * <h3>With Context Persistence</h3>
 *
 * <pre>{@code
 * AgentContext context = AgentContext.create();
 *
 * // First turn
 * agent.interact("Hi, I need help with my order", context);
 *
 * // Second turn (remembers first)
 * agent.interact("It's order #12345", context);
 *
 * // Third turn
 * AgentResult result = agent.interact("Can you refund it?", context);
 * }</pre>
 *
 * <h3>With Structured Output</h3>
 *
 * <pre>{@code
 * Agent agent = Agent.builder()
 *     .name("PersonExtractor")
 *     .instructions("Extract person information from text.")
 *     .model("openai/gpt-4o")
 *     .responder(responder)
 *     .outputType(Person.class)  // Structured output type
 *     .build();
 *
 * AgentResult result = agent.interact("John is a 30-year-old engineer.");
 * Person person = result.parsed();  // Type-safe parsed output
 * }</pre>
 *
 * <h3>Multi-Agent Handoff</h3>
 *
 * <pre>{@code
 * Agent salesAgent = Agent.builder()
 *     .name("Sales")
 *     .instructions("Handle sales inquiries.")
 *     .addHandoff(Handoff.to(supportAgent)
 *         .withDescription("Transfer to support for technical issues"))
 *     .build();
 *
 * // When a handoff is detected, the target agent is automatically invoked
 * AgentResult result = salesAgent.interact("I have a billing question");
 * System.out.println(result.output());  // Final output from support agent
 * }</pre>
 *
 * @see AgentContext
 * @see AgentResult
 * @see Responder
 * @since 1.0
 */
public final class Agent implements Serializable {

  // ===== Configuration (Immutable) =====
  private final @NonNull String name;
  private final @NonNull String instructions;
  private final @NonNull String model;
  private final @NonNull List<FunctionTool<?>> tools;
  private final @NonNull List<Handoff> handoffs;
  private final @NonNull List<InputGuardrail> inputGuardrails;
  private final @NonNull List<OutputGuardrail> outputGuardrails;
  private final int maxTurns;
  private final @Nullable Class<?> outputType;
  private final @Nullable Double temperature;
  private final @NonNull TelemetryContext telemetryContext;

  // ===== Context Management =====
  private final @Nullable ContextManagementConfig contextManagementConfig;

  // ===== Runtime Dependencies =====
  private final transient @NonNull Responder responder;
  private final transient @NonNull FunctionToolStore toolStore;
  private final transient @NonNull ObjectMapper objectMapper;
  private final transient @NonNull ProcessorRegistry telemetryProcessors;

  private Agent(Builder builder) {
    this.name = Objects.requireNonNull(builder.name, "name is required");
    this.instructions = Objects.requireNonNull(builder.instructions, "instructions are required");
    this.model = Objects.requireNonNull(builder.model, "model is required");
    this.responder = Objects.requireNonNull(builder.responder, "responder is required");
    this.maxTurns = builder.maxTurns > 0 ? builder.maxTurns : 10;
    this.outputType = builder.outputType;
    this.temperature = builder.temperature;
    this.telemetryContext =
        builder.telemetryContext != null ? builder.telemetryContext : TelemetryContext.empty();
    this.telemetryProcessors =
        builder.telemetryProcessors.isEmpty()
            ? ProcessorRegistry.empty()
            : ProcessorRegistry.of(builder.telemetryProcessors);

    // Context management
    this.contextManagementConfig = builder.contextManagementConfig;

    // Copy tools
    this.tools = List.copyOf(builder.tools);
    this.handoffs = List.copyOf(builder.handoffs);
    this.inputGuardrails = List.copyOf(builder.inputGuardrails);
    this.outputGuardrails = List.copyOf(builder.outputGuardrails);

    // Build tool store
    this.objectMapper = builder.objectMapper != null ? builder.objectMapper : new ObjectMapper();
    this.toolStore = FunctionToolStore.create(objectMapper);
    for (FunctionTool<?> tool : tools) {
      toolStore.add(tool);
    }
    // Add handoff tools
    for (Handoff handoff : handoffs) {
      toolStore.add(handoff.asTool());
    }
  }

  // ===== Loop Callbacks Interface (for code reuse) =====

  /** Callbacks for agentic loop events. Package-private for AgentStream. */
  interface LoopCallbacks {
    /** Called at start of each turn. */
    default void onTurnStart(int turn) {}

    /** Called after LLM response received. */
    default void onTurnComplete(Response response) {}

    /** Called when tool call is detected. Returns true to execute, false to skip. */
    default boolean onToolCall(FunctionToolCall call) {
      return true;
    }

    /** Called after tool is executed. */
    default void onToolExecuted(ToolExecution execution) {}

    /** Called when handoff is detected. */
    default void onHandoff(Handoff handoff) {}

    /** Called when guardrail fails. */
    default void onGuardrailFailed(GuardrailResult.Failed failed) {}

    /** Called to pause for approval. Return non-null to pause. */
    default AgentRunState onPauseRequested(
        FunctionToolCall call,
        Response lastResponse,
        List<ToolExecution> executions,
        AgentContext context) {
      return null;
    }
  }

  /**
   * Creates a new Agent builder.
   *
   * @return a new builder instance
   */
  public static @NonNull Builder builder() {
    return new Builder();
  }

  // ===== Accessors =====

  /**
   * Returns the agent's name.
   *
   * @return the name
   */
  public @NonNull String name() {
    return name;
  }

  /**
   * Returns the agent's instructions (system prompt).
   *
   * @return the instructions
   */
  public @NonNull String instructions() {
    return instructions;
  }

  /**
   * Returns the model identifier.
   *
   * @return the model
   */
  public @NonNull String model() {
    return model;
  }

  /**
   * Returns the maximum number of LLM turns allowed.
   *
   * @return the max turns limit
   */
  public int maxTurns() {
    return maxTurns;
  }

  /**
   * Returns the input guardrails.
   *
   * @return the input guardrails (unmodifiable)
   */
  public @NonNull List<InputGuardrail> inputGuardrails() {
    return inputGuardrails;
  }

  /**
   * Returns the output guardrails.
   *
   * @return the output guardrails (unmodifiable)
   */
  public @NonNull List<OutputGuardrail> outputGuardrails() {
    return outputGuardrails;
  }

  /**
   * Returns the handoffs.
   *
   * @return the handoffs (unmodifiable)
   */
  public @NonNull List<Handoff> handoffs() {
    return handoffs;
  }

  /** Returns the tool store. Package-private for AgentStream. */
  @NonNull FunctionToolStore toolStore() {
    return toolStore;
  }

  /** Builds a payload from context. Package-private for AgentStream. */
  @NonNull CreateResponsePayload buildPayloadInternal(@NonNull AgentContext context) {
    return buildPayload(context);
  }

  // ===== Interact Methods (All Async) =====

  /**
   * Interacts with the agent using a text message. Creates a fresh context.
   *
   * <p>All interact methods are async by default. For blocking, call {@code .join()}.
   *
   * @param input the user's text input
   * @return a future completing with the agent's result
   */
  public @NonNull CompletableFuture<AgentResult> interact(@NonNull String input) {
    AgentContext context = AgentContext.create();
    context.addInput(Message.user(Text.valueOf(input)));
    return interact(context);
  }

  /**
   * Interacts with the agent using a Text content. Creates a fresh context.
   *
   * @param text the text content
   * @return a future completing with the agent's result
   */
  public @NonNull CompletableFuture<AgentResult> interact(@NonNull Text text) {
    Objects.requireNonNull(text, "text cannot be null");
    AgentContext context = AgentContext.create();
    context.addInput(Message.user(text));
    return interact(context);
  }

  /**
   * Interacts with the agent using a file. Creates a fresh context.
   *
   * @param file the file input
   * @return a future completing with the agent's result
   */
  public @NonNull CompletableFuture<AgentResult> interact(@NonNull File file) {
    Objects.requireNonNull(file, "file cannot be null");
    AgentContext context = AgentContext.create();
    context.addInput(Message.user(file));
    return interact(context);
  }

  /**
   * Interacts with the agent using an image. Creates a fresh context.
   *
   * @param image the image input
   * @return a future completing with the agent's result
   */
  public @NonNull CompletableFuture<AgentResult> interact(@NonNull Image image) {
    Objects.requireNonNull(image, "image cannot be null");
    AgentContext context = AgentContext.create();
    context.addInput(Message.user(image));
    return interact(context);
  }

  /**
   * Interacts with the agent using a Message. Creates a fresh context.
   *
   * @param message the message input
   * @return a future completing with the agent's result
   */
  public @NonNull CompletableFuture<AgentResult> interact(@NonNull Message message) {
    Objects.requireNonNull(message, "message cannot be null");
    AgentContext context = AgentContext.create();
    context.addInput(message);
    return interact(context);
  }

  /**
   * Interacts with the agent using a ResponseInputItem. Creates a fresh context.
   *
   * @param input the input item
   * @return a future completing with the agent's result
   */
  public @NonNull CompletableFuture<AgentResult> interact(@NonNull ResponseInputItem input) {
    Objects.requireNonNull(input, "input cannot be null");
    AgentContext context = AgentContext.create();
    context.addInput(input);
    return interact(context);
  }

  /**
   * Interacts with the agent using multiple inputs. Creates a fresh context.
   *
   * @param input the input items
   * @return a future completing with the agent's result
   */
  public @NonNull CompletableFuture<AgentResult> interact(@NonNull List<ResponseInputItem> input) {
    AgentContext context = AgentContext.create();
    for (ResponseInputItem item : input) {
      context.addInput(item);
    }
    return interact(context);
  }

  // ===== Streaming API =====

  /**
   * Interacts with the agent using streaming with full agentic loop. Creates a fresh context.
   *
   * <p>Returns an {@link AgentStream} that runs the complete agentic loop including guardrails,
   * tool execution, and multi-turn conversations, emitting events at each phase.
   *
   * <p>Example:
   *
   * <pre>{@code
   * agent.interactStream("Help me debug this code")
   *     .onTurnStart(turn -> System.out.println("--- Turn " + turn + " ---"))
   *     .onTextDelta(chunk -> System.out.print(chunk))
   *     .onToolCallPending((call, approve) -> approve.accept(true))  // Human-in-the-loop
   *     .onToolExecuted(exec -> System.out.println("Tool: " + exec.toolName()))
   *     .onComplete(result -> System.out.println("\nDone!"))
   *     .start();
   * }</pre>
   *
   * @param input the user's text input
   * @return an AgentStream for processing streaming events
   */
  public @NonNull AgentStream interactStream(@NonNull String input) {
    Objects.requireNonNull(input, "input cannot be null");

    AgentContext context = AgentContext.create();
    List<ResponseInputItem> inputList = List.of(Message.user(input));

    // Validate input guardrails before creating stream
    String inputText = extractTextFromInput(inputList);
    for (InputGuardrail guardrail : inputGuardrails) {
      GuardrailResult result = guardrail.validate(inputText, context);
      if (result.isFailed()) {
        GuardrailResult.Failed failed = (GuardrailResult.Failed) result;
        GuardrailException guardEx = GuardrailException.inputViolation(failed.reason());
        broadcastFailedEvent(guardEx, context);
        return AgentStream.failed(AgentResult.error(guardEx, context, context.getTurnCount()));
      }
    }

    return new AgentStream(this, inputList, context, responder, objectMapper);
  }

  /**
   * Interacts with the agent using streaming with an existing context.
   *
   * <p>This is the core streaming method. All other streaming overloads ultimately delegate here
   * after adding their input to the context.
   *
   * @param context the conversation context containing all history
   * @return an AgentStream for processing streaming events
   */
  public @NonNull AgentStream interactStream(@NonNull AgentContext context) {
    Objects.requireNonNull(context, "context cannot be null");
    return new AgentStream(this, context, responder, objectMapper);
  }

  // ===== Resume Methods =====

  /**
   * Resumes a paused agent run.
   *
   * <p>Use this after calling {@code state.approveToolCall()} or {@code state.rejectToolCall()} on
   * a previously paused run.
   *
   * <p>Example:
   *
   * <pre>{@code
   * // Load saved state from database
   * AgentRunState state = loadFromDatabase(runId);
   *
   * // User approved the tool
   * state.approveToolCall(toolOutput);
   *
   * // Resume the run
   * AgentResult result = agent.resume(state);
   * }</pre>
   *
   * @param state the paused run state with approval decision set
   * @return the agent result after resumption
   * @throws IllegalStateException if state is not pending approval or no decision was made
   */
  public @NonNull AgentResult resume(@NonNull AgentRunState state) {
    Objects.requireNonNull(state, "state cannot be null");

    if (!state.isPendingApproval()) {
      throw new IllegalStateException("Cannot resume: state is not pending approval");
    }

    AgentRunState.ToolApprovalResult approval = state.approvalResult();
    if (approval == null) {
      throw new IllegalStateException(
          "Cannot resume: call approveToolCall() or rejectToolCall() first");
    }

    // Add the tool result to context
    FunctionToolCall pendingCall = state.pendingToolCall();
    FunctionToolCallOutput output;
    if (approval.approved()) {
      output = FunctionToolCallOutput.success(pendingCall.callId(), approval.outputOrReason());
    } else {
      String reason =
          approval.outputOrReason() != null
              ? approval.outputOrReason()
              : "Tool execution was rejected by user";
      output = FunctionToolCallOutput.error(pendingCall.callId(), reason);
    }
    state.context().addToolResult(output);

    // Continue the agentic loop from where we left off
    return continueAgenticLoop(
        state.context(), state.lastResponse(), state.toolExecutions(), state.currentTurn());
  }

  /**
   * Resumes a paused agent run with streaming.
   *
   * @param state the paused run state with approval decision set
   * @return an AgentStream for processing remaining events
   */
  public @NonNull AgentStream resumeStream(@NonNull AgentRunState state) {
    Objects.requireNonNull(state, "state cannot be null");

    if (!state.isPendingApproval()) {
      throw new IllegalStateException("Cannot resume: state is not pending approval");
    }

    AgentRunState.ToolApprovalResult approval = state.approvalResult();
    if (approval == null) {
      throw new IllegalStateException(
          "Cannot resume: call approveToolCall() or rejectToolCall() first");
    }

    // Add the tool result to context
    FunctionToolCall pendingCall = state.pendingToolCall();
    FunctionToolCallOutput output;
    if (approval.approved()) {
      output = FunctionToolCallOutput.success(pendingCall.callId(), approval.outputOrReason());
    } else {
      String reason =
          approval.outputOrReason() != null
              ? approval.outputOrReason()
              : "Tool execution was rejected by user";
      output = FunctionToolCallOutput.error(pendingCall.callId(), reason);
    }
    state.context().addToolResult(output);

    // Return stream that continues from saved state
    return new AgentStream(
        this,
        List.of(), // No new input - resuming
        state.context(),
        responder,
        objectMapper,
        state.toolExecutions(),
        state.currentTurn());
  }

  // ===== Core Interaction Methods =====

  /**
   * Core interact method. Executes the agentic loop asynchronously.
   *
   * <p>This is the primary execution method. All other interact overloads ultimately delegate here
   * after adding their input to the context.
   *
   * @param context the conversation context containing all history
   * @return a future completing with the agent's result
   */
  public @NonNull CompletableFuture<AgentResult> interact(@NonNull AgentContext context) {
    return CompletableFuture.supplyAsync(() -> interactBlocking(context, null));
  }

  /**
   * Core blocking interact method with callbacks. Package-private for AgentStream.
   *
   * @param context the conversation context containing all history
   * @param callbacks optional loop callbacks for streaming/events
   * @return the agent result
   */
  @NonNull AgentResult interactBlocking(
      @NonNull AgentContext context, @Nullable LoopCallbacks callbacks) {
    Objects.requireNonNull(context, "context cannot be null");

    // Auto-initialize trace context if not set (enables automatic correlation)
    if (!context.hasTraceContext()) {
      String traceId = TraceIdGenerator.generateTraceId();
      String spanId = TraceIdGenerator.generateSpanId();
      context.withTraceContext(traceId, spanId);
    }

    // Validate input guardrails before processing
    String inputText = extractTextFromInput(context.getHistory());
    for (InputGuardrail guardrail : inputGuardrails) {
      GuardrailResult result = guardrail.validate(inputText, context);
      if (result.isFailed()) {
        GuardrailResult.Failed failed = (GuardrailResult.Failed) result;
        GuardrailException guardEx = GuardrailException.inputViolation(failed.reason());
        broadcastFailedEvent(guardEx, context);
        return AgentResult.error(guardEx, context, context.getTurnCount());
      }
    }

    // Execute agentic loop
    return executeAgenticLoop(context, new ArrayList<>(), callbacks, "");
  }

  /** Unified agentic loop. Shared by interact, resume, and AgentStream. */
  private AgentResult executeAgenticLoop(
      AgentContext context,
      List<ToolExecution> initialExecutions,
      @Nullable LoopCallbacks callbacks,
      String fallbackHandoffText) {

    List<ToolExecution> allToolExecutions = new ArrayList<>(initialExecutions);
    Response lastResponse = null;
    String lastResponseId = null;

    try {
      while (context.incrementTurn() <= maxTurns) {
        int turn = context.getTurnCount();
        if (callbacks != null) callbacks.onTurnStart(turn);

        // Build TelemetryContext from AgentContext for trace correlation
        TelemetryContext telemetryCtx = buildTelemetryContext(context);

        // Build payload and call LLM with trace context
        CreateResponsePayload payload = buildPayload(context);
        try {
          lastResponse = responder.respond(payload, telemetryCtx).join();
          lastResponseId = lastResponse.id();
        } catch (Exception e) {
          // Wrap LLM call failures in AgentExecutionException
          AgentExecutionException agentEx =
              AgentExecutionException.llmCallFailed(name, context.getTurnCount(), e);
          broadcastFailedEvent(agentEx, context);
          return AgentResult.error(agentEx, context, context.getTurnCount());
        }

        if (callbacks != null) callbacks.onTurnComplete(lastResponse);

        // Add assistant response to context
        if (lastResponse.output() != null) {
          for (ResponseOutput outputItem : lastResponse.output()) {
            if (outputItem instanceof Message msg) {
              context.addMessage(msg);
            }
          }
        }

        // Check for tool calls
        List<FunctionToolCall> toolCalls = extractFunctionToolCalls(lastResponse);
        if (toolCalls.isEmpty()) {
          break; // No tools → final answer
        }

        // Check for handoffs
        Handoff handoff = detectHandoff(toolCalls);
        if (handoff != null) {
          if (callbacks != null) callbacks.onHandoff(handoff);
          String handoffMessage = extractHandoffMessage(toolCalls, handoff.name());

          // Fork context with new parent span for child agent
          String childSpanId = TraceIdGenerator.generateSpanId();
          AgentContext childContext = context.fork(childSpanId);

          // Add handoff message to child context
          String message = handoffMessage != null ? handoffMessage : fallbackHandoffText;
          if (message != null && !message.isEmpty()) {
            childContext.addInput(Message.user(message));
          }

          try {
            AgentResult innerResult = handoff.targetAgent().interact(childContext).join();
            return AgentResult.handoff(handoff.targetAgent(), innerResult, context);
          } catch (Exception e) {
            AgentExecutionException agentEx =
                AgentExecutionException.handoffFailed(
                    name, handoff.targetAgent().name(), context.getTurnCount(), e);
            broadcastFailedEvent(agentEx, context);
            return AgentResult.error(agentEx, context, context.getTurnCount());
          }
        }

        // Execute tools (with callback hooks)
        for (FunctionToolCall call : toolCalls) {
          if (call == null) continue;

          // Non-streaming: auto-pause for tools requiring confirmation
          if (callbacks == null) {
            FunctionTool<?> tool = toolStore.get(call.name());
            if (tool != null && tool.requiresConfirmation()) {
              AgentRunState pauseState =
                  AgentRunState.pendingApproval(
                      name, context, call, lastResponse, allToolExecutions, turn);
              return AgentResult.paused(pauseState, context);
            }
          }

          // Check for pause request (streaming)
          if (callbacks != null) {
            AgentRunState pauseState =
                callbacks.onPauseRequested(call, lastResponse, allToolExecutions, context);
            if (pauseState != null) {
              return AgentResult.paused(pauseState, context);
            }
          }

          // Check if tool should execute
          boolean shouldExecute = callbacks == null || callbacks.onToolCall(call);

          if (shouldExecute) {
            ToolExecution exec = executeSingleToolWithErrorHandling(call, context);
            allToolExecutions.add(exec);
            if (callbacks != null) callbacks.onToolExecuted(exec);
            context.addToolResult(exec.output());
          } else {
            // Tool rejected
            FunctionToolCallOutput rejectedOutput =
                FunctionToolCallOutput.error(call.callId(), "Tool execution was rejected");
            context.addToolResult(rejectedOutput);
          }
        }
      }

      // Check if max turns exceeded
      if (context.getTurnCount() > maxTurns) {
        AgentExecutionException agentEx =
            AgentExecutionException.maxTurnsExceeded(name, maxTurns, context.getTurnCount());
        broadcastFailedEvent(agentEx, context);
        return AgentResult.error(agentEx, context, context.getTurnCount());
      }

      // Get final output
      String output = lastResponse != null ? lastResponse.outputText() : "";

      // Validate output with guardrails
      for (OutputGuardrail guardrail : outputGuardrails) {
        GuardrailResult result = guardrail.validate(output, context);
        if (result.isFailed()) {
          GuardrailResult.Failed failed = (GuardrailResult.Failed) result;
          if (callbacks != null) callbacks.onGuardrailFailed(failed);

          // Create typed exception for output guardrail failure
          GuardrailException guardEx = GuardrailException.outputViolation(failed.reason());
          broadcastFailedEvent(guardEx, context);
          return AgentResult.error(guardEx, context, context.getTurnCount());
        }
      }

      // Parse structured output if applicable
      if (outputType != null && lastResponse != null) {
        try {
          Object parsed = lastResponse.parse(outputType, objectMapper);
          return AgentResult.successWithParsed(
              output, parsed, lastResponse, context, allToolExecutions, context.getTurnCount());
        } catch (JsonProcessingException e) {
          AgentExecutionException agentEx =
              AgentExecutionException.parsingFailed(name, context.getTurnCount(), e);
          broadcastFailedEvent(agentEx, context);
          return AgentResult.error(agentEx, context, context.getTurnCount());
        }
      }

      return AgentResult.success(
          output, lastResponse, context, allToolExecutions, context.getTurnCount());

    } catch (Exception e) {
      // Wrap unexpected exceptions in AgentExecutionException
      AgentExecutionException agentEx =
          new AgentExecutionException(
              name,
              AgentExecutionException.Phase.LLM_CALL,
              context.getTurnCount(),
              String.format("Agent '%s' failed unexpectedly: %s", name, e.getMessage()),
              e);
      broadcastFailedEvent(agentEx, context);
      return AgentResult.error(agentEx, context, context.getTurnCount());
    }
  }

  /** Broadcasts a failed event for telemetry. */
  private void broadcastFailedEvent(Exception exception, AgentContext context) {
    if (telemetryProcessors != null) {
      String sessionId =
          context.requestId() != null
              ? context.requestId()
              : java.util.UUID.randomUUID().toString();
      AgentFailedEvent event =
          AgentFailedEvent.from(
              name,
              context.getTurnCount(),
              exception,
              sessionId,
              context.parentTraceId(),
              context.parentSpanId(),
              null);
      telemetryProcessors.broadcast(event);
    }
  }

  /** Continues the agentic loop from a saved state (used by resume). */
  private AgentResult continueAgenticLoop(
      AgentContext context,
      Response lastResponse,
      List<ToolExecution> previousExecutions,
      int startTurn) {
    // Delegate to unified loop
    return executeAgenticLoop(context, previousExecutions, null, "");
  }

  // ===== Private Helper Methods =====

  private CreateResponsePayload buildPayload(AgentContext context) {
    List<ResponseInputItem> input = context.getHistoryMutable();

    // Apply context management if configured
    if (contextManagementConfig != null) {
      input =
          contextManagementConfig
              .strategy()
              .manage(
                  input,
                  contextManagementConfig.maxTokens(),
                  contextManagementConfig.tokenCounter());
    }

    CreateResponsePayload.Builder builder =
        CreateResponsePayload.builder().model(model).instructions(instructions).input(input);

    // Add tools
    for (FunctionTool<?> tool : tools) {
      builder.addTool(tool);
    }
    // Add handoff tools
    for (Handoff handoff : handoffs) {
      builder.addTool(handoff.asTool());
    }

    // Add temperature if set
    if (temperature != null) {
      builder.temperature(temperature);
    }

    return builder.build();
  }

  /** Builds a TelemetryContext from AgentContext for trace correlation. */
  private TelemetryContext buildTelemetryContext(AgentContext context) {
    TelemetryContext.Builder builder =
        TelemetryContext.builder().traceName(name + ".turn-" + context.getTurnCount());

    if (context.parentTraceId() != null) {
      builder.parentTraceId(context.parentTraceId());
    }
    if (context.parentSpanId() != null) {
      builder.parentSpanId(context.parentSpanId());
    }
    if (context.requestId() != null) {
      builder.requestId(context.requestId());
    }

    // Merge with agent-level telemetry context
    builder.metadata(telemetryContext.metadata());
    builder.tags(telemetryContext.tags().stream().toList());
    if (telemetryContext.userId() != null) {
      builder.userId(telemetryContext.userId());
    }

    return builder.build();
  }

  private List<FunctionToolCall> extractFunctionToolCalls(Response response) {
    List<FunctionToolCall> calls = new ArrayList<>();
    if (response.output() == null) {
      return calls;
    }
    for (ResponseOutput item : response.output()) {
      if (item instanceof FunctionToolCall ftc) {
        calls.add(ftc);
      }
    }
    return calls;
  }

  private @Nullable Handoff detectHandoff(List<FunctionToolCall> toolCalls) {
    for (FunctionToolCall call : toolCalls) {
      for (Handoff handoff : handoffs) {
        if (handoff.name().equals(call.name())) {
          return handoff;
        }
      }
    }
    return null;
  }

  private @Nullable String extractHandoffMessage(
      List<FunctionToolCall> toolCalls, String handoffName) {
    for (FunctionToolCall call : toolCalls) {
      if (handoffName.equals(call.name())) {
        try {
          Handoff.HandoffParams params =
              objectMapper.readValue(call.arguments(), Handoff.HandoffParams.class);
          return params.message();
        } catch (JsonProcessingException e) {
          return null;
        }
      }
    }
    return null;
  }

  private List<ToolExecution> executeTools(List<FunctionToolCall> toolCalls) {
    List<ToolExecution> executions = new ArrayList<>();
    for (FunctionToolCall call : toolCalls) {
      // Skip handoff tools (handled separately)
      boolean isHandoff = false;
      for (Handoff handoff : handoffs) {
        if (handoff.name().equals(call.name())) {
          isHandoff = true;
          break;
        }
      }
      if (isHandoff) {
        continue;
      }

      Instant start = Instant.now();
      try {
        FunctionToolCallOutput output = toolStore.execute(call);
        Duration duration = Duration.between(start, Instant.now());
        executions.add(
            new ToolExecution(call.name(), call.callId(), call.arguments(), output, duration));
      } catch (JsonProcessingException e) {
        Duration duration = Duration.between(start, Instant.now());
        FunctionToolCallOutput errorOutput =
            FunctionToolCallOutput.error(
                call.callId(), "Failed to parse arguments: " + e.getMessage());
        executions.add(
            new ToolExecution(call.name(), call.callId(), call.arguments(), errorOutput, duration));
      }
    }
    return executions;
  }

  private ToolExecution executeSingleTool(FunctionToolCall call) {
    // Skip handoff tools (handled separately)
    for (Handoff handoff : handoffs) {
      if (handoff.name().equals(call.name())) {
        FunctionToolCallOutput output =
            FunctionToolCallOutput.error(
                call.callId(), "Handoff tool should not be executed directly");
        return new ToolExecution(
            call.name(), call.callId(), call.arguments(), output, Duration.ZERO);
      }
    }

    Instant start = Instant.now();
    try {
      FunctionToolCallOutput output = toolStore.execute(call);
      Duration duration = Duration.between(start, Instant.now());
      return new ToolExecution(call.name(), call.callId(), call.arguments(), output, duration);
    } catch (JsonProcessingException e) {
      Duration duration = Duration.between(start, Instant.now());
      FunctionToolCallOutput errorOutput =
          FunctionToolCallOutput.error(
              call.callId(), "Failed to parse arguments: " + e.getMessage());
      return new ToolExecution(call.name(), call.callId(), call.arguments(), errorOutput, duration);
    }
  }

  /**
   * Executes a single tool with proper error handling and telemetry. Wraps errors in
   * ToolExecutionException for better diagnostics.
   */
  private ToolExecution executeSingleToolWithErrorHandling(
      FunctionToolCall call, AgentContext context) {
    // Skip handoff tools (handled separately)
    for (Handoff handoff : handoffs) {
      if (handoff.name().equals(call.name())) {
        FunctionToolCallOutput output =
            FunctionToolCallOutput.error(
                call.callId(), "Handoff tool should not be executed directly");
        return new ToolExecution(
            call.name(), call.callId(), call.arguments(), output, Duration.ZERO);
      }
    }

    Instant start = Instant.now();
    try {
      FunctionToolCallOutput output = toolStore.execute(call);
      Duration duration = Duration.between(start, Instant.now());
      return new ToolExecution(call.name(), call.callId(), call.arguments(), output, duration);
    } catch (JsonProcessingException e) {
      Duration duration = Duration.between(start, Instant.now());

      // Create typed exception for tool execution failure
      ToolExecutionException toolEx =
          new ToolExecutionException(
              call.name(),
              call.callId(),
              call.arguments(),
              "Failed to parse tool arguments: " + e.getMessage(),
              e);

      // Broadcast failure event for telemetry
      broadcastFailedEvent(toolEx, context);

      FunctionToolCallOutput errorOutput =
          FunctionToolCallOutput.error(call.callId(), "Tool execution failed: " + e.getMessage());
      return new ToolExecution(call.name(), call.callId(), call.arguments(), errorOutput, duration);
    } catch (Exception e) {
      Duration duration = Duration.between(start, Instant.now());

      // Create typed exception for unexpected tool execution failure
      ToolExecutionException toolEx =
          new ToolExecutionException(
              call.name(),
              call.callId(),
              call.arguments(),
              "Tool execution failed: " + e.getMessage(),
              e);

      // Broadcast failure event for telemetry
      broadcastFailedEvent(toolEx, context);

      FunctionToolCallOutput errorOutput =
          FunctionToolCallOutput.error(call.callId(), "Tool execution failed: " + e.getMessage());
      return new ToolExecution(call.name(), call.callId(), call.arguments(), errorOutput, duration);
    }
  }

  private String extractTextFromInput(List<ResponseInputItem> input) {
    StringBuilder sb = new StringBuilder();
    for (ResponseInputItem item : input) {
      if (item instanceof Message msg) {
        sb.append(msg.outputText());
      }
      // Item and ItemReference don't have direct text content to extract
    }
    return sb.toString();
  }

  // ===== Builder =====

  /** Builder for creating Agent instances. */
  public static final class Builder {
    private @Nullable String name;
    private @Nullable String instructions;
    private @Nullable String model;
    private @Nullable Responder responder;
    private @Nullable ObjectMapper objectMapper;
    private @Nullable Class<?> outputType;
    private @Nullable Double temperature;
    private @Nullable Integer maxOutputTokens;
    private @Nullable Map<String, String> metadata;
    private @Nullable TelemetryContext telemetryContext;
    private com.paragon.http.RetryPolicy retryPolicy; // nullable by default
    private int maxTurns = 10;
    private final List<FunctionTool<?>> tools = new ArrayList<>();
    private final List<Handoff> handoffs = new ArrayList<>();
    private final List<InputGuardrail> inputGuardrails = new ArrayList<>();
    private final List<OutputGuardrail> outputGuardrails = new ArrayList<>();
    private final List<TelemetryProcessor> telemetryProcessors = new ArrayList<>();

    // Context management
    private @Nullable ContextManagementConfig contextManagementConfig;

    /**
     * Sets the agent's name (required).
     *
     * @param name the agent name
     * @return this builder
     */
    public @NonNull Builder name(@NonNull String name) {
      this.name = Objects.requireNonNull(name);
      return this;
    }

    /**
     * Sets the agent's instructions/system prompt (required).
     *
     * @param instructions the system prompt
     * @return this builder
     */
    public @NonNull Builder instructions(@NonNull String instructions) {
      this.instructions = Objects.requireNonNull(instructions);
      return this;
    }

    /**
     * Sets the model to use (required).
     *
     * @param model the model identifier
     * @return this builder
     */
    public @NonNull Builder model(@NonNull String model) {
      this.model = Objects.requireNonNull(model);
      return this;
    }

    /**
     * Sets the Responder to use for API calls (required).
     *
     * @param responder the responder instance
     * @return this builder
     */
    public @NonNull Builder responder(@NonNull Responder responder) {
      this.responder = Objects.requireNonNull(responder);
      return this;
    }

    /**
     * Sets a custom ObjectMapper for JSON serialization.
     *
     * @param objectMapper the object mapper
     * @return this builder
     */
    public @NonNull Builder objectMapper(@NonNull ObjectMapper objectMapper) {
      this.objectMapper = Objects.requireNonNull(objectMapper);
      return this;
    }

    /**
     * Sets the structured output type for parsing responses.
     *
     * <p>When set, the agent will parse the final response as this type.
     *
     * @param outputType the class to parse responses as
     * @return this builder
     */
    public @NonNull Builder outputType(@NonNull Class<?> outputType) {
      this.outputType = Objects.requireNonNull(outputType);
      return this;
    }

    /**
     * Sets the maximum number of LLM turns (default: 10).
     *
     * @param maxTurns the maximum turns
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
     * Sets the temperature for LLM calls (0.0 to 2.0).
     *
     * <p>Lower values make output more deterministic, higher values more creative.
     *
     * @param temperature the temperature value
     * @return this builder
     */
    public @NonNull Builder temperature(double temperature) {
      if (temperature < 0.0 || temperature > 2.0) {
        throw new IllegalArgumentException("temperature must be between 0.0 and 2.0");
      }
      this.temperature = temperature;
      return this;
    }

    /**
     * Sets the maximum number of output tokens for LLM responses.
     *
     * @param maxOutputTokens the maximum tokens
     * @return this builder
     */
    public @NonNull Builder maxOutputTokens(int maxOutputTokens) {
      if (maxOutputTokens <= 0) {
        throw new IllegalArgumentException("maxOutputTokens must be positive");
      }
      this.maxOutputTokens = maxOutputTokens;
      return this;
    }

    /**
     * Sets metadata to attach to the agent's requests.
     *
     * @param metadata key-value pairs
     * @return this builder
     */
    public @NonNull Builder metadata(@NonNull Map<String, String> metadata) {
      this.metadata = Objects.requireNonNull(metadata);
      return this;
    }

    /**
     * Adds a function tool.
     *
     * @param tool the tool to add
     * @return this builder
     */
    public @NonNull Builder addTool(@NonNull FunctionTool<?> tool) {
      this.tools.add(Objects.requireNonNull(tool));
      return this;
    }

    /**
     * Adds multiple function tools.
     *
     * @param tools the tools to add
     * @return this builder
     */
    public @NonNull Builder addTools(@NonNull FunctionTool<?>... tools) {
      for (FunctionTool<?> tool : tools) {
        addTool(tool);
      }
      return this;
    }

    /**
     * Adds a handoff to another agent.
     *
     * @param handoff the handoff to add
     * @return this builder
     */
    public @NonNull Builder addHandoff(@NonNull Handoff handoff) {
      this.handoffs.add(Objects.requireNonNull(handoff));
      return this;
    }

    /**
     * Adds an input guardrail.
     *
     * @param guardrail the guardrail to add
     * @return this builder
     */
    public @NonNull Builder addInputGuardrail(@NonNull InputGuardrail guardrail) {
      this.inputGuardrails.add(Objects.requireNonNull(guardrail));
      return this;
    }

    /**
     * Adds an output guardrail.
     *
     * @param guardrail the guardrail to add
     * @return this builder
     */
    public @NonNull Builder addOutputGuardrail(@NonNull OutputGuardrail guardrail) {
      this.outputGuardrails.add(Objects.requireNonNull(guardrail));
      return this;
    }

    /**
     * Adds all memory tools from a Memory storage.
     *
     * <p>This adds 4 tools: add_memory, retrieve_memories, update_memory, delete_memory. The userId
     * is passed securely via {@code interact(input, context, userId)}, NOT by the LLM, to prevent
     * prompt injection attacks.
     *
     * @param memory the memory storage
     * @return this builder
     */
    public @NonNull Builder addMemoryTools(@NonNull Memory memory) {
      for (FunctionTool<?> tool : MemoryTool.all(memory)) {
        this.tools.add(tool);
      }
      return this;
    }

    /**
     * Sets the telemetry context for agent runs.
     *
     * <p>This context is used as the default for all runs. Can be overridden per-run.
     *
     * @param context the telemetry context
     * @return this builder
     */
    public @NonNull Builder telemetryContext(@NonNull TelemetryContext context) {
      this.telemetryContext = Objects.requireNonNull(context);
      return this;
    }

    /**
     * Adds a telemetry processor for OpenTelemetry integration.
     *
     * <p>Telemetry is enabled by default. Processors receive events for each agent run, tool
     * execution, and handoff.
     *
     * @param processor the processor to add
     * @return this builder
     */
    public @NonNull Builder addTelemetryProcessor(@NonNull TelemetryProcessor processor) {
      this.telemetryProcessors.add(Objects.requireNonNull(processor));
      return this;
    }

    /**
     * Configures context management with a configuration object.
     *
     * <p>Context management controls how conversation history is handled when it exceeds the
     * model's token limit.
     *
     * <p>Example:
     *
     * <pre>{@code
     * Agent agent = Agent.builder()
     *     .name("Assistant")
     *     .model("openai/gpt-4o")
     *     .responder(responder)
     *     .contextManagement(ContextManagementConfig.builder()
     *         .strategy(new SlidingWindowStrategy())
     *         .maxTokens(4000)
     *         .build())
     *     .build();
     * }</pre>
     *
     * @param config the context management configuration
     * @return this builder
     * @see ContextManagementConfig
     * @see SlidingWindowStrategy
     * @see SummarizationStrategy
     */
    public @NonNull Builder contextManagement(@NonNull ContextManagementConfig config) {
      this.contextManagementConfig = Objects.requireNonNull(config);
      return this;
    }

    /**
     * Sets the retry policy for handling transient API failures.
     *
     * <p>This configures automatic retry with exponential backoff for:
     *
     * <ul>
     *   <li>429 - Rate limiting
     *   <li>500, 502, 503, 504 - Server errors
     *   <li>Network failures (connection timeout, etc.)
     * </ul>
     *
     * <p>Note: If you provide a custom Responder via {@link #responder(Responder)}, configure retry
     * on the Responder instead. This setting is used when no Responder is provided and one is
     * created internally.
     *
     * @param retryPolicy the retry policy to use
     * @return this builder
     * @see com.paragon.http.RetryPolicy
     */
    public @NonNull Builder retryPolicy(com.paragon.http.RetryPolicy retryPolicy) {
      this.retryPolicy = Objects.requireNonNull(retryPolicy);
      return this;
    }

    /**
     * Sets the maximum number of retry attempts with default backoff settings.
     *
     * <p>Convenience method equivalent to:
     *
     * <pre>{@code
     * .retryPolicy(RetryPolicy.builder().maxRetries(n).build())
     * }</pre>
     *
     * <p>Note: If you provide a custom Responder via {@link #responder(Responder)}, configure retry
     * on the Responder instead.
     *
     * @param maxRetries maximum retry attempts (0 = no retries)
     * @return this builder
     */
    public @NonNull Builder maxRetries(int maxRetries) {
      this.retryPolicy = com.paragon.http.RetryPolicy.builder().maxRetries(maxRetries).build();
      return this;
    }

    /**
     * Configures the agent to produce structured output of the specified type.
     *
     * <p>Returns a {@link StructuredBuilder} that will build an {@link Agent.Structured} instead of
     * a regular {@link Agent}.
     *
     * <p>Example:
     *
     * <pre>{@code
     * var agent = Agent.builder()
     *     .name("Extractor")
     *     .structured(Person.class)
     *     .build();
     *
     * StructuredAgentResult<Person> result = agent.interact("John is 30");
     * Person person = result.output();  // Type-safe!
     * }</pre>
     *
     * @param <T> the output type
     * @param outputType the class of the structured output
     * @return a structured builder that builds Agent.Structured
     */
    public <T> @NonNull StructuredBuilder<T> structured(@NonNull Class<T> outputType) {
      return new StructuredBuilder<>(this, outputType);
    }

    /**
     * Builds the Agent instance.
     *
     * @return the configured agent
     * @throws NullPointerException if required fields are missing
     */
    public @NonNull Agent build() {
      return new Agent(this);
    }
  }

  // ===== Structured Builder =====

  /**
   * Builder for creating type-safe structured output agents.
   *
   * <p>This builder is returned from {@code Agent.builder().structured(Class)} and builds an {@code
   * Agent.Structured<T>} directly.
   *
   * <p>Example usage:
   *
   * <pre>{@code
   * var agent = Agent.builder()
   *     .name("PersonExtractor")
   *     .instructions("Extract person information.")
   *     .model("openai/gpt-4o")
   *     .responder(responder)
   *     .structured(Person.class)
   *     .build();
   *
   * StructuredAgentResult<Person> result = agent.interact("John is 30 years old");
   * Person person = result.output();  // Type-safe!
   * }</pre>
   *
   * @param <T> the output type
   */
  public static final class StructuredBuilder<T> {
    private final Builder parentBuilder;
    private final Class<T> outputType;

    private StructuredBuilder(@NonNull Builder parentBuilder, @NonNull Class<T> outputType) {
      this.parentBuilder = Objects.requireNonNull(parentBuilder);
      this.outputType = Objects.requireNonNull(outputType);
    }

    // Forward all builder methods to parent

    public @NonNull StructuredBuilder<T> name(@NonNull String name) {
      parentBuilder.name(name);
      return this;
    }

    public @NonNull StructuredBuilder<T> instructions(@NonNull String instructions) {
      parentBuilder.instructions(instructions);
      return this;
    }

    public @NonNull StructuredBuilder<T> model(@NonNull String model) {
      parentBuilder.model(model);
      return this;
    }

    public @NonNull StructuredBuilder<T> responder(@NonNull Responder responder) {
      parentBuilder.responder(responder);
      return this;
    }

    public @NonNull StructuredBuilder<T> objectMapper(@NonNull ObjectMapper objectMapper) {
      parentBuilder.objectMapper(objectMapper);
      return this;
    }

    public @NonNull StructuredBuilder<T> temperature(double temperature) {
      parentBuilder.temperature(temperature);
      return this;
    }

    public @NonNull StructuredBuilder<T> maxTurns(int maxTurns) {
      parentBuilder.maxTurns(maxTurns);
      return this;
    }

    public @NonNull StructuredBuilder<T> addTool(@NonNull FunctionTool<?> tool) {
      parentBuilder.addTool(tool);
      return this;
    }

    public @NonNull StructuredBuilder<T> addMemoryTools(@NonNull Memory memory) {
      parentBuilder.addMemoryTools(memory);
      return this;
    }

    public @NonNull StructuredBuilder<T> addHandoff(@NonNull Handoff handoff) {
      parentBuilder.addHandoff(handoff);
      return this;
    }

    public @NonNull StructuredBuilder<T> addInputGuardrail(@NonNull InputGuardrail guardrail) {
      parentBuilder.addInputGuardrail(guardrail);
      return this;
    }

    public @NonNull StructuredBuilder<T> addOutputGuardrail(@NonNull OutputGuardrail guardrail) {
      parentBuilder.addOutputGuardrail(guardrail);
      return this;
    }

    public @NonNull StructuredBuilder<T> telemetryContext(@NonNull TelemetryContext context) {
      parentBuilder.telemetryContext(context);
      return this;
    }

    public @NonNull StructuredBuilder<T> addTelemetryProcessor(
        @NonNull TelemetryProcessor processor) {
      parentBuilder.addTelemetryProcessor(processor);
      return this;
    }

    public @NonNull StructuredBuilder<T> retryPolicy(
        com.paragon.http.RetryPolicy retryPolicy) {
      parentBuilder.retryPolicy(retryPolicy);
      return this;
    }

    public @NonNull StructuredBuilder<T> maxRetries(int maxRetries) {
      parentBuilder.maxRetries(maxRetries);
      return this;
    }

    public @NonNull StructuredBuilder<T> contextManagement(
        @NonNull ContextManagementConfig config) {
      parentBuilder.contextManagement(config);
      return this;
    }

    /**
     * Builds the type-safe structured agent.
     *
     * @return the configured Structured agent
     * @throws NullPointerException if required fields are missing
     */
    public @NonNull Structured<T> build() {
      Agent agent = parentBuilder.build();
      return new Structured<>(agent, outputType);
    }
  }

  // ===== Structured Output Inner Class =====

  /**
   * Type-safe wrapper for agents with structured output.
   *
   * <p>This class uses the template method pattern to delegate all core logic to the parent Agent
   * class, adding only type-safe output parsing.
   *
   * <p>Create via builder:
   *
   * <pre>{@code
   * var agent = Agent.builder()
   *     .name("PersonExtractor")
   *     .instructions("Extract person info.")
   *     .structured(Person.class)
   *     .build();
   * }</pre>
   *
   * @param <T> the output type
   */
  public static final class Structured<T> {
    private final Agent agent;
    private final Class<T> outputType;
    private final ObjectMapper objectMapper;

    private Structured(@NonNull Agent agent, @NonNull Class<T> outputType) {
      this.agent = Objects.requireNonNull(agent);
      this.outputType = Objects.requireNonNull(outputType);
      this.objectMapper = agent.objectMapper;
    }

    /** Returns the wrapped agent's name. */
    public @NonNull String name() {
      return agent.name();
    }

    /** Returns the structured output type. */
    public @NonNull Class<T> outputType() {
      return outputType;
    }

    /**
     * Interacts with the agent and returns type-safe structured output.
     *
     * <p>All methods are async by default. For blocking, call {@code .join()}.
     *
     * @param input the user's text input
     * @return a future completing with the typed result
     */
    public @NonNull CompletableFuture<StructuredAgentResult<T>> interact(@NonNull String input) {
      return interact(input, AgentContext.create());
    }

    /**
     * Interacts with the agent with context and returns type-safe structured output.
     *
     * @param input the user's text input
     * @param context the conversation context
     * @return a future completing with the typed result
     */
    public @NonNull CompletableFuture<StructuredAgentResult<T>> interact(
        @NonNull String input, @NonNull AgentContext context) {
      Objects.requireNonNull(input, "input cannot be null");
      context.addInput(Message.user(input));
      return interact(context);
    }

    /**
     * Interacts with the agent using an existing context.
     *
     * <p>This is the core method. All other interact overloads delegate here.
     *
     * @param context the conversation context containing all history
     * @return a future completing with the typed result
     */
    public @NonNull CompletableFuture<StructuredAgentResult<T>> interact(
        @NonNull AgentContext context) {
      return agent.interact(context).thenApply(this::parseResult);
    }

    /** Parses the AgentResult into a type-safe StructuredAgentResult. */
    private @NonNull StructuredAgentResult<T> parseResult(AgentResult result) {
      if (result.isError()) {
        return StructuredAgentResult.error(
            result.error(),
            result.output(),
            result.finalResponse(),
            result.history(),
            result.toolExecutions(),
            result.turnsUsed());
      }

      try {
        T parsed = objectMapper.readValue(result.output(), outputType);
        return StructuredAgentResult.success(
            parsed,
            result.output(),
            result.finalResponse(),
            result.history(),
            result.toolExecutions(),
            result.turnsUsed());
      } catch (JsonProcessingException e) {
        return StructuredAgentResult.error(
            new IllegalStateException("Failed to parse structured output: " + e.getMessage(), e),
            result.output(),
            result.finalResponse(),
            result.history(),
            result.toolExecutions(),
            result.turnsUsed());
      }
    }
  }
}
