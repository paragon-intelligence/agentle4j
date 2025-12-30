package com.paragon.broadcasting;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for broadcasting package DTOs.
 *
 * <p>Tests cover: - TraceContext creation and minimal factory - ObservationContext creation and
 * minimal factory - Enums (ObservationType, ObservationLevel, ObservationStatus) - Value objects
 * (TokenUsage, CostDetails)
 */
@DisplayName("Broadcasting DTOs Tests")
class BroadcastingDtoTest {

  // ═══════════════════════════════════════════════════════════════════════════
  // TRACE CONTEXT
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("TraceContext")
  class TraceContextTests {

    @Test
    @DisplayName("minimal creates empty context")
    void minimal_createsEmptyContext() {
      TraceContext ctx = TraceContext.minimal();

      assertNull(ctx.userId());
      assertNull(ctx.sessionId());
      assertTrue(ctx.metadata().isEmpty());
      assertTrue(ctx.tags().isEmpty());
      assertNull(ctx.version());
      assertNull(ctx.release());
      assertNull(ctx.environment());
      assertFalse(ctx.isPublic());
    }

    @Test
    @DisplayName("creates context with all fields")
    void createsContextWithAllFields() {
      TraceContext ctx =
          new TraceContext(
              "user-123",
              "session-456",
              Map.of("key", "value"),
              List.of("tag1", "tag2"),
              "1.0.0",
              "v1.0.0",
              "production",
              true);

      assertEquals("user-123", ctx.userId());
      assertEquals("session-456", ctx.sessionId());
      assertEquals("value", ctx.metadata().get("key"));
      assertEquals(2, ctx.tags().size());
      assertEquals("1.0.0", ctx.version());
      assertEquals("v1.0.0", ctx.release());
      assertEquals("production", ctx.environment());
      assertTrue(ctx.isPublic());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // OBSERVATION CONTEXT
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("ObservationContext")
  class ObservationContextTests {

    @Test
    @DisplayName("minimal creates default context")
    void minimal_createsDefaultContext() {
      ObservationContext ctx = ObservationContext.minimal();

      assertEquals(ObservationLevel.DEFAULT, ctx.level());
      assertNull(ctx.input());
      assertTrue(ctx.metadata().isEmpty());
      assertTrue(ctx.attributes().isEmpty());
    }

    @Test
    @DisplayName("creates context with all fields")
    void createsContextWithAllFields() {
      ObservationContext ctx =
          new ObservationContext(
              ObservationLevel.WARNING,
              "input data",
              Map.of("meta-key", "meta-value"),
              Map.of("attr-key", "attr-value"));

      assertEquals(ObservationLevel.WARNING, ctx.level());
      assertEquals("input data", ctx.input());
      assertEquals("meta-value", ctx.metadata().get("meta-key"));
      assertEquals("attr-value", ctx.attributes().get("attr-key"));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // OBSERVATION TYPE
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("ObservationType")
  class ObservationTypeTests {

    @Test
    @DisplayName("all enum values exist")
    void allEnumValuesExist() {
      assertNotNull(ObservationType.SPAN);
      assertNotNull(ObservationType.GENERATION);
      assertNotNull(ObservationType.EVENT);
    }

    @Test
    @DisplayName("valueOf works")
    void valueOfWorks() {
      assertEquals(ObservationType.SPAN, ObservationType.valueOf("SPAN"));
      assertEquals(ObservationType.GENERATION, ObservationType.valueOf("GENERATION"));
      assertEquals(ObservationType.EVENT, ObservationType.valueOf("EVENT"));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // OBSERVATION LEVEL
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("ObservationLevel")
  class ObservationLevelTests {

    @Test
    @DisplayName("all enum values exist")
    void allEnumValuesExist() {
      assertNotNull(ObservationLevel.DEFAULT);
      assertNotNull(ObservationLevel.DEBUG);
      assertNotNull(ObservationLevel.WARNING);
      assertNotNull(ObservationLevel.ERROR);
    }

    @Test
    @DisplayName("valueOf works")
    void valueOfWorks() {
      assertEquals(ObservationLevel.DEFAULT, ObservationLevel.valueOf("DEFAULT"));
      assertEquals(ObservationLevel.ERROR, ObservationLevel.valueOf("ERROR"));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // OBSERVATION STATUS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("ObservationStatus")
  class ObservationStatusTests {

    @Test
    @DisplayName("all enum values exist")
    void allEnumValuesExist() {
      assertNotNull(ObservationStatus.RUNNING);
      assertNotNull(ObservationStatus.SUCCESS);
      assertNotNull(ObservationStatus.ERROR);
      assertNotNull(ObservationStatus.CANCELLED);
    }

    @Test
    @DisplayName("valueOf works")
    void valueOfWorks() {
      assertEquals(ObservationStatus.RUNNING, ObservationStatus.valueOf("RUNNING"));
      assertEquals(ObservationStatus.SUCCESS, ObservationStatus.valueOf("SUCCESS"));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // TOKEN USAGE
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("TokenUsage")
  class TokenUsageTests {

    @Test
    @DisplayName("creates record with values")
    void createsRecord() {
      TokenUsage usage = new TokenUsage(100, 200, 300);

      assertEquals(100, usage.inputTokens());
      assertEquals(200, usage.outputTokens());
      assertEquals(300, usage.totalTokens());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // COST DETAILS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("CostDetails")
  class CostDetailsTests {

    @Test
    @DisplayName("creates record with value")
    void createsRecord() {
      CostDetails cost = new CostDetails(0.02, 0.03, 0.05);

      assertEquals(0.02, cost.inputCost());
      assertEquals(0.03, cost.outputCost());
      assertEquals(0.05, cost.totalCost());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // MULTIMODAL CONTENT TYPE
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("MultimodalContentType")
  class MultimodalContentTypeTests {

    @Test
    @DisplayName("all enum values exist")
    void allEnumValuesExist() {
      assertNotNull(MultimodalContentType.TEXT);
      assertNotNull(MultimodalContentType.IMAGE);
      assertNotNull(MultimodalContentType.AUDIO);
      assertNotNull(MultimodalContentType.VIDEO);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // MULTIMODAL CONTENT
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("MultiModalContent")
  class MultiModalContentTests {

    @Test
    @DisplayName("creates text content")
    void createsTextContent() {
      MultiModalContent content = MultiModalContent.text("Hello world");

      assertEquals(MultimodalContentType.TEXT, content.type());
      assertEquals("Hello world", content.content());
    }

    @Test
    @DisplayName("creates image content")
    void createsImageContent() {
      MultiModalContent content = MultiModalContent.image("https://example.com/img.png");

      assertEquals(MultimodalContentType.IMAGE, content.type());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // TRACE END OPTIONS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("TraceEndOptions")
  class TraceEndOptionsTests {

    @Test
    @DisplayName("creates record with values")
    void createsRecord() {
      TraceEndOptions options = new TraceEndOptions("output text", null);

      assertEquals("output text", options.output());
      assertNull(options.statusMessage());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // OBSERVATION END OPTIONS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("ObservationEndOptions")
  class ObservationEndOptionsTests {

    @Test
    @DisplayName("creates record with values")
    void createsRecord() {
      ObservationEndOptions options =
          new ObservationEndOptions("output", null, ObservationStatus.SUCCESS);

      assertEquals("output", options.output());
      assertEquals(ObservationStatus.SUCCESS, options.status());
    }
  }
}
