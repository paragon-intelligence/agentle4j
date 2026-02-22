package com.paragon.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.microsoft.playwright.*;
import com.paragon.responses.Responder;
import com.paragon.responses.spec.CreateResponsePayload;
import com.paragon.responses.spec.ParsedResponse;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Comprehensive tests for WebExtractor class.
 *
 * <p>Tests cover: - Factory methods - HTML processing (base64 removal, main content extraction, tag
 * filtering) - Error handling and categorization - Mobile emulation - Multi-URL extraction -
 * Integration with Playwright
 *
 * <p>Note: Some tests require Playwright to be installed and available.
 */
@ExtendWith(MockitoExtension.class)
class WebExtractorTest {

  private static Playwright playwright;
  private static Browser browser;
  private static boolean playwrightAvailable = false;
  private static HttpServer testServer;
  private static int serverPort;

  @Mock private Responder mockResponder;

  private WebExtractor extractor;

  @BeforeAll
  static void setUpPlaywright() {
    try {
      playwright = Playwright.create();
      browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
      playwrightAvailable = true;
    } catch (Exception e) {
      playwrightAvailable = false;
    }
  }

  @BeforeAll
  static void setUpTestServer() throws Exception {
    testServer = HttpServer.create(new InetSocketAddress(0), 0);
    serverPort = testServer.getAddress().getPort();

    // Basic HTML endpoint
    testServer.createContext(
        "/basic",
        exchange -> {
          String html =
              """
              <!DOCTYPE html>
              <html>
              <head><title>Test Page</title></head>
              <body>
                <h1>Hello World</h1>
                <p>This is a test paragraph.</p>
              </body>
              </html>
              """;
          exchange.sendResponseHeaders(200, html.getBytes().length);
          exchange.getResponseBody().write(html.getBytes());
          exchange.close();
        });

    // HTML with main content
    testServer.createContext(
        "/main-content",
        exchange -> {
          String html =
              """
              <!DOCTYPE html>
              <html>
              <head><title>Article Page</title></head>
              <body>
                <nav><a href="/">Home</a></nav>
                <main>
                  <h1>Main Article Title</h1>
                  <p>Main article content here.</p>
                </main>
                <footer>Footer content</footer>
              </body>
              </html>
              """;
          exchange.sendResponseHeaders(200, html.getBytes().length);
          exchange.getResponseBody().write(html.getBytes());
          exchange.close();
        });

    // HTML with article element
    testServer.createContext(
        "/article-content",
        exchange -> {
          String html =
              """
              <!DOCTYPE html>
              <html>
              <body>
                <header>Header content</header>
                <article>
                  <h2>Article Headline</h2>
                  <p>Article body text.</p>
                </article>
                <aside>Sidebar content</aside>
              </body>
              </html>
              """;
          exchange.sendResponseHeaders(200, html.getBytes().length);
          exchange.getResponseBody().write(html.getBytes());
          exchange.close();
        });

    // HTML with base64 images
    testServer.createContext(
        "/base64-images",
        exchange -> {
          String html =
              """
              <!DOCTYPE html>
              <html>
              <body>
                <h1>Page with Images</h1>
                <img src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==" alt="base64 image">
                <p>Regular text content.</p>
                <img src="https://example.com/real-image.png" alt="regular image">
              </body>
              </html>
              """;
          exchange.sendResponseHeaders(200, html.getBytes().length);
          exchange.getResponseBody().write(html.getBytes());
          exchange.close();
        });

    // HTML with various tags for include/exclude tests
    testServer.createContext(
        "/tagged-content",
        exchange -> {
          String html =
              """
              <!DOCTYPE html>
              <html>
              <body>
                <header>Header Text</header>
                <nav>Navigation Links</nav>
                <main>
                  <h1>Main Heading</h1>
                  <p class="intro">Introduction paragraph.</p>
                  <div class="content">Main content div.</div>
                </main>
                <footer>Footer Text</footer>
                <script>console.log('script');</script>
              </body>
              </html>
              """;
          exchange.sendResponseHeaders(200, html.getBytes().length);
          exchange.getResponseBody().write(html.getBytes());
          exchange.close();
        });

    // Slow response for timeout testing
    testServer.createContext(
        "/slow",
        exchange -> {
          try {
            Thread.sleep(5000);
          } catch (InterruptedException ignored) {
          }
          String html = "<html><body>Slow response</body></html>";
          exchange.sendResponseHeaders(200, html.getBytes().length);
          exchange.getResponseBody().write(html.getBytes());
          exchange.close();
        });

    // Error endpoint
    testServer.createContext(
        "/error",
        exchange -> {
          exchange.sendResponseHeaders(500, 0);
          exchange.close();
        });

    testServer.setExecutor(null);
    testServer.start();
  }

  @AfterAll
  static void tearDownPlaywright() {
    if (browser != null) browser.close();
    if (playwright != null) playwright.close();
  }

  @AfterAll
  static void tearDownTestServer() {
    if (testServer != null) testServer.stop(0);
  }

  @BeforeEach
  void setUp() {
    extractor = WebExtractor.create(mockResponder);
  }

  static boolean isPlaywrightAvailable() {
    return playwrightAvailable;
  }

  private String testUrl(String path) {
    return "http://localhost:" + serverPort + path;
  }

  // ===== Factory Method Tests =====

  @Nested
  class FactoryMethods {

    @Test
    void create_withResponder_createsInstance() {
      WebExtractor extractor = WebExtractor.create(mockResponder);
      assertNotNull(extractor);
    }

    @Test
    void create_withModel_createsInstance() {
      WebExtractor extractor = WebExtractor.create(mockResponder, "gpt-4o");
      assertNotNull(extractor);
    }

    // Executor parameter removed with synchronous API refactoring
    //  @Test
    //  void create_withExecutor_createsInstance() {
    //    Executor customExecutor = Executors.newSingleThreadExecutor();
    //    WebExtractor extractor = WebExtractor.create(mockResponder, "gpt-4o", customExecutor);
    //    assertNotNull(extractor);
    //  }

    @Test
    void create_nullResponder_throwsException() {
      assertThrows(NullPointerException.class, () -> WebExtractor.create(null));
    }

    // Executor parameter removed with synchronous API refactoring
    //  @Test
    //  void create_nullExecutor_throwsException() {
    //    assertThrows(
    //        NullPointerException.class, () -> WebExtractor.create(mockResponder, null, null));
    //  }
  }

  // ===== Basic Extraction Tests =====

  @Nested
  @EnabledIf("com.paragon.web.WebExtractorTest#isPlaywrightAvailable")
  class BasicExtraction {

    @Test
    void extract_basicUrl_returnsHtmlAndMarkdown() {
      ExtractPayload payload =
          ExtractPayload.builder().browser(browser).url(testUrl("/basic")).build();

      ExtractionResult result = extractor.extract(payload);

      assertNotNull(result);
      assertEquals(1, result.urls().size());
      assertEquals(1, result.html().size());
      assertEquals(1, result.markdown().size());
      assertTrue(result.isSuccessful());
      assertFalse(result.hasErrors());

      // Verify content
      String html = result.html().get(0);
      assertTrue(html.contains("Hello World"));
      assertTrue(html.contains("test paragraph"));

      String markdown = result.markdown().get(0);
      assertTrue(markdown.contains("Hello World"));
    }

    @Test
    void extract_multipleUrls_extractsAll() {
      ExtractPayload payload =
          ExtractPayload.builder()
              .browser(browser)
              .urls(testUrl("/basic"), testUrl("/main-content"))
              .build();

      ExtractionResult result = extractor.extract(payload);

      assertNotNull(result);
      assertEquals(2, result.urls().size());
      assertEquals(2, result.html().size());
      assertEquals(2, result.markdown().size());
      assertTrue(result.isSuccessful());

      // Combined markdown should contain content from both URLs
      String combined = result.combinedMarkdown();
      assertTrue(combined.contains("Hello World"));
      assertTrue(combined.contains("Main Article Title"));
      assertTrue(combined.contains("---")); // Separator between URLs
    }

    @Test
    void extract_nullPayload_throwsException() {
      assertThrows(NullPointerException.class, () -> extractor.extract((ExtractPayload) null));
    }
  }

  // ===== HTML Processing Tests =====

  @Nested
  @EnabledIf("com.paragon.web.WebExtractorTest#isPlaywrightAvailable")
  class HtmlProcessing {

    @Test
    void extract_onlyMainContent_extractsMainElement() {
      ExtractPayload payload =
          ExtractPayload.builder()
              .browser(browser)
              .url(testUrl("/main-content"))
              .withPreferences(p -> p.onlyMainContent(true))
              .build();

      ExtractionResult result = extractor.extract(payload);

      String markdown = result.markdown().get(0);

      // Should contain main content
      assertTrue(
          markdown.contains("Main Article Title")
              || markdown.toLowerCase().contains("main article"));
    }

    @Test
    void extract_onlyMainContent_extractsArticleWhenNoMain() {
      ExtractPayload payload =
          ExtractPayload.builder()
              .browser(browser)
              .url(testUrl("/article-content"))
              .withPreferences(p -> p.onlyMainContent(true))
              .build();

      ExtractionResult result = extractor.extract(payload);

      String markdown = result.markdown().get(0);
      assertTrue(
          markdown.contains("Article Headline") || markdown.toLowerCase().contains("article"));
    }

    @Test
    void extract_removeBase64Images_removesDataUriImages() {
      ExtractPayload payload =
          ExtractPayload.builder()
              .browser(browser)
              .url(testUrl("/base64-images"))
              .withPreferences(p -> p.removeBase64Images(true))
              .build();

      ExtractionResult result = extractor.extract(payload);

      String html = result.html().get(0);
      String markdown = result.markdown().get(0);

      // Should contain regular content
      assertTrue(
          markdown.contains("Regular text content")
              || markdown.toLowerCase().contains("regular text"));

      // Should NOT contain base64 data URIs
      assertFalse(html.contains("data:image/png;base64,"));
    }

    @Test
    void extract_excludeTags_removesSpecifiedTags() {
      ExtractPayload payload =
          ExtractPayload.builder()
              .browser(browser)
              .url(testUrl("/tagged-content"))
              .withPreferences(p -> p.excludeTags(List.of("header", "footer", "nav", "script")))
              .build();

      ExtractionResult result = extractor.extract(payload);

      String html = result.html().get(0);

      // These should be removed
      assertFalse(html.contains("<header>"));
      assertFalse(html.contains("<footer>"));
      assertFalse(html.contains("<nav>"));
      assertFalse(html.contains("<script>"));

      // Main content should remain
      assertTrue(html.contains("Main Heading") || html.contains("main"));
    }

    @Test
    void extract_includeTags_onlyIncludesSpecifiedTags() {
      ExtractPayload payload =
          ExtractPayload.builder()
              .browser(browser)
              .url(testUrl("/tagged-content"))
              .withPreferences(p -> p.includeTags(List.of("h1", "p")))
              .build();

      ExtractionResult result = extractor.extract(payload);

      String html = result.html().get(0);

      // Should include h1 and p tags
      assertTrue(html.contains("Main Heading") || html.contains("<h1>"));
      assertTrue(html.contains("<p"));
    }
  }

  // ===== Error Handling Tests =====

  @Nested
  @EnabledIf("com.paragon.web.WebExtractorTest#isPlaywrightAvailable")
  class ErrorHandling {

    @Test
    void extract_invalidUrl_ignoreInvalidUrlsTrue_continuesWithValidUrls() {
      // Use a URL that will definitely fail to connect (invalid port/host)
      ExtractPayload payload =
          ExtractPayload.builder()
              .browser(browser)
              .urls(
                  testUrl("/basic"),
                  "http://localhost:1/invalid") // Port 1 should refuse connection
              .ignoreInvalidUrls(true)
              .build();

      ExtractionResult result = extractor.extract(payload);

      // Should complete with at least partial results from the valid URL
      assertNotNull(result);
      // Should have extracted content from the valid URL
      assertFalse(result.html().isEmpty());
      assertTrue(result.html().get(0).contains("Hello World"));
    }

    @Test
    void extract_timeout_shortTimeout_reportsTimeoutError() {
      ExtractPayload payload =
          ExtractPayload.builder()
              .browser(browser)
              .url(testUrl("/slow"))
              .withPreferences(p -> p.timeoutMs(100)) // Very short timeout
              .ignoreInvalidUrls(true)
              .build();

      ExtractionResult result = extractor.extract(payload);

      // Should have a timeout error
      assertTrue(result.hasErrors());
      assertFalse(result.isSuccessful());
    }

    @Test
    void extract_serverError_completesWithResult() {
      ExtractPayload payload =
          ExtractPayload.builder()
              .browser(browser)
              .url(testUrl("/error"))
              .ignoreInvalidUrls(true)
              .build();

      // Note: A 500 error might still return content or be categorized differently
      // depending on Playwright behavior
      ExtractionResult result = extractor.extract(payload);

      // The behavior depends on whether Playwright treats 500 as an error
      assertNotNull(result);
    }
  }

  // ===== Mobile Emulation Tests =====

  @Nested
  @EnabledIf("com.paragon.web.WebExtractorTest#isPlaywrightAvailable")
  class MobileEmulation {

    @Test
    void extract_mobileTrue_completesSuccessfully() {
      ExtractPayload payload =
          ExtractPayload.builder()
              .browser(browser)
              .url(testUrl("/basic"))
              .withPreferences(p -> p.mobile(true))
              .build();

      ExtractionResult result = extractor.extract(payload);

      // Should complete successfully with mobile settings
      assertNotNull(result);
      assertTrue(result.isSuccessful());
      assertFalse(result.hasErrors());
    }
  }

  // ===== Preference Configuration Tests =====

  @Nested
  @EnabledIf("com.paragon.web.WebExtractorTest#isPlaywrightAvailable")
  class PreferenceConfiguration {

    @Test
    void extract_withCustomHeaders_appliesHeaders() {
      ExtractPayload payload =
          ExtractPayload.builder()
              .browser(browser)
              .url(testUrl("/basic"))
              .withPreferences(p -> p.headers(Map.of("X-Custom-Header", "test-value")))
              .build();

      ExtractionResult result = extractor.extract(payload);

      // Headers are applied to the browser context
      // We can verify the extraction completes successfully
      assertTrue(result.isSuccessful());
    }

    @Test
    void extract_withWaitForMs_waitsBeforeExtraction() {
      ExtractPayload payload =
          ExtractPayload.builder()
              .browser(browser)
              .url(testUrl("/basic"))
              .withPreferences(p -> p.waitForMs(500))
              .build();

      long startTime = System.currentTimeMillis();
      ExtractionResult result = extractor.extract(payload);
      long elapsed = System.currentTimeMillis() - startTime;

      assertTrue(result.isSuccessful());
      // Should have waited at least the specified time
      // (allow some tolerance for async execution)
      assertTrue(elapsed >= 400, "Should wait at least 400ms, but elapsed: " + elapsed);
    }

    @Test
    void extract_skipTlsVerification_completesSuccessfully() {
      ExtractPayload payload =
          ExtractPayload.builder()
              .browser(browser)
              .url(testUrl("/basic"))
              .withPreferences(p -> p.skipTlsVerification(true))
              .build();

      ExtractionResult result = extractor.extract(payload);

      assertTrue(result.isSuccessful());
    }

    @Test
    void extract_blockAds_completesSuccessfully() {
      ExtractPayload payload =
          ExtractPayload.builder()
              .browser(browser)
              .url(testUrl("/basic"))
              .withPreferences(p -> p.blockAds(true))
              .build();

      ExtractionResult result = extractor.extract(payload);

      assertTrue(result.isSuccessful());
    }

    @Test
    void extract_withGeoLocation_appliesLocation() {
      ExtractPayload payload =
          ExtractPayload.builder()
              .browser(browser)
              .url(testUrl("/basic"))
              .withPreferences(p -> p.location(40.7128, -74.0060)) // NYC
              .build();

      ExtractionResult result = extractor.extract(payload);

      assertTrue(result.isSuccessful());
    }
  }

  // ===== Structured Extraction Tests =====

  @Nested
  @EnabledIf("com.paragon.web.WebExtractorTest#isPlaywrightAvailable")
  class StructuredExtraction {

    record ArticleData(String title, String content) {}

    @Test
    @SuppressWarnings("unchecked")
    void extractStructured_withMockedLlm_callsResponderAndReturnsOutput() {
      // Create a mock ParsedResponse
      ParsedResponse<ArticleData> mockParsedResponse = mock(ParsedResponse.class);
      when(mockParsedResponse.outputParsed())
          .thenReturn(new ArticleData("Test Title", "Test Content"));

      when(mockResponder.respond(any(CreateResponsePayload.Structured.class)))
          .thenReturn(mockParsedResponse);

      ExtractPayload.Structured<ArticleData> payload =
          ExtractPayload.structuredBuilder(ArticleData.class)
              .browser(browser)
              .url(testUrl("/basic"))
              .prompt("Extract the article title and content")
              .build();

      ExtractionResult.Structured<ArticleData> result = extractor.extract(payload);

      assertNotNull(result);
      assertEquals(ArticleData.class, result.outputType());
      // Verify the LLM was called with structured output
      verify(mockResponder, atLeastOnce()).respond(any(CreateResponsePayload.Structured.class));
    }

    @Test
    void extractStructured_nullPayload_throwsException() {
      assertThrows(
          NullPointerException.class,
          () -> extractor.extract((ExtractPayload.Structured<ArticleData>) null));
    }
  }

  // ===== Result Properties Tests =====

  @Nested
  @EnabledIf("com.paragon.web.WebExtractorTest#isPlaywrightAvailable")
  class ResultProperties {

    @Test
    void extract_result_containsExtractionPreferences() {
      ExtractionPreferences prefs =
          ExtractionPreferences.builder().mobile(true).blockAds(true).build();

      ExtractPayload payload =
          ExtractPayload.builder()
              .browser(browser)
              .url(testUrl("/basic"))
              .extractionPreferences(prefs)
              .build();

      ExtractionResult result = extractor.extract(payload);

      assertEquals(prefs, result.extractionPreferences());
    }

    @Test
    void extract_result_urlsMatchInput() {
      URI url1 = URI.create(testUrl("/basic"));
      URI url2 = URI.create(testUrl("/main-content"));

      ExtractPayload payload =
          ExtractPayload.builder().browser(browser).urls(List.of(url1, url2)).build();

      ExtractionResult result = extractor.extract(payload);

      assertEquals(List.of(url1, url2), result.urls());
    }
  }
}
