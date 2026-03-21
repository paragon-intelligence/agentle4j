package com.paragon.agents;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.paragon.responses.spec.FunctionToolCall;
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
 * <p>AgenticContext is designed to be passed per-run, making the {@link Agent} thread-safe and
 * reusable across multiple conversations.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * // Create a fresh context for a new conversation
 * AgenticContext context = AgenticContext.create();
 *
 * // Resume safely from optional history and append a new user turn
 * AgenticContext resumed = AgenticContext.ofHistory(loadHistory())
 *     .addUserMessage("I still need help");
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
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public final class AgenticContext {

  // ScopedValue for context propagation — virtual thread optimized
  static final ScopedValue<AgenticContext> CURRENT_CONTEXT = ScopedValue.newInstance();

  @JsonProperty("history")
  private final List<ResponseInputItem> history;

  @JsonProperty("state")
  private final Map<String, Object> state;

  @JsonProperty("turn_count")
  private int turnCount;

  // Trace correlation fields
  @JsonProperty("parent_trace_id")
  private @Nullable String parentTraceId;

  @JsonProperty("parent_span_id")
  private @Nullable String parentSpanId;

  @JsonProperty("request_id")
  private @Nullable String requestId;

  private AgenticContext(
      List<ResponseInputItem> history, Map<String, Object> state, int turnCount) {
    this.history = history;
    this.state = state;
    this.turnCount = turnCount;
  }

  /**
   * Creates a new, empty AgenticContext.
   *
   * @return a fresh context with no history or state
   */
  public static @NonNull AgenticContext create() {
    return new AgenticContext(new ArrayList<>(), new HashMap<>(), 0);
  }

  /**
   * Creates a context with pre-populated history.
   *
   * @param history the initial history items
   * @return a new context with the provided history
   */
  public static @NonNull AgenticContext create(@NonNull List<? extends ResponseInputItem> history) {
    Objects.requireNonNull(history, "history cannot be null");
    return new AgenticContext(new ArrayList<>(history), new HashMap<>(), 0);
  }

  /**
   * Creates a context with pre-populated history and state.
   *
   * @param history the initial history items
   * @param state the initial state map
   * @return a new context with the provided history and state
   */
  public static @NonNull AgenticContext create(
      @NonNull List<? extends ResponseInputItem> history, @NonNull Map<String, Object> state) {
    Objects.requireNonNull(history, "history cannot be null");
    Objects.requireNonNull(state, "state cannot be null");
    return new AgenticContext(new ArrayList<>(history), state, 0);
  }

  /**
   * Creates a context with pre-populated history, state, and turn count.
   *
   * @param history the initial history items
   * @param state the initial state map
   * @param turnCount the current turn count
   * @return a new context with the provided history, state, and turn count
   */
  public static @NonNull AgenticContext create(
      @NonNull List<? extends ResponseInputItem> history,
      @NonNull Map<String, Object> state,
      int turnCount) {
    Objects.requireNonNull(history, "history cannot be null");
    Objects.requireNonNull(state, "state cannot be null");
    return new AgenticContext(new ArrayList<>(history), state, turnCount);
  }

  /**
   * Creates a context from a possibly-null history collection.
   *
   * <p>This is the null-safe convenience factory for callers that may or may not have persisted
   * history available yet.
   *
   * @param history the history collection, or null
   * @return a fresh empty context when history is null/empty, otherwise a context with that history
   */
  public static @NonNull AgenticContext ofHistory(
      @Nullable Collection<? extends ResponseInputItem> history) {
    if (history == null || history.isEmpty()) {
      return create();
    }
    return new AgenticContext(new ArrayList<>(history), new HashMap<>(), 0);
  }

  /**
   * Creates a context from the provided input items.
   *
   * @param items the input items to add
   * @return a context populated with the given inputs
   */
  public static @NonNull AgenticContext ofInputs(@NonNull ResponseInputItem... items) {
    Objects.requireNonNull(items, "items cannot be null");
    return create().addInputs(List.of(items));
  }

  /**
   * Creates a context from the provided messages.
   *
   * @param messages the messages to add
   * @return a context populated with the given messages
   */
  public static @NonNull AgenticContext ofMessages(@NonNull Message... messages) {
    Objects.requireNonNull(messages, "messages cannot be null");
    return create().addMessages(List.of(messages));
  }

  /**
   * Jackson deserialization entry point.
   *
   * <p>State map values are deserialized as standard Jackson types: JSON objects become {@link
   * java.util.LinkedHashMap}, arrays become {@link java.util.ArrayList}, and primitives map to
   * their Java equivalents. For full type fidelity with custom objects, use {@code
   * mapper.convertValue(ctx.getState("key").orElseThrow(), MyType.class)}.
   */
  @JsonCreator
  static AgenticContext fromJson(
      @JsonProperty("history") @Nullable List<ResponseInputItem> history,
      @JsonProperty("state") @Nullable Map<String, Object> state,
      @JsonProperty("turn_count") int turnCount,
      @JsonProperty("parent_trace_id") @Nullable String parentTraceId,
      @JsonProperty("parent_span_id") @Nullable String parentSpanId,
      @JsonProperty("request_id") @Nullable String requestId) {
    AgenticContext ctx =
        new AgenticContext(
            history != null ? new ArrayList<>(history) : new ArrayList<>(),
            state != null ? new HashMap<>(state) : new HashMap<>(),
            turnCount);
    ctx.parentTraceId = parentTraceId;
    ctx.parentSpanId = parentSpanId;
    ctx.requestId = requestId;
    return ctx;
  }

  /**
   * Creates an AgenticContext pre-populated with conversation history.
   *
   * <p>Useful for resuming a previous conversation or providing initial context.
   *
   * @param initialHistory the messages to pre-populate
   * @return a context with the given history
   */
  public static @NonNull AgenticContext withHistory(
      @NonNull List<? extends ResponseInputItem> initialHistory) {
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
   * Adds multiple messages to the conversation history.
   *
   * @param messages the messages to add
   * @return this context for method chaining
   */
  public @NonNull AgenticContext addMessages(@NonNull Iterable<? extends Message> messages) {
    Objects.requireNonNull(messages, "messages cannot be null");
    for (Message message : messages) {
      addMessage(Objects.requireNonNull(message, "message cannot be null"));
    }
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
   * Adds multiple response input items to the conversation history.
   *
   * @param items the input items to add
   * @return this context for method chaining
   */
  public @NonNull AgenticContext addInputs(@NonNull Iterable<? extends ResponseInputItem> items) {
    Objects.requireNonNull(items, "items cannot be null");
    for (ResponseInputItem item : items) {
      addInput(Objects.requireNonNull(item, "item cannot be null"));
    }
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
   * Adds a user text message to the conversation history.
   *
   * @param text the user text to add
   * @return this context for method chaining
   */
  public @NonNull AgenticContext addUserMessage(@NonNull String text) {
    return addMessage(Message.user(Objects.requireNonNull(text, "text cannot be null")));
  }

  /**
   * Adds an assistant text message to the conversation history.
   *
   * @param text the assistant text to add
   * @return this context for method chaining
   */
  public @NonNull AgenticContext addAssistantMessage(@NonNull String text) {
    return addMessage(Message.assistant(Objects.requireNonNull(text, "text cannot be null")));
  }

  /**
   * Adds a developer text message to the conversation history.
   *
   * @param text the developer text to add
   * @return this context for method chaining
   */
  public @NonNull AgenticContext addDeveloperMessage(@NonNull String text) {
    return addMessage(Message.developer(Objects.requireNonNull(text, "text cannot be null")));
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
   * Returns an unmodifiable view of the conversation history.
   *
   * @return the conversation history
   */
  public @NonNull List<ResponseInputItem> history() {
    return getHistory();
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
   * Returns a mutable copy of the conversation history for ad-hoc manipulation.
   *
   * @return a mutable copy of the history
   */
  public @NonNull List<ResponseInputItem> historyMutable() {
    return getHistoryMutable();
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
   * Removes a value from the context's state.
   *
   * @param key the key to remove
   * @return this context for method chaining
   */
  public @NonNull AgenticContext removeState(@NonNull String key) {
    Objects.requireNonNull(key, "key cannot be null");
    state.remove(key);
    return this;
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
   * Returns an unmodifiable view of the state map.
   *
   * @return the state map
   */
  public @NonNull Map<String, Object> state() {
    return getAllState();
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

  /**
   * Returns whether this context currently contains any history items.
   *
   * @return true when history is not empty
   */
  public boolean hasHistory() {
    return !history.isEmpty();
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
   * Returns all messages in the conversation history.
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
   * Returns all user messages in the conversation history.
   *
   * @return an immutable list of user messages
   */
  public @NonNull List<Message> userMessages() {
    return messages(MessageRole.USER);
  }

  /**
   * Returns all assistant messages in the conversation history.
   *
   * @return an immutable list of assistant messages
   */
  public @NonNull List<Message> assistantMessages() {
    return messages(MessageRole.ASSISTANT);
  }

  /**
   * Returns all developer messages in the conversation history.
   *
   * @return an immutable list of developer messages
   */
  public @NonNull List<Message> developerMessages() {
    return messages(MessageRole.DEVELOPER);
  }

  /**
   * Returns all function tool calls in the conversation history.
   *
   * @return an immutable list of function tool calls
   */
  public @NonNull List<FunctionToolCall> toolCalls() {
    return historyItems(FunctionToolCall.class);
  }

  /**
   * Returns all function tool outputs in the conversation history.
   *
   * @return an immutable list of function tool outputs
   */
  public @NonNull List<FunctionToolCallOutput> toolOutputs() {
    return historyItems(FunctionToolCallOutput.class);
  }

  // ===== Utility Methods =====

  /**
   * Returns the most recent message in the conversation history.
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
   * Returns the most recent user message.
   *
   * @return an Optional containing the last user message, or empty if none exist
   */
  public @NonNull Optional<Message> lastUserMessage() {
    return lastMessage(MessageRole.USER);
  }

  /**
   * Returns the most recent assistant message.
   *
   * @return an Optional containing the last assistant message, or empty if none exist
   */
  public @NonNull Optional<Message> lastAssistantMessage() {
    return lastMessage(MessageRole.ASSISTANT);
  }

  /**
   * Returns the most recent developer message.
   *
   * @return an Optional containing the last developer message, or empty if none exist
   */
  public @NonNull Optional<Message> lastDeveloperMessage() {
    return lastMessage(MessageRole.DEVELOPER);
  }

  /**
   * Returns the first text segment from the most recent user message.
   *
   * <p>This is an alias for {@link #extractLastUserMessageText()}.
   *
   * @return an Optional containing the last user text, or empty if none found
   */
  public @NonNull Optional<String> lastUserMessageText() {
    return extractLastUserMessageText();
  }

  /**
   * Returns the first text segment from the most recent user message, or a fallback value.
   *
   * <p>This is an alias for {@link #extractLastUserMessageText(String)}.
   *
   * @param fallback the fallback value if no user message text is found
   * @return the last user message text, or the fallback
   */
  public @NonNull String lastUserMessageText(@NonNull String fallback) {
    return extractLastUserMessageText(fallback);
  }

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

  private @NonNull Optional<Message> lastMessageMatching(@Nullable MessageRole role) {
    for (int i = history.size() - 1; i >= 0; i--) {
      ResponseInputItem item = history.get(i);
      if (item instanceof Message message && (role == null || message.role() == role)) {
        return Optional.of(message);
      }
    }
    return Optional.empty();
  }
}
