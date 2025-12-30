package com.paragon.responses.spec;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for CreateResponsePayload builder.
 *
 * <p>Tests cover: - Builder pattern with various options - Input methods - Tool configuration -
 * Model parameters
 */
@DisplayName("CreateResponsePayload Tests")
class CreateResponsePayloadTest {

  // ═══════════════════════════════════════════════════════════════════════════
  // BUILDER BASICS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Builder Basics")
  class BuilderBasics {

    @Test
    @DisplayName("creates payload with model")
    void createsPayloadWithModel() {
      CreateResponsePayload payload =
          CreateResponsePayload.builder()
              .model("gpt-4o")
              .input(List.of(Message.user("Hello")))
              .build();

      assertEquals("gpt-4o", payload.model());
    }

    @Test
    @DisplayName("creates payload with message input")
    void createsPayloadWithMessageInput() {
      CreateResponsePayload payload =
          CreateResponsePayload.builder()
              .model("gpt-4o")
              .input(List.of(Message.user("Hello, world!")))
              .build();

      assertNotNull(payload.input());
      assertEquals(1, payload.input().size());
    }

    @Test
    @DisplayName("creates payload with multiple messages")
    void createsPayloadWithMultipleMessages() {
      List<ResponseInputItem> inputs = List.of(Message.user("First"), Message.user("Second"));

      CreateResponsePayload payload =
          CreateResponsePayload.builder().model("gpt-4o").input(inputs).build();

      assertEquals(2, payload.input().size());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // INSTRUCTIONS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Instructions")
  class Instructions {

    @Test
    @DisplayName("sets instructions")
    void setsInstructions() {
      CreateResponsePayload payload =
          CreateResponsePayload.builder()
              .model("gpt-4o")
              .input(List.of(Message.user("Hello")))
              .instructions("Be helpful")
              .build();

      assertEquals("Be helpful", payload.instructions());
    }

    @Test
    @DisplayName("null instructions allowed")
    void nullInstructionsAllowed() {
      CreateResponsePayload payload =
          CreateResponsePayload.builder()
              .model("gpt-4o")
              .input(List.of(Message.user("Hello")))
              .build();

      assertNull(payload.instructions());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // MODEL PARAMETERS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Model Parameters")
  class ModelParameters {

    @Test
    @DisplayName("sets temperature")
    void setsTemperature() {
      CreateResponsePayload payload =
          CreateResponsePayload.builder()
              .model("gpt-4o")
              .input(List.of(Message.user("Hello")))
              .temperature(0.7)
              .build();

      assertEquals(0.7, payload.temperature());
    }

    @Test
    @DisplayName("sets max output tokens")
    void setsMaxOutputTokens() {
      CreateResponsePayload payload =
          CreateResponsePayload.builder()
              .model("gpt-4o")
              .input(List.of(Message.user("Hello")))
              .maxOutputTokens(1000)
              .build();

      assertEquals(1000, payload.maxOutputTokens());
    }

    @Test
    @DisplayName("sets top P")
    void setsTopP() {
      CreateResponsePayload payload =
          CreateResponsePayload.builder()
              .model("gpt-4o")
              .input(List.of(Message.user("Hello")))
              .topP(0.9)
              .build();

      assertEquals(0.9, payload.topP());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // STREAMING
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Streaming")
  class Streaming {

    @Test
    @DisplayName("sets stream to true")
    void setsStreamTrue() {
      CreateResponsePayload payload =
          CreateResponsePayload.builder()
              .model("gpt-4o")
              .input(List.of(Message.user("Hello")))
              .stream(true)
              .build();

      assertTrue(payload.stream());
    }

    @Test
    @DisplayName("sets stream to false")
    void setsStreamFalse() {
      CreateResponsePayload payload =
          CreateResponsePayload.builder()
              .model("gpt-4o")
              .input(List.of(Message.user("Hello")))
              .stream(false)
              .build();

      assertFalse(payload.stream());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // METADATA
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Metadata")
  class Metadata {

    @Test
    @DisplayName("sets metadata map")
    void setsMetadata() {
      CreateResponsePayload payload =
          CreateResponsePayload.builder()
              .model("gpt-4o")
              .input(List.of(Message.user("Hello")))
              .metadata(Map.of("key", "value"))
              .build();

      assertNotNull(payload.metadata());
      assertEquals("value", payload.metadata().get("key"));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // TOOL CONFIGURATION
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Tool Configuration")
  class ToolConfiguration {

    @Test
    @DisplayName("sets parallel tool calls")
    void setsParallelToolCalls() {
      CreateResponsePayload payload =
          CreateResponsePayload.builder()
              .model("gpt-4o")
              .input(List.of(Message.user("Hello")))
              .parallelToolCalls(true)
              .build();

      assertTrue(payload.parallelToolCalls());
    }

    @Test
    @DisplayName("sets max tool calls")
    void setsMaxToolCalls() {
      CreateResponsePayload payload =
          CreateResponsePayload.builder()
              .model("gpt-4o")
              .input(List.of(Message.user("Hello")))
              .maxToolCalls(5)
              .build();

      assertEquals(5, payload.maxToolCalls());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ACCESSORS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Accessors")
  class Accessors {

    @Test
    @DisplayName("all accessors return correct values")
    void allAccessorsWork() {
      CreateResponsePayload payload =
          CreateResponsePayload.builder()
              .model("gpt-4o")
              .input(List.of(Message.user("Hello")))
              .instructions("Be helpful")
              .temperature(0.5)
              .maxOutputTokens(500)
              .stream(true)
              .build();

      assertEquals("gpt-4o", payload.model());
      assertEquals("Be helpful", payload.instructions());
      assertEquals(0.5, payload.temperature());
      assertEquals(500, payload.maxOutputTokens());
      assertTrue(payload.stream());
    }

    @Test
    @DisplayName("toString contains model")
    void toStringContainsModel() {
      CreateResponsePayload payload =
          CreateResponsePayload.builder()
              .model("gpt-4o-mini")
              .input(List.of(Message.user("Test")))
              .build();

      assertTrue(payload.toString().contains("gpt-4o-mini"));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // EQUALS AND HASHCODE
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Equals and HashCode")
  class EqualsAndHashCode {

    @Test
    @DisplayName("equals returns true for same payload")
    void equalsTrue() {
      UserMessage msg = Message.user("Hello");

      CreateResponsePayload p1 =
          CreateResponsePayload.builder().model("gpt-4o").input(List.of(msg)).build();

      CreateResponsePayload p2 =
          CreateResponsePayload.builder().model("gpt-4o").input(List.of(msg)).build();

      assertEquals(p1, p2);
    }

    @Test
    @DisplayName("equals returns false for different model")
    void equalsFalse() {
      UserMessage msg = Message.user("Hello");

      CreateResponsePayload p1 =
          CreateResponsePayload.builder().model("gpt-4o").input(List.of(msg)).build();

      CreateResponsePayload p2 =
          CreateResponsePayload.builder().model("gpt-3.5-turbo").input(List.of(msg)).build();

      assertNotEquals(p1, p2);
    }

    @Test
    @DisplayName("hashCode consistent with equals")
    void hashCodeConsistent() {
      UserMessage msg = Message.user("Hello");

      CreateResponsePayload p1 =
          CreateResponsePayload.builder().model("gpt-4o").input(List.of(msg)).build();

      CreateResponsePayload p2 =
          CreateResponsePayload.builder().model("gpt-4o").input(List.of(msg)).build();

      assertEquals(p1.hashCode(), p2.hashCode());
    }
  }
}
