package com.paragon.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.Media;
import org.jspecify.annotations.NonNull;

/**
 * Action to generate a PDF of the current page.
 *
 * <p>The PDF will be returned as a byte array.
 *
 * @param format    The format of the PDF to generate
 * @param landscape Whether to generate the PDF in landscape orientation
 * @param scale     The scale multiplier of the resulting PDF (0.1 to 10.0)
 */
public record GeneratePdf(
    @JsonProperty("format")
    @JsonPropertyDescription("The format of the PDF to generate.")
    @NonNull PdfFormat format,
    @JsonProperty("landscape")
    @JsonPropertyDescription("Whether to generate the PDF in landscape orientation.")
    boolean landscape,
    @JsonProperty("scale")
    @JsonPropertyDescription("The scale multiplier of the resulting PDF.")
    double scale)
    implements Action {

  /**
   * Creates a GeneratePdf action with default values.
   *
   * @return A new GeneratePdf instance with Letter format, portrait orientation, and scale 1.0
   */
  public static GeneratePdf defaults() {
    return new GeneratePdf(PdfFormat.LETTER, false, 1.0);
  }

  /**
   * Creates a GeneratePdf action with the specified format.
   *
   * @param format The format of the PDF
   * @return A new GeneratePdf instance
   */
  public static GeneratePdf of(@NonNull PdfFormat format) {
    return new GeneratePdf(format, false, 1.0);
  }

  /**
   * Creates a GeneratePdf action builder.
   *
   * @return A new Builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  @Override
  public void execute(Page page) {
    page.pdf(new Page.PdfOptions()
        .setFormat(format.getValue())
        .setLandscape(landscape)
        .setScale(scale));
  }

  /**
   * Builder for GeneratePdf.
   */
  public static class Builder {
    private PdfFormat format = PdfFormat.LETTER;
    private boolean landscape = false;
    private double scale = 1.0;

    private Builder() {}

    public Builder format(@NonNull PdfFormat format) {
      this.format = format;
      return this;
    }

    public Builder landscape(boolean landscape) {
      this.landscape = landscape;
      return this;
    }

    public Builder scale(double scale) {
      if (scale < 0.1 || scale > 10.0) {
        throw new IllegalArgumentException("Scale must be between 0.1 and 10.0");
      }
      this.scale = scale;
      return this;
    }

    public GeneratePdf build() {
      return new GeneratePdf(format, landscape, scale);
    }
  }
}
