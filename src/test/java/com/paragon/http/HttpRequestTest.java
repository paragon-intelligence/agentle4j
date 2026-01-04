package com.paragon.http;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link HttpRequest}. */
class HttpRequestTest {

  // ==================== Factory Method Tests ====================

  @Test
  void get_createsGetRequest() {
    HttpRequest request = HttpRequest.get("/users").build();

    assertEquals("GET", request.method());
    assertEquals("/users", request.url());
    assertNull(request.body());
  }

  @Test
  void post_createsPostRequest() {
    HttpRequest request = HttpRequest.post("/users").build();

    assertEquals("POST", request.method());
    assertEquals("/users", request.url());
  }

  @Test
  void put_createsPutRequest() {
    HttpRequest request = HttpRequest.put("/users/1").build();

    assertEquals("PUT", request.method());
    assertEquals("/users/1", request.url());
  }

  @Test
  void patch_createsPatchRequest() {
    HttpRequest request = HttpRequest.patch("/users/1").build();

    assertEquals("PATCH", request.method());
    assertEquals("/users/1", request.url());
  }

  @Test
  void delete_createsDeleteRequest() {
    HttpRequest request = HttpRequest.delete("/users/1").build();

    assertEquals("DELETE", request.method());
    assertEquals("/users/1", request.url());
  }

  // ==================== Header Tests ====================

  @Test
  void header_addsHeader() {
    HttpRequest request =
        HttpRequest.get("/test").header("X-Custom", "value1").header("X-Custom", "value2").build();

    var headers = request.headers();
    assertEquals(2, headers.get("x-custom").size());
    assertTrue(headers.get("x-custom").contains("value1"));
    assertTrue(headers.get("x-custom").contains("value2"));
  }

  @Test
  void setHeader_replacesHeader() {
    HttpRequest request =
        HttpRequest.get("/test")
            .header("X-Custom", "value1")
            .setHeader("X-Custom", "value2")
            .build();

    var headers = request.headers();
    assertEquals(1, headers.get("x-custom").size());
    assertEquals("value2", headers.get("x-custom").get(0));
  }

  @Test
  void headers_areCaseInsensitive() {
    HttpRequest request =
        HttpRequest.get("/test").header("Content-Type", "application/json").build();

    assertTrue(request.headers().containsKey("content-type"));
  }

  // ==================== Query Parameter Tests ====================

  @Test
  void queryParam_addsToUrl() {
    HttpRequest request = HttpRequest.get("/search").queryParam("q", "test").build();

    assertEquals("/search?q=test", request.url());
  }

  @Test
  void queryParam_addsMultipleParams() {
    HttpRequest request =
        HttpRequest.get("/search").queryParam("q", "test").queryParam("page", "1").build();

    assertEquals("/search?q=test&page=1", request.url());
  }

  @Test
  void queryParam_encodesSpecialCharacters() {
    HttpRequest request = HttpRequest.get("/search").queryParam("q", "hello world").build();

    assertTrue(request.url().contains("hello+world") || request.url().contains("hello%20world"));
  }

  @Test
  void queryParam_handlesExistingQueryString() {
    HttpRequest request = HttpRequest.get("/search?existing=1").queryParam("page", "2").build();

    assertEquals("/search?existing=1&page=2", request.url());
  }

  @Test
  void queryParam_supportsMultipleValues() {
    HttpRequest request =
        HttpRequest.get("/filter").queryParam("tag", "java").queryParam("tag", "maven").build();

    assertTrue(request.url().contains("tag=java"));
    assertTrue(request.url().contains("tag=maven"));
  }

  // ==================== Body Tests ====================

  @Test
  void body_setsBodyWithContentType() {
    HttpRequest request =
        HttpRequest.post("/test").body("raw data".getBytes(), "text/plain").build();

    assertArrayEquals("raw data".getBytes(), request.body());
    assertEquals("text/plain", request.contentType());
  }

  @Test
  void body_setsStringBody() {
    HttpRequest request = HttpRequest.post("/test").body("text content", "text/plain").build();

    assertEquals("text content", new String(request.body()));
    assertEquals("text/plain", request.contentType());
  }

  @Test
  void jsonBody_setsJsonContentType() {
    HttpRequest request = HttpRequest.post("/test").jsonBody("{\"key\":\"value\"}").build();

    assertEquals("{\"key\":\"value\"}", new String(request.body()));
    assertEquals("application/json", request.contentType());
  }

  @Test
  void jsonBody_serializesObject() {
    TestPayload payload = new TestPayload("test", 42);
    HttpRequest request = HttpRequest.post("/test").jsonBody(payload).build();

    assertTrue(new String(request.body()).contains("\"name\""));
    assertTrue(new String(request.body()).contains("\"test\""));
    assertTrue(new String(request.body()).contains("\"value\""));
    assertTrue(new String(request.body()).contains("42"));
    assertEquals("application/json", request.contentType());
  }

  @Test
  void jsonBody_withCustomMapper() {
    TestPayload payload = new TestPayload("test", 42);
    ObjectMapper mapper = new ObjectMapper();
    HttpRequest request = HttpRequest.post("/test").jsonBody(payload, mapper).build();

    assertEquals("application/json", request.contentType());
    assertNotNull(request.body());
  }

  @Test
  void jsonBody_throwsOnSerializationFailure() {
    Object unserializable =
        new Object() {
          public Object getSelf() {
            return this;
          } // Circular reference
        };

    assertThrows(
        IllegalArgumentException.class,
        () -> HttpRequest.post("/test").jsonBody(unserializable).build());
  }

  // ==================== Timeout Tests ====================

  @Test
  void timeout_setsDuration() {
    HttpRequest request = HttpRequest.get("/test").timeout(Duration.ofSeconds(30)).build();

    assertEquals(Duration.ofSeconds(30), request.timeout());
  }

  @Test
  void timeout_defaultsToNull() {
    HttpRequest request = HttpRequest.get("/test").build();

    assertNull(request.timeout());
  }

  // ==================== Authentication Tests ====================

  @Test
  void bearerAuth_setsAuthorizationHeader() {
    HttpRequest request = HttpRequest.get("/test").bearerAuth("mytoken123").build();

    assertEquals("Bearer mytoken123", request.headers().get("authorization").get(0));
  }

  @Test
  void basicAuth_setsEncodedHeader() {
    HttpRequest request = HttpRequest.get("/test").basicAuth("user", "pass").build();

    String header = request.headers().get("authorization").get(0);
    assertTrue(header.startsWith("Basic "));
    // Base64 of "user:pass" = "dXNlcjpwYXNz"
    assertEquals("Basic dXNlcjpwYXNz", header);
  }

  // ==================== Accept Tests ====================

  @Test
  void accept_setsAcceptHeader() {
    HttpRequest request = HttpRequest.get("/test").accept("text/html").build();

    assertEquals("text/html", request.headers().get("accept").get(0));
  }

  @Test
  void acceptJson_setsJsonAcceptHeader() {
    HttpRequest request = HttpRequest.get("/test").acceptJson().build();

    assertEquals("application/json", request.headers().get("accept").get(0));
  }

  // ==================== ToString Tests ====================

  @Test
  void toString_includesMethodAndUrl() {
    HttpRequest request = HttpRequest.get("/users").build();

    String str = request.toString();
    assertTrue(str.contains("GET"));
    assertTrue(str.contains("/users"));
  }

  @Test
  void toString_includesBodySize() {
    HttpRequest request = HttpRequest.post("/test").jsonBody("{\"a\":1}").build();

    String str = request.toString();
    assertTrue(str.contains("B")); // Contains body size in bytes
  }

  @Test
  void toString_handlesNullBody() {
    HttpRequest request = HttpRequest.get("/test").build();

    String str = request.toString();
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
