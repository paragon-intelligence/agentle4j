package com.paragon.agents;

import com.paragon.responses.Responder;
import com.paragon.responses.spec.FunctionToolCall;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import org.jspecify.annotations.NonNull;

/**
 * Streaming wrapper for RouterAgent that provides event callbacks during routing and execution.
 *
 * <p>RouterStream first classifies the input, then executes the selected agent with streaming.
 *
 * <pre>{@code
 * AgenticContext context = AgenticContext.create()
 *     .addMessage(Message.user("Help with billing"));
 *
 * router.routeStream(context)
 *     .onRouteSelected(agent -> System.out.println("Routed to: " + agent.name()))
 *     .onTextDelta(System.out::print)
 *     .onComplete(result -> System.out.println("\nDone!"))
 *     .start();
 * }</pre>
 *
 * @since 1.0
 */
public final class RouterStream {

  private final RouterAgent router;
  private final AgenticContext context;
  private final Responder responder;

  // Callbacks
  private Consumer<Interactable> onRouteSelected;
  private Consumer<String> onRoutingFailed;
  private Consumer<String> onTextDelta;
  private Consumer<AgentResult> onComplete;
  private Consumer<Throwable> onError;
  private Consumer<Integer> onTurnStart;
  private Consumer<ToolExecution> onToolExecuted;
  private Consumer<Handoff> onHandoff;
  private AgentStream.ToolConfirmationHandler onToolCallPending;
  private AgentStream.PauseHandler onPause;
  private Consumer<GuardrailResult.Failed> onGuardrailFailed;
  private Consumer<FunctionToolCall> onClientSideTool;

  RouterStream(RouterAgent router, AgenticContext context, Responder responder) {
    this.router = Objects.requireNonNull(router, "router cannot be null");
    this.context = Objects.requireNonNull(context, "context cannot be null");
    this.responder = Objects.requireNonNull(responder, "responder cannot be null");
  }

  /**
   * Called when a route is selected.
   *
   * @param callback receives the selected Interactable
   * @return this stream
   */
  public @NonNull RouterStream onRouteSelected(@NonNull Consumer<Interactable> callback) {
    this.onRouteSelected = Objects.requireNonNull(callback);
    return this;
  }

  /**
   * Called when no route is found for the input (empty input or no matching route).
   *
   * @param callback receives a description of the routing failure
   * @return this stream
   */
  public @NonNull RouterStream onRoutingFailed(@NonNull Consumer<String> callback) {
    this.onRoutingFailed = Objects.requireNonNull(callback);
    return this;
  }

  /**
   * Called for each text delta during streaming.
   *
   * @param callback receives text chunks
   * @return this stream
   */
  public @NonNull RouterStream onTextDelta(@NonNull Consumer<String> callback) {
    this.onTextDelta = Objects.requireNonNull(callback);
    return this;
  }

  /**
   * Called when streaming completes successfully.
   *
   * @param callback receives the final result
   * @return this stream
   */
  public @NonNull RouterStream onComplete(@NonNull Consumer<AgentResult> callback) {
    this.onComplete = Objects.requireNonNull(callback);
    return this;
  }

  /**
   * Called when an error occurs.
   *
   * @param callback receives the error
   * @return this stream
   */
  public @NonNull RouterStream onError(@NonNull Consumer<Throwable> callback) {
    this.onError = Objects.requireNonNull(callback);
    return this;
  }

  /**
   * Called at the start of each turn.
   *
   * @param callback receives the turn number
   * @return this stream
   */
  public @NonNull RouterStream onTurnStart(@NonNull Consumer<Integer> callback) {
    this.onTurnStart = Objects.requireNonNull(callback);
    return this;
  }

  /**
   * Called when a tool execution completes.
   *
   * @param callback receives the tool execution result
   * @return this stream
   */
  public @NonNull RouterStream onToolExecuted(@NonNull Consumer<ToolExecution> callback) {
    this.onToolExecuted = Objects.requireNonNull(callback);
    return this;
  }

  /**
   * Called when a handoff occurs within the selected agent.
   *
   * @param callback receives the handoff
   * @return this stream
   */
  public @NonNull RouterStream onHandoff(@NonNull Consumer<Handoff> callback) {
    this.onHandoff = Objects.requireNonNull(callback);
    return this;
  }

  /**
   * Called when a tool call requires confirmation (human-in-the-loop), forwarded from the child agent.
   *
   * @param handler receives the pending tool call and approval callback
   * @return this stream
   */
  public @NonNull RouterStream onToolCallPending(AgentStream.ToolConfirmationHandler handler) {
    this.onToolCallPending = Objects.requireNonNull(handler);
    return this;
  }

  /**
   * Called when the child agent should pause for async approval, forwarded from the child agent.
   *
   * @param handler receives the serializable run state
   * @return this stream
   */
  public @NonNull RouterStream onPause(AgentStream.PauseHandler handler) {
    this.onPause = Objects.requireNonNull(handler);
    return this;
  }

  /**
   * Called when an output guardrail fails in the child agent.
   *
   * @param callback receives the failed guardrail result
   * @return this stream
   */
  public @NonNull RouterStream onGuardrailFailed(@NonNull Consumer<GuardrailResult.Failed> callback) {
    this.onGuardrailFailed = Objects.requireNonNull(callback);
    return this;
  }

  /**
   * Called when a client-side tool ({@code stopsLoop = true}) is detected in the child agent.
   *
   * @param callback receives the tool call that triggered the exit
   * @return this stream
   */
  public @NonNull RouterStream onClientSideTool(@NonNull Consumer<FunctionToolCall> callback) {
    this.onClientSideTool = Objects.requireNonNull(callback);
    return this;
  }

  /**
   * Starts the streaming router execution. Blocks until completion.
   *
   * <p>On virtual threads, blocking is efficient and does not consume platform threads.
   *
   * @return the final result
   */
  public @NonNull AgentResult startBlocking() {
    return start();
  }

  /**
   * Starts the streaming router execution. Blocks until completion.
   *
   * <p>On virtual threads, blocking is efficient and does not consume platform threads.
   *
   * @return the final result
   */
  public @NonNull AgentResult start() {
    // First, classify the input
    Optional<String> inputTextOpt = context.extractLastUserMessageText();
    if (inputTextOpt.isEmpty() || inputTextOpt.get().isBlank()) {
      AgentResult errorResult =
          AgentResult.error(
              new IllegalStateException("No user message found in context for routing"),
              context,
              0);
      if (onRoutingFailed != null) {
        onRoutingFailed.accept("No user message found in context for routing");
      }
      if (onError != null) {
        onError.accept(errorResult.error());
      }
      if (onComplete != null) {
        onComplete.accept(errorResult);
      }
      return errorResult;
    }

    String inputText = inputTextOpt.get();

    // Classify synchronously (returns Optional<Interactable> directly)
    Optional<Interactable> selectedOpt = router.classify(inputText);

    if (selectedOpt.isEmpty()) {
      AgentResult errorResult =
          AgentResult.error(
              new IllegalStateException("No suitable route found for input"), context, 0);
      if (onRoutingFailed != null) {
        onRoutingFailed.accept("No suitable route found for input: " + inputText);
      }
      if (onError != null) {
        onError.accept(errorResult.error());
      }
      if (onComplete != null) {
        onComplete.accept(errorResult);
      }
      return errorResult;
    }

    Interactable selected = selectedOpt.get();

    // Notify about route selection
    if (onRouteSelected != null) {
      onRouteSelected.accept(selected);
    }

    // Execute the selected Interactable with streaming
    AgentStream agentStream = selected.asStreaming().interact(context);

    // Wire up callbacks
    if (onTextDelta != null) {
      agentStream.onTextDelta(onTextDelta);
    }
    if (onTurnStart != null) {
      agentStream.onTurnStart(onTurnStart);
    }

    if (onToolExecuted != null) {
      agentStream.onToolExecuted(onToolExecuted);
    }
    if (onHandoff != null) {
      agentStream.onHandoff(onHandoff);
    }
    if (onToolCallPending != null) {
      agentStream.onToolCallPending(onToolCallPending);
    }
    if (onPause != null) {
      agentStream.onPause(onPause);
    }
    if (onGuardrailFailed != null) {
      agentStream.onGuardrailFailed(onGuardrailFailed);
    }
    if (onClientSideTool != null) {
      agentStream.onClientSideTool(onClientSideTool);
    }
    if (onError != null) {
      agentStream.onError(onError);
    }

    // Execute and wrap result
    AgentResult innerResult = agentStream.startBlocking();

    if (onComplete != null) {
      onComplete.accept(innerResult);
    }

    return innerResult;
  }
}
