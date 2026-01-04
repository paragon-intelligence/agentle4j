package com.paragon.agents;

import com.paragon.responses.spec.FunctionToolCallOutput;
import com.paragon.responses.spec.Message;
import com.paragon.responses.spec.ResponseInputItem;
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
public final class AgentContext {

  private final List<ResponseInputItem> history;
  private final Map<String, Object> state;
  private int turnCount;

  // Trace correlation fields
  private @Nullable String parentTraceId;
  private @Nullable String parentSpanId;
  private @Nullable String requestId;

  private AgentContext(List<ResponseInputItem> history, Map<String, Object> state, int turnCount) {
    this.history = history;
    this.state = state;
    this.turnCount = turnCount;
  }

  /**
   * Creates a new, empty AgentContext.
   *
   * @return a fresh context with no history or state
   */
  public static @NonNull AgentContext create() {
    return new AgentContext(new ArrayList<>(), new HashMap<>(), 0);
  }

  public static @NonNull AgentContext create(@NonNull List<ResponseInputItem> history) {
    return new AgentContext(history, new HashMap<>(), 0);
  }

  public static @NonNull AgentContext create(
      @NonNull List<ResponseInputItem> history, @NonNull Map<String, Object> state) {
    return new AgentContext(history, state, 0);
  }

  public static @NonNull AgentContext create(
      @NonNull List<ResponseInputItem> history, @NonNull Map<String, Object> state, int turnCount) {
    return new AgentContext(history, state, turnCount);
  }

  /**
   * Creates an AgentContext pre-populated with conversation history.
   *
   * <p>Useful for resuming a previous conversation or providing initial context.
   *
   * @param initialHistory the messages to pre-populate
   * @return a context with the given history
   */
  public static @NonNull AgentContext withHistory(@NonNull List<ResponseInputItem> initialHistory) {
    Objects.requireNonNull(initialHistory, "initialHistory cannot be null");
    return AgentContext.create(initialHistory);
  }

  /**
   * Adds a message to the conversation history.
   *
   * @param message the message to add
   * @return this context for method chaining
   */
  public @NonNull AgentContext addMessage(@NonNull Message message) {
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
  public @NonNull AgentContext addInput(@NonNull ResponseInputItem item) {
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
  public @NonNull AgentContext addToolResult(@NonNull FunctionToolCallOutput output) {
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
  public @NonNull AgentContext setState(@NonNull String key, @Nullable Object value) {
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
   * @return an Optional containing the stored value cast to the expected type, or empty if not found
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
  public @NonNull AgentContext clear() {
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
  public @NonNull AgentContext copy() {
    AgentContext copy =
        new AgentContext(new ArrayList<>(this.history), new HashMap<>(this.state), this.turnCount);
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
  public @NonNull AgentContext withTraceContext(@NonNull String traceId, @NonNull String spanId) {
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
  public @NonNull AgentContext withRequestId(@NonNull String requestId) {
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
  public @NonNull AgentContext fork(@NonNull String newParentSpanId) {
    AgentContext forked = copy();
    forked.parentSpanId = Objects.requireNonNull(newParentSpanId, "newParentSpanId cannot be null");
    forked.turnCount = 0; // Reset turn count for child agent
    return forked;
  }
}
