package com.paragon.agents;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.paragon.responses.Responder;
import com.paragon.responses.spec.*;
import com.paragon.responses.streaming.ReasoningTextDeltaEvent;
import com.paragon.responses.streaming.ResponseStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import com.paragon.responses.TraceMetadata;
import com.paragon.telemetry.processors.TraceIdGenerator;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Streaming agent interaction with full agentic loop.
 *
 * <p>Unlike simple streaming which only streams one LLM response, AgentStream runs the complete
 * agentic loop including guardrails, tool execution, and multi-turn conversations, emitting events
 * at each phase.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * agent.asStreaming().interact("Help me debug this code")
 *     .onTurnStart(turn -> System.out.println("--- Turn " + turn + " ---"))
 *     .onTextDelta(chunk -> System.out.print(chunk))
 *     .onToolCallPending((call, approve) -> {
 *         if (isSafe(call)) approve.accept(true);  // Human-in-the-loop
 *         else approve.accept(false);
 *     })
 *     .onToolExecuted(exec -> System.out.println("Tool: " + exec.toolName()))
 *     .onComplete(result -> System.out.println("\nDone!"))
 *     .onError(e -> e.printStackTrace())
 *     .start();
 * }</pre>
 *
 * @since 1.0
 */
public final class AgentStream {

  private final Agent agent;
  private final AgenticContext context;
  private final Responder responder;
  private final ObjectMapper objectMapper;

  // For resuming
  private final List<ToolExecution> initialExecutions;
  private final int startTurn;

  // For pre-failed streams
  private final @Nullable AgentResult preFailedResult;

  // Callbacks
  private Consumer<Integer> onTurnStart;
  private Consumer<String> onTextDelta;
  private Consumer<Response> onTurnComplete;
  private ToolConfirmationHandler onToolCallPending;
  private PauseHandler onPause;
  private Consumer<ToolExecution> onToolExecuted;
  private Consumer<GuardrailResult.Failed> onGuardrailFailed;
  private Consumer<Handoff> onHandoff;
  private Consumer<AgentResult> onComplete;
  private Consumer<Throwable> onError;
  private Consumer<FunctionToolCall> onClientSideTool;
  private @Nullable Consumer<Map<String, Object>> onPartialJson;
  private @Nullable Consumer<String> onReasoningDeltaHandler;

  // Settings
  private boolean autoExecuteTools = true;

  // Trace metadata (propagated via asStreaming().interact(ctx, trace))
  private @Nullable TraceMetadata trace;

  AgentStream(
      @NonNull Agent agent,
      @NonNull List<ResponseInputItem> input,
      @NonNull AgenticContext context,
      @NonNull Responder responder,
      @NonNull ObjectMapper objectMapper) {
    this(agent, input, context, responder, objectMapper, List.of(), 0);
  }

  /** Constructor for context-only (no new input). */
  AgentStream(
      @NonNull Agent agent,
      @NonNull AgenticContext context,
      @NonNull Responder responder,
      @NonNull ObjectMapper objectMapper) {
    this(agent, List.of(), context, responder, objectMapper, List.of(), 0);
  }

  /** Constructor for resuming from a saved state. */
  AgentStream(
      @NonNull Agent agent,
      @NonNull List<ResponseInputItem> input,
      @NonNull AgenticContext context,
      @NonNull Responder responder,
      @NonNull ObjectMapper objectMapper,
      @NonNull List<ToolExecution> initialExecutions,
      int startTurn) {
    this.agent = agent;
    this.context = context;
    this.responder = responder;
    this.objectMapper = objectMapper;
    this.initialExecutions = new ArrayList<>(initialExecutions);
    this.startTurn = startTurn;
    this.preFailedResult = null;

    // Add input to context immediately
    for (ResponseInputItem item : input) {
      context.addInput(item);
    }
  }

  /** Creates a pre-failed AgentStream for immediate error return (e.g., guardrail failure). */
  private AgentStream(@NonNull AgentResult failedResult) {
    this.agent = null;
    this.context = null; // Not needed for pre-failed streams
    this.responder = null;
    this.objectMapper = null;
    this.initialExecutions = List.of();
    this.startTurn = 0;
    this.preFailedResult = failedResult;
  }

  /** Creates a pre-failed stream that immediately completes with the given result. */
  static @NonNull AgentStream failed(@NonNull AgentResult failedResult) {
    return new AgentStream(failedResult);
  }

  /** Creates a pre-completed stream that immediately returns the given result. */
  static @NonNull AgentStream completed(@NonNull AgentResult completedResult) {
    return new AgentStream(completedResult);
  }

  /** Sets the trace metadata. Package-private for Agent. */
  @NonNull AgentStream withTrace(@Nullable TraceMetadata trace) {
    this.trace = trace;
    return this;
  }

  // ===== Event Registration =====

  /**
   * Called at the start of each turn in the agentic loop.
   *
   * @param handler receives the turn number (1-indexed)
   * @return this for chaining
   */
  public @NonNull AgentStream onTurnStart(@NonNull Consumer<Integer> handler) {
    this.onTurnStart = Objects.requireNonNull(handler);
    return this;
  }

  /**
   * Called for each text chunk as it streams from the LLM.
   *
   * @param handler receives text deltas
   * @return this for chaining
   */
  public @NonNull AgentStream onTextDelta(@NonNull Consumer<String> handler) {
    this.onTextDelta = Objects.requireNonNull(handler);
    return this;
  }

  /**
   * Called when each turn's LLM response is complete (before tool execution).
   *
   * @param handler receives the Response
   * @return this for chaining
   */
  public @NonNull AgentStream onTurnComplete(@NonNull Consumer<Response> handler) {
    this.onTurnComplete = Objects.requireNonNull(handler);
    return this;
  }

  /**
   * Called when a tool call is detected, BEFORE execution. Enables human-in-the-loop.
   *
   * <p>If this handler is set, tools are NOT auto-executed. The handler must call the callback with
   * {@code true} to approve or {@code false} to reject.
   *
   * <p>Example:
   *
   * <pre>{@code
   * .onToolCallPending((call, approve) -> {
   *     System.out.println("Tool: " + call.name() + " - Args: " + call.arguments());
   *     boolean userApproved = askUser("Execute this tool?");
   *     approve.accept(userApproved);
   * })
   * }</pre>
   *
   * @param handler receives (FunctionToolCall, approval callback)
   * @return this for chaining
   */
  public @NonNull AgentStream onToolCallPending(@NonNull ToolConfirmationHandler handler) {
    this.onToolCallPending = Objects.requireNonNull(handler);
    return this;
  }

  /**
   * Called when a tool call should pause for approval. Use for long-running approvals.
   *
   * <p>Unlike {@code onToolCallPending}, this handler receives a serializable {@link AgentRunState}
   * that can be persisted (e.g., to a database) and resumed later with {@code Agent.resume()}.
   *
   * <p>Example:
   *
   * <pre>{@code
   * .onPause(state -> {
   *     // Save to database for later approval
   *     saveToDatabase(state);
   *     System.out.println("Run paused. Pending: " + state.pendingToolCall().name());
   * })
   * }</pre>
   *
   * @param handler receives the pausable state
   * @return this for chaining
   */
  public @NonNull AgentStream onPause(@NonNull PauseHandler handler) {
    this.onPause = Objects.requireNonNull(handler);
    return this;
  }

  /**
   * Called after a tool is executed (whether auto or manually approved).
   *
   * @param handler receives the ToolExecution result
   * @return this for chaining
   */
  public @NonNull AgentStream onToolExecuted(@NonNull Consumer<ToolExecution> handler) {
    this.onToolExecuted = Objects.requireNonNull(handler);
    return this;
  }

  /**
   * Called if a guardrail check fails.
   *
   * @param handler receives the failed guardrail result
   * @return this for chaining
   */
  public @NonNull AgentStream onGuardrailFailed(@NonNull Consumer<GuardrailResult.Failed> handler) {
    this.onGuardrailFailed = Objects.requireNonNull(handler);
    return this;
  }

  /**
   * Called when a handoff to another agent is triggered.
   *
   * @param handler receives the Handoff
   * @return this for chaining
   */
  public @NonNull AgentStream onHandoff(@NonNull Consumer<Handoff> handler) {
    this.onHandoff = Objects.requireNonNull(handler);
    return this;
  }

  /**
   * Called when the agentic loop completes successfully.
   *
   * @param handler receives the final AgentResult
   * @return this for chaining
   */
  public @NonNull AgentStream onComplete(@NonNull Consumer<AgentResult> handler) {
    this.onComplete = Objects.requireNonNull(handler);
    return this;
  }

  /**
   * Called if an error occurs during the agentic loop.
   *
   * @param handler receives the error
   * @return this for chaining
   */
  public @NonNull AgentStream onError(@NonNull Consumer<Throwable> handler) {
    this.onError = Objects.requireNonNull(handler);
    return this;
  }

  /**
   * Called when a client-side only tool ({@code stopsLoop = true}) is detected.
   *
   * <p>The tool call is NOT persisted to history and NOT executed. The loop exits immediately
   * after this callback fires, followed by {@code onComplete} with the {@code clientSideTool}
   * result.
   *
   * @param handler receives the FunctionToolCall that triggered the exit
   * @return this for chaining
   */
  public @NonNull AgentStream onClientSideTool(@NonNull Consumer<FunctionToolCall> handler) {
    this.onClientSideTool = Objects.requireNonNull(handler);
    return this;
  }

  /**
   * Called for each text delta with partially-parsed JSON fields (map form). Only fires when the
   * agent has {@code outputType} configured. Enables real-time UI updates during structured output
   * streaming.
   *
   * @param handler receives partially-parsed JSON as a Map
   * @return this for chaining
   */
  public @NonNull AgentStream onPartialJson(@NonNull Consumer<Map<String, Object>> handler) {
    this.onPartialJson = Objects.requireNonNull(handler);
    return this;
  }

  /**
   * Called for each reasoning token delta streamed by the model (e.g., o-series / extended
   * thinking models). Only fires when the model emits {@code response.reasoning_text.delta} SSE
   * events.
   *
   * @param handler receives reasoning token chunks
   * @return this for chaining
   */
  public @NonNull AgentStream onReasoningDelta(@NonNull Consumer<String> handler) {
    this.onReasoningDeltaHandler = Objects.requireNonNull(handler);
    return this;
  }

  // ===== Execution =====

  /**
   * Starts the streaming agentic loop on a virtual thread. Non-blocking — returns immediately.
   *
   * <p>Callbacks ({@code onComplete}, {@code onError}, etc.) fire asynchronously on the virtual
   * thread. Use {@link #startBlocking()} if you need to wait for the result inline.
   */
  public void start() {
    Thread.startVirtualThread(this::runAgenticLoop);
  }

  /**
   * Starts the streaming agentic loop synchronously (blocking).
   *
   * @return the final AgentResult
   */
  public @NonNull AgentResult startBlocking() {
    return runAgenticLoop();
  }

  private AgentResult runAgenticLoop() {
    // Check for pre-failed result (e.g., guardrail failure from input guardrail check)
    if (preFailedResult != null) {
      emit(onComplete, preFailedResult);
      return preFailedResult;
    }

    // Issue 4: fire beforeRun hooks
    agent.hookRegistry().fireBeforeRun(context);

    try {

      // 3. Execute agentic loop
      List<ToolExecution> allToolExecutions = new ArrayList<>(initialExecutions);
      Response lastResponse = null;

      while (context.incrementTurn() <= agent.maxTurns()) {
        int turn = context.getTurnCount();
        emit(onTurnStart, turn);

        // 3a. Stream LLM response
        lastResponse = streamLLMResponse(context);
        emit(onTurnComplete, lastResponse);

        // 3b. Add assistant response to context
        if (lastResponse.output() != null) {
          for (ResponseOutput outputItem : lastResponse.output()) {
            if (outputItem instanceof OutputMessage outMsg) {
              // Convert to simple string-content AssistantMessage for API compatibility
              String text = outMsg.toString();
              if (!text.isBlank()) {
                context.addMessage(Message.assistant(text));
              }
            } else if (outputItem instanceof Message msg) {
              context.addMessage(msg);
            } else if (outputItem instanceof FunctionToolCall ftc) {
              // stopsLoop tools must NOT be persisted to history (they are UI signals, not API items)
              FunctionTool<?> maybeTool = agent.toolStore().get(ftc.name());
              if (maybeTool == null || !maybeTool.stopsLoop()) {
                // FunctionToolCall must be in history before the tool output (required by API)
                context.addInput(ftc);
              }
            }
          }
        }

        // 3c. Check for handoffs — execute the target agent (Issue 2 fix)
        List<FunctionToolCall> allCalls = lastResponse.functionToolCalls();
        for (Handoff handoff : agent.handoffs()) {
          boolean hasHandoffCall =
              allCalls.stream().anyMatch(call -> handoff.name().equals(call.name()));
          if (hasHandoffCall) {
            emit(onHandoff, handoff);

            // Close the tool exchange: add a synthetic output for the handoff tool call.
            // Without it the history contains an assistant message with tool_calls that has no
            // corresponding tool result, which the OpenAI API rejects as invalid.
            for (FunctionToolCall call : allCalls) {
              if (handoff.name().equals(call.name())) {
                context.addToolResult(
                    FunctionToolCallOutput.success(
                        call.callId(),
                        "Transferring to " + handoff.targetAgent().name() + "."));
                break;
              }
            }

            String childSpanId = TraceIdGenerator.generateSpanId();
            AgenticContext childContext = context.fork(childSpanId);

            // Inject handoff awareness (DEVELOPER priority — highest precedence)
            String awareness = handoff.buildAwarenessMessage(agent.name());
            if (awareness != null && !awareness.isEmpty()) {
              childContext.addInput(Message.developer(awareness));
            }

            extractHandoffMessage(allCalls, handoff.name())
                .ifPresent(msg -> childContext.addInput(Message.user(msg)));

            // Use the child's streaming path so text deltas and tool callbacks
            // propagate back to the caller — avoids the blocking interact() call.
            AgentStream childStream =
                handoff.targetAgent().asStreaming().interact(childContext, trace);
            if (onTextDelta != null)             childStream.onTextDelta(onTextDelta);
            if (onTurnStart != null)             childStream.onTurnStart(onTurnStart);
            if (onTurnComplete != null)          childStream.onTurnComplete(onTurnComplete);
            if (onToolExecuted != null)          childStream.onToolExecuted(onToolExecuted);
            if (onToolCallPending != null)       childStream.onToolCallPending(onToolCallPending);
            if (onPause != null)                 childStream.onPause(onPause);
            if (onGuardrailFailed != null)       childStream.onGuardrailFailed(onGuardrailFailed);
            if (onHandoff != null)               childStream.onHandoff(onHandoff);
            if (onClientSideTool != null)        childStream.onClientSideTool(onClientSideTool);
            if (onPartialJson != null)           childStream.onPartialJson(onPartialJson);
            if (onReasoningDeltaHandler != null) childStream.onReasoningDelta(onReasoningDeltaHandler);
            // onComplete / onError are intentionally NOT forwarded — the parent fires those
            AgentResult innerResult = childStream.startBlocking();
            AgentResult handoffResult =
                AgentResult.handoff(handoff.targetAgent(), innerResult, context);
            agent.hookRegistry().fireAfterRun(handoffResult, context);
            emit(onComplete, handoffResult);
            return handoffResult;
          }
        }

        // 3d. Check for tool calls
        List<FunctionToolCall> toolCalls = lastResponse.functionToolCalls();
        if (toolCalls.isEmpty()) {
          break; // No tool calls, loop complete
        }

        // 3e. Execute tools (with optional per-tool human-in-the-loop)
        for (FunctionToolCall call : toolCalls) {
          if (call == null) continue;

          // stopsLoop: client-side only tool — exit immediately, call not persisted
          {
            FunctionTool<?> maybeTool = agent.toolStore().get(call.name());
            if (maybeTool != null && maybeTool.stopsLoop()) {
              AgentResult clientResult =
                  AgentResult.clientSideTool(call, context, context.getTurnCount());
              emit(onClientSideTool, call);
              agent.hookRegistry().fireAfterRun(clientResult, context);
              emit(onComplete, clientResult);
              return clientResult;
            }
          }

          // Check if this specific tool requires confirmation
          FunctionTool<?> tool = agent.toolStore().get(call.name());
          boolean toolRequiresConfirmation = tool != null && tool.requiresConfirmation();

          boolean shouldExecute = true;

          // Only trigger HITL callbacks for tools that require confirmation
          if (toolRequiresConfirmation) {
            // Handle synchronous approval callback
            if (onToolCallPending != null) {
              AtomicBoolean approved = new AtomicBoolean(false);
              onToolCallPending.handle(call, approved::set);
              shouldExecute = approved.get();
            }

            // Handle async pause for long-running approvals
            if (onPause != null) {
              AgentRunState pauseState =
                  AgentRunState.pendingApproval(
                      agent.name(),
                      context,
                      call,
                      lastResponse,
                      allToolExecutions,
                      context.getTurnCount());
              onPause.onPause(pauseState);
              AgentResult pausedResult = AgentResult.paused(pauseState, context);
              agent.hookRegistry().fireAfterRun(pausedResult, context);
              return pausedResult;
            }
          }

          if (shouldExecute) {
            // Issue 4: fire tool hooks
            agent.hookRegistry().fireBeforeToolCall(call, context);
            ToolExecution exec = executeToolCall(call);
            agent.hookRegistry().fireAfterToolCall(call, exec, context);
            allToolExecutions.add(exec);
            emit(onToolExecuted, exec);
            context.addToolResult(exec.output());
          } else {
            // Tool rejected - add rejection message
            FunctionToolCallOutput rejectedOutput =
                FunctionToolCallOutput.error(call.callId(), "Tool execution was rejected by user");
            context.addToolResult(rejectedOutput);
          }
        }
      }

      // 4. Validate output guardrails
      String output = lastResponse != null ? lastResponse.outputText() : "";
      for (OutputGuardrail guardrail : agent.outputGuardrails()) {
        GuardrailResult result = guardrail.validate(output, context);
        if (result.isFailed()) {
          GuardrailResult.Failed failed = (GuardrailResult.Failed) result;
          emit(onGuardrailFailed, failed);
          AgentResult errorResult =
              AgentResult.guardrailFailed("Output guardrail: " + failed.reason(), context);
          agent.hookRegistry().fireAfterRun(errorResult, context);
          emit(onComplete, errorResult);
          return errorResult;
        }
      }

      // 5. Success
      AgentResult successResult =
          AgentResult.success(
              output, lastResponse, context, allToolExecutions, context.getTurnCount());
      agent.hookRegistry().fireAfterRun(successResult, context);
      emit(onComplete, successResult);
      return successResult;

    } catch (Exception e) {
      emit(onError, e);
      AgentResult errorResult = AgentResult.error(e, context, context.getTurnCount());
      agent.hookRegistry().fireAfterRun(errorResult, context);
      emit(onComplete, errorResult);
      return errorResult;
    }
  }

  @SuppressWarnings("unchecked")
  private Response streamLLMResponse(AgenticContext context) {
    CreateResponsePayload payload = agent.buildPayloadInternal(context);

    if (onPartialJson != null) {
      // True structured streaming path — requires outputType to be configured on the agent
      Class<?> outputTypeClass = agent.outputType();
      if (outputTypeClass == null) {
        throw new IllegalStateException(
            "onPartialJson requires the agent to have outputType() configured");
      }
      CreateResponsePayload.StructuredStreaming<Object> structured =
          new CreateResponsePayload.StructuredStreaming<>(payload, (Class<Object>) outputTypeClass);
      ResponseStream<Object> stream = responder.respond(structured);
      if (onTextDelta != null) {
        stream.onTextDelta(onTextDelta);
      }
      stream.onPartialJson(onPartialJson);
      return stream.startBlocking();
    }

    // Issue 1: when onTextDelta or onReasoningDelta is registered, use true SSE so deltas arrive
    // incrementally. Fallback to blocking if the provider's SSE format produces no
    // ResponseCompletedEvent (e.g., OpenRouter sends Chat-Completions-format chunks instead of
    // Responses-API events).
    if (onTextDelta != null || onReasoningDeltaHandler != null) {
      CreateResponsePayload.Streaming streamingPayload = new CreateResponsePayload.Streaming(payload);
      ResponseStream<Void> responseStream = responder.respond(streamingPayload);
      if (onTextDelta != null) {
        responseStream.onTextDelta(onTextDelta);
      }
      if (onReasoningDeltaHandler != null) {
        Consumer<String> handler = onReasoningDeltaHandler;
        responseStream.onEvent(event -> {
          if (event instanceof ReasoningTextDeltaEvent rde) {
            handler.accept(rde.delta());
          }
        });
      }
      Response sseResponse = responseStream.startBlocking();
      if (sseResponse != null) {
        return sseResponse;
      }
      // Provider didn't emit a response.completed event — fallback to blocking request.
      // onTextDelta will still fire once via the blocking path below.
    }

    Response response = responder.respond(payload);
    if (onTextDelta != null && response.outputText() != null) {
      onTextDelta.accept(response.outputText());
    }
    return response;
  }

  private ToolExecution executeToolCall(FunctionToolCall call) {
    Instant start = Instant.now();
    try {
      FunctionToolCallOutput output = agent.toolStore().execute(call);
      Duration duration = Duration.between(start, Instant.now());
      return new ToolExecution(call.name(), call.callId(), call.arguments(), output, duration);
    } catch (JacksonException e) {
      Duration duration = Duration.between(start, Instant.now());
      FunctionToolCallOutput errorOutput =
          FunctionToolCallOutput.error(
              call.callId(), "Failed to parse arguments: " + e.getMessage());
      return new ToolExecution(call.name(), call.callId(), call.arguments(), errorOutput, duration);
    }
  }

  private String extractTextFromInput(List<ResponseInputItem> input) {
    StringBuilder sb = new StringBuilder();
    for (ResponseInputItem item : input) {
      if (item instanceof Message msg) {
        sb.append(msg.outputText());
      }
    }
    return sb.toString();
  }

  private Optional<String> extractHandoffMessage(
      List<FunctionToolCall> toolCalls, String handoffName) {
    for (FunctionToolCall call : toolCalls) {
      if (handoffName.equals(call.name())) {
        try {
          Handoff.HandoffParams params =
              objectMapper.readValue(call.arguments(), Handoff.HandoffParams.class);
          return Optional.ofNullable(params.message());
        } catch (JacksonException e) {
          return Optional.empty();
        }
      }
    }
    return Optional.empty();
  }

  private <T> void emit(@Nullable Consumer<T> handler, T value) {
    if (handler != null && value != null) {
      handler.accept(value);
    }
  }

  // ===== Functional Interfaces =====

  /** Handler for tool call confirmation (human-in-the-loop). */
  @FunctionalInterface
  public interface ToolConfirmationHandler {
    /**
     * Called when a tool call is pending.
     *
     * @param call the pending tool call
     * @param approvalCallback call with true to execute, false to reject
     */
    void handle(@NonNull FunctionToolCall call, @NonNull Consumer<Boolean> approvalCallback);
  }

  /** Handler for pausing agent runs (for long-running approvals). */
  @FunctionalInterface
  public interface PauseHandler {
    /**
     * Called when an agent run should pause for tool approval.
     *
     * <p>The state is serializable and can be persisted to a database. Resume later with {@code
     * Agent.resume(state)}.
     *
     * @param state the serializable run state to save
     */
    void onPause(@NonNull AgentRunState state);
  }
}
