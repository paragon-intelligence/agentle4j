package com.paragon.web;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.List;
import java.util.Objects;

/**
 * Result of a web content extraction operation.
 * Contains the extracted HTML, Markdown, and optionally structured output.
 */
public class ExtractionResult {

  private final @NonNull List<URI> urls;
  private final @NonNull List<String> html;
  private final @NonNull List<String> markdown;
  private final @NonNull String combinedMarkdown;
  private final @NonNull ExtractionPreferences extractionPreferences;
  private final @NonNull List<ExtractionError> errors;

  public ExtractionResult(
          @NonNull List<URI> urls,
          @NonNull List<String> html,
          @NonNull List<String> markdown,
          @NonNull String combinedMarkdown,
          @NonNull ExtractionPreferences extractionPreferences,
          @NonNull List<ExtractionError> errors
  ) {
    this.urls = List.copyOf(urls);
    this.html = List.copyOf(html);
    this.markdown = List.copyOf(markdown);
    this.combinedMarkdown = combinedMarkdown;
    this.extractionPreferences = extractionPreferences;
    this.errors = List.copyOf(errors);
  }

  public @NonNull List<URI> urls() {
    return urls;
  }

  public @NonNull List<String> html() {
    return html;
  }

  public @NonNull List<String> markdown() {
    return markdown;
  }

  public @NonNull String combinedMarkdown() {
    return combinedMarkdown;
  }

  public @NonNull ExtractionPreferences extractionPreferences() {
    return extractionPreferences;
  }

  public @NonNull List<ExtractionError> errors() {
    return errors;
  }

  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  public boolean isSuccessful() {
    return errors.isEmpty() && !markdown.isEmpty();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ExtractionResult that)) return false;
    return Objects.equals(urls, that.urls) &&
            Objects.equals(html, that.html) &&
            Objects.equals(markdown, that.markdown) &&
            Objects.equals(combinedMarkdown, that.combinedMarkdown) &&
            Objects.equals(extractionPreferences, that.extractionPreferences) &&
            Objects.equals(errors, that.errors);
  }

  @Override
  public int hashCode() {
    return Objects.hash(urls, html, markdown, combinedMarkdown, extractionPreferences, errors);
  }

  @Override
  public String toString() {
    return "ExtractionResult{" +
            "urls=" + urls +
            ", htmlCount=" + html.size() +
            ", markdownCount=" + markdown.size() +
            ", errorsCount=" + errors.size() +
            '}';
  }

  public enum ErrorType {
    NAVIGATION,
    TIMEOUT,
    PARSING,
    LLM_PROCESSING,
    UNKNOWN
  }

  /**
   * Structured extraction result containing parsed output.
   */
  public static final class Structured<T> extends ExtractionResult {

    private final @Nullable T output;
    private final @NonNull Class<T> outputType;

    public Structured(
            @NonNull List<URI> urls,
            @NonNull List<String> html,
            @NonNull List<String> markdown,
            @NonNull String combinedMarkdown,
            @NonNull ExtractionPreferences extractionPreferences,
            @NonNull List<ExtractionError> errors,
            @Nullable T output,
            @NonNull Class<T> outputType
    ) {
      super(urls, html, markdown, combinedMarkdown, extractionPreferences, errors);
      this.output = output;
      this.outputType = outputType;
    }

    public @Nullable T output() {
      return output;
    }

    public @NonNull Class<T> outputType() {
      return outputType;
    }

    /**
     * Get the output, throwing if not present.
     *
     * @throws IllegalStateException if output is null
     */
    public @NonNull T requireOutput() {
      if (output == null) {
        throw new IllegalStateException("Extraction output is null - check errors()");
      }
      return output;
    }

    @Override
    public boolean isSuccessful() {
      return super.isSuccessful() && output != null;
    }

    @Override
    public boolean equals(Object o) {
      if (!super.equals(o)) return false;
      if (!(o instanceof Structured<?> that)) return false;
      return Objects.equals(output, that.output) &&
              Objects.equals(outputType, that.outputType);
    }

    @Override
    public int hashCode() {
      return Objects.hash(super.hashCode(), output, outputType);
    }

    @Override
    public String toString() {
      return "Structured{" +
              "outputType=" + outputType.getSimpleName() +
              ", hasOutput=" + (output != null) +
              ", urls=" + urls() +
              ", htmlCount=" + html().size() +
              ", markdownCount=" + markdown().size() +
              ", errorsCount=" + errors().size() +
              '}';
    }
  }

  /**
   * Represents an error that occurred during extraction.
   */
  public record ExtractionError(
          @NonNull URI url,
          @NonNull ErrorType type,
          @NonNull String message,
          @Nullable Throwable cause
  ) {
    public ExtractionError {
      Objects.requireNonNull(url, "url cannot be null");
      Objects.requireNonNull(type, "type cannot be null");
      Objects.requireNonNull(message, "message cannot be null");
    }

    public static @NonNull ExtractionError navigation(@NonNull URI url, @NonNull String message, @Nullable Throwable cause) {
      return new ExtractionError(url, ErrorType.NAVIGATION, message, cause);
    }

    public static @NonNull ExtractionError timeout(@NonNull URI url, @NonNull String message, @Nullable Throwable cause) {
      return new ExtractionError(url, ErrorType.TIMEOUT, message, cause);
    }

    public static @NonNull ExtractionError parsing(@NonNull URI url, @NonNull String message, @Nullable Throwable cause) {
      return new ExtractionError(url, ErrorType.PARSING, message, cause);
    }

    public static @NonNull ExtractionError llm(@NonNull URI url, @NonNull String message, @Nullable Throwable cause) {
      return new ExtractionError(url, ErrorType.LLM_PROCESSING, message, cause);
    }
  }
}