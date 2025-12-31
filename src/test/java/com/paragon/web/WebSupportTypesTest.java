package com.paragon.web;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/**
 * Tests for supporting types in the web package:
 * - Viewport
 * - Location
 * - ProxyMode
 * - PdfFormat
 * - ScrollDirection
 * - ScrapeResult
 */
class WebSupportTypesTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  // ===== Viewport Tests =====

  @Test
  void viewport_of_createsViewport() {
    Viewport viewport = Viewport.of(1920, 1080);

    assertEquals(1920, viewport.width());
    assertEquals(1080, viewport.height());
  }

  @Test
  void viewport_constructor_createsViewport() {
    Viewport viewport = new Viewport(1280, 720);

    assertEquals(1280, viewport.width());
    assertEquals(720, viewport.height());
  }

  @Test
  void viewport_serialization() throws JsonProcessingException {
    Viewport viewport = Viewport.of(800, 600);

    String json = objectMapper.writeValueAsString(viewport);

    assertTrue(json.contains("\"width\":800"));
    assertTrue(json.contains("\"height\":600"));
  }

  @Test
  void viewport_deserialization() throws JsonProcessingException {
    String json = "{\"width\":1024,\"height\":768}";

    Viewport viewport = objectMapper.readValue(json, Viewport.class);

    assertEquals(1024, viewport.width());
    assertEquals(768, viewport.height());
  }

  @Test
  void viewport_equals() {
    Viewport v1 = Viewport.of(1920, 1080);
    Viewport v2 = Viewport.of(1920, 1080);
    Viewport v3 = Viewport.of(1280, 720);

    assertEquals(v1, v2);
    assertEquals(v1.hashCode(), v2.hashCode());
    assertNotEquals(v1, v3);
  }

  // ===== Location Tests =====

  @Test
  void location_of_withCountryOnly() {
    Location location = Location.of("US");

    assertEquals("US", location.country());
    assertNull(location.language());
  }

  @Test
  void location_of_withCountryAndLanguage() {
    Location location = Location.of("BR", "pt");

    assertEquals("BR", location.country());
    assertEquals("pt", location.language());
  }

  @Test
  void location_serialization() throws JsonProcessingException {
    Location location = Location.of("GB", "en");

    String json = objectMapper.writeValueAsString(location);

    assertTrue(json.contains("\"country\":\"GB\""));
    assertTrue(json.contains("\"language\":\"en\""));
  }

  @Test
  void location_deserialization() throws JsonProcessingException {
    String json = "{\"country\":\"DE\",\"language\":\"de\"}";

    Location location = objectMapper.readValue(json, Location.class);

    assertEquals("DE", location.country());
    assertEquals("de", location.language());
  }

  @Test
  void location_equals() {
    Location loc1 = Location.of("US", "en");
    Location loc2 = Location.of("US", "en");
    Location loc3 = Location.of("US", "es");

    assertEquals(loc1, loc2);
    assertEquals(loc1.hashCode(), loc2.hashCode());
    assertNotEquals(loc1, loc3);
  }

  // ===== ProxyMode Tests =====

  @Test
  void proxyMode_hasAllValues() {
    ProxyMode[] modes = ProxyMode.values();

    assertEquals(3, modes.length);
    assertNotNull(ProxyMode.BASIC);
    assertNotNull(ProxyMode.STEALTH);
    assertNotNull(ProxyMode.AUTO);
  }

  @Test
  void proxyMode_getValue_returnsCorrectString() {
    assertEquals("basic", ProxyMode.BASIC.getValue());
    assertEquals("stealth", ProxyMode.STEALTH.getValue());
    assertEquals("auto", ProxyMode.AUTO.getValue());
  }

  @Test
  void proxyMode_serialization() throws JsonProcessingException {
    String json = objectMapper.writeValueAsString(ProxyMode.STEALTH);

    assertEquals("\"stealth\"", json);
  }

  @Test
  void proxyMode_deserialization() throws JsonProcessingException {
    ProxyMode mode = objectMapper.readValue("\"auto\"", ProxyMode.class);

    assertEquals(ProxyMode.AUTO, mode);
  }

  // ===== PdfFormat Tests =====

  @Test
  void pdfFormat_hasAllValues() {
    PdfFormat[] formats = PdfFormat.values();

    assertEquals(11, formats.length);
  }

  @Test
  void pdfFormat_getValue_returnsCorrectString() {
    assertEquals("A4", PdfFormat.A4.getValue());
    assertEquals("Letter", PdfFormat.LETTER.getValue());
    assertEquals("Legal", PdfFormat.LEGAL.getValue());
    assertEquals("Tabloid", PdfFormat.TABLOID.getValue());
    assertEquals("Ledger", PdfFormat.LEDGER.getValue());
  }

  @Test
  void pdfFormat_serialization() throws JsonProcessingException {
    String json = objectMapper.writeValueAsString(PdfFormat.A4);

    assertEquals("\"A4\"", json);
  }

  @Test
  void pdfFormat_deserialization() throws JsonProcessingException {
    PdfFormat format = objectMapper.readValue("\"Letter\"", PdfFormat.class);

    assertEquals(PdfFormat.LETTER, format);
  }

  // ===== ScrollDirection Tests =====

  @Test
  void scrollDirection_hasAllValues() {
    ScrollDirection[] directions = ScrollDirection.values();

    assertEquals(4, directions.length);
    assertNotNull(ScrollDirection.UP);
    assertNotNull(ScrollDirection.DOWN);
    assertNotNull(ScrollDirection.LEFT);
    assertNotNull(ScrollDirection.RIGHT);
  }

  @Test
  void scrollDirection_getValue_returnsCorrectString() {
    assertEquals("up", ScrollDirection.UP.getValue());
    assertEquals("down", ScrollDirection.DOWN.getValue());
    assertEquals("left", ScrollDirection.LEFT.getValue());
    assertEquals("right", ScrollDirection.RIGHT.getValue());
  }

  @Test
  void scrollDirection_serialization() throws JsonProcessingException {
    String json = objectMapper.writeValueAsString(ScrollDirection.DOWN);

    assertEquals("\"down\"", json);
  }

  @Test
  void scrollDirection_deserialization() throws JsonProcessingException {
    ScrollDirection direction = objectMapper.readValue("\"up\"", ScrollDirection.class);

    assertEquals(ScrollDirection.UP, direction);
  }

  // ===== ScrapeResult Tests =====

  @Test
  void scrapeResult_storesUrlAndHtml() {
    ScrapeResult result = new ScrapeResult("https://example.com", "<html><body>Hello</body></html>");

    assertEquals("https://example.com", result.url());
    assertEquals("<html><body>Hello</body></html>", result.html());
  }

  @Test
  void scrapeResult_serialization() throws JsonProcessingException {
    ScrapeResult result = new ScrapeResult("https://test.com", "<html></html>");

    String json = objectMapper.writeValueAsString(result);

    assertTrue(json.contains("\"url\":\"https://test.com\""));
    assertTrue(json.contains("\"html\":\"<html></html>\""));
  }

  @Test
  void scrapeResult_deserialization() throws JsonProcessingException {
    String json = "{\"url\":\"https://example.com\",\"html\":\"<html>content</html>\"}";

    ScrapeResult result = objectMapper.readValue(json, ScrapeResult.class);

    assertEquals("https://example.com", result.url());
    assertEquals("<html>content</html>", result.html());
  }

  @Test
  void scrapeResult_equals() {
    ScrapeResult r1 = new ScrapeResult("https://example.com", "<html></html>");
    ScrapeResult r2 = new ScrapeResult("https://example.com", "<html></html>");
    ScrapeResult r3 = new ScrapeResult("https://other.com", "<html></html>");

    assertEquals(r1, r2);
    assertEquals(r1.hashCode(), r2.hashCode());
    assertNotEquals(r1, r3);
  }
}
