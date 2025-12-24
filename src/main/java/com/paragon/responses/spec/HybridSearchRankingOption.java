package com.paragon.responses.spec;

import org.jspecify.annotations.NonNull;

/**
 * Weights that control how reciprocal rank fusion balances semantic embedding matches versus sparse
 * keyword matches when hybrid search is enabled.
 *
 * @param embeddingWeight The weight of the embedding in the reciprocal ranking fusion.
 * @param textWeight The weight of the text in the reciprocal ranking fusion.
 */
public record HybridSearchRankingOption(
    @NonNull Number embeddingWeight, @NonNull Number textWeight) {}
