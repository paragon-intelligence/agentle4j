package com.paragon.telemetry.otel;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for OpenTelemetry DTOs.
 *
 * <p>Tests cover: - OtelAttribute creation and factory methods - OtelSpan builder and serialization
 * - OtelStatus, OtelResource, OtelScope - JSON serialization compatibility
 */
@DisplayName("OpenTelemetry DTOs Tests")
class OtelDtoTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // OTEL ATTRIBUTE
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("OtelAttribute")
  class OtelAttributeTests {

    @Test
    @DisplayName("ofString creates string attribute")
    void ofString_createsStringAttribute() {
      OtelAttribute attr = OtelAttribute.ofString("service.name", "my-service");

      assertEquals("service.name", attr.key());
      assertNotNull(attr.value());
      assertEquals("my-service", attr.value().stringValue());
    }

    @Test
    @DisplayName("ofBool creates boolean attribute")
    void ofBool_createsBoolAttribute() {
      OtelAttribute attr = OtelAttribute.ofBool("enabled", true);

      assertEquals("enabled", attr.key());
      assertTrue(attr.value().boolValue());
    }

    @Test
    @DisplayName("ofInt creates integer attribute")
    void ofInt_createsIntAttribute() {
      OtelAttribute attr = OtelAttribute.ofInt("count", 42);

      assertEquals("count", attr.key());
      assertEquals(42L, attr.value().intValue());
    }

    @Test
    @DisplayName("ofDouble creates double attribute")
    void ofDouble_createsDoubleAttribute() {
      OtelAttribute attr = OtelAttribute.ofDouble("rate", 3.14);

      assertEquals("rate", attr.key());
      assertEquals(3.14, attr.value().doubleValue());
    }

    @Test
    @DisplayName("of infers type from value")
    void of_infersType() {
      OtelAttribute stringAttr = OtelAttribute.of("key", "value");
      OtelAttribute intAttr = OtelAttribute.of("key", 123);
      OtelAttribute boolAttr = OtelAttribute.of("key", true);

      assertNotNull(stringAttr.value().stringValue());
      assertNotNull(intAttr.value().intValue());
      assertTrue(boolAttr.value().boolValue());
    }

    @Test
    @DisplayName("serializes to JSON correctly")
    void serializesToJson() throws JsonProcessingException {
      OtelAttribute attr = OtelAttribute.ofString("key", "value");
      String json = objectMapper.writeValueAsString(attr);

      assertTrue(json.contains("\"key\""));
      assertTrue(json.contains("\"value\""));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // OTEL SPAN
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("OtelSpan")
  class OtelSpanTests {

    @Test
    @DisplayName("builder creates span with required fields")
    void builder_createsSpan() {
      OtelSpan span =
          OtelSpan.builder()
              .traceId("abc123")
              .spanId("span456")
              .name("test-operation")
              .startTimeNanos(System.nanoTime())
              .build();

      assertEquals("abc123", span.traceId());
      assertEquals("span456", span.spanId());
      assertEquals("test-operation", span.name());
    }

    @Test
    @DisplayName("builder sets optional fields")
    void builder_setsOptionalFields() {
      OtelSpan span =
          OtelSpan.builder()
              .traceId("abc123")
              .spanId("span456")
              .parentSpanId("parent789")
              .name("test-operation")
              .clientKind()
              .startTimeNanos(1000L)
              .endTimeNanos(2000L)
              .status(OtelStatus.ok())
              .build();

      assertEquals("parent789", span.parentSpanId());
      assertEquals(OtelSpan.SPAN_KIND_CLIENT, span.kind());
      assertEquals("1000", span.startTimeUnixNano());
      assertEquals("2000", span.endTimeUnixNano());
      assertNotNull(span.status());
    }

    @Test
    @DisplayName("builder sets attributes")
    void builder_setsAttributes() {
      OtelSpan span =
          OtelSpan.builder()
              .traceId("abc123")
              .spanId("span456")
              .name("test")
              .startTimeNanos(1000L)
              .attributes(
                  List.of(
                      OtelAttribute.ofString("gen_ai.system", "openai"),
                      OtelAttribute.ofString("gen_ai.request.model", "gpt-4")))
              .build();

      assertNotNull(span.attributes());
      assertEquals(2, span.attributes().size());
    }

    @Test
    @DisplayName("kind constants are correct")
    void kindConstants_areCorrect() {
      assertEquals(0, OtelSpan.SPAN_KIND_UNSPECIFIED);
      assertEquals(1, OtelSpan.SPAN_KIND_INTERNAL);
      assertEquals(2, OtelSpan.SPAN_KIND_SERVER);
      assertEquals(3, OtelSpan.SPAN_KIND_CLIENT);
      assertEquals(4, OtelSpan.SPAN_KIND_PRODUCER);
      assertEquals(5, OtelSpan.SPAN_KIND_CONSUMER);
    }

    @Test
    @DisplayName("serializes to JSON correctly")
    void serializesToJson() throws JsonProcessingException {
      OtelSpan span =
          OtelSpan.builder()
              .traceId("trace123")
              .spanId("span456")
              .name("my-span")
              .startTimeNanos(1234567890L)
              .build();

      String json = objectMapper.writeValueAsString(span);

      assertTrue(json.contains("\"traceId\":\"trace123\""));
      assertTrue(json.contains("\"spanId\":\"span456\""));
      assertTrue(json.contains("\"name\":\"my-span\""));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // OTEL STATUS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("OtelStatus")
  class OtelStatusTests {

    @Test
    @DisplayName("ok creates OK status")
    void ok_createsOkStatus() {
      OtelStatus status = OtelStatus.ok();

      assertEquals(OtelStatus.STATUS_CODE_OK, status.code());
    }

    @Test
    @DisplayName("error creates error status with message")
    void error_createsErrorStatus() {
      OtelStatus status = OtelStatus.error("Something went wrong");

      assertEquals(OtelStatus.STATUS_CODE_ERROR, status.code());
      assertEquals("Something went wrong", status.message());
    }

    @Test
    @DisplayName("unset creates unset status")
    void unset_createsUnsetStatus() {
      OtelStatus status = OtelStatus.unset();

      assertEquals(OtelStatus.STATUS_CODE_UNSET, status.code());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // OTEL RESOURCE
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("OtelResource")
  class OtelResourceTests {

    @Test
    @DisplayName("creates resource with attributes")
    void createsResourceWithAttributes() {
      OtelResource resource =
          new OtelResource(
              List.of(
                  OtelAttribute.ofString("service.name", "my-service"),
                  OtelAttribute.ofString("service.version", "1.0.0")));

      assertNotNull(resource.attributes());
      assertEquals(2, resource.attributes().size());
    }

    @Test
    @DisplayName("serializes to JSON correctly")
    void serializesToJson() throws JsonProcessingException {
      OtelResource resource =
          new OtelResource(List.of(OtelAttribute.ofString("service.name", "test-service")));

      String json = objectMapper.writeValueAsString(resource);

      assertTrue(json.contains("service.name"));
      assertTrue(json.contains("test-service"));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // OTEL SCOPE
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("OtelScope")
  class OtelScopeTests {

    @Test
    @DisplayName("creates scope with name and version")
    void createsScope() {
      OtelScope scope = new OtelScope("agentle", "1.0.0", null);

      assertEquals("agentle", scope.name());
      assertEquals("1.0.0", scope.version());
    }

    @Test
    @DisplayName("serializes to JSON correctly")
    void serializesToJson() throws JsonProcessingException {
      OtelScope scope = new OtelScope("agentle", "1.0.0", null);

      String json = objectMapper.writeValueAsString(scope);

      assertTrue(json.contains("\"name\":\"agentle\""));
      assertTrue(json.contains("\"version\":\"1.0.0\""));
    }
  }
}
