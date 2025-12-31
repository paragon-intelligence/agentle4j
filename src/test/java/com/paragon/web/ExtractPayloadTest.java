package com.paragon.web;

import static org.junit.jupiter.api.Assertions.*;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.net.URI;
import java.util.List;

/**
 * Tests for ExtractPayload and ExtractPayload.Structured classes.
 * 
 * Note: Some tests require Playwright to be installed. Tests that require
 * a real browser are skipped if Playwright is not available.
 */
class ExtractPayloadTest {

  private static Playwright playwright;
  private static Browser browser;
  private static boolean playwrightAvailable = false;

  @BeforeAll
  static void setUp() {
    try {
      playwright = Playwright.create();
      browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
      playwrightAvailable = true;
    } catch (Exception e) {
      // Playwright not available - tests requiring browser will be skipped
      playwrightAvailable = false;
    }
  }

  @AfterAll
  static void tearDown() {
    if (browser != null) {
      browser.close();
    }
    if (playwright != null) {
      playwright.close();
    }
  }

  static boolean isPlaywrightAvailable() {
    return playwrightAvailable;
  }

  // ===== Basic Builder Tests =====

  @Test
  @EnabledIf("isPlaywrightAvailable")
  void builder_withRequiredFields_createsPayload() {
    ExtractPayload payload = ExtractPayload.builder()
        .browser(browser)
        .url("https://example.com")
        .build();

    assertNotNull(payload);
    assertEquals(browser, payload.browser());
    assertEquals(1, payload.urls().size());
    assertEquals(URI.create("https://example.com"), payload.urls().get(0));
    assertNotNull(payload.prompt());
    assertNotNull(payload.extractionPreferences());
    assertTrue(payload.ignoreInvalidUrls());
  }

  @Test
  @EnabledIf("isPlaywrightAvailable")
  void builder_withAllFields_createsPayload() {
    ExtractionPreferences prefs = ExtractionPreferences.builder()
        .mobile(true)
        .build();

    ExtractPayload payload = ExtractPayload.builder()
        .browser(browser)
        .url("https://example.com")
        .prompt("Extract article content")
        .extractionPreferences(prefs)
        .ignoreInvalidUrls(false)
        .build();

    assertEquals("Extract article content", payload.prompt());
    assertEquals(prefs, payload.extractionPreferences());
    assertFalse(payload.ignoreInvalidUrls());
  }

  @Test
  @EnabledIf("isPlaywrightAvailable")
  void builder_withMultipleUrls_createsPayload() {
    ExtractPayload payload = ExtractPayload.builder()
        .browser(browser)
        .urls("https://example.com", "https://test.com")
        .build();

    assertEquals(2, payload.urls().size());
    assertEquals(URI.create("https://example.com"), payload.urls().get(0));
    assertEquals(URI.create("https://test.com"), payload.urls().get(1));
  }

  @Test
  @EnabledIf("isPlaywrightAvailable")
  void builder_addUrl_appendsToList() {
    // Note: addUrl creates an ArrayList internally, but url() creates an immutable list
    // So we need to start with addUrl or use multiple URLs via urls()
    ExtractPayload payload = ExtractPayload.builder()
        .browser(browser)
        .addUrl("https://example.com")
        .addUrl("https://test.com")
        .build();

    assertEquals(2, payload.urls().size());
  }

  @Test
  @EnabledIf("isPlaywrightAvailable")
  void builder_withUriList_acceptsURIs() {
    List<URI> urls = List.of(
        URI.create("https://example.com"),
        URI.create("https://test.com")
    );

    ExtractPayload payload = ExtractPayload.builder()
        .browser(browser)
        .urls(urls)
        .build();

    assertEquals(2, payload.urls().size());
  }

  @Test
  @EnabledIf("isPlaywrightAvailable")
  void builder_withPreferencesCallback_configuresPreferences() {
    ExtractPayload payload = ExtractPayload.builder()
        .browser(browser)
        .url("https://example.com")
        .withPreferences(prefs -> prefs
            .mobile(true)
            .blockAds(true)
            .timeoutMs(60_000))
        .build();

    assertTrue(payload.extractionPreferences().mobile());
    assertTrue(payload.extractionPreferences().blockAds());
    assertEquals(60_000, payload.extractionPreferences().timeoutMs());
  }

  // ===== Validation Tests (don't require browser) =====

  @Test
  void builder_withoutBrowser_throwsException() {
    assertThrows(IllegalStateException.class, () ->
        ExtractPayload.builder()
            .url("https://example.com")
            .build()
    );
  }

  @Test
  @EnabledIf("isPlaywrightAvailable")
  void builder_withoutUrls_throwsException() {
    assertThrows(IllegalStateException.class, () ->
        ExtractPayload.builder()
            .browser(browser)
            .build()
    );
  }

  @Test
  @EnabledIf("isPlaywrightAvailable")
  void constructor_withEmptyUrls_throwsException() {
    // Builder.validate() throws IllegalStateException when urls is empty
    assertThrows(IllegalStateException.class, () ->
        ExtractPayload.builder()
            .browser(browser)
            .urls(List.of())
            .build()
    );
  }

  // ===== Structured Builder Tests =====

  @Test
  @EnabledIf("isPlaywrightAvailable")
  void structuredBuilder_createsStructuredPayload() {
    ExtractPayload.Structured<TestData> payload = ExtractPayload.structuredBuilder(TestData.class)
        .browser(browser)
        .url("https://example.com")
        .prompt("Extract test data")
        .build();

    assertNotNull(payload);
    assertEquals(TestData.class, payload.outputType());
    assertEquals("Extract test data", payload.prompt());
  }

  @Test
  @EnabledIf("isPlaywrightAvailable")
  void structuredBuilder_build_returnsStructuredPayload() {
    ExtractPayload.Structured<TestData> payload = ExtractPayload.structuredBuilder(TestData.class)
        .browser(browser)
        .url("https://example.com")
        .build();

    assertInstanceOf(ExtractPayload.Structured.class, payload);
  }

  // ===== Equals, HashCode, ToString Tests =====

  @Test
  @EnabledIf("isPlaywrightAvailable")
  void equals_sameValues_returnsTrue() {
    ExtractPayload payload1 = ExtractPayload.builder()
        .browser(browser)
        .url("https://example.com")
        .prompt("test")
        .build();

    ExtractPayload payload2 = ExtractPayload.builder()
        .browser(browser)
        .url("https://example.com")
        .prompt("test")
        .build();

    assertEquals(payload1, payload2);
    assertEquals(payload1.hashCode(), payload2.hashCode());
  }

  @Test
  @EnabledIf("isPlaywrightAvailable")
  void equals_differentUrls_returnsFalse() {
    ExtractPayload payload1 = ExtractPayload.builder()
        .browser(browser)
        .url("https://example.com")
        .build();

    ExtractPayload payload2 = ExtractPayload.builder()
        .browser(browser)
        .url("https://other.com")
        .build();

    assertNotEquals(payload1, payload2);
  }

  @Test
  @EnabledIf("isPlaywrightAvailable")
  void equals_sameInstance_returnsTrue() {
    ExtractPayload payload = ExtractPayload.builder()
        .browser(browser)
        .url("https://example.com")
        .build();

    assertEquals(payload, payload);
  }

  @Test
  @EnabledIf("isPlaywrightAvailable")
  void toString_containsRelevantInfo() {
    ExtractPayload payload = ExtractPayload.builder()
        .browser(browser)
        .url("https://example.com")
        .prompt("Extract content")
        .build();

    String str = payload.toString();

    assertNotNull(str);
    assertTrue(str.contains("ExtractPayload"));
  }

  @Test
  @EnabledIf("isPlaywrightAvailable")
  void structured_toString_containsOutputType() {
    ExtractPayload.Structured<TestData> payload = ExtractPayload.structuredBuilder(TestData.class)
        .browser(browser)
        .url("https://example.com")
        .build();

    String str = payload.toString();

    assertNotNull(str);
    assertTrue(str.contains("Structured"));
    assertTrue(str.contains("TestData"));
  }

  @Test
  @EnabledIf("isPlaywrightAvailable")
  void structured_equals_includesOutputType() {
    ExtractPayload.Structured<TestData> payload1 = ExtractPayload.structuredBuilder(TestData.class)
        .browser(browser)
        .url("https://example.com")
        .build();

    ExtractPayload.Structured<TestData> payload2 = ExtractPayload.structuredBuilder(TestData.class)
        .browser(browser)
        .url("https://example.com")
        .build();

    assertEquals(payload1, payload2);
    assertEquals(payload1.hashCode(), payload2.hashCode());
  }

  // ===== Test Data Class =====

  private static class TestData {
    public String name;
    public int value;
  }
}
