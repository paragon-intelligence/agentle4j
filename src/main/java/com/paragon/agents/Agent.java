package com.paragon.agents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.agents.context.ContextManagementConfig;
import com.paragon.agents.toolplan.ToolPlanTool;
import com.paragon.agents.toolsearch.ToolRegistry;
import com.paragon.prompts.Prompt;
import com.paragon.responses.Responder;
import com.paragon.responses.TraceMetadata;
import com.paragon.responses.exception.AgentExecutionException;
import com.paragon.responses.exception.GuardrailException;
import com.paragon.responses.exception.ToolExecutionException;
import com.paragon.responses.spec.*;
import com.paragon.skills.Skill;
import com.paragon.skills.SkillProvider;
import com.paragon.skills.SkillStore;
import com.paragon.telemetry.TelemetryContext;
import com.paragon.telemetry.events.AgentFailedEvent;
import com.paragon.telemetry.processors.ProcessorRegistry;
import com.paragon.telemetry.processors.TelemetryProcessor;
import com.paragon.telemetry.processors.TraceIdGenerator;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A stateful AI agent that can perceive, plan, and act using tools.
 *
 * <p>Unlike {@link Responder} which is stateless, Agent maintains:
 *
 * <ul>
 *   <li>Conversation history (via {@link AgenticContext})
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
 * // Blocking call - cheap with virtual threads
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
 * @see AgenticContext
 * @see AgentResult
 * @see Responder
 * @since 1.0
 */
public final class Agent implements Serializable, Interactable {

  // ===== Configuration (Immutable) =====
  private final @NonNull String name;
  private final @NonNull Prompt instructions;
  private final @NonNull String model;
  private final @NonNull List<FunctionTool<?>> tools;
  private final @NonNull List<Handoff> handoffs;
  private final @NonNull List<InputGuardrail> inputGuardrails;
  private final @NonNull List<OutputGuardrail> outputGuardrails;
  private final int maxTurns;
  private final @Nullable Class<?> outputType;
  private final @Nullable Double temperature;
  private final @NonNull TelemetryContext telemetryContext;
  private final @Nullable TraceMetadata traceMetadata;

  // ===== Tool Search =====
  private final @Nullable ToolRegistry toolRegistry;

  // ===== Context Management =====
  private final @Nullable ContextManagementConfig contextManagementConfig;

  // ===== Runtime Dependencies =====
  private final transient @NonNull Responder responder;
  private final transient @NonNull FunctionToolStore toolStore;
  private final transient @NonNull ObjectMapper objectMapper;
  private final transient @NonNull ProcessorRegistry telemetryProcessors;

  private Agent(Builder builder) {
    this.name = Objects.requireNonNull(builder.name, "name is required");
    this.model = Objects.requireNonNull(builder.model, "model is required");
    this.responder = Objects.requireNonNull(builder.responder, "responder is required");
    this.maxTurns = builder.maxTurns > 0 ? builder.maxTurns : 10;
    this.outputType = builder.outputType;
    this.temperature = builder.temperature;
    this.telemetryContext =
        builder.telemetryContext != null ? builder.telemetryContext : TelemetryContext.empty();
    this.traceMetadata = builder.traceMetadata;
    this.telemetryProcessors =
        builder.telemetryProcessors.isEmpty()
            ? ProcessorRegistry.empty()
            : ProcessorRegistry.of(builder.telemetryProcessors);

    // Context management
    this.contextManagementConfig = builder.contextManagementConfig;

    // Tool search registry
    this.toolRegistry = builder.toolRegistry;

    // Build augmented instructions with skills and tool planning
    Prompt baseInstructions =
        Objects.requireNonNull(builder.instructions, "instructions are required");
    StringBuilder instructionAugmentation = new StringBuilder(baseInstructions.text());
    boolean augmented = false;

    if (!builder.pendingSkills.isEmpty()) {
      instructionAugmentation.append("\n\n# Skills\n");
      instructionAugmentation.append(
          "You have the following skills available. Apply them when relevant:\n");
      for (Skill skill : builder.pendingSkills) {
        instructionAugmentation.append(skill.toPromptSection());
      }
      augmented = true;
    }

    if (builder.toolPlanningEnabled && !builder.tools.isEmpty()) {
      instructionAugmentation.append(
          """

          \n# Tool Planning
          You have access to an `execute_tool_plan` tool that lets you batch multiple tool \
          calls into a single execution plan. Use it when:
          - Multiple tools need to be called and some depend on others' results
          - You want to run independent tool calls in parallel for efficiency
          - You want to reduce context usage by processing intermediate results locally

          Plan format:
          - Each step has an `id` (unique identifier), `tool` (function name), and `arguments` \
          (a JSON string of the tool's arguments)
          - Use `"$ref:step_id"` in arguments to reference the full output of a previous step
          - Use `"$ref:step_id.field_name"` to extract a specific JSON field from a step's output
          - List which step IDs' results you need in `output_steps` (omit for all results)
          - Steps with no `$ref` dependencies execute in parallel automatically
          """);
      augmented = true;
    }

    this.instructions = augmented ? Prompt.of(instructionAugmentation.toString()) : baseInstructions;

    // Build tool store and tools list
    this.objectMapper = builder.objectMapper != null ? builder.objectMapper : new ObjectMapper();
    this.toolStore = FunctionToolStore.create(objectMapper);

    // Copy tools — if a tool registry is present, include all registry tools
    List<FunctionTool<?>> allTools;
    if (toolRegistry != null) {
      allTools = new ArrayList<>(builder.tools);
      for (FunctionTool<?> regTool : toolRegistry.allTools()) {
        if (!allTools.contains(regTool)) {
          allTools.add(regTool);
        }
      }
    } else {
      allTools = new ArrayList<>(builder.tools);
    }

    // Register all user tools in the store
    for (FunctionTool<?> tool : allTools) {
      toolStore.add(tool);
    }

    this.handoffs = List.copyOf(builder.handoffs);
    this.inputGuardrails = List.copyOf(builder.inputGuardrails);
    this.outputGuardrails = List.copyOf(builder.outputGuardrails);

    // Add handoff tools
    for (Handoff handoff : handoffs) {
      toolStore.add(handoff.asTool());
    }

    // Register tool plan meta-tool if enabled (must be after all tools are in the store)
    if (builder.toolPlanningEnabled && !allTools.isEmpty()) {
      ToolPlanTool planTool = new ToolPlanTool(this.toolStore);
      this.toolStore.add(planTool);
      allTools.add(planTool);
    }

    this.tools = List.copyOf(allTools);
  }

  // ===== Loop Callbacks Interface (for code reuse) =====

  /**
   * Creates a new Agent builder.
   *
   * @return a new builder instance
   */
  public static @NonNull Builder builder() {
    return new Builder();
  }

  /**
   * Returns the agent's name.
   *
   * @return the name
   */
  public @NonNull String name() {
    return name;
  }

  @Override
  public @NonNull InteractableBlueprint toBlueprint() {
    // Extract tool class names (skip internal tools like InteractableSubAgentTool)
    List<String> toolClassNames =
        tools.stream()
            .filter(t -> !t.getClass().isAnonymousClass() && !t.getClass().isSynthetic())
            .filter(
                t -> {
                  try {
                    t.getClass().getDeclaredConstructor(); // must have no-arg constructor
                    return true;
                  } catch (NoSuchMethodException e) {
                    return false;
                  }
                })
            .map(t -> t.getClass().getName())
            .toList();

    // Extract guardrail references
    List<InteractableBlueprint.GuardrailReference> inputRefs =
        inputGuardrails.stream().map(InteractableBlueprint.GuardrailReference::fromInput).toList();
    List<InteractableBlueprint.GuardrailReference> outputRefs =
        outputGuardrails.stream()
            .map(InteractableBlueprint.GuardrailReference::fromOutput)
            .toList();

    // Extract handoff descriptors
    List<InteractableBlueprint.HandoffDescriptor> handoffDescs =
        handoffs.stream().map(InteractableBlueprint.HandoffDescriptor::from).toList();

    // Extract context management
    InteractableBlueprint.ContextBlueprint ctxBlueprint = null;
    if (contextManagementConfig != null) {
      var strategy = contextManagementConfig.strategy();
      String strategyType = "sliding";
      Boolean preserveDevMsgs = null;
      String summModel = null;
      Integer keepRecent = null;
      String summPrompt = null;

      if (strategy instanceof com.paragon.agents.context.SlidingWindowStrategy sliding) {
        preserveDevMsgs = sliding.preservesDeveloperMessage();
      } else if (strategy
          instanceof com.paragon.agents.context.SummarizationStrategy summarization) {
        strategyType = "summarization";
        summModel = summarization.model();
        keepRecent = summarization.keepRecentMessages();
      }

      String tokenCounterClass = null;
      if (contextManagementConfig.tokenCounter() != null) {
        tokenCounterClass = contextManagementConfig.tokenCounter().getClass().getName();
      }

      ctxBlueprint =
          new InteractableBlueprint.ContextBlueprint(
              strategyType,
              preserveDevMsgs,
              summModel,
              keepRecent,
              summPrompt,
              contextManagementConfig.maxTokens(),
              tokenCounterClass);
    }

    return new InteractableBlueprint.AgentBlueprint(
        name,
        model,
        instructions.text(),
        maxTurns,
        temperature,
        outputType != null ? outputType.getName() : null,
        traceMetadata,
        InteractableBlueprint.ResponderBlueprint.from(responder),
        toolClassNames,
        handoffDescs,
        inputRefs,
        outputRefs,
        ctxBlueprint);
  }

  /**
   * Returns the agent's instructions (system prompt).
   *
   * @return the instructions
   */
  public @NonNull Prompt instructions() {
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
   * Returns the responder used by this agent.
   *
   * @return the responder
   */
  public @NonNull Responder responder() {
    return responder;
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

  /**
   * Returns the tool registry, if one is configured.
   *
   * @return the tool registry or null if not using tool search
   */
  public @Nullable ToolRegistry toolRegistry() {
    return toolRegistry;
  }

  /** Returns the tool store. Package-private for AgentStream. */
  @NonNull FunctionToolStore toolStore() {
    return toolStore;
  }

  /** Builds a payload from context. Package-private for AgentStream. */
  @NonNull CreateResponsePayload buildPayloadInternal(@NonNull AgenticContext context) {
    return buildPayload(context);
  }

  // ===== Interact Methods (All Async) =====

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

    AgenticContext context = AgenticContext.create();
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
   * Interacts with the agent with streaming using an existing context and trace metadata.
   *
   * @param context the conversation context containing history
   * @param trace optional trace metadata (overrides agent-level configuration)
   * @return an AgentStream for processing streaming events
   */
  @Override
  public @NonNull AgentStream interactStream(
      @NonNull AgenticContext context, @Nullable TraceMetadata trace) {
    // Trace metadata for streaming will be handled when AgentStream is updated
    // For now, just delegate to AgentStream constructor
    return new AgentStream(this, List.of(), context, responder, objectMapper);
  }

  // ===== Streaming API =====

  /**
   * Interacts with the agent using streaming with an existing context.
   *
   * <p>This is the core streaming method. All other streaming overloads ultimately delegate here
   * after adding their input to the context.
   *
   * @param context the conversation context containing all history
   * @return an AgentStream for processing streaming events
   */
  public @NonNull AgentStream interactStream(@NonNull AgenticContext context) {
    Objects.requireNonNull(context, "context cannot be null");
    return new AgentStream(this, context, responder, objectMapper);
  }

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

  // ===== Resume Methods =====

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

  /**
   * Core interact method. Executes the agentic loop.
   *
   * <p>This is the primary execution method. All other interact overloads ultimately delegate here
   * after adding their input to the context.
   *
   * <p>This method blocks until the interaction completes. When running on virtual threads,
   * blocking is cheap and does not consume platform threads.
   *
   * @param context the conversation context containing all history
   * @param trace optional trace metadata (overrides agent-level configuration)
   * @return the agent's result
   */
  @Override
  public @NonNull AgentResult interact(
      @NonNull AgenticContext context, @Nullable TraceMetadata trace) {
    // Merge trace: method parameter > instance field
    TraceMetadata finalTrace = trace != null ? trace : this.traceMetadata;
    return interactBlocking(context, null, finalTrace);
  }

  // ===== Core Interaction Methods =====

  /**
   * Core blocking interact method with callbacks. Package-private for AgentStream.
   *
   * @param context the conversation context containing all history
   * @param callbacks optional loop callbacks for streaming/events
   * @param trace optional trace metadata to include in API requests
   * @return the agent result
   */
  @NonNull AgentResult interactBlocking(
      @NonNull AgenticContext context,
      @Nullable LoopCallbacks callbacks,
      @Nullable TraceMetadata trace) {
    Objects.requireNonNull(context, "context cannot be null");

    context.ensureTraceContext();

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

    // Execute agentic loop with trace
    return executeAgenticLoop(context, new ArrayList<>(), callbacks, "", trace);
  }

  /** Unified agentic loop. Shared by interact, resume, and AgentStream. */
  private AgentResult executeAgenticLoop(
      AgenticContext context,
      List<ToolExecution> initialExecutions,
      @Nullable LoopCallbacks callbacks,
      String fallbackHandoffText,
      @Nullable TraceMetadata trace) {

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
          lastResponse = responder.respond(payload, telemetryCtx, trace);
          // lastResponseId = lastResponse.id();
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
        Optional<Handoff> handoffOpt = detectHandoff(toolCalls);
        if (handoffOpt.isPresent()) {
          Handoff handoff = handoffOpt.get();
          if (callbacks != null) callbacks.onHandoff(handoff);
          String handoffMessage =
              extractHandoffMessage(toolCalls, handoff.name()).orElse(fallbackHandoffText);

          // Fork context with new parent span for child agent
          String childSpanId = TraceIdGenerator.generateSpanId();
          AgenticContext childContext = context.fork(childSpanId);

          // Add handoff message to child context
          if (handoffMessage != null && !handoffMessage.isEmpty()) {
            childContext.addInput(Message.user(handoffMessage));
          }

          try {
            AgentResult innerResult = handoff.targetAgent().interact(childContext);
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
  private void broadcastFailedEvent(Exception exception, AgenticContext context) {
    if (telemetryProcessors != null) {
      String sessionId =
          context.requestId().orElseGet(() -> java.util.UUID.randomUUID().toString());
      AgentFailedEvent event =
          AgentFailedEvent.from(
              name,
              context.getTurnCount(),
              exception,
              sessionId,
              context.parentTraceId().orElse(null),
              context.parentSpanId().orElse(null),
              null);
      telemetryProcessors.broadcast(event);
    }
  }

  /** Continues the agentic loop from a saved state (used by resume). */
  private AgentResult continueAgenticLoop(
      AgenticContext context,
      Response lastResponse,
      List<ToolExecution> previousExecutions,
      int startTurn) {
    // Delegate to unified loop
    return executeAgenticLoop(context, previousExecutions, null, "", null);
  }

  private CreateResponsePayload buildPayload(AgenticContext context) {
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
        CreateResponsePayload.builder().model(model).instructions(instructions.text()).input(input);

    // Add tools — use registry to dynamically select, or include all
    if (toolRegistry != null) {
      String inputText = extractInputTextForToolSearch(input);
      List<FunctionTool<?>> resolvedTools = toolRegistry.resolveTools(inputText);
      for (FunctionTool<?> tool : resolvedTools) {
        builder.addTool(tool);
      }
    } else {
      for (FunctionTool<?> tool : tools) {
        builder.addTool(tool);
      }
    }
    // Add handoff tools (always included, not searchable)
    for (Handoff handoff : handoffs) {
      builder.addTool(handoff.asTool());
    }

    // Add temperature if set
    if (temperature != null) {
      builder.temperature(temperature);
    }

    return builder.build();
  }

  /**
   * Extracts text from the conversation input for tool search.
   * Uses the most recent user message as the search query.
   */
  private String extractInputTextForToolSearch(List<ResponseInputItem> input) {
    // Walk backwards to find the most recent user message
    for (int i = input.size() - 1; i >= 0; i--) {
      ResponseInputItem item = input.get(i);
      if (item instanceof Message msg && MessageRole.USER.equals(msg.role())) {
        return msg.getTextContent();
      }
    }
    return "";
  }

  // ===== Private Helper Methods =====

  /** Builds a TelemetryContext from AgentContext for trace correlation. */
  private TelemetryContext buildTelemetryContext(AgenticContext context) {
    TelemetryContext.Builder builder =
        TelemetryContext.builder().traceName(name + ".turn-" + context.getTurnCount());

    context.parentTraceId().ifPresent(builder::parentTraceId);
    context.parentSpanId().ifPresent(builder::parentSpanId);
    context.requestId().ifPresent(builder::requestId);

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

  private Optional<Handoff> detectHandoff(List<FunctionToolCall> toolCalls) {
    for (FunctionToolCall call : toolCalls) {
      for (Handoff handoff : handoffs) {
        if (handoff.name().equals(call.name())) {
          return Optional.of(handoff);
        }
      }
    }
    return Optional.empty();
  }

  private Optional<String> extractHandoffMessage(
      List<FunctionToolCall> toolCalls, String handoffName) {
    for (FunctionToolCall call : toolCalls) {
      if (handoffName.equals(call.name())) {
        try {
          Handoff.HandoffParams params =
              objectMapper.readValue(call.arguments(), Handoff.HandoffParams.class);
          return Optional.ofNullable(params.message());
        } catch (JsonProcessingException e) {
          return Optional.empty();
        }
      }
    }
    return Optional.empty();
  }

  /**
   * Executes a single tool with proper error handling and telemetry. Wraps errors in
   * ToolExecutionException for better diagnostics.
   */
  private ToolExecution executeSingleToolWithErrorHandling(
      FunctionToolCall call, AgenticContext context) {
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
      // Execute tool with context using virtual-thread-safe ScopedValue
      FunctionToolCallOutput output =
          context.callAsCurrent(
              () -> {
                try {
                  return toolStore.execute(call);
                } catch (JsonProcessingException e) {
                  throw new RuntimeException(e); // Will be handled in outer catch
                }
              });

      Duration duration = Duration.between(start, Instant.now());
      return new ToolExecution(call.name(), call.callId(), call.arguments(), output, duration);
    } catch (RuntimeException e) {
      Duration duration = Duration.between(start, Instant.now());

      // Unwrap if it's our wrapped JsonProcessingException
      if (e.getCause() instanceof JsonProcessingException jpe) {
        ToolExecutionException toolEx =
            new ToolExecutionException(
                call.name(),
                call.callId(),
                call.arguments(),
                "Failed to parse tool arguments: " + jpe.getMessage(),
                jpe);
        broadcastFailedEvent(toolEx, context);
        FunctionToolCallOutput errorOutput =
            FunctionToolCallOutput.error(
                call.callId(), "Tool execution failed: " + jpe.getMessage());
        return new ToolExecution(
            call.name(), call.callId(), call.arguments(), errorOutput, duration);
      }

      // Other exceptions
      ToolExecutionException toolEx =
          new ToolExecutionException(
              call.name(),
              call.callId(),
              call.arguments(),
              "Tool execution failed: " + e.getMessage(),
              e);
      broadcastFailedEvent(toolEx, context);
      FunctionToolCallOutput errorOutput =
          FunctionToolCallOutput.error(call.callId(), "Tool execution failed: " + e.getMessage());
      return new ToolExecution(call.name(), call.callId(), call.arguments(), errorOutput, duration);
    } catch (Exception e) {
      Duration duration = Duration.between(start, Instant.now());
      ToolExecutionException toolEx =
          new ToolExecutionException(
              call.name(),
              call.callId(),
              call.arguments(),
              "Tool execution failed: " + e.getMessage(),
              e);
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
        AgenticContext context) {
      return null;
    }
  }

  // ===== Builder =====

  /** Builder for creating Agent instances. */
  public static final class Builder {
    private final List<FunctionTool<?>> tools = new ArrayList<>();
    private final List<Handoff> handoffs = new ArrayList<>();
    private final List<InputGuardrail> inputGuardrails = new ArrayList<>();
    private final List<OutputGuardrail> outputGuardrails = new ArrayList<>();
    private final List<TelemetryProcessor> telemetryProcessors = new ArrayList<>();
    // Skills to merge into agent's prompt
    private final List<Skill> pendingSkills = new ArrayList<>();
    private @Nullable String name;
    private @Nullable Prompt instructions;
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
    // Context management
    private @Nullable ContextManagementConfig contextManagementConfig;
    // Trace metadata
    private @Nullable TraceMetadata traceMetadata;
    // Tool search registry
    private @Nullable ToolRegistry toolRegistry;
    // Tool planning
    private boolean toolPlanningEnabled = false;

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
    public @NonNull Builder instructions(@NonNull Prompt instructions) {
      this.instructions = Objects.requireNonNull(instructions);
      return this;
    }

    /**
     * Sets the agent's instructions/system prompt (required).
     *
     * @param instructions the system prompt
     * @return this builder
     */
    public @NonNull Builder instructions(@NonNull String instructions) {
      this.instructions = Prompt.of(Objects.requireNonNull(instructions));
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
     * Sets a {@link com.paragon.agents.toolsearch.ToolRegistry} for dynamic tool selection.
     *
     * <p>When a registry is configured, the agent dynamically selects which tools to include in
     * each API call based on the user's input, using the registry's search strategy. This is
     * useful when the agent has many tools and sending all of them would waste context tokens.
     *
     * <p>Tools configured via {@link #addTool} are always included (eager). The registry's
     * deferred tools are only included when the search strategy deems them relevant.
     *
     * @param toolRegistry the tool registry
     * @return this builder
     * @see com.paragon.agents.toolsearch.ToolRegistry
     * @see com.paragon.agents.toolsearch.ToolSearchStrategy
     */
    public @NonNull Builder toolRegistry(
        @NonNull ToolRegistry toolRegistry) {
      this.toolRegistry = Objects.requireNonNull(toolRegistry, "toolRegistry cannot be null");
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
     * Adds a sub-agent that can be invoked as a tool during execution.
     *
     * <p>Unlike handoffs which transfer control permanently, sub-agents are invoked like tools: the
     * parent agent calls the sub-agent, receives its output, and continues processing.
     *
     * <p>By default, the sub-agent inherits custom state (userId, sessionId, etc.) but starts with
     * a fresh conversation history.
     *
     * <p>Example:
     *
     * <pre>{@code
     * Agent orchestrator = Agent.builder()
     *     .name("Orchestrator")
     *     .addSubAgent(dataAnalyst, "For data analysis and statistical insights")
     *     .build();
     * }</pre>
     *
     * @param subAgent the agent to add as a callable tool
     * @param description describes when to use this sub-agent
     * @return this builder
     * @see SubAgentTool
     */
    public @NonNull Builder addSubAgent(@NonNull Agent subAgent, @NonNull String description) {
      this.tools.add(new SubAgentTool(subAgent, description));
      return this;
    }

    /**
     * Adds a sub-agent with custom configuration.
     *
     * <p>Example:
     *
     * <pre>{@code
     * Agent orchestrator = Agent.builder()
     *     .name("Orchestrator")
     *     .addSubAgent(dataAnalyst, SubAgentTool.Config.builder()
     *         .description("For data analysis")
     *         .shareHistory(true)  // Include full conversation context
     *         .build())
     *     .build();
     * }</pre>
     *
     * @param subAgent the agent to add as a callable tool
     * @param config configuration for context sharing and description
     * @return this builder
     * @see SubAgentTool.Config
     */
    public @NonNull Builder addSubAgent(@NonNull Agent subAgent, SubAgentTool.Config config) {
      this.tools.add(new SubAgentTool(subAgent, config));
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
     * Adds a skill that augments this agent's capabilities.
     *
     * <p>Skills extend the agent's knowledge by adding their instructions to the agent's system
     * prompt. When the skill's expertise is relevant, the LLM can automatically apply it.
     *
     * <p>Example:
     *
     * <pre>{@code
     * Agent agent = Agent.builder()
     *     .name("Assistant")
     *     .addSkill(pdfProcessorSkill)
     *     .addSkill(dataAnalyzerSkill)
     *     .build();
     * }</pre>
     *
     * @param skill the skill to add
     * @return this builder
     * @see Skill
     */
    public @NonNull Builder addSkill(@NonNull Skill skill) {
      Objects.requireNonNull(skill, "skill cannot be null");
      this.pendingSkills.add(skill);
      return this;
    }

    /**
     * Loads and adds a skill from a provider.
     *
     * <p>Example:
     *
     * <pre>{@code
     * SkillProvider provider = FilesystemSkillProvider.create(Path.of("./skills"));
     *
     * Agent agent = Agent.builder()
     *     .name("Assistant")
     *     .addSkillFrom(provider, "pdf-processor")
     *     .addSkillFrom(provider, "data-analyzer")
     *     .build();
     * }</pre>
     *
     * @param provider the skill provider
     * @param skillId the skill identifier
     * @return this builder
     * @see SkillProvider
     */
    public @NonNull Builder addSkillFrom(@NonNull SkillProvider provider, @NonNull String skillId) {
      Objects.requireNonNull(provider, "provider cannot be null");
      Objects.requireNonNull(skillId, "skillId cannot be null");
      return addSkill(provider.provide(skillId));
    }

    /**
     * Registers all skills from a SkillStore.
     *
     * <p>Each skill's instructions are merged into the agent's system prompt.
     *
     * <p>Example:
     *
     * <pre>{@code
     * SkillStore store = new SkillStore();
     * store.register(pdfSkill);
     * store.register(dataSkill);
     *
     * Agent agent = Agent.builder()
     *     .name("MultiSkillAgent")
     *     .skillStore(store)
     *     .build();
     * }</pre>
     *
     * @param store the skill store containing skills to add
     * @return this builder
     * @see SkillStore
     */
    public @NonNull Builder skillStore(@NonNull SkillStore store) {
      Objects.requireNonNull(store, "store cannot be null");
      for (Skill skill : store.all()) {
        addSkill(skill);
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
     * Sets the trace metadata for API requests (optional).
     *
     * @param trace the trace metadata
     * @return this builder
     */
    public @NonNull Builder traceMetadata(@Nullable TraceMetadata trace) {
      this.traceMetadata = trace;
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
     * Enables programmatic tool planning for this agent.
     *
     * <p>When enabled, a special {@code execute_tool_plan} meta-tool is registered that allows the
     * LLM to batch multiple tool calls into a single declarative execution plan. The framework
     * executes the plan locally — topologically sorting steps, running independent steps in
     * parallel, resolving {@code $ref} references — and returns only the designated output steps'
     * results to the LLM context. Intermediate results never touch the context window, saving
     * tokens and reducing latency.
     *
     * <p>Example usage:
     *
     * <pre>{@code
     * Agent agent = Agent.builder()
     *     .name("Researcher")
     *     .instructions("You are a research assistant.")
     *     .model("openai/gpt-4o")
     *     .responder(responder)
     *     .addTool(new GetWeatherTool())
     *     .addTool(new CompareDataTool())
     *     .enableToolPlanning()
     *     .build();
     * }</pre>
     *
     * @return this builder
     * @see com.paragon.agents.toolplan.ToolPlanTool
     * @see com.paragon.agents.toolplan.ToolPlan
     */
    public @NonNull Builder enableToolPlanning() {
      this.toolPlanningEnabled = true;
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

    public @NonNull StructuredBuilder<T> instructions(@NonNull Prompt instructions) {
      parentBuilder.instructions(instructions);
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

    public @NonNull StructuredBuilder<T> addSubAgent(
        @NonNull Agent subAgent, @NonNull String description) {
      parentBuilder.addSubAgent(subAgent, description);
      return this;
    }

    public @NonNull StructuredBuilder<T> addSubAgent(
        @NonNull Agent subAgent, SubAgentTool.Config config) {
      parentBuilder.addSubAgent(subAgent, config);
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

    public @NonNull StructuredBuilder<T> retryPolicy(com.paragon.http.RetryPolicy retryPolicy) {
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

    public @NonNull StructuredBuilder<T> addSkill(@NonNull Skill skill) {
      parentBuilder.addSkill(skill);
      return this;
    }

    public @NonNull StructuredBuilder<T> addSkillFrom(
        @NonNull SkillProvider provider, @NonNull String skillId) {
      parentBuilder.addSkillFrom(provider, skillId);
      return this;
    }

    public @NonNull StructuredBuilder<T> skillStore(@NonNull SkillStore store) {
      parentBuilder.skillStore(store);
      return this;
    }

    public @NonNull StructuredBuilder<T> enableToolPlanning() {
      parentBuilder.enableToolPlanning();
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
     * @param input the user's text input
     * @return the typed result
     */
    public @NonNull StructuredAgentResult<T> interact(@NonNull String input) {
      return interact(input, AgenticContext.create());
    }

    /**
     * Interacts with the agent with context and returns type-safe structured output.
     *
     * @param input the user's text input
     * @param context the conversation context
     * @return the typed result
     */
    public @NonNull StructuredAgentResult<T> interact(
        @NonNull String input, @NonNull AgenticContext context) {
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
     * @return the typed result
     */
    public @NonNull StructuredAgentResult<T> interact(@NonNull AgenticContext context) {
      AgentResult result = agent.interact(context);
      return parseResult(result);
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
