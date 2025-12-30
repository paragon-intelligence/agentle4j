package com.paragon.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Immutable HTTP response.
 */
public final class HttpResponse {

  private final int statusCode;
  private final String statusMessage;
  private final Map<String, List<String>> headers;
  private final byte[] body;
  private final long latencyMs;

  private HttpResponse(int statusCode, String statusMessage, Map<String, List<String>> headers, byte[] body, long latencyMs) {
    this.statusCode = statusCode;
    this.statusMessage = statusMessage;
    this.headers = Map.copyOf(headers);
    this.body = body;
    this.latencyMs = latencyMs;
  }

  public static HttpResponse of(int status, String message, Map<String, List<String>> headers, byte[] body, long latencyMs) {
    return new HttpResponse(status, message, headers, body, latencyMs);
  }

  public int statusCode() {
    return statusCode;
  }

  public String statusMessage() {
    return statusMessage;
  }

  public Map<String, List<String>> headers() {
    return headers;
  }

  public byte[] body() {
    return body;
  }

  public long latencyMs() {
    return latencyMs;
  }

  public boolean isSuccessful() {
    return statusCode >= 200 && statusCode < 300;
  }

  public boolean isClientError() {
    return statusCode >= 400 && statusCode < 500;
  }

  public boolean isServerError() {
    return statusCode >= 500 && statusCode < 600;
  }

  public String header(String name) {
    var values = headers.get(name.toLowerCase());
    return values != null && !values.isEmpty() ? values.getFirst() : null;
  }

  public String bodyAsString() {
    return body != null ? new String(body, StandardCharsets.UTF_8) : null;
  }

  public <T> T bodyAs(Class<T> type, ObjectMapper mapper) {
    if (body == null) throw new IllegalStateException("Response has no body");
    try {
      return mapper.readValue(body, type);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to deserialize response", e);
    }
  }

  public <T> T bodyAs(TypeReference<T> type, ObjectMapper mapper) {
    if (body == null) throw new IllegalStateException("Response has no body");
    try {
      return mapper.readValue(body, type);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to deserialize response", e);
    }
  }

  @Override
  public String toString() {
    return "HttpResponse{status=%d, body=%s, latency=%dms}".formatted(
            statusCode, body != null ? body.length + "B" : "null", latencyMs
    );
  }
}