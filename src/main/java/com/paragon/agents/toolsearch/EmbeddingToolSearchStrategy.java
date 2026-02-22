package com.paragon.agents.toolsearch;

import com.paragon.embeddings.Embedding;
import com.paragon.embeddings.EmbeddingProvider;
import com.paragon.responses.spec.FunctionTool;
import java.util.*;
import org.jspecify.annotations.NonNull;

/**
 * A tool search strategy that uses embedding vectors for semantic similarity matching.
 *
 * <p>This strategy embeds both the query and tool metadata (name + description) using an {@link
 * EmbeddingProvider}, then ranks tools by cosine similarity to the query embedding. This enables
 * semantic matching — e.g., matching "temperature outside" to a tool named "get_weather" even though
 * they share no keywords.
 *
 * <p><b>Note:</b> This strategy makes an API call to the embedding provider on each search, so it
 * has higher latency than {@link BM25ToolSearchStrategy} or {@link RegexToolSearchStrategy}. For
 * best performance, tool embeddings are computed once and cached.
 *
 * <h2>Example</h2>
 *
 * <pre>{@code
 * EmbeddingProvider provider = new OpenRouterEmbeddingProvider(apiKey);
 * ToolSearchStrategy strategy = new EmbeddingToolSearchStrategy(provider, "text-embedding-3-small", 5);
 *
 * Agent agent = Agent.builder()
 *     .name("Assistant")
 *     .toolRegistry(ToolRegistry.builder()
 *         .strategy(strategy)
 *         .deferredTools(hundredsOfTools)
 *         .build())
 *     .build();
 * }</pre>
 *
 * @see ToolSearchStrategy
 * @see EmbeddingProvider
 * @since 1.0
 */
public final class EmbeddingToolSearchStrategy implements ToolSearchStrategy {

  private final EmbeddingProvider embeddingProvider;
  private final String model;
  private final int maxResults;

  // Cached tool embeddings (computed lazily on first search)
  private List<FunctionTool<?>> cachedTools;
  private List<List<Double>> cachedToolEmbeddings;

  /**
   * Creates a new embedding-based tool search strategy.
   *
   * @param embeddingProvider the provider for creating embeddings
   * @param model the embedding model identifier (e.g., "text-embedding-3-small")
   * @param maxResults maximum number of tools to return
   */
  public EmbeddingToolSearchStrategy(
      @NonNull EmbeddingProvider embeddingProvider, @NonNull String model, int maxResults) {
    this.embeddingProvider =
        Objects.requireNonNull(embeddingProvider, "embeddingProvider cannot be null");
    this.model = Objects.requireNonNull(model, "model cannot be null");
    if (maxResults < 1) {
      throw new IllegalArgumentException("maxResults must be at least 1, got: " + maxResults);
    }
    this.maxResults = maxResults;
  }

  /**
   * Creates a new embedding-based tool search strategy with a default limit of 5.
   *
   * @param embeddingProvider the provider for creating embeddings
   * @param model the embedding model identifier
   */
  public EmbeddingToolSearchStrategy(
      @NonNull EmbeddingProvider embeddingProvider, @NonNull String model) {
    this(embeddingProvider, model, 5);
  }

  @Override
  public @NonNull List<FunctionTool<?>> search(
      @NonNull String query, @NonNull List<FunctionTool<?>> allTools) {
    Objects.requireNonNull(query, "query cannot be null");
    Objects.requireNonNull(allTools, "allTools cannot be null");

    if (query.isBlank() || allTools.isEmpty()) {
      return List.of();
    }

    // Compute/cache tool embeddings
    ensureToolEmbeddings(allTools);

    // Embed the query
    List<Embedding> queryEmbeddings =
        embeddingProvider.createEmbeddings(List.of(query), model);
    if (queryEmbeddings.isEmpty()) {
      return List.of();
    }
    List<Double> queryVector = queryEmbeddings.getFirst().embedding();

    // Score each tool by cosine similarity
    record ScoredTool(FunctionTool<?> tool, double score) {}
    List<ScoredTool> scored = new ArrayList<>();

    for (int i = 0; i < cachedTools.size(); i++) {
      double similarity = cosineSimilarity(queryVector, cachedToolEmbeddings.get(i));
      if (similarity > 0) {
        scored.add(new ScoredTool(cachedTools.get(i), similarity));
      }
    }

    scored.sort((a, bItem) -> Double.compare(bItem.score(), a.score()));

    List<FunctionTool<?>> results = new ArrayList<>();
    for (int i = 0; i < Math.min(maxResults, scored.size()); i++) {
      results.add(scored.get(i).tool());
    }

    return List.copyOf(results);
  }

  private synchronized void ensureToolEmbeddings(List<FunctionTool<?>> allTools) {
    // Recompute only if tools list changed
    if (cachedTools != null && cachedTools.equals(allTools)) {
      return;
    }

    List<String> toolTexts = new ArrayList<>();
    for (FunctionTool<?> tool : allTools) {
      StringBuilder sb = new StringBuilder(tool.getName());
      if (tool.getDescription() != null) {
        sb.append(": ").append(tool.getDescription());
      }
      toolTexts.add(sb.toString());
    }

    List<Embedding> embeddings = embeddingProvider.createEmbeddings(toolTexts, model);
    cachedToolEmbeddings = new ArrayList<>();
    for (Embedding emb : embeddings) {
      cachedToolEmbeddings.add(emb.embedding());
    }
    cachedTools = List.copyOf(allTools);
  }

  private static double cosineSimilarity(List<Double> a, List<Double> b) {
    if (a.size() != b.size()) return 0.0;
    double dotProduct = 0.0;
    double normA = 0.0;
    double normB = 0.0;
    for (int i = 0; i < a.size(); i++) {
      double ai = a.get(i);
      double bi = b.get(i);
      dotProduct += ai * bi;
      normA += ai * ai;
      normB += bi * bi;
    }
    double denominator = Math.sqrt(normA) * Math.sqrt(normB);
    return denominator == 0 ? 0.0 : dotProduct / denominator;
  }

  /**
   * Returns the maximum number of results this strategy returns.
   *
   * @return the max results limit
   */
  public int maxResults() {
    return maxResults;
  }

  /**
   * Invalidates the cached tool embeddings, forcing recomputation on the next search.
   *
   * <p>Call this if the set of deferred tools changes after construction.
   */
  public synchronized void invalidateCache() {
    cachedTools = null;
    cachedToolEmbeddings = null;
  }
}
