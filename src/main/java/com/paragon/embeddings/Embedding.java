package com.paragon.embeddings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import org.jspecify.annotations.NonNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Embedding(@NonNull List<Double> embedding, @NonNull Integer index) {
  public static @NonNull Embedding fromEmbeddings(@NonNull List<Double> embedding) {
    return new Embedding(embedding, 0);
  }
}
