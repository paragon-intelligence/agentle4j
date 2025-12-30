package com.paragon.embeddings;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.NonNull;

public interface EmbeddingProvider {
  @NonNull CompletableFuture<List<Embedding>> createEmbeddings(
      @NonNull List<String> input, @NonNull String model);
}
