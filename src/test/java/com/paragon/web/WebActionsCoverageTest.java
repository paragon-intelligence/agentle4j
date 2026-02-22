package com.paragon.web;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Comprehensive coverage tests for web action records, builders, and enums. */
@DisplayName("Web Actions Coverage Tests")
class WebActionsCoverageTest {

  // =========================================================================
  // Click Action Tests
  // =========================================================================
  @Nested
  @DisplayName("Click Action")
  class ClickTest {

    @Test
    @DisplayName("should create Click with factory method")
    void shouldCreateClickWithFactoryMethod() {
      Click click = Click.of("#submit-button");
      assertEquals("#submit-button", click.selector());
      assertFalse(click.all());
    }

    @Test
    @DisplayName("should create Click for all matching elements")
    void shouldCreateClickForAllMatchingElements() {
      Click click = Click.all(".checkbox");
      assertEquals(".checkbox", click.selector());
      assertTrue(click.all());
    }

    @Test
    @DisplayName("should create Click with record constructor")
    void shouldCreateClickWithRecordConstructor() {
      Click click = new Click("[data-testid='login']", true);
      assertEquals("[data-testid='login']", click.selector());
      assertTrue(click.all());
    }

    @Test
    @DisplayName("Click should implement Action interface")
    void clickShouldImplementActionInterface() {
      Click click = Click.of("button");
      assertInstanceOf(Action.class, click);
    }
  }

  // =========================================================================
  // Screenshot Action Tests
  // =========================================================================
  @Nested
  @DisplayName("Screenshot Action")
  class ScreenshotTest {

    @Test
    @DisplayName("should create Screenshot with factory method")
    void shouldCreateScreenshotWithFactoryMethod() {
      Screenshot screenshot = Screenshot.of(80);
      assertEquals(80, screenshot.quality());
      assertFalse(screenshot.fullPage());
      assertNull(screenshot.viewport());
    }

    @Test
    @DisplayName("should create full-page Screenshot")
    void shouldCreateFullPageScreenshot() {
      Screenshot screenshot = Screenshot.fullPage(90);
      assertEquals(90, screenshot.quality());
      assertTrue(screenshot.fullPage());
    }

    @Test
    @DisplayName("should create Screenshot with builder")
    void shouldCreateScreenshotWithBuilder() {
      Screenshot screenshot =
          Screenshot.builder().fullPage(true).quality(85).viewport(1920, 1080).build();

      assertTrue(screenshot.fullPage());
      assertEquals(85, screenshot.quality());
      assertNotNull(screenshot.viewport());
      assertEquals(1920, screenshot.viewport().width());
      assertEquals(1080, screenshot.viewport().height());
    }

    @Test
    @DisplayName("should create Screenshot with Viewport object")
    void shouldCreateScreenshotWithViewportObject() {
      Viewport viewport = Viewport.of(800, 600);
      Screenshot screenshot = Screenshot.builder().viewport(viewport).build();

      assertEquals(viewport, screenshot.viewport());
    }

    @Test
    @DisplayName("should reject invalid quality values")
    void shouldRejectInvalidQualityValues() {
      assertThrows(IllegalArgumentException.class, () -> Screenshot.of(-1));
      assertThrows(IllegalArgumentException.class, () -> Screenshot.of(101));
      assertThrows(IllegalArgumentException.class, () -> Screenshot.builder().quality(-10));
      assertThrows(IllegalArgumentException.class, () -> Screenshot.builder().quality(200));
    }

    @Test
    @DisplayName("should accept boundary quality values")
    void shouldAcceptBoundaryQualityValues() {
      assertDoesNotThrow(() -> Screenshot.of(0));
      assertDoesNotThrow(() -> Screenshot.of(100));
      assertDoesNotThrow(() -> Screenshot.of(1));
      assertDoesNotThrow(() -> Screenshot.of(99));
    }

    @Test
    @DisplayName("builder should have reasonable defaults")
    void builderShouldHaveReasonableDefaults() {
      Screenshot screenshot = Screenshot.builder().build();
      assertFalse(screenshot.fullPage());
      assertEquals(80, screenshot.quality()); // default quality
      assertNull(screenshot.viewport());
    }

    @Test
    @DisplayName("Screenshot should implement Action interface")
    void screenshotShouldImplementActionInterface() {
      Screenshot screenshot = Screenshot.of(80);
      assertInstanceOf(Action.class, screenshot);
    }
  }

  // =========================================================================
  // Wait Action Tests
  // =========================================================================
  @Nested
  @DisplayName("Wait Action")
  class WaitTest {

    @Test
    @DisplayName("should create Wait with selector and timeout")
    void shouldCreateWaitWithSelectorAndTimeout() {
      Wait wait = Wait.forSelector("#content", 5000);
      assertEquals("#content", wait.selector());
      assertEquals(5000, wait.milliseconds());
    }

    @Test
    @DisplayName("should create Wait with default timeout")
    void shouldCreateWaitWithDefaultTimeout() {
      Wait wait = Wait.forSelector("#lazy-content");
      assertEquals("#lazy-content", wait.selector());
      assertEquals(30000, wait.milliseconds()); // 30 second default
    }

    @Test
    @DisplayName("should create Wait with record constructor")
    void shouldCreateWaitWithRecordConstructor() {
      Wait wait = new Wait(10000, ".ajax-container");
      assertEquals(".ajax-container", wait.selector());
      assertEquals(10000, wait.milliseconds());
    }

    @Test
    @DisplayName("Wait should implement Action interface")
    void waitShouldImplementActionInterface() {
      Wait wait = Wait.forSelector("div");
      assertInstanceOf(Action.class, wait);
    }
  }

  // =========================================================================
  // GeneratePdf Action Tests
  // =========================================================================
  @Nested
  @DisplayName("GeneratePdf Action")
  class GeneratePdfTest {

    @Test
    @DisplayName("should create GeneratePdf with defaults")
    void shouldCreateGeneratePdfWithDefaults() {
      GeneratePdf pdf = GeneratePdf.defaults();
      assertEquals(PdfFormat.LETTER, pdf.format());
      assertFalse(pdf.landscape());
      assertEquals(1.0, pdf.scale());
    }

    @Test
    @DisplayName("should create GeneratePdf with format")
    void shouldCreateGeneratePdfWithFormat() {
      GeneratePdf pdf = GeneratePdf.of(PdfFormat.A4);
      assertEquals(PdfFormat.A4, pdf.format());
      assertFalse(pdf.landscape());
      assertEquals(1.0, pdf.scale());
    }

    @Test
    @DisplayName("should create GeneratePdf with builder")
    void shouldCreateGeneratePdfWithBuilder() {
      GeneratePdf pdf =
          GeneratePdf.builder().format(PdfFormat.LEGAL).landscape(true).scale(0.8).build();

      assertEquals(PdfFormat.LEGAL, pdf.format());
      assertTrue(pdf.landscape());
      assertEquals(0.8, pdf.scale());
    }

    @Test
    @DisplayName("should reject invalid scale values")
    void shouldRejectInvalidScaleValues() {
      assertThrows(IllegalArgumentException.class, () -> GeneratePdf.builder().scale(0.05));
      assertThrows(IllegalArgumentException.class, () -> GeneratePdf.builder().scale(10.5));
    }

    @Test
    @DisplayName("should accept boundary scale values")
    void shouldAcceptBoundaryScaleValues() {
      assertDoesNotThrow(() -> GeneratePdf.builder().scale(0.1));
      assertDoesNotThrow(() -> GeneratePdf.builder().scale(10.0));
      assertDoesNotThrow(() -> GeneratePdf.builder().scale(1.0));
    }

    @Test
    @DisplayName("GeneratePdf should implement Action interface")
    void generatePdfShouldImplementActionInterface() {
      GeneratePdf pdf = GeneratePdf.defaults();
      assertInstanceOf(Action.class, pdf);
    }
  }

  // =========================================================================
  // Viewport Record Tests
  // =========================================================================
  @Nested
  @DisplayName("Viewport Record")
  class ViewportTest {

    @Test
    @DisplayName("should create Viewport with factory method")
    void shouldCreateViewportWithFactoryMethod() {
      Viewport viewport = Viewport.of(1920, 1080);
      assertEquals(1920, viewport.width());
      assertEquals(1080, viewport.height());
    }

    @Test
    @DisplayName("should create Viewport with record constructor")
    void shouldCreateViewportWithRecordConstructor() {
      Viewport viewport = new Viewport(800, 600);
      assertEquals(800, viewport.width());
      assertEquals(600, viewport.height());
    }

    @Test
    @DisplayName("should support common viewport sizes")
    void shouldSupportCommonViewportSizes() {
      // Mobile
      Viewport mobile = Viewport.of(375, 667);
      assertEquals(375, mobile.width());

      // Tablet
      Viewport tablet = Viewport.of(768, 1024);
      assertEquals(768, tablet.width());

      // Desktop
      Viewport desktop = Viewport.of(1920, 1080);
      assertEquals(1920, desktop.width());
    }

    @Test
    @DisplayName("should have proper equals")
    void shouldHaveProperEquals() {
      Viewport v1 = Viewport.of(1920, 1080);
      Viewport v2 = Viewport.of(1920, 1080);
      Viewport v3 = Viewport.of(800, 600);

      assertEquals(v1, v2);
      assertNotEquals(v1, v3);
    }
  }

  // =========================================================================
  // Location Record Tests
  // =========================================================================
  @Nested
  @DisplayName("Location Record")
  class LocationTest {

    @Test
    @DisplayName("should create Location with country only")
    void shouldCreateLocationWithCountryOnly() {
      Location location = Location.of("US");
      assertEquals("US", location.country());
      assertNull(location.language());
    }

    @Test
    @DisplayName("should create Location with country and language")
    void shouldCreateLocationWithCountryAndLanguage() {
      Location location = Location.of("BR", "pt");
      assertEquals("BR", location.country());
      assertEquals("pt", location.language());
    }

    @Test
    @DisplayName("should support various country codes")
    void shouldSupportVariousCountryCodes() {
      assertDoesNotThrow(() -> Location.of("US", "en"));
      assertDoesNotThrow(() -> Location.of("GB", "en"));
      assertDoesNotThrow(() -> Location.of("DE", "de"));
      assertDoesNotThrow(() -> Location.of("JP", "ja"));
    }
  }

  // =========================================================================
  // PdfFormat Enum Tests
  // =========================================================================
  @Nested
  @DisplayName("PdfFormat Enum")
  class PdfFormatTest {

    @Test
    @DisplayName("should have all expected values")
    void shouldHaveAllExpectedValues() {
      PdfFormat[] formats = PdfFormat.values();
      assertTrue(formats.length >= 11); // A0-A6, Letter, Legal, Tabloid, Ledger
    }

    @Test
    @DisplayName("should have correct string values")
    void shouldHaveCorrectStringValues() {
      assertEquals("A4", PdfFormat.A4.getValue());
      assertEquals("Letter", PdfFormat.LETTER.getValue());
      assertEquals("Legal", PdfFormat.LEGAL.getValue());
      assertEquals("Tabloid", PdfFormat.TABLOID.getValue());
      assertEquals("Ledger", PdfFormat.LEDGER.getValue());
    }

    @Test
    @DisplayName("should support valueOf")
    void shouldSupportValueOf() {
      assertEquals(PdfFormat.A4, PdfFormat.valueOf("A4"));
      assertEquals(PdfFormat.LETTER, PdfFormat.valueOf("LETTER"));
      assertEquals(PdfFormat.LEGAL, PdfFormat.valueOf("LEGAL"));
    }

    @Test
    @DisplayName("should have A-series formats")
    void shouldHaveASeriesFormats() {
      assertNotNull(PdfFormat.A0);
      assertNotNull(PdfFormat.A1);
      assertNotNull(PdfFormat.A2);
      assertNotNull(PdfFormat.A3);
      assertNotNull(PdfFormat.A4);
      assertNotNull(PdfFormat.A5);
      assertNotNull(PdfFormat.A6);
    }
  }

  // =========================================================================
  // Edge Cases
  // =========================================================================
  @Nested
  @DisplayName("Edge Cases")
  class EdgeCasesTest {

    @Test
    @DisplayName("should handle complex CSS selectors in Click")
    void shouldHandleComplexCssSelectorsInClick() {
      Click click = Click.of("div.container > ul li:nth-child(2) a[href^='https']");
      assertTrue(click.selector().contains("nth-child"));
    }

    @Test
    @DisplayName("should handle selector with special characters")
    void shouldHandleSelectorWithSpecialCharacters() {
      Click click = Click.of("[data-testid=\"submit\"]");
      assertTrue(click.selector().contains("data-testid"));
    }

    @Test
    @DisplayName("should handle minimum viewport dimensions")
    void shouldHandleMinimumViewportDimensions() {
      Viewport viewport = Viewport.of(1, 1);
      assertEquals(1, viewport.width());
      assertEquals(1, viewport.height());
    }

    @Test
    @DisplayName("should handle large viewport dimensions")
    void shouldHandleLargeViewportDimensions() {
      Viewport viewport = Viewport.of(7680, 4320); // 8K
      assertEquals(7680, viewport.width());
      assertEquals(4320, viewport.height());
    }

    @Test
    @DisplayName("should handle zero wait timeout")
    void shouldHandleZeroWaitTimeout() {
      Wait wait = Wait.forSelector("div", 0);
      assertEquals(0, wait.milliseconds());
    }
  }
}
