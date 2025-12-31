package com.paragon.web;

import com.microsoft.playwright.Browser;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Payload for web content extraction operations.
 * Use {@link #builder()} to construct instances.
 */
public class ExtractPayload {

  private final @NonNull Browser browser;
  private final @NonNull List<URI> urls;
  private final @NonNull String prompt;
  private final @NonNull ExtractionPreferences extractionPreferences;
  private final boolean ignoreInvalidUrls;

  protected ExtractPayload(
          @NonNull Browser browser,
          @NonNull List<URI> urls,
          @NonNull String prompt,
          @NonNull ExtractionPreferences extractionPreferences,
          boolean ignoreInvalidUrls
  ) {
    this.browser = Objects.requireNonNull(browser, "browser cannot be null");
    this.urls = List.copyOf(Objects.requireNonNull(urls, "urls cannot be null"));
    this.prompt = Objects.requireNonNull(prompt, "prompt cannot be null");
    this.extractionPreferences = Objects.requireNonNull(extractionPreferences, "extractionPreferences cannot be null");
    this.ignoreInvalidUrls = ignoreInvalidUrls;

    if (urls.isEmpty()) {
      throw new IllegalArgumentException("urls cannot be empty");
    }
  }

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static <T> @NonNull StructuredBuilder<T> structuredBuilder(@NonNull Class<T> outputType) {
    return new StructuredBuilder<>(outputType);
  }

  public @NonNull Browser browser() {
    return browser;
  }

  public @NonNull List<URI> urls() {
    return urls;
  }

  public @NonNull String prompt() {
    return prompt;
  }

  public @NonNull ExtractionPreferences extractionPreferences() {
    return extractionPreferences;
  }

  public boolean ignoreInvalidUrls() {
    return ignoreInvalidUrls;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (!(obj instanceof ExtractPayload that)) return false;
    return Objects.equals(browser, that.browser) &&
            Objects.equals(urls, that.urls) &&
            Objects.equals(prompt, that.prompt) &&
            Objects.equals(extractionPreferences, that.extractionPreferences) &&
            ignoreInvalidUrls == that.ignoreInvalidUrls;
  }

  @Override
  public int hashCode() {
    return Objects.hash(browser, urls, prompt, extractionPreferences, ignoreInvalidUrls);
  }

  @Override
  public String toString() {
    return "ExtractPayload{" +
            "browser=" + browser +
            ", urls=" + urls +
            ", prompt='" + prompt + '\'' +
            ", extractionPreferences=" + extractionPreferences +
            ", ignoreInvalidUrls=" + ignoreInvalidUrls +
            '}';
  }

  /**
   * Structured extraction payload that includes output type information.
   */
  public static final class Structured<T> extends ExtractPayload {

    private final @NonNull Class<T> outputType;

    Structured(
            @NonNull Browser browser,
            @NonNull List<URI> urls,
            @NonNull String prompt,
            @NonNull ExtractionPreferences extractionPreferences,
            boolean ignoreInvalidUrls,
            @NonNull Class<T> outputType
    ) {
      super(browser, urls, prompt, extractionPreferences, ignoreInvalidUrls);
      this.outputType = Objects.requireNonNull(outputType, "outputType cannot be null");
    }

    public @NonNull Class<T> outputType() {
      return outputType;
    }

    @Override
    public boolean equals(Object obj) {
      if (!super.equals(obj)) return false;
      if (!(obj instanceof Structured<?> that)) return false;
      return Objects.equals(outputType, that.outputType);
    }

    @Override
    public int hashCode() {
      return Objects.hash(super.hashCode(), outputType);
    }

    @Override
    public String toString() {
      return "Structured{" +
              "outputType=" + outputType +
              ", browser=" + browser() +
              ", urls=" + urls() +
              ", prompt='" + prompt() + '\'' +
              ", extractionPreferences=" + extractionPreferences() +
              ", ignoreInvalidUrls=" + ignoreInvalidUrls() +
              '}';
    }
  }

  /**
   * Builder for unstructured extraction payloads.
   */
  public static class Builder {
    protected @Nullable Browser browser;
    protected @Nullable List<URI> urls;
    protected @Nullable String prompt;
    protected @Nullable ExtractionPreferences extractionPreferences;
    protected boolean ignoreInvalidUrls = true;

    protected Builder() {
    }

    public @NonNull Builder browser(@NonNull Browser browser) {
      this.browser = browser;
      return this;
    }

    public @NonNull Builder urls(@NonNull List<URI> urls) {
      this.urls = new ArrayList<>(urls);
      return this;
    }

    public @NonNull Builder url(@NonNull URI url) {
      this.urls = List.of(url);
      return this;
    }

    public @NonNull Builder url(@NonNull String url) {
      this.urls = List.of(URI.create(url));
      return this;
    }

    public @NonNull Builder urls(@NonNull String... urls) {
      List<URI> uriList = new ArrayList<>(urls.length);
      for (String url : urls) {
        uriList.add(URI.create(url));
      }
      this.urls = uriList;
      return this;
    }

    public @NonNull Builder addUrl(@NonNull URI url) {
      if (this.urls == null) {
        this.urls = new ArrayList<>();
      }
      this.urls.add(url);
      return this;
    }

    public @NonNull Builder addUrl(@NonNull String url) {
      return addUrl(URI.create(url));
    }

    public @NonNull Builder prompt(@NonNull String prompt) {
      this.prompt = prompt;
      return this;
    }

    public @NonNull Builder extractionPreferences(@NonNull ExtractionPreferences preferences) {
      this.extractionPreferences = preferences;
      return this;
    }

    public @NonNull Builder ignoreInvalidUrls(boolean ignoreInvalidUrls) {
      this.ignoreInvalidUrls = ignoreInvalidUrls;
      return this;
    }

    /**
     * Configure extraction preferences using a builder callback.
     */
    public @NonNull Builder withPreferences(
            java.util.function.@NonNull Consumer<ExtractionPreferences.Builder> configurator
    ) {
      ExtractionPreferences.Builder prefBuilder = ExtractionPreferences.builder();
      configurator.accept(prefBuilder);
      this.extractionPreferences = prefBuilder.build();
      return this;
    }

    public @NonNull ExtractPayload build() {
      validate();
      return new ExtractPayload(
              browser,
              urls,
              prompt != null ? prompt : "Extract all relevant information from the page.",
              extractionPreferences != null ? extractionPreferences : ExtractionPreferences.defaults(),
              ignoreInvalidUrls
      );
    }

    protected void validate() {
      if (browser == null) {
        throw new IllegalStateException("browser is required");
      }
      if (urls == null || urls.isEmpty()) {
        throw new IllegalStateException("at least one URL is required");
      }
    }
  }

  /**
   * Builder for structured extraction payloads with type information.
   */
  public static final class StructuredBuilder<T> extends Builder {

    private final @NonNull Class<T> outputType;

    StructuredBuilder(@NonNull Class<T> outputType) {
      this.outputType = Objects.requireNonNull(outputType, "outputType cannot be null");
    }

    @Override
    public @NonNull StructuredBuilder<T> browser(@NonNull Browser browser) {
      super.browser(browser);
      return this;
    }

    @Override
    public @NonNull StructuredBuilder<T> urls(@NonNull List<URI> urls) {
      super.urls(urls);
      return this;
    }

    @Override
    public @NonNull StructuredBuilder<T> url(@NonNull URI url) {
      super.url(url);
      return this;
    }

    @Override
    public @NonNull StructuredBuilder<T> url(@NonNull String url) {
      super.url(url);
      return this;
    }

    @Override
    public @NonNull StructuredBuilder<T> urls(@NonNull String... urls) {
      super.urls(urls);
      return this;
    }

    @Override
    public @NonNull StructuredBuilder<T> addUrl(@NonNull URI url) {
      super.addUrl(url);
      return this;
    }

    @Override
    public @NonNull StructuredBuilder<T> addUrl(@NonNull String url) {
      super.addUrl(url);
      return this;
    }

    @Override
    public @NonNull StructuredBuilder<T> prompt(@NonNull String prompt) {
      super.prompt(prompt);
      return this;
    }

    @Override
    public @NonNull StructuredBuilder<T> extractionPreferences(@NonNull ExtractionPreferences preferences) {
      super.extractionPreferences(preferences);
      return this;
    }

    @Override
    public @NonNull StructuredBuilder<T> ignoreInvalidUrls(boolean ignoreInvalidUrls) {
      super.ignoreInvalidUrls(ignoreInvalidUrls);
      return this;
    }

    @Override
    public @NonNull StructuredBuilder<T> withPreferences(
            java.util.function.@NonNull Consumer<ExtractionPreferences.Builder> configurator
    ) {
      super.withPreferences(configurator);
      return this;
    }

    @Override
    public @NonNull Structured<T> build() {
      validate();
      return new Structured<>(
              browser,
              urls,
              prompt != null ? prompt : "Extract all relevant information from the page.",
              extractionPreferences != null ? extractionPreferences : ExtractionPreferences.defaults(),
              ignoreInvalidUrls,
              outputType
      );
    }
  }
}