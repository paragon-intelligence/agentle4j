package com.paragon.http;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Unit tests for {@link HttpException}. */
class HttpExceptionTest {

  // ==================== Factory Method Tests ====================

  @Test
  void networkFailure_createsExceptionWithCause() {
    Exception cause = new RuntimeException("Connection reset");
    HttpException ex = HttpException.networkFailure("Network error", cause);

    assertEquals("Network error", ex.getMessage());
    assertEquals(-1, ex.statusCode());
    assertNull(ex.responseBody());
    assertNull(ex.requestMethod());
    assertNull(ex.requestUrl());
    assertEquals(cause, ex.getCause());
  }

  @Test
  void fromResponse_createsExceptionWithDetails() {
    HttpException ex = HttpException.fromResponse(404, "Not Found", "GET", "/users/1");

    assertTrue(ex.getMessage().contains("404"));
    assertTrue(ex.getMessage().contains("GET"));
    assertTrue(ex.getMessage().contains("/users/1"));
    assertEquals(404, ex.statusCode());
    assertEquals("Not Found", ex.responseBody());
    assertEquals("GET", ex.requestMethod());
    assertEquals("/users/1", ex.requestUrl());
    assertNull(ex.getCause());
  }

  @Test
  void deserializationFailed_createsExceptionWithCause() {
    Exception cause = new RuntimeException("Invalid JSON");
    HttpException ex = HttpException.deserializationFailed(200, "{invalid}", cause);

    assertEquals("Failed to deserialize response", ex.getMessage());
    assertEquals(200, ex.statusCode());
    assertEquals("{invalid}", ex.responseBody());
    assertNull(ex.requestMethod());
    assertNull(ex.requestUrl());
    assertEquals(cause, ex.getCause());
  }

  // ==================== Status Code Classification Tests ====================

  @Test
  void isClientError_returnsTrueFor4xx() {
    assertTrue(HttpException.fromResponse(400, null, "GET", "/").isClientError());
    assertTrue(HttpException.fromResponse(401, null, "GET", "/").isClientError());
    assertTrue(HttpException.fromResponse(404, null, "GET", "/").isClientError());
    assertTrue(HttpException.fromResponse(429, null, "GET", "/").isClientError());
    assertTrue(HttpException.fromResponse(499, null, "GET", "/").isClientError());
  }

  @Test
  void isClientError_returnsFalseForNon4xx() {
    assertFalse(HttpException.fromResponse(200, null, "GET", "/").isClientError());
    assertFalse(HttpException.fromResponse(399, null, "GET", "/").isClientError());
    assertFalse(HttpException.fromResponse(500, null, "GET", "/").isClientError());
    assertFalse(HttpException.networkFailure("error", null).isClientError());
  }

  @Test
  void isServerError_returnsTrueFor5xx() {
    assertTrue(HttpException.fromResponse(500, null, "GET", "/").isServerError());
    assertTrue(HttpException.fromResponse(502, null, "GET", "/").isServerError());
    assertTrue(HttpException.fromResponse(503, null, "GET", "/").isServerError());
    assertTrue(HttpException.fromResponse(599, null, "GET", "/").isServerError());
  }

  @Test
  void isServerError_returnsFalseForNon5xx() {
    assertFalse(HttpException.fromResponse(200, null, "GET", "/").isServerError());
    assertFalse(HttpException.fromResponse(400, null, "GET", "/").isServerError());
    assertFalse(HttpException.fromResponse(499, null, "GET", "/").isServerError());
    assertFalse(HttpException.networkFailure("error", null).isServerError());
  }

  // ==================== Retryable Tests ====================

  @Test
  void isRetryable_returnsTrueForNetworkError() {
    HttpException ex = HttpException.networkFailure("Connection reset", null);
    assertTrue(ex.isRetryable());
  }

  @Test
  void isRetryable_returnsTrueFor408() {
    HttpException ex = HttpException.fromResponse(408, null, "GET", "/");
    assertTrue(ex.isRetryable());
  }

  @Test
  void isRetryable_returnsTrueFor429() {
    HttpException ex = HttpException.fromResponse(429, null, "GET", "/");
    assertTrue(ex.isRetryable());
  }

  @Test
  void isRetryable_returnsTrueFor5xx() {
    assertTrue(HttpException.fromResponse(500, null, "GET", "/").isRetryable());
    assertTrue(HttpException.fromResponse(502, null, "GET", "/").isRetryable());
    assertTrue(HttpException.fromResponse(503, null, "GET", "/").isRetryable());
    assertTrue(HttpException.fromResponse(504, null, "GET", "/").isRetryable());
  }

  @Test
  void isRetryable_returnsFalseFor4xxExcept408And429() {
    assertFalse(HttpException.fromResponse(400, null, "GET", "/").isRetryable());
    assertFalse(HttpException.fromResponse(401, null, "GET", "/").isRetryable());
    assertFalse(HttpException.fromResponse(403, null, "GET", "/").isRetryable());
    assertFalse(HttpException.fromResponse(404, null, "GET", "/").isRetryable());
  }

  // ==================== ToString Tests ====================

  @Test
  void toString_includesStatusCode() {
    HttpException ex = HttpException.fromResponse(404, null, "GET", "/users");

    String str = ex.toString();
    assertTrue(str.contains("status=404"));
  }

  @Test
  void toString_includesMethodAndUrl() {
    HttpException ex = HttpException.fromResponse(404, null, "GET", "/users");

    String str = ex.toString();
    assertTrue(str.contains("method=GET"));
    assertTrue(str.contains("url=/users"));
  }

  @Test
  void toString_includesBodyWhenPresent() {
    HttpException ex = HttpException.fromResponse(404, "Not Found", "GET", "/users");

    String str = ex.toString();
    assertTrue(str.contains("body=Not Found"));
  }

  @Test
  void toString_truncatesLongBody() {
    String longBody = "x".repeat(200);
    HttpException ex = HttpException.fromResponse(404, longBody, "GET", "/users");

    String str = ex.toString();
    assertTrue(str.contains("..."));
    assertTrue(str.length() < 250); // Should be truncated
  }

  @Test
  void toString_excludesBodyWhenNull() {
    HttpException ex = HttpException.fromResponse(404, null, "GET", "/users");

    String str = ex.toString();
    assertFalse(str.contains("body="));
  }

  @Test
  void toString_excludesBodyWhenEmpty() {
    HttpException ex = HttpException.fromResponse(404, "", "GET", "/users");

    String str = ex.toString();
    assertFalse(str.contains("body="));
  }

  @Test
  void toString_handlesNetworkFailure() {
    HttpException ex = HttpException.networkFailure("Connection refused", null);

    String str = ex.toString();
    assertTrue(str.contains("status=-1"));
  }

  // ==================== Null Safety Tests ====================

  @Test
  void fromResponse_handlesNullBody() {
    HttpException ex = HttpException.fromResponse(404, null, "GET", "/");

    assertNull(ex.responseBody());
    assertNotNull(ex.getMessage());
  }

  @Test
  void deserializationFailed_handlesNullBody() {
    HttpException ex = HttpException.deserializationFailed(200, null, new RuntimeException());

    assertNull(ex.responseBody());
    assertNotNull(ex.getMessage());
  }
}
