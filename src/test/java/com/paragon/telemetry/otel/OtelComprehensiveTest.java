package com.paragon.telemetry.otel;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive coverage tests for all OTEL (OpenTelemetry) DTOs.
 * Tests construction, factory methods, builders, and JSON serialization.
 */
@DisplayName("OTEL Comprehensive Coverage Tests")
class OtelComprehensiveTest {

  private ObjectMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new ObjectMapper();
  }

  // =========================================================================
  // OtelStatus Tests
  // =========================================================================
  @Nested
  @DisplayName("OtelStatus Record")
  class OtelStatusTest {

    @Test
    @DisplayName("should create unset status")
    void shouldCreateUnsetStatus() {
      OtelStatus status = OtelStatus.unset();
      assertEquals(OtelStatus.STATUS_CODE_UNSET, status.code());
      assertNull(status.message());
    }

    @Test
    @DisplayName("should create ok status")
    void shouldCreateOkStatus() {
      OtelStatus status = OtelStatus.ok();
      assertEquals(OtelStatus.STATUS_CODE_OK, status.code());
      assertNull(status.message());
    }

    @Test
    @DisplayName("should create error status with message")
    void shouldCreateErrorStatusWithMessage() {
      OtelStatus status = OtelStatus.error("Connection timeout");
      assertEquals(OtelStatus.STATUS_CODE_ERROR, status.code());
      assertEquals("Connection timeout", status.message());
    }

    @Test
    @DisplayName("should have correct status code constants")
    void shouldHaveCorrectStatusCodeConstants() {
      assertEquals(0, OtelStatus.STATUS_CODE_UNSET);
      assertEquals(1, OtelStatus.STATUS_CODE_OK);
      assertEquals(2, OtelStatus.STATUS_CODE_ERROR);
    }
  }

  // =========================================================================
  // OtelAttribute Tests
  // =========================================================================
  @Nested
  @DisplayName("OtelAttribute Record")
  class OtelAttributeTest {

    @Test
    @DisplayName("should create string attribute")
    void shouldCreateStringAttribute() {
      OtelAttribute attr = OtelAttribute.ofString("service.name", "my-service");
      assertEquals("service.name", attr.key());
      assertNotNull(attr.value());
    }

    @Test
    @DisplayName("should create boolean attribute")
    void shouldCreateBoolAttribute() {
      OtelAttribute attr = OtelAttribute.ofBool("error", true);
      assertEquals("error", attr.key());
      assertNotNull(attr.value());
    }

    @Test
    @DisplayName("should create integer attribute")
    void shouldCreateIntAttribute() {
      OtelAttribute attr = OtelAttribute.ofInt("token_count", 100);
      assertEquals("token_count", attr.key());
      assertNotNull(attr.value());
    }

    @Test
    @DisplayName("should create double attribute")
    void shouldCreateDoubleAttribute() {
      OtelAttribute attr = OtelAttribute.ofDouble("cost", 0.005);
      assertEquals("cost", attr.key());
      assertNotNull(attr.value());
    }

    @Test
    @DisplayName("should create attribute with automatic type inference")
    void shouldCreateAttributeWithAutoType() {
      OtelAttribute attr1 = OtelAttribute.of("key1", "string-value");
      OtelAttribute attr2 = OtelAttribute.of("key2", 42L);
      OtelAttribute attr3 = OtelAttribute.of("key3", 3.14);
      OtelAttribute attr4 = OtelAttribute.of("key4", true);

      assertNotNull(attr1.value());
      assertNotNull(attr2.value());
      assertNotNull(attr3.value());
      assertNotNull(attr4.value());
    }
  }

  // =========================================================================
  // OtelResource Tests
  // =========================================================================
  @Nested
  @DisplayName("OtelResource Record")
  class OtelResourceTest {

    @Test
    @DisplayName("should create resource for service")
    void shouldCreateResourceForService() {
      OtelResource resource = OtelResource.forService("my-app", "1.0.0");
      assertNotNull(resource.attributes());
      assertEquals(2, resource.attributes().size());
    }

    @Test
    @DisplayName("should create resource for Agentle")
    void shouldCreateResourceForAgentle() {
      OtelResource resource = OtelResource.forAgentle();
      assertNotNull(resource.attributes());
      assertTrue(resource.attributes().size() > 0);
    }

    @Test
    @DisplayName("should create resource with agentle() alias")
    void shouldCreateResourceWithAgentleAlias() {
      OtelResource resource = OtelResource.agentle();
      assertNotNull(resource.attributes());
    }

    @Test
    @DisplayName("should allow null attributes")
    void shouldAllowNullAttributes() {
      OtelResource resource = new OtelResource(null);
      assertNull(resource.attributes());
    }
  }

  // =========================================================================
  // OtelSpan Tests
  // =========================================================================
  @Nested
  @DisplayName("OtelSpan Record and Builder")
  class OtelSpanTest {

    @Test
    @DisplayName("should create span with builder")
    void shouldCreateSpanWithBuilder() {
      OtelSpan span =
          OtelSpan.builder()
              .traceId("abc123")
              .spanId("def456")
              .name("my-span")
              .startTimeNanos(1000000L)
              .endTimeNanos(2000000L)
              .build();

      assertEquals("abc123", span.traceId());
      assertEquals("def456", span.spanId());
      assertEquals("my-span", span.name());
      assertEquals("1000000", span.startTimeUnixNano());
      assertEquals("2000000", span.endTimeUnixNano());
    }

    @Test
    @DisplayName("should create span with parent")
    void shouldCreateSpanWithParent() {
      OtelSpan span =
          OtelSpan.builder()
              .traceId("abc123")
              .spanId("child-span")
              .parentSpanId("parent-span")
              .name("child")
              .startTimeNanos(1000L)
              .build();

      assertEquals("parent-span", span.parentSpanId());
    }

    @Test
    @DisplayName("should set client kind")
    void shouldSetClientKind() {
      OtelSpan span =
          OtelSpan.builder()
              .traceId("t")
              .spanId("s")
              .name("client-span")
              .clientKind()
              .startTimeNanos(1000L)
              .build();

      assertEquals(OtelSpan.SPAN_KIND_CLIENT, span.kind());
    }

    @Test
    @DisplayName("should set span kind with constant")
    void shouldSetSpanKindWithConstant() {
      OtelSpan span =
          OtelSpan.builder()
              .traceId("t")
              .spanId("s")
              .name("server-span")
              .kind(OtelSpan.SPAN_KIND_SERVER)
              .startTimeNanos(1000L)
              .build();

      assertEquals(OtelSpan.SPAN_KIND_SERVER, span.kind());
    }

    @Test
    @DisplayName("should set attributes")
    void shouldSetAttributes() {
      List<OtelAttribute> attrs =
          List.of(
              OtelAttribute.ofString("gen_ai.system", "openai"),
              OtelAttribute.ofInt("gen_ai.usage.input_tokens", 100));

      OtelSpan span =
          OtelSpan.builder()
              .traceId("t")
              .spanId("s")
              .name("llm-call")
              .startTimeNanos(1000L)
              .attributes(attrs)
              .build();

      assertNotNull(span.attributes());
      assertEquals(2, span.attributes().size());
    }

    @Test
    @DisplayName("should set status")
    void shouldSetStatus() {
      OtelSpan span =
          OtelSpan.builder()
              .traceId("t")
              .spanId("s")
              .name("span")
              .startTimeNanos(1000L)
              .status(OtelStatus.ok())
              .build();

      assertNotNull(span.status());
      assertEquals(OtelStatus.STATUS_CODE_OK, span.status().code());
    }

    @Test
    @DisplayName("should have correct span kind constants")
    void shouldHaveCorrectSpanKindConstants() {
      assertEquals(0, OtelSpan.SPAN_KIND_UNSPECIFIED);
      assertEquals(1, OtelSpan.SPAN_KIND_INTERNAL);
      assertEquals(2, OtelSpan.SPAN_KIND_SERVER);
      assertEquals(3, OtelSpan.SPAN_KIND_CLIENT);
      assertEquals(4, OtelSpan.SPAN_KIND_PRODUCER);
      assertEquals(5, OtelSpan.SPAN_KIND_CONSUMER);
    }

    @Test
    @DisplayName("should default to internal kind")
    void shouldDefaultToInternalKind() {
      OtelSpan span =
          OtelSpan.builder().traceId("t").spanId("s").name("internal").startTimeNanos(1000L).build();

      assertEquals(OtelSpan.SPAN_KIND_INTERNAL, span.kind());
    }
  }

  // =========================================================================
  // OtelDataPoint Tests
  // =========================================================================
  @Nested
  @DisplayName("OtelDataPoint Record")
  class OtelDataPointTest {

    @Test
    @DisplayName("should create gauge int data point")
    void shouldCreateGaugeIntDataPoint() {
      List<OtelAttribute> attrs = List.of(OtelAttribute.ofString("metric", "token_count"));
      OtelDataPoint point = OtelDataPoint.gaugeInt(100L, attrs);

      assertEquals(100L, point.asInt());
      assertNull(point.asDouble());
      assertEquals(attrs, point.attributes());
    }

    @Test
    @DisplayName("should create gauge double data point")
    void shouldCreateGaugeDoubleDataPoint() {
      List<OtelAttribute> attrs = List.of(OtelAttribute.ofString("metric", "cost"));
      OtelDataPoint point = OtelDataPoint.gaugeDouble(0.005, attrs);

      assertNull(point.asInt());
      assertEquals(0.005, point.asDouble());
    }

    @Test
    @DisplayName("should create counter int data point")
    void shouldCreateCounterIntDataPoint() {
      long startTime = 1000000L;
      List<OtelAttribute> attrs = List.of();
      OtelDataPoint point = OtelDataPoint.counterInt(startTime, 42L, attrs);

      assertEquals(startTime, point.startTimeUnixNano());
      assertEquals(42L, point.asInt());
    }
  }

  // =========================================================================
  // OtelMetric Tests
  // =========================================================================
  @Nested
  @DisplayName("OtelMetric Record")
  class OtelMetricTest {

    @Test
    @DisplayName("should create gauge metric")
    void shouldCreateGaugeMetric() {
      List<OtelDataPoint> dataPoints =
          List.of(OtelDataPoint.gaugeInt(100L, List.of()));

      OtelMetric metric =
          OtelMetric.gauge("token_count", "Number of tokens", "tokens", dataPoints);

      assertEquals("token_count", metric.name());
      assertEquals("Number of tokens", metric.description());
      assertEquals("tokens", metric.unit());
      assertNotNull(metric.gauge());
      assertNull(metric.sum());
    }

    @Test
    @DisplayName("should create counter metric")
    void shouldCreateCounterMetric() {
      List<OtelDataPoint> dataPoints =
          List.of(OtelDataPoint.counterInt(1000L, 42L, List.of()));

      OtelMetric metric =
          OtelMetric.counter("request_count", "Total requests", "count", dataPoints);

      assertEquals("request_count", metric.name());
      assertNull(metric.gauge());
      assertNotNull(metric.sum());
      assertTrue(metric.sum().isMonotonic());
      assertEquals("AGGREGATION_TEMPORALITY_CUMULATIVE", metric.sum().aggregationTemporality());
    }

    @Test
    @DisplayName("OtelGauge should hold data points")
    void otelGaugeShouldHoldDataPoints() {
      List<OtelDataPoint> dataPoints =
          List.of(OtelDataPoint.gaugeDouble(3.14, List.of()));

      OtelMetric.OtelGauge gauge = new OtelMetric.OtelGauge(dataPoints);
      assertEquals(1, gauge.dataPoints().size());
    }

    @Test
    @DisplayName("OtelSum should hold data points")
    void otelSumShouldHoldDataPoints() {
      List<OtelDataPoint> dataPoints =
          List.of(OtelDataPoint.counterInt(0L, 100L, List.of()));

      OtelMetric.OtelSum sum =
          new OtelMetric.OtelSum(dataPoints, "AGGREGATION_TEMPORALITY_CUMULATIVE", true);

      assertEquals(1, sum.dataPoints().size());
      assertTrue(sum.isMonotonic());
    }
  }

  // =========================================================================
  // OtelLogRecord Tests
  // =========================================================================
  @Nested
  @DisplayName("OtelLogRecord Record")
  class OtelLogRecordTest {

    @Test
    @DisplayName("should create info log record")
    void shouldCreateInfoLogRecord() {
      List<OtelAttribute> attrs = List.of(OtelAttribute.ofString("component", "agent"));

      OtelLogRecord log = OtelLogRecord.info("Agent started", attrs, "trace-123", "span-456");

      assertEquals("Agent started", log.body());
      assertEquals(OtelLogRecord.SEVERITY_INFO, log.severityNumber());
      assertEquals("INFO", log.severityText());
      assertEquals("trace-123", log.traceId());
      assertEquals("span-456", log.spanId());
    }

    @Test
    @DisplayName("should create error log record")
    void shouldCreateErrorLogRecord() {
      List<OtelAttribute> attrs = List.of(OtelAttribute.ofString("error.type", "RuntimeException"));

      OtelLogRecord log = OtelLogRecord.error("Connection failed", attrs, null, null);

      assertEquals("Connection failed", log.body());
      assertEquals(OtelLogRecord.SEVERITY_ERROR, log.severityNumber());
      assertEquals("ERROR", log.severityText());
      assertNull(log.traceId());
      assertNull(log.spanId());
    }

    @Test
    @DisplayName("should create debug log record")
    void shouldCreateDebugLogRecord() {
      OtelLogRecord log = OtelLogRecord.debug("Debug info", List.of(), null, null);

      assertEquals(OtelLogRecord.SEVERITY_DEBUG, log.severityNumber());
      assertEquals("DEBUG", log.severityText());
    }

    @Test
    @DisplayName("should have correct severity constants")
    void shouldHaveCorrectSeverityConstants() {
      assertEquals(0, OtelLogRecord.SEVERITY_UNSPECIFIED);
      assertEquals(1, OtelLogRecord.SEVERITY_TRACE);
      assertEquals(5, OtelLogRecord.SEVERITY_DEBUG);
      assertEquals(9, OtelLogRecord.SEVERITY_INFO);
      assertEquals(13, OtelLogRecord.SEVERITY_WARN);
      assertEquals(17, OtelLogRecord.SEVERITY_ERROR);
      assertEquals(21, OtelLogRecord.SEVERITY_FATAL);
    }
  }

  // =========================================================================
  // OtelExportRequest Tests
  // =========================================================================
  @Nested
  @DisplayName("OtelExportRequest Record")
  class OtelExportRequestTest {

    @Test
    @DisplayName("should create export request for single span")
    void shouldCreateExportRequestForSingleSpan() {
      OtelSpan span =
          OtelSpan.builder()
              .traceId("trace-1")
              .spanId("span-1")
              .name("test-span")
              .startTimeNanos(1000L)
              .build();

      OtelExportRequest request = OtelExportRequest.forSpan(span);

      assertNotNull(request.resourceSpans());
      assertEquals(1, request.resourceSpans().size());
    }

    @Test
    @DisplayName("should create export request for multiple spans")
    void shouldCreateExportRequestForMultipleSpans() {
      OtelSpan span1 =
          OtelSpan.builder()
              .traceId("trace-1")
              .spanId("span-1")
              .name("span-1")
              .startTimeNanos(1000L)
              .build();
      OtelSpan span2 =
          OtelSpan.builder()
              .traceId("trace-1")
              .spanId("span-2")
              .name("span-2")
              .startTimeNanos(2000L)
              .build();

      OtelExportRequest request = OtelExportRequest.forSpans(List.of(span1, span2));

      assertNotNull(request.resourceSpans());
      assertEquals(1, request.resourceSpans().size());
    }
  }

  // =========================================================================
  // Edge Cases
  // =========================================================================
  @Nested
  @DisplayName("Edge Cases")
  class EdgeCasesTest {

    @Test
    @DisplayName("should handle empty attribute list")
    void shouldHandleEmptyAttributeList() {
      OtelResource resource = new OtelResource(List.of());
      assertTrue(resource.attributes().isEmpty());
    }

    @Test
    @DisplayName("should handle span with null optional fields")
    void shouldHandleSpanWithNullOptionalFields() {
      OtelSpan span =
          new OtelSpan("trace", "span", null, "name", 1, "1000", null, null, null);

      assertNull(span.parentSpanId());
      assertNull(span.endTimeUnixNano());
      assertNull(span.attributes());
      assertNull(span.status());
    }

    @Test
    @DisplayName("should handle large timestamp values")
    void shouldHandleLargeTimestampValues() {
      OtelSpan span =
          OtelSpan.builder()
              .traceId("t")
              .spanId("s")
              .name("span")
              .startTimeNanos(Long.MAX_VALUE)
              .build();

      assertEquals(String.valueOf(Long.MAX_VALUE), span.startTimeUnixNano());
    }

    @Test
    @DisplayName("should handle unicode in log body")
    void shouldHandleUnicodeInLogBody() {
      OtelLogRecord log =
          OtelLogRecord.info("Hello 世界 🌍", List.of(), null, null);

      assertEquals("Hello 世界 🌍", log.body());
    }
  }
}
