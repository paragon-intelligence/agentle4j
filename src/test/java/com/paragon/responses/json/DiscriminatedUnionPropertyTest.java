package com.paragon.responses.json;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.responses.ResponsesApiObjectMapper;
import com.paragon.responses.spec.*;
import net.jqwik.api.*;

/**
 * Property-based tests for discriminated union serialization.
 *
 * <p>Feature: responses-api-jackson-serialization Property 8: Discriminated union type preservation
 * Validates: Requirements 5.1, 5.2, 5.5, 6.1, 6.2, 6.3, 6.4, 6.5
 */
class DiscriminatedUnionPropertyTest {

  private final ObjectMapper mapper = ResponsesApiObjectMapper.create();

  /**
   * Property 8: Discriminated union type preservation
   *
   * <p>For any discriminated union type (MessageContent, ComputerUseAction), when serialized to
   * JSON, the discriminator field should be present with the correct type value, and when
   * deserialized, the correct concrete subtype should be instantiated.
   */
  @Property(tries = 100)
  void messageContentSerializationIncludesTypeDiscriminator(
      @ForAll("messageContents") MessageContent content) throws Exception {
    // Serialize
    String json = mapper.writeValueAsString(content);

    // Parse to verify structure
    JsonNode node = mapper.readTree(json);

    // Verify type field exists
    assertTrue(node.has("type"), "Serialized MessageContent should have 'type' field");

    // Verify type value is correct
    String typeValue = node.get("type").asText();
    assertNotNull(typeValue, "Type field should not be null");
    assertFalse(typeValue.isEmpty(), "Type field should not be empty");

    // Verify type matches expected discriminator
    if (content instanceof Text) {
      assertEquals("input_text", typeValue, "Text should have type 'input_text'");
    } else if (content instanceof Image) {
      assertEquals("input_image", typeValue, "Image should have type 'input_image'");
    } else if (content instanceof File) {
      assertEquals("input_file", typeValue, "File should have type 'input_file'");
    }
  }

  @Property(tries = 100)
  void messageContentRoundTripPreservesType(@ForAll("messageContents") MessageContent original)
      throws Exception {
    // Serialize
    String json = mapper.writeValueAsString(original);

    // Deserialize
    MessageContent deserialized = mapper.readValue(json, MessageContent.class);

    // Verify same concrete type
    assertEquals(
        original.getClass(),
        deserialized.getClass(),
        "Deserialized MessageContent should be same concrete type as original");
  }

  @Property(tries = 100)
  void computerUseActionSerializationIncludesTypeDiscriminator(
      @ForAll("computerUseActions") ComputerUseAction action) throws Exception {
    // Serialize
    String json = mapper.writeValueAsString(action);

    // Parse to verify structure
    JsonNode node = mapper.readTree(json);

    // Verify type field exists
    assertTrue(node.has("type"), "Serialized ComputerUseAction should have 'type' field");

    // Verify type value is correct
    String typeValue = node.get("type").asText();
    assertNotNull(typeValue, "Type field should not be null");
    assertFalse(typeValue.isEmpty(), "Type field should not be empty");

    // Verify type matches expected discriminator
    if (action instanceof ClickAction) {
      assertEquals("click", typeValue);
    } else if (action instanceof DoubleClickAction) {
      assertEquals("double_click", typeValue);
    } else if (action instanceof DragAction) {
      assertEquals("drag", typeValue);
    } else if (action instanceof KeyPressAction) {
      assertEquals("keypress", typeValue);
    } else if (action instanceof MoveAction) {
      assertEquals("move", typeValue);
    } else if (action instanceof ScreenshotAction) {
      assertEquals("screenshot", typeValue);
    } else if (action instanceof ScrollAction) {
      assertEquals("scroll", typeValue);
    } else if (action instanceof TypeAction) {
      assertEquals("type", typeValue);
    } else if (action instanceof WaitAction) {
      assertEquals("wait", typeValue);
    }
  }

  @Property(tries = 100)
  void computerUseActionRoundTripPreservesType(
      @ForAll("computerUseActions") ComputerUseAction original) throws Exception {
    // Serialize
    String json = mapper.writeValueAsString(original);

    // Deserialize
    ComputerUseAction deserialized = mapper.readValue(json, ComputerUseAction.class);

    // Verify same concrete type
    assertEquals(
        original.getClass(),
        deserialized.getClass(),
        "Deserialized ComputerUseAction should be same concrete type as original");
  }

  // Arbitraries (generators) for discriminated union types

  @Provide
  Arbitrary<MessageContent> messageContents() {
    return Arbitraries.oneOf(texts(), images(), files());
  }

  @Provide
  Arbitrary<Text> texts() {
    return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(100).map(Text::new);
  }

  @Provide
  Arbitrary<Image> images() {
    return Combinators.combine(
            Arbitraries.of(ImageDetail.values()),
            Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(20).injectNull(0.3),
            Arbitraries.strings().alpha().ofMinLength(10).ofMaxLength(50).injectNull(0.3))
        .as(Image::new);
  }

  @Provide
  Arbitrary<File> files() {
    return Combinators.combine(
            Arbitraries.strings().alpha().ofMinLength(10).ofMaxLength(100).injectNull(0.3),
            Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(20).injectNull(0.3),
            Arbitraries.strings().alpha().ofMinLength(10).ofMaxLength(50).injectNull(0.3),
            Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(20).injectNull(0.3))
        .as(File::new);
  }

  @Provide
  Arbitrary<ComputerUseAction> computerUseActions() {
    return Arbitraries.oneOf(
        clickActions(),
        doubleClickActions(),
        dragActions(),
        keyPressActions(),
        moveActions(),
        screenshotActions(),
        scrollActions(),
        typeActions(),
        waitActions());
  }

  @Provide
  Arbitrary<ClickAction> clickActions() {
    return Combinators.combine(Arbitraries.of(ClickButton.values()), coordinates())
        .as(ClickAction::new);
  }

  @Provide
  Arbitrary<DoubleClickAction> doubleClickActions() {
    return coordinates().map(DoubleClickAction::new);
  }

  @Provide
  Arbitrary<DragAction> dragActions() {
    return coordinates().list().ofMinSize(2).ofMaxSize(5).map(DragAction::new);
  }

  @Provide
  Arbitrary<KeyPressAction> keyPressActions() {
    return Arbitraries.strings()
        .alpha()
        .ofMinLength(1)
        .ofMaxLength(10)
        .list()
        .ofMinSize(1)
        .ofMaxSize(3)
        .map(KeyPressAction::new);
  }

  @Provide
  Arbitrary<MoveAction> moveActions() {
    return coordinates().map(MoveAction::new);
  }

  @Provide
  Arbitrary<ScreenshotAction> screenshotActions() {
    return Arbitraries.just(new ScreenshotAction());
  }

  @Provide
  Arbitrary<ScrollAction> scrollActions() {
    return Combinators.combine(
            Arbitraries.integers().between(-1000, 1000),
            Arbitraries.integers().between(-1000, 1000),
            coordinates())
        .as(ScrollAction::new);
  }

  @Provide
  Arbitrary<TypeAction> typeActions() {
    return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50).map(TypeAction::new);
  }

  @Provide
  Arbitrary<WaitAction> waitActions() {
    return Arbitraries.just(new WaitAction());
  }

  @Provide
  Arbitrary<Coordinate> coordinates() {
    return Combinators.combine(
            Arbitraries.integers().between(0, 1920), Arbitraries.integers().between(0, 1080))
        .as(Coordinate::new);
  }
}
