package com.paragon.http;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link HttpResponse}. */
class HttpResponseTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  // ==================== Factory Method Tests ====================

  @Test
  void of_createsResponse() {
    HttpResponse response = HttpResponse.of(200, "OK", Map.of(), "test".getBytes(), 100);

    assertEquals(200, response.statusCode());
    assertEquals("OK", response.statusMessage());
    assertArrayEquals("test".getBytes(), response.body());
    assertEquals(100, response.latencyMs());
  }

  @Test
  void of_createsResponseWithNullBody() {
    HttpResponse response = HttpResponse.of(204, "No Content", Map.of(), null, 50);

    assertEquals(204, response.statusCode());
    assertNull(response.body());
  }

  // ==================== Status Code Tests ====================

  @Test
  void isSuccessful_returnsTrueFor2xx() {
    assertTrue(HttpResponse.of(200, "OK", Map.of(), null, 0).isSuccessful());
    assertTrue(HttpResponse.of(201, "Created", Map.of(), null, 0).isSuccessful());
    assertTrue(HttpResponse.of(204, "No Content", Map.of(), null, 0).isSuccessful());
    assertTrue(HttpResponse.of(299, "Custom Success", Map.of(), null, 0).isSuccessful());
  }

  @Test
  void isSuccessful_returnsFalseForNon2xx() {
    assertFalse(HttpResponse.of(199, "Continue", Map.of(), null, 0).isSuccessful());
    assertFalse(HttpResponse.of(300, "Redirect", Map.of(), null, 0).isSuccessful());
    assertFalse(HttpResponse.of(400, "Bad Request", Map.of(), null, 0).isSuccessful());
    assertFalse(HttpResponse.of(500, "Server Error", Map.of(), null, 0).isSuccessful());
  }

  @Test
  void isClientError_returnsTrueFor4xx() {
    assertTrue(HttpResponse.of(400, "Bad Request", Map.of(), null, 0).isClientError());
    assertTrue(HttpResponse.of(401, "Unauthorized", Map.of(), null, 0).isClientError());
    assertTrue(HttpResponse.of(404, "Not Found", Map.of(), null, 0).isClientError());
    assertTrue(HttpResponse.of(429, "Too Many Requests", Map.of(), null, 0).isClientError());
    assertTrue(HttpResponse.of(499, "Custom Client Error", Map.of(), null, 0).isClientError());
  }

  @Test
  void isClientError_returnsFalseForNon4xx() {
    assertFalse(HttpResponse.of(200, "OK", Map.of(), null, 0).isClientError());
    assertFalse(HttpResponse.of(399, "Redirect", Map.of(), null, 0).isClientError());
    assertFalse(HttpResponse.of(500, "Server Error", Map.of(), null, 0).isClientError());
  }

  @Test
  void isServerError_returnsTrueFor5xx() {
    assertTrue(HttpResponse.of(500, "Internal Server Error", Map.of(), null, 0).isServerError());
    assertTrue(HttpResponse.of(502, "Bad Gateway", Map.of(), null, 0).isServerError());
    assertTrue(HttpResponse.of(503, "Service Unavailable", Map.of(), null, 0).isServerError());
    assertTrue(HttpResponse.of(599, "Custom Server Error", Map.of(), null, 0).isServerError());
  }

  @Test
  void isServerError_returnsFalseForNon5xx() {
    assertFalse(HttpResponse.of(200, "OK", Map.of(), null, 0).isServerError());
    assertFalse(HttpResponse.of(400, "Bad Request", Map.of(), null, 0).isServerError());
    assertFalse(HttpResponse.of(499, "Client Error", Map.of(), null, 0).isServerError());
  }

  // ==================== Header Tests ====================

  @Test
  void header_returnsFirstValue() {
    Map<String, List<String>> headers = Map.of("content-type", List.of("application/json"));
    HttpResponse response = HttpResponse.of(200, "OK", headers, null, 0);

    assertEquals("application/json", response.header("Content-Type"));
  }

  @Test
  void header_isCaseInsensitive() {
    Map<String, List<String>> headers = Map.of("x-custom-header", List.of("value"));
    HttpResponse response = HttpResponse.of(200, "OK", headers, null, 0);

    assertEquals("value", response.header("X-Custom-Header"));
    assertEquals("value", response.header("x-custom-header"));
  }

  @Test
  void header_returnsNullForMissingHeader() {
    HttpResponse response = HttpResponse.of(200, "OK", Map.of(), null, 0);

    assertNull(response.header("X-Non-Existent"));
  }

  @Test
  void header_returnsNullForEmptyList() {
    Map<String, List<String>> headers = Map.of("empty", List.of());
    HttpResponse response = HttpResponse.of(200, "OK", headers, null, 0);

    assertNull(response.header("empty"));
  }

  @Test
  void headers_returnsImmutableCopy() {
    Map<String, List<String>> headers = Map.of("key", List.of("value"));
    HttpResponse response = HttpResponse.of(200, "OK", headers, null, 0);

    assertThrows(
        UnsupportedOperationException.class, () -> response.headers().put("new", List.of()));
  }

  // ==================== Body Tests ====================

  @Test
  void bodyAsString_returnsStringContent() {
    byte[] body = "Hello, World!".getBytes();
    HttpResponse response = HttpResponse.of(200, "OK", Map.of(), body, 0);

    assertEquals("Hello, World!", response.bodyAsString());
  }

  @Test
  void bodyAsString_returnsNullForNullBody() {
    HttpResponse response = HttpResponse.of(200, "OK", Map.of(), null, 0);

    assertNull(response.bodyAsString());
  }

  @Test
  void bodyAs_deserializesToClass() throws Exception {
    byte[] body = "{\"name\":\"test\",\"value\":42}".getBytes();
    HttpResponse response = HttpResponse.of(200, "OK", Map.of(), body, 0);

    TestPayload payload = response.bodyAs(TestPayload.class, MAPPER);

    assertEquals("test", payload.name);
    assertEquals(42, payload.value);
  }

  @Test
  void bodyAs_deserializesWithTypeReference() throws Exception {
    byte[] body = "[{\"name\":\"a\"},{\"name\":\"b\"}]".getBytes();
    HttpResponse response = HttpResponse.of(200, "OK", Map.of(), body, 0);

    List<TestPayload> list = response.bodyAs(new TypeReference<List<TestPayload>>() {}, MAPPER);

    assertEquals(2, list.size());
    assertEquals("a", list.get(0).name);
    assertEquals("b", list.get(1).name);
  }

  @Test
  void bodyAs_throwsForNullBody() {
    HttpResponse response = HttpResponse.of(200, "OK", Map.of(), null, 0);

    assertThrows(IllegalStateException.class, () -> response.bodyAs(TestPayload.class, MAPPER));
  }

  @Test
  void bodyAs_throwsForInvalidJson() {
    byte[] body = "not valid json".getBytes();
    HttpResponse response = HttpResponse.of(200, "OK", Map.of(), body, 0);

    assertThrows(IllegalStateException.class, () -> response.bodyAs(TestPayload.class, MAPPER));
  }

  @Test
  void bodyAsTypeReference_throwsForNullBody() {
    HttpResponse response = HttpResponse.of(200, "OK", Map.of(), null, 0);

    assertThrows(
        IllegalStateException.class,
        () -> response.bodyAs(new TypeReference<List<TestPayload>>() {}, MAPPER));
  }

  // ==================== ToString Tests ====================

  @Test
  void toString_includesStatusCode() {
    HttpResponse response = HttpResponse.of(200, "OK", Map.of(), "body".getBytes(), 150);

    String str = response.toString();
    assertTrue(str.contains("200"));
    assertTrue(str.contains("150"));
    assertTrue(str.contains("4B")); // body length
  }

  @Test
  void toString_handlesNullBody() {
    HttpResponse response = HttpResponse.of(204, "No Content", Map.of(), null, 50);

    String str = response.toString();
    assertTrue(str.contains("204"));
    assertTrue(str.contains("null"));
  }

  // Test payload class
  public static class TestPayload {
    public String name;
    public int value;

    public TestPayload() {}

    public TestPayload(String name, int value) {
      this.name = name;
      this.value = value;
    }
  }
}
