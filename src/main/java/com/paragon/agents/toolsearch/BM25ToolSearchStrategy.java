package com.paragon.agents.toolsearch;

import com.paragon.responses.spec.FunctionTool;
import java.util.*;
import org.jspecify.annotations.NonNull;

/**
 * A tool search strategy that uses BM25 (Best Matching 25) scoring to rank tools by relevance.
 *
 * <p>BM25 is a widely-used information retrieval algorithm that computes relevance scores based on
 * term frequency (TF) and inverse document frequency (IDF). It handles:
 *
 * <ul>
 *   <li><b>Term frequency saturation</b> — repeated terms have diminishing returns
 *   <li><b>Document length normalization</b> — short, focused descriptions aren't penalized
 *   <li><b>Inverse document frequency</b> — rare terms are weighted higher
 * </ul>
 *
 * <h2>Example</h2>
 *
 * <pre>{@code
 * ToolSearchStrategy strategy = new BM25ToolSearchStrategy(5);
 * List<FunctionTool<?>> ranked = strategy.search("search database records", allTools);
 * // Returns up to 5 most relevant tools, ranked by BM25 score
 * }</pre>
 *
 * @see ToolSearchStrategy
 * @since 1.0
 */
public final class BM25ToolSearchStrategy implements ToolSearchStrategy {

  private final int maxResults;
  private final double k1;
  private final double b;

  /**
   * Creates a new BM25 strategy with custom parameters.
   *
   * @param maxResults maximum number of tools to return
   * @param k1 term frequency saturation parameter (typically 1.2–2.0)
   * @param b document length normalization factor (0.0 = no normalization, 1.0 = full)
   */
  public BM25ToolSearchStrategy(int maxResults, double k1, double b) {
    if (maxResults < 1) {
      throw new IllegalArgumentException("maxResults must be at least 1, got: " + maxResults);
    }
    this.maxResults = maxResults;
    this.k1 = k1;
    this.b = b;
  }

  /**
   * Creates a new BM25 strategy with default parameters (k1=1.5, b=0.75).
   *
   * @param maxResults maximum number of tools to return
   */
  public BM25ToolSearchStrategy(int maxResults) {
    this(maxResults, 1.5, 0.75);
  }

  /** Creates a new BM25 strategy with default parameters and a limit of 5. */
  public BM25ToolSearchStrategy() {
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

    List<String> queryTerms = tokenize(query);
    if (queryTerms.isEmpty()) {
      return List.of();
    }

    // Build "documents" from tool metadata
    List<List<String>> documents = new ArrayList<>();
    for (FunctionTool<?> tool : allTools) {
      documents.add(tokenize(toolText(tool)));
    }

    // Compute average document length
    double avgDl = documents.stream().mapToInt(List::size).average().orElse(1.0);
    int n = documents.size();

    // Compute IDF for each query term
    Map<String, Double> idf = new HashMap<>();
    for (String term : queryTerms) {
      int df = 0;
      for (List<String> doc : documents) {
        if (doc.contains(term)) df++;
      }
      // Standard BM25 IDF formula
      double idfValue = Math.log((n - df + 0.5) / (df + 0.5) + 1);
      idf.put(term, idfValue);
    }

    // Score each tool
    record ScoredTool(FunctionTool<?> tool, double score) {}
    List<ScoredTool> scored = new ArrayList<>();

    for (int i = 0; i < allTools.size(); i++) {
      List<String> doc = documents.get(i);
      double score = 0.0;

      Map<String, Integer> termFreq = new HashMap<>();
      for (String token : doc) {
        termFreq.merge(token, 1, Integer::sum);
      }

      for (String term : queryTerms) {
        int tf = termFreq.getOrDefault(term, 0);
        if (tf == 0) continue;

        double idfVal = idf.getOrDefault(term, 0.0);
        double numerator = tf * (k1 + 1);
        double denominator = tf + k1 * (1 - b + b * doc.size() / avgDl);
        score += idfVal * (numerator / denominator);
      }

      if (score > 0) {
        scored.add(new ScoredTool(allTools.get(i), score));
      }
    }

    // Sort by score descending and take top N
    scored.sort((a, bItem) -> Double.compare(bItem.score(), a.score()));

    List<FunctionTool<?>> results = new ArrayList<>();
    for (int i = 0; i < Math.min(maxResults, scored.size()); i++) {
      results.add(scored.get(i).tool());
    }

    return List.copyOf(results);
  }

  private static String toolText(FunctionTool<?> tool) {
    StringBuilder sb = new StringBuilder(tool.getName());
    if (tool.getDescription() != null) {
      sb.append(' ').append(tool.getDescription());
    }
    return sb.toString();
  }

  private static List<String> tokenize(String text) {
    if (text == null || text.isBlank()) return List.of();
    // Convert snake_case and camelCase to words, then lowercase
    String normalized = text.replaceAll("_", " ").replaceAll("([a-z])([A-Z])", "$1 $2");
    String[] parts = normalized.toLowerCase(Locale.ROOT).split("\\s+");
    List<String> tokens = new ArrayList<>();
    for (String part : parts) {
      String cleaned = part.replaceAll("[^a-z0-9]", "");
      if (!cleaned.isEmpty()) {
        tokens.add(cleaned);
      }
    }
    return tokens;
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
