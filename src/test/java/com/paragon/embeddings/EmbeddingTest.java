package com.paragon.embeddings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for Embedding record and EmbeddingProvider interface. */
class EmbeddingTest {

  @Test
  @DisplayName("Embedding can be instantiated")
  void embeddingCanBeInstantiated() {
    Embedding embedding = Embedding.fromEmbeddings(List.of(1.0, 2.0, 3.0));
    assertNotNull(embedding);
  }

  @Test
  @DisplayName("Embedding records are equal")
  void embeddingEquality() {
    Embedding e1 = Embedding.fromEmbeddings(List.of(1.0, 2.0, 3.0));
    Embedding e2 = Embedding.fromEmbeddings(List.of(1.0, 2.0, 3.0));
    assertEquals(e1, e2);
    assertEquals(e1.hashCode(), e2.hashCode());
  }

  @Test
  @DisplayName("EmbeddingProvider interface can be implemented")
  void embeddingProviderImplementation() {
    EmbeddingProvider provider = (input, model) -> List.of();

    assertNotNull(provider);
  }
}
