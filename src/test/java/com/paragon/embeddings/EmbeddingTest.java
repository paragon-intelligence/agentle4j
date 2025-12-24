package com.paragon.embeddings;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for Embedding record and EmbeddingProvider interface.
 */
class EmbeddingTest {

  @Test
  @DisplayName("Embedding can be instantiated")
  void embeddingCanBeInstantiated() {
    Embedding embedding = new Embedding();
    assertNotNull(embedding);
  }

  @Test
  @DisplayName("Embedding records are equal")
  void embeddingEquality() {
    Embedding e1 = new Embedding();
    Embedding e2 = new Embedding();
    assertEquals(e1, e2);
    assertEquals(e1.hashCode(), e2.hashCode());
  }

  @Test
  @DisplayName("EmbeddingProvider interface can be implemented")
  void embeddingProviderImplementation() {
    EmbeddingProvider provider =
        new EmbeddingProvider() {
          // Empty implementation
        };

    assertNotNull(provider);
    assertTrue(provider instanceof EmbeddingProvider);
  }
}
