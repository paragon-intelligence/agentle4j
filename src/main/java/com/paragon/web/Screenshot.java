package com.paragon.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.microsoft.playwright.Page;
import org.jspecify.annotations.Nullable;

/**
 * Action to capture a screenshot of the current page or a specific element.
 *
 * @param fullPage Whether to capture a full-page screenshot or limit to the current viewport
 * @param quality  The quality of the screenshot, from 1 to 100. 100 is the highest quality.
 * @param viewport Optional viewport configuration to set before taking the screenshot
 */
public record Screenshot(
    @JsonProperty("full_page")
    @JsonPropertyDescription(
        "Whether to capture a full-page screenshot or limit to the current viewport.")
    boolean fullPage,
    @JsonProperty("quality")
    @JsonPropertyDescription(
        "The quality of the screenshot, from 1 to 100. 100 is the highest quality.")
    int quality,
    @JsonProperty("viewport") @Nullable Viewport viewport)
    implements Action {

  /**
   * Creates a Screenshot action with default values.
   *
   * @param quality The quality of the screenshot (1-100)
   * @return A new Screenshot instance
   */
  public static Screenshot of(int quality) {
    validateQuality(quality);
    return new Screenshot(false, quality, null);
  }

  /**
   * Creates a full-page Screenshot action.
   *
   * @param quality The quality of the screenshot (1-100)
   * @return A new full-page Screenshot instance
   */
  public static Screenshot fullPage(int quality) {
    validateQuality(quality);
    return new Screenshot(true, quality, null);
  }

  /**
   * Creates a Screenshot action builder.
   *
   * @return A new Builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  private static void validateQuality(int quality) {
    if (quality < 0 || quality > 100) {
      throw new IllegalArgumentException("Quality must be between 0 and 100");
    }
  }

  @Override
  public void execute(Page page) {
    if (viewport != null) {
      page.setViewportSize(viewport.width(), viewport.height());
    }

    page.screenshot(new Page.ScreenshotOptions()
        .setFullPage(fullPage)
        .setQuality(quality)
        .setType(com.microsoft.playwright.options.ScreenshotType.JPEG));
  }

  /**
   * Builder for Screenshot.
   */
  public static class Builder {
    private boolean fullPage = false;
    private int quality = 80;
    private Viewport viewport = null;

    private Builder() {}

    public Builder fullPage(boolean fullPage) {
      this.fullPage = fullPage;
      return this;
    }

    public Builder quality(int quality) {
      validateQuality(quality);
      this.quality = quality;
      return this;
    }

    public Builder viewport(@Nullable Viewport viewport) {
      this.viewport = viewport;
      return this;
    }

    public Builder viewport(int width, int height) {
      this.viewport = Viewport.of(width, height);
      return this;
    }

    public Screenshot build() {
      return new Screenshot(fullPage, quality, viewport);
    }
  }
}
