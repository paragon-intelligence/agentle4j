package com.paragon.web;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * Tests for ExtractionPreferences class.
 */
class ExtractionPreferencesTest {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  // ===== Builder Default Values Tests =====

  @Test
  void defaults_createsInstanceWithExpectedDefaults() {
    ExtractionPreferences prefs = ExtractionPreferences.defaults();

    assertFalse(prefs.mobile());
    assertFalse(prefs.blockAds());
    assertFalse(prefs.removeBase64Images());
    assertFalse(prefs.onlyMainContent());
    assertFalse(prefs.skipTlsVerification());
    assertEquals(30_000, prefs.timeoutMs());
    assertEquals(0, prefs.waitForMs());
    assertNull(prefs.location());
    assertNull(prefs.headers());
    assertNull(prefs.includeTags());
    assertNull(prefs.excludeTags());
    assertNull(prefs.proxy());
  }

  @Test
  void builder_setsAllProperties() {
    ExtractionPreferences.GeoLocation location = new ExtractionPreferences.GeoLocation(40.7128, -74.0060);
    Map<String, String> headers = Map.of("Authorization", "Bearer token");
    List<String> includeTags = List.of("article", "main");
    List<String> excludeTags = List.of("nav", "footer");

    ExtractionPreferences prefs = ExtractionPreferences.builder()
        .mobile(true)
        .blockAds(true)
        .removeBase64Images(true)
        .onlyMainContent(true)
        .skipTlsVerification(true)
        .timeoutMs(60_000)
        .waitForMs(5_000)
        .location(location)
        .headers(headers)
        .includeTags(includeTags)
        .excludeTags(excludeTags)
        .proxy(ExtractionPreferences.ProxyMode.STEALTH)
        .build();

    assertTrue(prefs.mobile());
    assertTrue(prefs.blockAds());
    assertTrue(prefs.removeBase64Images());
    assertTrue(prefs.onlyMainContent());
    assertTrue(prefs.skipTlsVerification());
    assertEquals(60_000, prefs.timeoutMs());
    assertEquals(5_000, prefs.waitForMs());
    assertEquals(location, prefs.location());
    assertEquals(headers, prefs.headers());
    assertEquals(includeTags, prefs.includeTags());
    assertEquals(excludeTags, prefs.excludeTags());
    assertEquals(ExtractionPreferences.ProxyMode.STEALTH, prefs.proxy());
  }

  @Test
  void builder_locationWithCoordinates_createsGeoLocation() {
    ExtractionPreferences prefs = ExtractionPreferences.builder()
        .location(37.7749, -122.4194)
        .build();

    assertNotNull(prefs.location());
    assertEquals(37.7749, prefs.location().latitude());
    assertEquals(-122.4194, prefs.location().longitude());
  }

  // ===== Validation Tests =====

  @Test
  void builder_negativeTimeoutMs_throwsException() {
    assertThrows(IllegalArgumentException.class, () ->
        ExtractionPreferences.builder().timeoutMs(-1).build()
    );
  }

  @Test
  void builder_negativeWaitForMs_throwsException() {
    assertThrows(IllegalArgumentException.class, () ->
        ExtractionPreferences.builder().waitForMs(-1).build()
    );
  }

  // ===== Equals, HashCode, ToString Tests =====

  @Test
  void equals_sameValues_returnsTrue() {
    ExtractionPreferences prefs1 = ExtractionPreferences.builder()
        .mobile(true)
        .blockAds(true)
        .timeoutMs(5000)
        .build();

    ExtractionPreferences prefs2 = ExtractionPreferences.builder()
        .mobile(true)
        .blockAds(true)
        .timeoutMs(5000)
        .build();

    assertEquals(prefs1, prefs2);
    assertEquals(prefs1.hashCode(), prefs2.hashCode());
  }

  @Test
  void equals_differentValues_returnsFalse() {
    ExtractionPreferences prefs1 = ExtractionPreferences.builder()
        .mobile(true)
        .build();

    ExtractionPreferences prefs2 = ExtractionPreferences.builder()
        .mobile(false)
        .build();

    assertNotEquals(prefs1, prefs2);
  }

  @Test
  void equals_sameInstance_returnsTrue() {
    ExtractionPreferences prefs = ExtractionPreferences.defaults();
    assertEquals(prefs, prefs);
  }

  @Test
  void equals_null_returnsFalse() {
    ExtractionPreferences prefs = ExtractionPreferences.defaults();
    assertNotEquals(null, prefs);
  }

  @Test
  void equals_differentType_returnsFalse() {
    ExtractionPreferences prefs = ExtractionPreferences.defaults();
    assertNotEquals("not a preference", prefs);
  }

  @Test
  void toString_containsRelevantInfo() {
    ExtractionPreferences prefs = ExtractionPreferences.builder()
        .mobile(true)
        .blockAds(true)
        .build();

    String str = prefs.toString();

    assertNotNull(str);
    assertTrue(str.contains("ExtractionPreferences"));
    assertTrue(str.contains("mobile=true"));
    assertTrue(str.contains("blockAds=true"));
  }

  // ===== ProxyMode Enum Tests =====

  @Test
  void proxyMode_hasExpectedValues() {
    assertEquals(2, ExtractionPreferences.ProxyMode.values().length);
    assertNotNull(ExtractionPreferences.ProxyMode.BASIC);
    assertNotNull(ExtractionPreferences.ProxyMode.STEALTH);
  }

  // ===== GeoLocation Record Tests =====

  @Test
  void geoLocation_storesCoordinates() {
    ExtractionPreferences.GeoLocation location = new ExtractionPreferences.GeoLocation(51.5074, -0.1278);

    assertEquals(51.5074, location.latitude());
    assertEquals(-0.1278, location.longitude());
  }

  @Test
  void geoLocation_equals_sameValues() {
    ExtractionPreferences.GeoLocation loc1 = new ExtractionPreferences.GeoLocation(51.5074, -0.1278);
    ExtractionPreferences.GeoLocation loc2 = new ExtractionPreferences.GeoLocation(51.5074, -0.1278);

    assertEquals(loc1, loc2);
    assertEquals(loc1.hashCode(), loc2.hashCode());
  }
}
