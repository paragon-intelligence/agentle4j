package com.paragon.agents.toolsearch;

import com.paragon.responses.spec.FunctionTool;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * A container for tools that supports both eager (always included) and deferred (search-discoverable)
 * tools.
 *
 * <p>This mirrors Anthropic's {@code defer_loading} concept: eager tools are sent in every API call,
 * while deferred tools are only included when a {@link ToolSearchStrategy} determines they are
 * relevant to the user's input.
 *
 * <p>The registry is used by the Agent to build each API payload:
 *
 * <pre>{@code
 * ToolRegistry registry = ToolRegistry.builder()
 *     .strategy(new BM25ToolSearchStrategy(5))
 *     .eagerTool(criticalTool)        // always in every API call
 *     .deferredTool(rarelyUsedTool)   // only when search finds it relevant
 *     .deferredTools(List.of(tool1, tool2, tool3))
 *     .build();
 *
 * Agent agent = Agent.builder()
 *     .name("Assistant")
 *     .model("openai/gpt-4o")
 *     .instructions("You help with everything")
 *     .responder(responder)
 *     .toolRegistry(registry)
 *     .build();
 * }</pre>
 *
 * @see ToolSearchStrategy
 * @since 1.0
 */
public final class ToolRegistry {

  private final @NonNull List<FunctionTool<?>> eagerTools;
  private final @NonNull List<FunctionTool<?>> deferredTools;
  private final @NonNull ToolSearchStrategy strategy;

  private ToolRegistry(
      @NonNull List<FunctionTool<?>> eagerTools,
      @NonNull List<FunctionTool<?>> deferredTools,
      @NonNull ToolSearchStrategy strategy) {
    this.eagerTools = List.copyOf(eagerTools);
    this.deferredTools = List.copyOf(deferredTools);
    this.strategy = strategy;
  }

  /**
   * Creates a new ToolRegistry builder.
   *
   * @return a new builder
   */
  public static @NonNull Builder builder() {
    return new Builder();
  }

  /**
   * Resolves which tools to include in the next API call.
   *
   * <p>Returns all eager tools plus any deferred tools that the search strategy deems relevant
   * to the query. Deduplication is handled — a tool will not appear twice even if it would be
   * returned by both eager and search.
   *
   * @param query the user's input text
   * @return the combined list of relevant tools
   */
  public @NonNull List<FunctionTool<?>> resolveTools(@NonNull String query) {
    Objects.requireNonNull(query, "query cannot be null");

    if (deferredTools.isEmpty()) {
      return eagerTools;
    }

    List<FunctionTool<?>> searchResults = strategy.search(query, deferredTools);

    // Combine eager + search results, avoiding duplicates
    List<FunctionTool<?>> combined = new ArrayList<>(eagerTools);
    for (FunctionTool<?> tool : searchResults) {
      if (!combined.contains(tool)) {
        combined.add(tool);
      }
    }

    return List.copyOf(combined);
  }

  /**
   * Returns all tools (both eager and deferred).
   *
   * <p>This is used by the tool store to register all tools for execution, since the LLM might
   * reference a previously discovered tool.
   *
   * @return all registered tools
   */
  public @NonNull List<FunctionTool<?>> allTools() {
    List<FunctionTool<?>> all = new ArrayList<>(eagerTools);
    all.addAll(deferredTools);
    return List.copyOf(all);
  }

  /**
   * Returns the eager tools (always included in every API call).
   *
   * @return unmodifiable list of eager tools
   */
  public @NonNull List<FunctionTool<?>> eagerTools() {
    return eagerTools;
  }

  /**
   * Returns the deferred tools (only included via search).
   *
   * @return unmodifiable list of deferred tools
   */
  public @NonNull List<FunctionTool<?>> deferredTools() {
    return deferredTools;
  }

  /**
   * Returns the search strategy used by this registry.
   *
   * @return the search strategy
   */
  public @NonNull ToolSearchStrategy strategy() {
    return strategy;
  }

  /** Builder for creating {@link ToolRegistry} instances. */
  public static final class Builder {

    private final List<FunctionTool<?>> eagerTools = new ArrayList<>();
    private final List<FunctionTool<?>> deferredTools = new ArrayList<>();
    private ToolSearchStrategy strategy;

    private Builder() {}

    /**
     * Sets the search strategy for discovering deferred tools.
     *
     * @param strategy the search strategy
     * @return this builder
     */
    public @NonNull Builder strategy(@NonNull ToolSearchStrategy strategy) {
      this.strategy = Objects.requireNonNull(strategy, "strategy cannot be null");
      return this;
    }

    /**
     * Adds a tool that is always included in every API call (eager loading).
     *
     * @param tool the tool to always include
     * @return this builder
     */
    public @NonNull Builder eagerTool(@NonNull FunctionTool<?> tool) {
      eagerTools.add(Objects.requireNonNull(tool, "tool cannot be null"));
      return this;
    }

    /**
     * Adds multiple eager tools.
     *
     * @param tools the tools to always include
     * @return this builder
     */
    public @NonNull Builder eagerTools(@NonNull List<? extends FunctionTool<?>> tools) {
      Objects.requireNonNull(tools, "tools cannot be null");
      for (FunctionTool<?> tool : tools) {
        eagerTool(tool);
      }
      return this;
    }

    /**
     * Adds a tool that is only included when the search strategy finds it relevant (deferred
     * loading).
     *
     * @param tool the tool to defer
     * @return this builder
     */
    public @NonNull Builder deferredTool(@NonNull FunctionTool<?> tool) {
      deferredTools.add(Objects.requireNonNull(tool, "tool cannot be null"));
      return this;
    }

    /**
     * Adds multiple deferred tools.
     *
     * @param tools the tools to defer
     * @return this builder
     */
    public @NonNull Builder deferredTools(@NonNull List<? extends FunctionTool<?>> tools) {
      Objects.requireNonNull(tools, "tools cannot be null");
      for (FunctionTool<?> tool : tools) {
        deferredTool(tool);
      }
      return this;
    }

    /**
     * Builds the ToolRegistry.
     *
     * @return a new ToolRegistry
     * @throws IllegalStateException if deferred tools are present but no strategy was set
     */
    public @NonNull ToolRegistry build() {
      if (!deferredTools.isEmpty() && strategy == null) {
        throw new IllegalStateException(
            "A ToolSearchStrategy must be set when deferred tools are present. "
                + "Use .strategy(new BM25ToolSearchStrategy()) or similar.");
      }

      // If no strategy set and no deferred tools, use a no-op strategy
      ToolSearchStrategy resolvedStrategy =
          strategy != null ? strategy : (query, tools) -> List.of();

      return new ToolRegistry(eagerTools, deferredTools, resolvedStrategy);
    }
  }
}
