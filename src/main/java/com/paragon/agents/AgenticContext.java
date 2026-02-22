package com.paragon.agents;

import com.paragon.responses.spec.FunctionToolCallOutput;
import com.paragon.responses.spec.Message;
import com.paragon.responses.spec.MessageRole;
import com.paragon.responses.spec.ResponseInputItem;
import com.paragon.responses.spec.Text;
import com.paragon.telemetry.processors.TraceIdGenerator;
import java.util.*;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Holds conversation state for an agent interaction.
 *
 * <p>This is the "short-term memory" for an agent - it tracks:
 *
 * <ul>
 *   <li>Conversation history (messages exchanged between user and agent)
 *   <li>Tool call results
 *   <li>Custom user-defined state via key-value store
 * </ul>
 *
 * <p>AgentContext is designed to be passed per-run, making the {@link Agent} thread-safe and
 * reusable across multiple conversations.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * // Create a fresh context for a new conversation
 * AgentContext context = AgentContext.create();
 *
 * // First interaction
 * agent.interact("Hi, I need help with my order", context);
 *
 * // Second interaction (remembers first)
 * agent.interact("It's order #12345", context);
 *
 * // Store custom state
 * context.setState("orderNumber", "12345");
 * String orderNum = (String) context.getState("orderNumber");
 * }</pre>
 *
 * @see Agent
 * @since 1.0
 */
public final class AgenticContext {

  // ScopedValue for context propagation — virtual thread optimized
  static final ScopedValue<AgenticContext> CURRENT_CONTEXT = ScopedValue.newInstance();

  private final List<ResponseInputItem> history;
  private final Map<String, Object> state;
  private int turnCount;

  // Trace correlation fields
  private @Nullable String parentTraceId;
  private @Nullable String parentSpanId;
  private @Nullable String requestId;

  private AgenticContext(
      List<ResponseInputItem> history, Map<String, Object> state, int turnCount) {
    this.history = history;
    this.state = state;
    this.turnCount = turnCount;
  }

  /**
   * Creates a new, empty AgentContext.
   *
   * @return a fresh context with no history or state
   */
  public static @NonNull AgenticContext create() {
    return new AgenticContext(new ArrayList<>(), new HashMap<>(), 0);
  }

  public static @NonNull AgenticContext create(@NonNull List<ResponseInputItem> history) {
    return new AgenticContext(new ArrayList<>(history), new HashMap<>(), 0);
  }

  public static @NonNull AgenticContext create(
      @NonNull List<ResponseInputItem> history, @NonNull Map<String, Object> state) {
    return new AgenticContext(new ArrayList<>(history), state, 0);
  }

  public static @NonNull AgenticContext create(
      @NonNull List<ResponseInputItem> history, @NonNull Map<String, Object> state, int turnCount) {
    return new AgenticContext(new ArrayList<>(history), state, turnCount);
  }

  /**
   * Creates an AgentContext pre-populated with conversation history.
   *
   * <p>Useful for resuming a previous conversation or providing initial context.
   *
   * @param initialHistory the messages to pre-populate
   * @return a context with the given history
   */
  public static @NonNull AgenticContext withHistory(
      @NonNull List<ResponseInputItem> initialHistory) {
    Objects.requireNonNull(initialHistory, "initialHistory cannot be null");
    return AgenticContext.create(initialHistory);
  }

  /**
   * Adds a message to the conversation history.
   *
   * @param message the message to add
   * @return this context for method chaining
   */
  public @NonNull AgenticContext addMessage(@NonNull Message message) {
    Objects.requireNonNull(message, "message cannot be null");
    this.history.add(message);
    return this;
  }

  /**
   * Adds a response input item to the conversation history.
   *
   * @param item the input item to add
   * @return this context for method chaining
   */
  public @NonNull AgenticContext addInput(@NonNull ResponseInputItem item) {
    Objects.requireNonNull(item, "item cannot be null");
    this.history.add(item);
    return this;
  }

  /**
   * Adds a tool result to the conversation history.
   *
   * <p>Tool results are tracked so the agent can see the output of previous tool executions.
   *
   * @param output the tool execution result (already contains callId)
   * @return this context for method chaining
   */
  public @NonNull AgenticContext addToolResult(@NonNull FunctionToolCallOutput output) {
    Objects.requireNonNull(output, "output cannot be null");
    // FunctionToolCallOutput implements Item which extends ResponseInputItem
    this.history.add(output);
    return this;
  }

  /**
   * Returns an unmodifiable view of the conversation history.
   *
   * @return the conversation history
   */
  public @NonNull List<ResponseInputItem> getHistory() {
    return Collections.unmodifiableList(history);
  }

  /**
   * Returns a mutable copy of the conversation history for building payloads.
   *
   * @return a mutable copy of the history
   */
  public @NonNull List<ResponseInputItem> getHistoryMutable() {
    return new ArrayList<>(history);
  }

  /**
   * Stores a custom value in the context's state.
   *
   * @param key the key to store under
   * @param value the value to store (can be null to remove)
   * @return this context for method chaining
   */
  public @NonNull AgenticContext setState(@NonNull String key, @Nullable Object value) {
    Objects.requireNonNull(key, "key cannot be null");
    if (value == null) {
      state.remove(key);
    } else {
      state.put(key, value);
    }
    return this;
  }

  /**
   * Retrieves a value from the context's state.
   *
   * @param key the key to look up
   * @return an Optional containing the stored value, or empty if not found
   */
  public Optional<Object> getState(@NonNull String key) {
    Objects.requireNonNull(key, "key cannot be null");
    return Optional.ofNullable(state.get(key));
  }

  /**
   * Retrieves a typed value from the context's state.
   *
   * @param key the key to look up
   * @param type the expected type
   * @param <T> the type parameter
   * @return an Optional containing the stored value cast to the expected type, or empty if not
   *     found
   * @throws ClassCastException if the stored value is not of the expected type
   */
  @SuppressWarnings("unchecked")
  public <T> Optional<T> getState(@NonNull String key, @NonNull Class<T> type) {
    Objects.requireNonNull(key, "key cannot be null");
    Objects.requireNonNull(type, "type cannot be null");
    Object value = state.get(key);
    if (value == null) {
      return Optional.empty();
    }
    return Optional.of((T) value);
  }

  /**
   * Checks if a key exists in the state.
   *
   * @param key the key to check
   * @return true if the key exists
   */
  public boolean hasState(@NonNull String key) {
    Objects.requireNonNull(key, "key cannot be null");
    return state.containsKey(key);
  }

  /**
   * Returns an unmodifiable view of all state entries.
   *
   * @return the state map
   */
  public @NonNull Map<String, Object> getAllState() {
    return Collections.unmodifiableMap(state);
  }

  /**
   * Increments and returns the turn count.
   *
   * <p>A "turn" is one LLM call in the agent loop.
   *
   * @return the new turn count after incrementing
   */
  int incrementTurn() {
    return ++turnCount;
  }

  /**
   * Returns the current turn count.
   *
   * @return the number of LLM calls made in this context
   */
  public int getTurnCount() {
    return turnCount;
  }

  /**
   * Clears all history and state, resetting the context.
   *
   * @return this context for method chaining
   */
  public @NonNull AgenticContext clear() {
    history.clear();
    state.clear();
    turnCount = 0;
    return this;
  }

  /**
   * Creates a copy of this context with the same history and state.
   *
   * <p>Useful for parallel agent execution where each agent needs an isolated copy.
   *
   * @return a new context with copied history and state
   */
  public @NonNull AgenticContext copy() {
    AgenticContext copy =
        new AgenticContext(
            new ArrayList<>(this.history), new HashMap<>(this.state), this.turnCount);
    copy.parentTraceId = this.parentTraceId;
    copy.parentSpanId = this.parentSpanId;
    copy.requestId = this.requestId;
    return copy;
  }

  /**
   * Returns the size of the conversation history.
   *
   * @return the number of items in history
   */
  public int historySize() {
    return history.size();
  }

  // ===== Utility Methods =====

  /**
   * Extracts the text of the last user message from the conversation history.
   *
   * <p>Iterates backwards through history to find the most recent user message and returns its
   * first text content.
   *
   * @return an Optional containing the last user message text, or empty if none found
   */
  public @NonNull Optional<String> extractLastUserMessageText() {
    for (int i = history.size() - 1; i >= 0; i--) {
      ResponseInputItem item = history.get(i);
      if (item instanceof Message msg && msg.role() == MessageRole.USER) {
        if (msg.content() != null) {
          for (var content : msg.content()) {
            if (content instanceof Text text) {
              return Optional.of(text.text());
            }
          }
        }
      }
    }
    return Optional.empty();
  }

  /**
   * Extracts the text of the last user message, or returns a fallback value.
   *
   * @param fallback the fallback value if no user message is found
   * @return the last user message text, or the fallback
   */
  public @NonNull String extractLastUserMessageText(@NonNull String fallback) {
    return extractLastUserMessageText().orElse(fallback);
  }

  /**
   * Ensures this context has trace context set, generating IDs if not already present.
   *
   * @return this context for method chaining
   */
  public @NonNull AgenticContext ensureTraceContext() {
    if (!hasTraceContext()) {
      withTraceContext(TraceIdGenerator.generateTraceId(), TraceIdGenerator.generateSpanId());
    }
    return this;
  }

  /**
   * Creates a child context for sub-agent execution based on sharing configuration.
   *
   * <ul>
   *   <li>If shareHistory is true, forks the full context including history
   *   <li>If shareState is true (but not history), copies state and trace but starts fresh history
   *   <li>Otherwise, creates a completely isolated context
   * </ul>
   *
   * @param shareState whether to copy custom state to the child
   * @param shareHistory whether to fork the full history to the child
   * @param request the user message to add to the child context
   * @return a new child context configured according to the sharing parameters
   */
  public @NonNull AgenticContext createChildContext(
      boolean shareState, boolean shareHistory, @NonNull String request) {
    Objects.requireNonNull(request, "request cannot be null");
    AgenticContext childContext;

    if (shareHistory) {
      String childSpanId = TraceIdGenerator.generateSpanId();
      childContext = this.fork(childSpanId);
      childContext.addInput(Message.user(request));
    } else if (shareState) {
      childContext = AgenticContext.create();
      if (this.hasTraceContext()) {
        String childSpanId = TraceIdGenerator.generateSpanId();
        childContext.withTraceContext(this.parentTraceId().orElseThrow(), childSpanId);
      }
      this.requestId().ifPresent(childContext::withRequestId);
      for (Map.Entry<String, Object> entry : this.getAllState().entrySet()) {
        childContext.setState(entry.getKey(), entry.getValue());
      }
      childContext.addInput(Message.user(request));
    } else {
      childContext = AgenticContext.create();
      childContext.addInput(Message.user(request));
    }

    return childContext;
  }

  // ===== Context Propagation =====

  /**
   * Runs a task with this context bound as the current context for sub-agent execution.
   *
   * <p>Uses {@link ScopedValue} for virtual-thread-safe context propagation.
   *
   * @param task the task to run within this context scope
   */
  void runAsCurrent(@NonNull Runnable task) {
    ScopedValue.where(CURRENT_CONTEXT, this).run(task);
  }

  /**
   * Calls a task with this context bound as the current context, returning the result.
   *
   * <p>Uses {@link ScopedValue} for virtual-thread-safe context propagation.
   *
   * @param task the task to call within this context scope
   * @param <T> the return type
   * @return the result from the task
   * @throws Exception if the task throws
   */
  <T> T callAsCurrent(ScopedValue.CallableOp<T, Exception> task) throws Exception {
    return ScopedValue.where(CURRENT_CONTEXT, this).call(task);
  }

  /**
   * Returns the currently bound context from the enclosing scope, if any.
   *
   * <p>Used by sub-agent tools to discover their parent context.
   *
   * @return an Optional containing the current context, or empty if none is bound
   */
  static @NonNull Optional<AgenticContext> current() {
    return CURRENT_CONTEXT.isBound() ? Optional.of(CURRENT_CONTEXT.get()) : Optional.empty();
  }

  // ===== Trace Context Methods =====

  /**
   * Sets the parent trace context for distributed tracing.
   *
   * <p>When set, child spans will be linked to this parent, enabling end-to-end trace correlation
   * across multi-agent runs.
   *
   * @param traceId the parent trace ID (32-char hex)
   * @param spanId the parent span ID (16-char hex)
   * @return this context for method chaining
   */
  public @NonNull AgenticContext withTraceContext(@NonNull String traceId, @NonNull String spanId) {
    this.parentTraceId = Objects.requireNonNull(traceId, "traceId cannot be null");
    this.parentSpanId = Objects.requireNonNull(spanId, "spanId cannot be null");
    return this;
  }

  /**
   * Sets the request ID for high-level correlation.
   *
   * <p>The request ID is a user-defined identifier that groups all operations from a single user
   * request, even across multiple traces.
   *
   * @param requestId the unique request identifier
   * @return this context for method chaining
   */
  public @NonNull AgenticContext withRequestId(@NonNull String requestId) {
    this.requestId = Objects.requireNonNull(requestId, "requestId cannot be null");
    return this;
  }

  /**
   * Returns the parent trace ID, if set.
   *
   * @return an Optional containing the parent trace ID, or empty if not set
   */
  public Optional<String> parentTraceId() {
    return Optional.ofNullable(parentTraceId);
  }

  /**
   * Returns the parent span ID, if set.
   *
   * @return an Optional containing the parent span ID, or empty if not set
   */
  public Optional<String> parentSpanId() {
    return Optional.ofNullable(parentSpanId);
  }

  /**
   * Returns the request ID, if set.
   *
   * @return an Optional containing the request ID, or empty if not set
   */
  public Optional<String> requestId() {
    return Optional.ofNullable(requestId);
  }

  /**
   * Checks if this context has trace context set.
   *
   * @return true if both parentTraceId and parentSpanId are set
   */
  public boolean hasTraceContext() {
    return parentTraceId != null && parentSpanId != null;
  }

  /**
   * Creates a forked copy for child agent execution with updated parent span.
   *
   * <p>Use this when handing off to a child agent. The child will have the same history but can
   * generate its own child spans under the given parent.
   *
   * @param newParentSpanId the span ID to use as the parent for the child
   * @return a new context with the updated parent span
   */
  public @NonNull AgenticContext fork(@NonNull String newParentSpanId) {
    AgenticContext forked = copy();
    forked.parentSpanId = Objects.requireNonNull(newParentSpanId, "newParentSpanId cannot be null");
    forked.turnCount = 0; // Reset turn count for child agent
    return forked;
  }
}
