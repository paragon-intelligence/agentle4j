package com.paragon.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Represents a viewport configuration with width and height dimensions.
 *
 * @param width  The width of the viewport in pixels
 * @param height The height of the viewport in pixels
 */
public record Viewport(
    @JsonProperty("width") @JsonPropertyDescription("The width of the viewport in pixels") int width,
    @JsonProperty("height") @JsonPropertyDescription("The height of the viewport in pixels") int height) {

  /**
   * Creates a Viewport.
   *
   * @param width  The width of the viewport in pixels
   * @param height The height of the viewport in pixels
   * @return A new Viewport instance
   */
  public static Viewport of(int width, int height) {
    return new Viewport(width, height);
  }
}
