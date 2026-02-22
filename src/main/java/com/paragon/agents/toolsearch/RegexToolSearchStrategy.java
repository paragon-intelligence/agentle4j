package com.paragon.agents.toolsearch;

import com.paragon.responses.spec.FunctionTool;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import org.jspecify.annotations.NonNull;

/**
 * A tool search strategy that uses regex patterns to match tools by name and description.
 *
 * <p>The strategy generates regex patterns from the input query by splitting it into words and
 * matching each word (case-insensitively) against tool names and descriptions. A tool is considered
 * a match if <b>any</b> query word appears in its name or description.
 *
 * <p>Results are limited to {@link #maxResults} matches.
 *
 * <h2>Example</h2>
 *
 * <pre>{@code
 * ToolSearchStrategy strategy = new RegexToolSearchStrategy(5);
 * List<FunctionTool<?>> matches = strategy.search("weather forecast", allTools);
 * // Returns tools whose name/description contains "weather" or "forecast"
 * }</pre>
 *
 * @see ToolSearchStrategy
 * @since 1.0
 */
public final class RegexToolSearchStrategy implements ToolSearchStrategy {

  private final int maxResults;

  /**
   * Creates a new regex tool search strategy.
   *
   * @param maxResults maximum number of tools to return
   * @throws IllegalArgumentException if maxResults is less than 1
   */
  public RegexToolSearchStrategy(int maxResults) {
    if (maxResults < 1) {
      throw new IllegalArgumentException("maxResults must be at least 1, got: " + maxResults);
    }
    this.maxResults = maxResults;
  }

  /** Creates a new regex tool search strategy with a default limit of 5. */
  public RegexToolSearchStrategy() {
    this(5);
  }

  @Override
  public @NonNull List<FunctionTool<?>> search(
      @NonNull String query, @NonNull List<FunctionTool<?>> allTools) {
    Objects.requireNonNull(query, "query cannot be null");
    Objects.requireNonNull(allTools, "allTools cannot be null");

    if (query.isBlank() || allTools.isEmpty()) {
      return List.of();
    }

    // Build a single pattern from all query words (OR'd together)
    String[] words = query.trim().split("\\s+");
    StringBuilder patternBuilder = new StringBuilder();
    for (int i = 0; i < words.length; i++) {
      if (i > 0) patternBuilder.append('|');
      patternBuilder.append(Pattern.quote(words[i]));
    }
    Pattern pattern = Pattern.compile(patternBuilder.toString(), Pattern.CASE_INSENSITIVE);

    List<FunctionTool<?>> results = new ArrayList<>();
    for (FunctionTool<?> tool : allTools) {
      if (results.size() >= maxResults) break;

      String searchText = tool.getName();
      if (tool.getDescription() != null) {
        searchText += " " + tool.getDescription();
      }

      if (pattern.matcher(searchText).find()) {
        results.add(tool);
      }
    }

    return List.copyOf(results);
  }

  /**
   * Returns the maximum number of results this strategy returns.
   *
   * @return the max results limit
   */
  public int maxResults() {
    return maxResults;
  }
}
