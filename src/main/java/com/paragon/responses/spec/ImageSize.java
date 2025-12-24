package com.paragon.responses.spec;

/**
 * The size of the generated image. One of {@code 1024x1024}, {@code 1024x1536}, {@code 1536x1024},
 * or {@code auto}.
 */
public enum ImageSize {
  AUTO("auto"),
  SQUARE_1024("1024x1024"),
  PORTRAIT_1024("1024x1536"),
  LANDSCAPE_1024("1536x1024");

  final String value;

  ImageSize(String value) {
    this.value = value;
  }
}
