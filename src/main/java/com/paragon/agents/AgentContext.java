package com.paragon.agents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.paragon.responses.spec.FunctionToolCallOutput;
import com.paragon.responses.spec.Message;
import com.paragon.responses.spec.ResponseInputItem;

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

  private AgentContext() {
    this.history = new ArrayList<>();
    this.state = new HashMap<>();
    this.turnCount = 0;
  }

  /**
   * Creates a new, empty AgentContext.
   *
   * @return a fresh context with no history or state
   */
  public static @NonNull AgentContext create() {
    return new AgentContext();
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
    AgentContext context = new AgentContext();
    context.history.addAll(initialHistory);
    return context;
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
   * @return the stored value, or null if not found
   */
  public @Nullable Object getState(@NonNull String key) {
    Objects.requireNonNull(key, "key cannot be null");
    return state.get(key);
  }

  /**
   * Retrieves a typed value from the context's state.
   *
   * @param key the key to look up
   * @param type the expected type
   * @param <T> the type parameter
   * @return the stored value cast to the expected type, or null if not found
   * @throws ClassCastException if the stored value is not of the expected type
   */
  @SuppressWarnings("unchecked")
  public <T> @Nullable T getState(@NonNull String key, @NonNull Class<T> type) {
    Objects.requireNonNull(key, "key cannot be null");
    Objects.requireNonNull(type, "type cannot be null");
    Object value = state.get(key);
    if (value == null) {
      return null;
    }
    return (T) value;
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
    AgentContext copy = new AgentContext();
    copy.history.addAll(this.history);
    copy.state.putAll(this.state);
    copy.turnCount = this.turnCount;
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
}
