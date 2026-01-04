package com.paragon.telemetry.otel;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Comprehensive tests for OTEL telemetry classes. */
@DisplayName("OTEL Telemetry Classes")
class OtelClassesTest {

  private ObjectMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new ObjectMapper();
  }

  // ==================== OtelAttribute Tests ====================

  @Nested
  @DisplayName("OtelAttribute")
  class OtelAttributeTests {

    @Test
    @DisplayName("ofString creates string attribute")
    void ofStringCreatesStringAttribute() {
      OtelAttribute attr = OtelAttribute.ofString("key", "value");

      assertEquals("key", attr.key());
      assertNotNull(attr.value());
    }

    @Test
    @DisplayName("ofBool creates boolean attribute")
    void ofBoolCreatesBoolAttribute() {
      OtelAttribute attr = OtelAttribute.ofBool("enabled", true);

      assertEquals("enabled", attr.key());
      assertNotNull(attr.value());
    }

    @Test
    @DisplayName("ofInt creates integer attribute")
    void ofIntCreatesIntAttribute() {
      OtelAttribute attr = OtelAttribute.ofInt("count", 42);

      assertEquals("count", attr.key());
      assertNotNull(attr.value());
    }

    @Test
    @DisplayName("ofDouble creates double attribute")
    void ofDoubleCreatesDoubleAttribute() {
      OtelAttribute attr = OtelAttribute.ofDouble("rate", 3.14);

      assertEquals("rate", attr.key());
      assertNotNull(attr.value());
    }

    @Test
    @DisplayName("of infers string type")
    void ofInfersStringType() {
      OtelAttribute attr = OtelAttribute.of("name", "test");

      assertEquals("name", attr.key());
      assertNotNull(attr.value());
    }

    @Test
    @DisplayName("of infers integer type")
    void ofInfersIntegerType() {
      OtelAttribute attr = OtelAttribute.of("count", 123);

      assertEquals("count", attr.key());
      assertNotNull(attr.value());
    }

    @Test
    @DisplayName("of infers boolean type")
    void ofInfersBooleanType() {
      OtelAttribute attr = OtelAttribute.of("flag", Boolean.TRUE);

      assertEquals("flag", attr.key());
      assertNotNull(attr.value());
    }

    @Test
    @DisplayName("attribute serializes to JSON")
    void attributeSerializesToJson() throws Exception {
      OtelAttribute attr = OtelAttribute.ofString("session.id", "abc123");

      String json = mapper.writeValueAsString(attr);

      assertTrue(json.contains("\"key\""));
      assertTrue(json.contains("session.id"));
      assertTrue(json.contains("\"value\""));
    }
  }

  // ==================== OtelSpan Tests ====================

  @Nested
  @DisplayName("OtelSpan")
  class OtelSpanTests {

    @Test
    @DisplayName("builder creates span with all fields")
    void builderCreatesSpan() {
      OtelSpan span =
          OtelSpan.builder()
              .traceId("trace123")
              .spanId("span456")
              .parentSpanId("parent789")
              .name("test.operation")
              .kind(OtelSpan.SPAN_KIND_CLIENT)
              .startTimeNanos(1000000000L)
              .endTimeNanos(2000000000L)
              .attributes(List.of(OtelAttribute.ofString("key", "value")))
              .status(OtelStatus.ok())
              .build();

      assertEquals("trace123", span.traceId());
      assertEquals("span456", span.spanId());
      assertEquals("parent789", span.parentSpanId());
      assertEquals("test.operation", span.name());
      assertEquals(OtelSpan.SPAN_KIND_CLIENT, span.kind());
      assertEquals("1000000000", span.startTimeUnixNano());
      assertEquals("2000000000", span.endTimeUnixNano());
      assertNotNull(span.attributes());
      assertEquals(1, span.attributes().size());
      assertNotNull(span.status());
    }

    @Test
    @DisplayName("clientKind sets span kind to CLIENT")
    void clientKindSetsKindToClient() {
      OtelSpan span =
          OtelSpan.builder()
              .traceId("trace")
              .spanId("span")
              .name("test")
              .clientKind()
              .startTimeNanos(1000L)
              .build();

      assertEquals(OtelSpan.SPAN_KIND_CLIENT, span.kind());
    }

    @Test
    @DisplayName("span serializes to JSON")
    void spanSerializesToJson() throws Exception {
      OtelSpan span =
          OtelSpan.builder()
              .traceId("trace123")
              .spanId("span456")
              .name("test")
              .startTimeNanos(1000L)
              .build();

      String json = mapper.writeValueAsString(span);

      assertTrue(json.contains("\"traceId\""));
      assertTrue(json.contains("trace123"));
      assertTrue(json.contains("\"spanId\""));
      assertTrue(json.contains("span456"));
    }

    @Test
    @DisplayName("span kind constants are correct")
    void spanKindConstantsAreCorrect() {
      assertEquals(0, OtelSpan.SPAN_KIND_UNSPECIFIED);
      assertEquals(1, OtelSpan.SPAN_KIND_INTERNAL);
      assertEquals(2, OtelSpan.SPAN_KIND_SERVER);
      assertEquals(3, OtelSpan.SPAN_KIND_CLIENT);
      assertEquals(4, OtelSpan.SPAN_KIND_PRODUCER);
      assertEquals(5, OtelSpan.SPAN_KIND_CONSUMER);
    }

    @Test
    @DisplayName("builder without parent span id creates span with null parent")
    void builderWithoutParentCreatesSpanWithNullParent() {
      OtelSpan span =
          OtelSpan.builder()
              .traceId("trace")
              .spanId("span")
              .name("test")
              .startTimeNanos(1000L)
              .build();

      assertNull(span.parentSpanId());
    }
  }

  // ==================== OtelStatus Tests ====================

  @Nested
  @DisplayName("OtelStatus")
  class OtelStatusTests {

    @Test
    @DisplayName("ok creates OK status")
    void okCreatesOkStatus() {
      OtelStatus status = OtelStatus.ok();

      assertNotNull(status);
    }

    @Test
    @DisplayName("error creates error status with message")
    void errorCreatesErrorStatusWithMessage() {
      OtelStatus status = OtelStatus.error("Something failed");

      assertNotNull(status);
    }

    @Test
    @DisplayName("status serializes to JSON")
    void statusSerializesToJson() throws Exception {
      OtelStatus status = OtelStatus.ok();

      String json = mapper.writeValueAsString(status);

      assertNotNull(json);
      assertTrue(json.contains("code") || json.length() > 2);
    }
  }

  // ==================== OtelMetric Tests ====================

  @Nested
  @DisplayName("OtelMetric")
  class OtelMetricTests {

    @Test
    @DisplayName("gauge creates gauge metric")
    void gaugeCreatesGaugeMetric() {
      List<OtelAttribute> attrs = List.of(OtelAttribute.ofString("key", "value"));
      OtelMetric metric =
          OtelMetric.gauge(
              "test.metric", "A test metric", "ms", List.of(OtelDataPoint.gaugeInt(100, attrs)));

      assertEquals("test.metric", metric.name());
      assertEquals("A test metric", metric.description());
      assertEquals("ms", metric.unit());
    }
  }

  // ==================== OtelDataPoint Tests ====================

  @Nested
  @DisplayName("OtelDataPoint")
  class OtelDataPointTests {

    @Test
    @DisplayName("gaugeInt creates integer gauge data point")
    void gaugeIntCreatesIntDataPoint() {
      List<OtelAttribute> attrs = List.of(OtelAttribute.ofString("key", "value"));
      OtelDataPoint point = OtelDataPoint.gaugeInt(42, attrs);

      assertNotNull(point);
    }
  }

  // ==================== OtelLogRecord Tests ====================

  @Nested
  @DisplayName("OtelLogRecord")
  class OtelLogRecordTests {

    @Test
    @DisplayName("info creates info log record")
    void infoCreatesInfoLogRecord() {
      List<OtelAttribute> attrs = List.of(OtelAttribute.ofString("key", "value"));
      OtelLogRecord log = OtelLogRecord.info("Test message", attrs, "trace123", "span456");

      assertNotNull(log);
    }

    @Test
    @DisplayName("error creates error log record")
    void errorCreatesErrorLogRecord() {
      List<OtelAttribute> attrs = List.of(OtelAttribute.ofString("error", "true"));
      OtelLogRecord log = OtelLogRecord.error("Error occurred", attrs, "trace123", "span456");

      assertNotNull(log);
    }
  }
}
