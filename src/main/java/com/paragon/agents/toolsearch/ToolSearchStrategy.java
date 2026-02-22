package com.paragon.agents.toolsearch;

import com.paragon.responses.spec.FunctionTool;
import java.util.List;
import org.jspecify.annotations.NonNull;

/**
 * Strategy for dynamically selecting which tools to include in an LLM API call.
 *
 * <p>When an agent has many tools, sending all of them in every API call wastes context window
 * tokens and can degrade tool selection accuracy. A {@code ToolSearchStrategy} solves this by
 * searching the available tools based on the user's input and returning only the most relevant
 * subset.
 *
 * <p>This is the framework-level equivalent of Anthropic's server-side tool search feature, but
 * works with <b>any LLM provider</b> (OpenAI, OpenRouter, local models) because the selection
 * happens client-side before the API call.
 *
 * <h2>Built-in Strategies</h2>
 *
 * <ul>
 *   <li>{@link RegexToolSearchStrategy} — pattern-based matching on tool names/descriptions
 *   <li>{@link BM25ToolSearchStrategy} — TF-IDF scoring for relevance ranking
 *   <li>{@link EmbeddingToolSearchStrategy} — semantic similarity via embedding vectors
 * </ul>
 *
 * <h2>Custom Strategies</h2>
 *
 * <pre>{@code
 * ToolSearchStrategy custom = (query, tools) -> tools.stream()
 *     .filter(t -> mySimilarityModel.score(query, t.getName()) > 0.7)
 *     .toList();
 * }</pre>
 *
 * @see ToolRegistry
 * @since 1.0
 */
@FunctionalInterface
public interface ToolSearchStrategy {

  /**
   * Searches the available tools and returns the subset most relevant to the query.
   *
   * @param query the user's input text used to determine tool relevance
   * @param allTools all deferred tools available for selection
   * @return the subset of tools to include in the API call (may be empty)
   */
  @NonNull List<FunctionTool<?>> search(@NonNull String query, @NonNull List<FunctionTool<?>> allTools);
}
