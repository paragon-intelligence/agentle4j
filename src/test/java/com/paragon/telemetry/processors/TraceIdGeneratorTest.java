package com.paragon.telemetry.processors;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for {@link TraceIdGenerator}.
 *
 * <p>Tests cover trace ID and span ID generation, format validation, and uniqueness.
 */
@DisplayName("TraceIdGenerator Tests")
class TraceIdGeneratorTest {

  // ═══════════════════════════════════════════════════════════════════════════
  // TRACE ID GENERATION
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Trace ID Generation")
  class TraceIdGenerationTests {

    @Test
    @DisplayName("generates 32-character trace ID")
    void generates32CharacterTraceId() {
      String traceId = TraceIdGenerator.generateTraceId();
      assertEquals(32, traceId.length());
    }

    @Test
    @DisplayName("generated trace ID is valid hex")
    void generatedTraceIdIsValidHex() {
      String traceId = TraceIdGenerator.generateTraceId();
      assertTrue(TraceIdGenerator.isValidTraceId(traceId));
    }

    @Test
    @DisplayName("trace IDs are unique")
    void traceIdsAreUnique() {
      Set<String> traceIds = new HashSet<>();
      for (int i = 0; i < 1000; i++) {
        String traceId = TraceIdGenerator.generateTraceId();
        assertTrue(traceIds.add(traceId), "Duplicate trace ID generated: " + traceId);
      }
    }

    @Test
    @DisplayName("trace ID contains only hex characters")
    void traceIdContainsOnlyHexCharacters() {
      for (int i = 0; i < 100; i++) {
        String traceId = TraceIdGenerator.generateTraceId();
        assertTrue(traceId.matches("[0-9a-f]{32}"), "Invalid trace ID: " + traceId);
      }
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // SPAN ID GENERATION
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Span ID Generation")
  class SpanIdGenerationTests {

    @Test
    @DisplayName("generates 16-character span ID")
    void generates16CharacterSpanId() {
      String spanId = TraceIdGenerator.generateSpanId();
      assertEquals(16, spanId.length());
    }

    @Test
    @DisplayName("generated span ID is valid hex")
    void generatedSpanIdIsValidHex() {
      String spanId = TraceIdGenerator.generateSpanId();
      assertTrue(TraceIdGenerator.isValidSpanId(spanId));
    }

    @Test
    @DisplayName("span IDs are unique")
    void spanIdsAreUnique() {
      Set<String> spanIds = new HashSet<>();
      for (int i = 0; i < 1000; i++) {
        String spanId = TraceIdGenerator.generateSpanId();
        assertTrue(spanIds.add(spanId), "Duplicate span ID generated: " + spanId);
      }
    }

    @Test
    @DisplayName("span ID contains only hex characters")
    void spanIdContainsOnlyHexCharacters() {
      for (int i = 0; i < 100; i++) {
        String spanId = TraceIdGenerator.generateSpanId();
        assertTrue(spanId.matches("[0-9a-f]{16}"), "Invalid span ID: " + spanId);
      }
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // TRACE ID VALIDATION
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Trace ID Validation")
  class TraceIdValidationTests {

    @Test
    @DisplayName("validates correct trace ID")
    void validatesCorrectTraceId() {
      assertTrue(TraceIdGenerator.isValidTraceId("0123456789abcdef0123456789abcdef"));
    }

    @Test
    @DisplayName("validates uppercase hex trace ID")
    void validatesUppercaseHexTraceId() {
      assertTrue(TraceIdGenerator.isValidTraceId("0123456789ABCDEF0123456789ABCDEF"));
    }

    @Test
    @DisplayName("validates mixed case hex trace ID")
    void validatesMixedCaseHexTraceId() {
      assertTrue(TraceIdGenerator.isValidTraceId("0123456789AbCdEf0123456789aBcDeF"));
    }

    @Test
    @DisplayName("rejects trace ID that is too short")
    void rejectsTraceIdTooShort() {
      assertFalse(TraceIdGenerator.isValidTraceId("0123456789abcdef01234567")); // 24 chars
    }

    @Test
    @DisplayName("rejects trace ID that is too long")
    void rejectsTraceIdTooLong() {
      assertFalse(
          TraceIdGenerator.isValidTraceId("0123456789abcdef0123456789abcdef0000")); // 36 chars
    }

    @Test
    @DisplayName("rejects trace ID with non-hex characters")
    void rejectsTraceIdWithNonHexCharacters() {
      assertFalse(
          TraceIdGenerator.isValidTraceId("0123456789abcdefghijklmnopqrstuv")); // g-v not hex
    }

    @Test
    @DisplayName("rejects trace ID with spaces")
    void rejectsTraceIdWithSpaces() {
      assertFalse(
          TraceIdGenerator.isValidTraceId("0123456789abcdef 123456789abcdef")); // space in middle
    }

    @Test
    @DisplayName("rejects empty trace ID")
    void rejectsEmptyTraceId() {
      assertFalse(TraceIdGenerator.isValidTraceId(""));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // SPAN ID VALIDATION
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Span ID Validation")
  class SpanIdValidationTests {

    @Test
    @DisplayName("validates correct span ID")
    void validatesCorrectSpanId() {
      assertTrue(TraceIdGenerator.isValidSpanId("0123456789abcdef"));
    }

    @Test
    @DisplayName("validates uppercase hex span ID")
    void validatesUppercaseHexSpanId() {
      assertTrue(TraceIdGenerator.isValidSpanId("0123456789ABCDEF"));
    }

    @Test
    @DisplayName("validates mixed case hex span ID")
    void validatesMixedCaseHexSpanId() {
      assertTrue(TraceIdGenerator.isValidSpanId("0123456789AbCdEf"));
    }

    @Test
    @DisplayName("rejects span ID that is too short")
    void rejectsSpanIdTooShort() {
      assertFalse(TraceIdGenerator.isValidSpanId("0123456789abcde")); // 15 chars
    }

    @Test
    @DisplayName("rejects span ID that is too long")
    void rejectsSpanIdTooLong() {
      assertFalse(TraceIdGenerator.isValidSpanId("0123456789abcdef0")); // 17 chars
    }

    @Test
    @DisplayName("rejects span ID with non-hex characters")
    void rejectsSpanIdWithNonHexCharacters() {
      assertFalse(TraceIdGenerator.isValidSpanId("0123456789ghijkl")); // g-l not hex
    }

    @Test
    @DisplayName("rejects span ID with spaces")
    void rejectsSpanIdWithSpaces() {
      assertFalse(TraceIdGenerator.isValidSpanId("01234567 9abcdef")); // space in middle
    }

    @Test
    @DisplayName("rejects empty span ID")
    void rejectsEmptySpanId() {
      assertFalse(TraceIdGenerator.isValidSpanId(""));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ROUNDTRIP TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Roundtrip Tests")
  class RoundtripTests {

    @Test
    @DisplayName("generated trace ID passes validation")
    void generatedTraceIdPassesValidation() {
      for (int i = 0; i < 100; i++) {
        String traceId = TraceIdGenerator.generateTraceId();
        assertTrue(
            TraceIdGenerator.isValidTraceId(traceId),
            "Generated trace ID failed validation: " + traceId);
      }
    }

    @Test
    @DisplayName("generated span ID passes validation")
    void generatedSpanIdPassesValidation() {
      for (int i = 0; i < 100; i++) {
        String spanId = TraceIdGenerator.generateSpanId();
        assertTrue(
            TraceIdGenerator.isValidSpanId(spanId),
            "Generated span ID failed validation: " + spanId);
      }
    }
  }
}
