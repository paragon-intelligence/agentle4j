package com.paragon.agents;

import com.paragon.responses.Responder;
import com.paragon.responses.spec.Message;
import com.paragon.responses.spec.ResponseInputItem;
import com.paragon.responses.spec.Text;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Streaming wrapper for RouterAgent that provides event callbacks during routing and execution.
 *
 * <p>RouterStream first classifies the input, then executes the selected agent with streaming.
 *
 * <pre>{@code
 * router.routeStream("Help with billing")
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
  private Consumer<String> onTextDelta;
  private Consumer<AgentResult> onComplete;
  private Consumer<Throwable> onError;
  private Consumer<Integer> onTurnStart;
  private Consumer<ToolExecution> onToolExecuted;
  private Consumer<Handoff> onHandoff;

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
    AgentStream agentStream = selected.interactStream(context);

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
    if (onError != null) {
      agentStream.onError(onError);
    }

    // Execute and wrap result
    AgentResult innerResult = agentStream.start();

    if (onComplete != null) {
      onComplete.accept(innerResult);
    }

    return innerResult;
  }

}
