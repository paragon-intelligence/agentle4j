package com.paragon.broadcasting;

import org.jspecify.annotations.NonNull;

public record MultiModalContent(
    @NonNull MultimodalContentType type,
    @NonNull Object content,
    java.util.Map<String, Object> metadata) {
  public static @NonNull MultiModalContent text(@NonNull String text) {
    return new MultiModalContent(MultimodalContentType.TEXT, text, java.util.Map.of());
  }

  public static @NonNull MultiModalContent image(@NonNull String imageUrl) {
    return new MultiModalContent(MultimodalContentType.IMAGE, imageUrl, java.util.Map.of());
  }

  public static @NonNull MultiModalContent audio(@NonNull String audioUrl) {
    return new MultiModalContent(MultimodalContentType.AUDIO, audioUrl, java.util.Map.of());
  }
}
