package com.paragon.web;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Geolocation;
import com.paragon.responses.Responder;
import com.paragon.responses.spec.CreateResponsePayload;
import com.paragon.responses.spec.ParsedResponse;
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extracts structured data from web pages using Playwright for rendering and an LLM for intelligent
 * data extraction.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * WebExtractor extractor = WebExtractor.create(responder);
 *
 * // Unstructured extraction
 * ExtractionResult result = extractor.extract(
 *     ExtractPayload.builder()
 *         .browser(browser)
 *         .url("https://example.com")
 *         .prompt("Extract all article titles")
 *         .withPreferences(p -> p
 *             .onlyMainContent(true)
 *             .timeoutMs(15000))
 *         .build()
 * ).join();
 *
 * // Structured extraction
 * record Article(String title, String author) {}
 *
 * ExtractionResult.Structured<Article> structured = extractor.extract(
 *     ExtractPayload.structuredBuilder(Article.class)
 *         .browser(browser)
 *         .url("https://example.com/article")
 *         .prompt("Extract the article title and author")
 *         .build()
 * ).join();
 *
 * Article article = structured.requireOutput();
 * }</pre>
 */
public final class WebExtractor {

  private static final Logger log = LoggerFactory.getLogger(WebExtractor.class);

  private static final String DEFAULT_SYSTEM_PROMPT =
      """
      <role>
      You are a precise data extraction specialist that converts web content into structured JSON.
      </role>

      <task>
      Extract the information specified in `user_instructions` from the Markdown content provided in `<markdown>` tags. Return the extracted data as a single valid JSON object matching the provided schema.
      </task>

      <rules>
      1. Extract only information explicitly present in the text content
      2. Return `null` for fields where information cannot be found
      3. Match the output schema exactly - include all required fields
      4. Output only the JSON object, starting with `{` and ending with `}`
      5. Do not include any explanation, preamble, or markdown formatting
      </rules>
      """;

  private static final String USER_PROMPT_TEMPLATE =
      """
      <user_instructions>
      %s
      </user_instructions>

      <markdown>
      %s
      </markdown>
      """;

  private static final Pattern BASE64_DATA_URI_PATTERN = Pattern.compile("data:image/[^\"')\\s]+");
  private static final Set<String> AD_DOMAINS =
      Set.of(
          "doubleclick.net",
          "googlesyndication.com",
          "adservice.google.com",
          "ads.",
          "analytics.",
          "tracking.",
          "facebook.com/tr",
          "googletagmanager.com");

  private static final String MOBILE_USER_AGENT =
      "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X) AppleWebKit/605.1.15";
  private static final int MOBILE_VIEWPORT_WIDTH = 375;
  private static final int MOBILE_VIEWPORT_HEIGHT = 667;

  private final @NonNull Responder responder;
  private final @Nullable String model;
  private final @NonNull Executor executor;
  private final @NonNull FlexmarkHtmlConverter htmlConverter;

  private WebExtractor(
      @NonNull Responder responder, @Nullable String model, @NonNull Executor executor) {
    this.responder = Objects.requireNonNull(responder, "responder cannot be null");
    this.model = model;
    this.executor = Objects.requireNonNull(executor, "executor cannot be null");
    this.htmlConverter = FlexmarkHtmlConverter.builder().build();
  }

  /** Creates a new WebExtractor with default settings. */
  public static @NonNull WebExtractor create(@NonNull Responder responder) {
    return new WebExtractor(responder, null, ForkJoinPool.commonPool());
  }

  /** Creates a new WebExtractor with a specific model. */
  public static @NonNull WebExtractor create(@NonNull Responder responder, @NonNull String model) {
    return new WebExtractor(responder, model, ForkJoinPool.commonPool());
  }

  /** Creates a new WebExtractor with custom executor. */
  public static @NonNull WebExtractor create(
      @NonNull Responder responder, @Nullable String model, @NonNull Executor executor) {
    return new WebExtractor(responder, model, executor);
  }

  /**
   * Extracts content from web pages without structured output parsing. Returns HTML and Markdown
   * content for all URLs.
   *
   * @param payload the extraction configuration
   * @return a future containing the extraction result
   */
  public @NonNull CompletableFuture<ExtractionResult> extract(@NonNull ExtractPayload payload) {
    Objects.requireNonNull(payload, "payload cannot be null");

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return extractSync(payload);
          } catch (Exception e) {
            log.error("Extraction failed", e);
            throw new CompletionException("Extraction failed: " + e.getMessage(), e);
          }
        },
        executor);
  }

  /**
   * Extracts structured data from web pages using LLM processing. The extracted content is parsed
   * into the specified output type.
   *
   * @param payload the structured extraction configuration
   * @param <T> the output type
   * @return a future containing the structured extraction result
   */
  public <T> @NonNull CompletableFuture<ExtractionResult.Structured<T>> extract(
      ExtractPayload.@NonNull Structured<T> payload) {
    Objects.requireNonNull(payload, "payload cannot be null");

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return extractStructuredSync(payload);
          } catch (Exception e) {
            log.error("Structured extraction failed", e);
            throw new CompletionException("Structured extraction failed: " + e.getMessage(), e);
          }
        },
        executor);
  }

  // ========== Internal Implementation ==========

  private @NonNull ExtractionResult extractSync(@NonNull ExtractPayload payload) {
    List<String> htmlList = new ArrayList<>();
    List<String> markdownList = new ArrayList<>();
    List<ExtractionResult.ExtractionError> errors = new ArrayList<>();

    BrowserContext context = null;
    try {
      context = createBrowserContext(payload.browser(), payload.extractionPreferences());
      Page page = context.newPage();

      for (URI url : payload.urls()) {
        try {
          ContentResult content = extractContentFromUrl(page, url, payload.extractionPreferences());
          htmlList.add(content.html());
          markdownList.add(content.markdown());
        } catch (Exception e) {
          log.warn("Failed to extract from URL: {}", url, e);
          if (!payload.ignoreInvalidUrls()) {
            throw e;
          }
          errors.add(categorizeError(url, e));
        }
      }

      String combinedMarkdown = String.join("\n\n---\n\n", markdownList);

      return new ExtractionResult(
          payload.urls(),
          htmlList,
          markdownList,
          combinedMarkdown,
          payload.extractionPreferences(),
          errors);
    } finally {
      closeQuietly(context);
    }
  }

  private <T> ExtractionResult.@NonNull Structured<T> extractStructuredSync(
      ExtractPayload.@NonNull Structured<T> payload) {
    // First extract the raw content
    ExtractionResult rawResult = extractSync(payload);

    T output = null;
    List<ExtractionResult.ExtractionError> errors = new ArrayList<>(rawResult.errors());

    if (!rawResult.combinedMarkdown().isBlank()) {
      try {
        output =
            processWithLlm(rawResult.combinedMarkdown(), payload.prompt(), payload.outputType());
      } catch (Exception e) {
        log.error("LLM processing failed", e);
        URI firstUrl =
            payload.urls().isEmpty() ? URI.create("unknown://") : payload.urls().getFirst();
        errors.add(
            ExtractionResult.ExtractionError.llm(
                firstUrl, "LLM processing failed: " + e.getMessage(), e));
      }
    }

    return new ExtractionResult.Structured<>(
        payload.urls(),
        rawResult.html(),
        rawResult.markdown(),
        rawResult.combinedMarkdown(),
        payload.extractionPreferences(),
        errors,
        output,
        payload.outputType());
  }

  private @NonNull BrowserContext createBrowserContext(
      @NonNull Browser browser, @NonNull ExtractionPreferences preferences) {
    Browser.NewContextOptions options = new Browser.NewContextOptions();

    // Configure viewport and user agent for mobile
    if (preferences.mobile()) {
      options.setViewportSize(MOBILE_VIEWPORT_WIDTH, MOBILE_VIEWPORT_HEIGHT);
      options.setUserAgent(MOBILE_USER_AGENT);
      options.setIsMobile(true);
    }

    // Configure extra headers
    if (preferences.headers() != null && !preferences.headers().isEmpty()) {
      options.setExtraHTTPHeaders(preferences.headers());
    }

    // Configure TLS verification
    if (preferences.skipTlsVerification()) {
      options.setIgnoreHTTPSErrors(true);
    }

    // Configure geolocation
    if (preferences.location() != null) {
      options.setGeolocation(
          new Geolocation(preferences.location().latitude(), preferences.location().longitude()));
      options.setPermissions(List.of("geolocation"));
    }

    BrowserContext context = browser.newContext(options);

    // Configure ad blocking
    if (preferences.blockAds()) {
      context.route(
          "**/*",
          route -> {
            String url = route.request().url().toLowerCase();
            String resourceType = route.request().resourceType();

            boolean isBlockedResource =
                ("image".equals(resourceType)
                        || "media".equals(resourceType)
                        || "font".equals(resourceType))
                    && AD_DOMAINS.stream().anyMatch(url::contains);

            if (isBlockedResource) {
              route.abort();
            } else {
              route.resume();
            }
          });
    }

    return context;
  }

  private @NonNull ContentResult extractContentFromUrl(
      @NonNull Page page, @NonNull URI url, @NonNull ExtractionPreferences preferences) {
    // Navigate to URL with timeout
    Page.NavigateOptions navOptions =
        new Page.NavigateOptions().setTimeout(preferences.timeoutMs());

    page.navigate(url.toString(), navOptions);

    // Wait for specified time if configured
    if (preferences.waitForMs() > 0) {
      page.waitForTimeout(preferences.waitForMs());
    }

    // Get HTML content
    String html = page.content();

    // Process HTML based on preferences
    html = processHtml(html, preferences);

    // Convert to Markdown
    String markdown = htmlConverter.convert(html);

    // Clean up markdown if base64 removal was requested
    if (preferences.removeBase64Images()) {
      markdown = removeBase64FromMarkdown(markdown);
    }

    return new ContentResult(html, markdown);
  }

  private @NonNull String processHtml(
      @NonNull String html, @NonNull ExtractionPreferences preferences) {
    if (!needsHtmlProcessing(preferences)) {
      return html;
    }

    Document doc = Jsoup.parse(html);

    // Remove base64 images
    if (preferences.removeBase64Images()) {
      removeBase64Images(doc);
    }

    // Extract main content
    if (preferences.onlyMainContent()) {
      doc = extractMainContent(doc);
    }

    // Exclude specific tags
    if (preferences.excludeTags() != null && !preferences.excludeTags().isEmpty()) {
      for (String tag : preferences.excludeTags()) {
        doc.select(tag).remove();
      }
    }

    // Include only specific tags
    if (preferences.includeTags() != null && !preferences.includeTags().isEmpty()) {
      Document newDoc = Document.createShell(doc.baseUri());
      for (String tag : preferences.includeTags()) {
        Elements elements = doc.select(tag);
        for (Element element : elements) {
          newDoc.body().appendChild(element.clone());
        }
      }
      doc = newDoc;
    }

    return doc.html();
  }

  private boolean needsHtmlProcessing(@NonNull ExtractionPreferences preferences) {
    return preferences.removeBase64Images()
        || preferences.onlyMainContent()
        || (preferences.includeTags() != null && !preferences.includeTags().isEmpty())
        || (preferences.excludeTags() != null && !preferences.excludeTags().isEmpty());
  }

  private void removeBase64Images(@NonNull Document doc) {
    // Remove img tags with base64 src
    doc.select("img[src^='data:image/']").remove();

    // Remove anchor tags containing base64 images
    for (Element a : doc.select("a")) {
      if (!a.select("img[src^='data:image/']").isEmpty()) {
        a.remove();
      }
    }

    // Remove elements with base64 href
    doc.select("[href^='data:image/']").remove();

    // Remove elements with base64 in style
    for (Element elem : doc.select("[style*='data:image/']")) {
      elem.remove();
    }

    // Remove SVG elements (often contain embedded data)
    doc.select("svg").remove();

    // Remove anchor tags containing SVGs
    for (Element a : doc.select("a")) {
      if (!a.select("svg").isEmpty()) {
        a.remove();
      }
    }
  }

  private @NonNull Document extractMainContent(@NonNull Document doc) {
    // Try to find main content in order of preference
    Element mainContent = doc.selectFirst("main");
    if (mainContent == null) {
      mainContent = doc.selectFirst("article");
    }
    if (mainContent == null) {
      mainContent = doc.selectFirst("#content");
    }
    if (mainContent == null) {
      mainContent = doc.selectFirst(".content");
    }
    if (mainContent == null) {
      mainContent = doc.selectFirst("[role='main']");
    }

    if (mainContent != null) {
      Document newDoc = Document.createShell(doc.baseUri());
      newDoc.body().appendChild(mainContent.clone());
      return newDoc;
    }

    return doc;
  }

  private @NonNull String removeBase64FromMarkdown(@NonNull String markdown) {
    // Remove any remaining base64 data URIs from markdown
    return BASE64_DATA_URI_PATTERN.matcher(markdown).replaceAll("[image removed]");
  }

  private <T> @NonNull T processWithLlm(
      @NonNull String markdown, @NonNull String prompt, @NonNull Class<T> outputType) {
    String userPrompt = String.format(USER_PROMPT_TEMPLATE, prompt, markdown);

    ParsedResponse<T> response =
        responder
            .<T>respond(
                CreateResponsePayload.<T>builder()
                    .addDeveloperMessage(DEFAULT_SYSTEM_PROMPT)
                    .addUserMessage(userPrompt)
                    .model(model)
                    .withStructuredOutput(outputType)
                    .build())
            .join();

    return response.outputParsed();
  }

  private ExtractionResult.@NonNull ExtractionError categorizeError(
      @NonNull URI url, @NonNull Exception e) {
    String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();

    if (e instanceof TimeoutError) {
      return ExtractionResult.ExtractionError.timeout(url, message, e);
    } else if (e instanceof PlaywrightException) {
      if (message.toLowerCase().contains("timeout")) {
        return ExtractionResult.ExtractionError.timeout(url, message, e);
      }
      return ExtractionResult.ExtractionError.navigation(url, message, e);
    } else {
      return new ExtractionResult.ExtractionError(
          url, ExtractionResult.ErrorType.UNKNOWN, message, e);
    }
  }

  private void closeQuietly(@Nullable AutoCloseable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (Exception e) {
        log.debug("Failed to close resource", e);
      }
    }
  }

  private record ContentResult(@NonNull String html, @NonNull String markdown) {}
}
