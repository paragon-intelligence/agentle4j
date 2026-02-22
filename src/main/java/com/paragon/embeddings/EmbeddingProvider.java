package com.paragon.embeddings;

import java.util.List;
import org.jspecify.annotations.NonNull;

/**
 * Interface for embedding providers.
 *
 * <p>Uses synchronous API optimized for Java 21+ virtual threads.
 */
public interface EmbeddingProvider {

  /**
   * Creates embeddings for the given inputs.
   *
   * @param input the input texts to embed
   * @param model the embedding model to use
   * @return the list of embeddings
   */
  @NonNull List<Embedding> createEmbeddings(@NonNull List<String> input, @NonNull String model);
}
