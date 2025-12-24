package com.paragon.telemetry.processors;

import java.security.SecureRandom;
import java.util.HexFormat;
import org.jspecify.annotations.NonNull;

/** Utility class for generating OpenTelemetry-compatible trace and span IDs. */
public final class TraceIdGenerator {

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final HexFormat HEX = HexFormat.of();

  private TraceIdGenerator() {}

  /** Generates a random 16-byte (32 hex character) trace ID. */
  public static @NonNull String generateTraceId() {
    byte[] bytes = new byte[16];
    RANDOM.nextBytes(bytes);
    return HEX.formatHex(bytes);
  }

  /** Generates a random 8-byte (16 hex character) span ID. */
  public static @NonNull String generateSpanId() {
    byte[] bytes = new byte[8];
    RANDOM.nextBytes(bytes);
    return HEX.formatHex(bytes);
  }

  /** Validates a trace ID format (32 hex characters). */
  public static boolean isValidTraceId(@NonNull String traceId) {
    return traceId.length() == 32
        && traceId
            .chars()
            .allMatch(
                c -> (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'));
  }

  /** Validates a span ID format (16 hex characters). */
  public static boolean isValidSpanId(@NonNull String spanId) {
    return spanId.length() == 16
        && spanId
            .chars()
            .allMatch(
                c -> (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'));
  }
}
