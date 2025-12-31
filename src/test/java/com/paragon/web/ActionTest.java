package com.paragon.web;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for Action interface and all action implementations.
 * Tests focus on factory methods, Jackson polymorphic serialization, and record behavior.
 */
class ActionTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
  }

  // ===== Click Tests =====

  @Test
  void click_of_createsSingleClickAction() {
    Click click = Click.of("#button");

    assertEquals("#button", click.selector());
    assertFalse(click.all());
  }

  @Test
  void click_all_createsMultiClickAction() {
    Click click = Click.all(".items");

    assertEquals(".items", click.selector());
    assertTrue(click.all());
  }

  @Test
  void click_serialization_includesType() throws JsonProcessingException {
    Click click = Click.of("#submit");

    String json = objectMapper.writeValueAsString(click);

    assertTrue(json.contains("\"type\":\"click\""));
    assertTrue(json.contains("\"selector\":\"#submit\""));
  }

  @Test
  void click_deserialization_fromJson() throws JsonProcessingException {
    String json = "{\"type\":\"click\",\"selector\":\"#btn\",\"all\":true}";

    Action action = objectMapper.readValue(json, Action.class);

    assertInstanceOf(Click.class, action);
    Click click = (Click) action;
    assertEquals("#btn", click.selector());
    assertTrue(click.all());
  }

  // ===== ExecuteJavascript Tests =====

  @Test
  void executeJavascript_of_createsAction() {
    ExecuteJavascript js = ExecuteJavascript.of("alert('hello')");

    assertEquals("alert('hello')", js.script());
  }

  @Test
  void executeJavascript_serialization_includesType() throws JsonProcessingException {
    ExecuteJavascript js = ExecuteJavascript.of("console.log('test')");

    String json = objectMapper.writeValueAsString(js);

    assertTrue(json.contains("\"type\":\"execute_javascript\""));
    assertTrue(json.contains("\"script\":\"console.log('test')\""));
  }

  @Test
  void executeJavascript_deserialization_fromJson() throws JsonProcessingException {
    String json = "{\"type\":\"execute_javascript\",\"script\":\"return 42\"}";

    Action action = objectMapper.readValue(json, Action.class);

    assertInstanceOf(ExecuteJavascript.class, action);
    assertEquals("return 42", ((ExecuteJavascript) action).script());
  }

  // ===== GeneratePdf Tests =====

  @Test
  void generatePdf_defaults_hasExpectedValues() {
    GeneratePdf pdf = GeneratePdf.defaults();

    assertEquals(PdfFormat.LETTER, pdf.format());
    assertFalse(pdf.landscape());
    assertEquals(1.0, pdf.scale());
  }

  @Test
  void generatePdf_of_createsWithFormat() {
    GeneratePdf pdf = GeneratePdf.of(PdfFormat.A4);

    assertEquals(PdfFormat.A4, pdf.format());
  }

  @Test
  void generatePdf_builder_setsAllProperties() {
    GeneratePdf pdf = GeneratePdf.builder()
        .format(PdfFormat.LEGAL)
        .landscape(true)
        .scale(2.0)
        .build();

    assertEquals(PdfFormat.LEGAL, pdf.format());
    assertTrue(pdf.landscape());
    assertEquals(2.0, pdf.scale());
  }

  @Test
  void generatePdf_builder_invalidScale_throwsException() {
    assertThrows(IllegalArgumentException.class, () ->
        GeneratePdf.builder().scale(0.05).build()
    );
    assertThrows(IllegalArgumentException.class, () ->
        GeneratePdf.builder().scale(11.0).build()
    );
  }

  @Test
  void generatePdf_deserialization_fromJson() throws JsonProcessingException {
    String json = "{\"type\":\"pdf\",\"format\":\"A4\",\"landscape\":true,\"scale\":1.5}";

    Action action = objectMapper.readValue(json, Action.class);

    assertInstanceOf(GeneratePdf.class, action);
    GeneratePdf pdf = (GeneratePdf) action;
    assertEquals(PdfFormat.A4, pdf.format());
    assertTrue(pdf.landscape());
    assertEquals(1.5, pdf.scale());
  }

  // ===== PressAKey Tests =====

  @Test
  void pressAKey_of_createsAction() {
    PressAKey press = PressAKey.of("Enter");

    assertEquals("Enter", press.key());
  }

  @Test
  void pressAKey_convenientMethods_createCorrectKeys() {
    assertEquals("Enter", PressAKey.enter().key());
    assertEquals("Space", PressAKey.space().key());
    assertEquals("Tab", PressAKey.tab().key());
    assertEquals("Escape", PressAKey.escape().key());
    assertEquals("Backspace", PressAKey.backspace().key());
    assertEquals("ArrowUp", PressAKey.arrowUp().key());
    assertEquals("ArrowDown", PressAKey.arrowDown().key());
    assertEquals("ArrowLeft", PressAKey.arrowLeft().key());
    assertEquals("ArrowRight", PressAKey.arrowRight().key());
  }

  @Test
  void pressAKey_deserialization_fromJson() throws JsonProcessingException {
    String json = "{\"type\":\"press\",\"key\":\"Tab\"}";

    Action action = objectMapper.readValue(json, Action.class);

    assertInstanceOf(PressAKey.class, action);
    assertEquals("Tab", ((PressAKey) action).key());
  }

  // ===== Scrape Tests =====

  @Test
  void scrape_create_returnsSingleton() {
    Scrape scrape1 = Scrape.create();
    Scrape scrape2 = Scrape.create();

    assertSame(scrape1, scrape2);
  }

  @Test
  void scrape_deserialization_fromJson() throws JsonProcessingException {
    String json = "{\"type\":\"scrape\"}";

    Action action = objectMapper.readValue(json, Action.class);

    assertInstanceOf(Scrape.class, action);
  }

  // ===== Screenshot Tests =====

  @Test
  void screenshot_of_createsWithQuality() {
    Screenshot screenshot = Screenshot.of(80);

    assertEquals(80, screenshot.quality());
    assertFalse(screenshot.fullPage());
    assertNull(screenshot.viewport());
  }

  @Test
  void screenshot_fullPage_createsFullPageScreenshot() {
    Screenshot screenshot = Screenshot.fullPage(90);

    assertEquals(90, screenshot.quality());
    assertTrue(screenshot.fullPage());
  }

  @Test
  void screenshot_builder_setsAllProperties() {
    Screenshot screenshot = Screenshot.builder()
        .fullPage(true)
        .quality(95)
        .viewport(1920, 1080)
        .build();

    assertTrue(screenshot.fullPage());
    assertEquals(95, screenshot.quality());
    assertNotNull(screenshot.viewport());
    assertEquals(1920, screenshot.viewport().width());
    assertEquals(1080, screenshot.viewport().height());
  }

  @Test
  void screenshot_invalidQuality_throwsException() {
    assertThrows(IllegalArgumentException.class, () -> Screenshot.of(-1));
    assertThrows(IllegalArgumentException.class, () -> Screenshot.of(101));
  }

  @Test
  void screenshot_deserialization_fromJson() throws JsonProcessingException {
    String json = "{\"type\":\"screenshot\",\"full_page\":true,\"quality\":85,\"viewport\":null}";

    Action action = objectMapper.readValue(json, Action.class);

    assertInstanceOf(Screenshot.class, action);
    Screenshot screenshot = (Screenshot) action;
    assertTrue(screenshot.fullPage());
    assertEquals(85, screenshot.quality());
  }

  // ===== Scroll Tests =====

  @Test
  void scroll_of_createsAction() {
    Scroll scroll = Scroll.of("#container", ScrollDirection.DOWN, 500);

    assertEquals("#container", scroll.selector());
    assertEquals(ScrollDirection.DOWN, scroll.direction());
    assertEquals(500, scroll.amount());
  }

  @Test
  void scroll_directionalMethods_createCorrectActions() {
    assertEquals(ScrollDirection.DOWN, Scroll.down("#el", 100).direction());
    assertEquals(ScrollDirection.UP, Scroll.up("#el", 100).direction());
    assertEquals(ScrollDirection.LEFT, Scroll.left("#el", 100).direction());
    assertEquals(ScrollDirection.RIGHT, Scroll.right("#el", 100).direction());
  }

  @Test
  void scroll_invalidAmount_throwsException() {
    assertThrows(IllegalArgumentException.class, () ->
        Scroll.of("#el", ScrollDirection.DOWN, -1)
    );
    assertThrows(IllegalArgumentException.class, () ->
        Scroll.of("#el", ScrollDirection.DOWN, 1001)
    );
  }

  @Test
  void scroll_deserialization_fromJson() throws JsonProcessingException {
    String json = "{\"type\":\"scroll\",\"direction\":\"up\",\"amount\":200,\"selector\":\"#content\"}";

    Action action = objectMapper.readValue(json, Action.class);

    assertInstanceOf(Scroll.class, action);
    Scroll scroll = (Scroll) action;
    assertEquals(ScrollDirection.UP, scroll.direction());
    assertEquals(200, scroll.amount());
    assertEquals("#content", scroll.selector());
  }

  // ===== Wait Tests =====

  @Test
  void wait_forSelector_createsWithTimeout() {
    Wait wait = Wait.forSelector("#loading", 5000);

    assertEquals("#loading", wait.selector());
    assertEquals(5000, wait.milliseconds());
  }

  @Test
  void wait_forSelector_withDefaultTimeout() {
    Wait wait = Wait.forSelector("#element");

    assertEquals("#element", wait.selector());
    assertEquals(30000, wait.milliseconds());
  }

  @Test
  void wait_deserialization_fromJson() throws JsonProcessingException {
    String json = "{\"type\":\"wait\",\"milliseconds\":10000,\"selector\":\"#modal\"}";

    Action action = objectMapper.readValue(json, Action.class);

    assertInstanceOf(Wait.class, action);
    Wait wait = (Wait) action;
    assertEquals("#modal", wait.selector());
    assertEquals(10000, wait.milliseconds());
  }

  // ===== WriteText Tests =====

  @Test
  void writeText_of_createsAction() {
    WriteText write = WriteText.of("Hello, World!");

    assertEquals("Hello, World!", write.text());
  }

  @Test
  void writeText_deserialization_fromJson() throws JsonProcessingException {
    String json = "{\"type\":\"write\",\"text\":\"Test input\"}";

    Action action = objectMapper.readValue(json, Action.class);

    assertInstanceOf(WriteText.class, action);
    assertEquals("Test input", ((WriteText) action).text());
  }

  // ===== Polymorphic Serialization Tests =====

  @Test
  void polymorphicDeserialization_allTypes() throws JsonProcessingException {
    String[] jsons = {
        "{\"type\":\"click\",\"selector\":\"#btn\",\"all\":false}",
        "{\"type\":\"execute_javascript\",\"script\":\"test\"}",
        "{\"type\":\"pdf\",\"format\":\"Letter\",\"landscape\":false,\"scale\":1.0}",
        "{\"type\":\"press\",\"key\":\"Enter\"}",
        "{\"type\":\"scrape\"}",
        "{\"type\":\"screenshot\",\"full_page\":false,\"quality\":80,\"viewport\":null}",
        "{\"type\":\"scroll\",\"direction\":\"down\",\"amount\":100,\"selector\":\"#s\"}",
        "{\"type\":\"wait\",\"milliseconds\":1000,\"selector\":\"#w\"}",
        "{\"type\":\"write\",\"text\":\"text\"}"
    };

    Class<?>[] expectedTypes = {
        Click.class,
        ExecuteJavascript.class,
        GeneratePdf.class,
        PressAKey.class,
        Scrape.class,
        Screenshot.class,
        Scroll.class,
        Wait.class,
        WriteText.class
    };

    for (int i = 0; i < jsons.length; i++) {
      Action action = objectMapper.readValue(jsons[i], Action.class);
      assertInstanceOf(expectedTypes[i], action, "Failed for: " + jsons[i]);
    }
  }
}
