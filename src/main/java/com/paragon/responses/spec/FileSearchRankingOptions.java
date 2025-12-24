package com.paragon.responses.spec;

import org.jspecify.annotations.Nullable;

/**
 * Ranking options for search.
 *
 * @param hybridSearch Weights that control how reciprocal rank fusion balances semantic embedding
 *     matches versus sparse keyword matches when hybrid search is enabled.
 * @param ranker The ranker to use for the file search.
 * @param scoreThreshold The score threshold for the file search, a number between 0 and 1. Numbers
 *     closer to 1 will attempt to return only the most relevant results, but may return fewer
 *     results.
 */
public record FileSearchRankingOptions(
    @Nullable HybridSearchRankingOption hybridSearch,
    @Nullable String ranker,
    @Nullable Number scoreThreshold) {}
