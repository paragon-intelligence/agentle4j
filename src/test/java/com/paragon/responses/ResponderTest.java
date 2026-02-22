package com.paragon.responses;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.responses.json.JacksonJsonSchemaProducer;
import com.paragon.responses.json.JsonSchemaProducer;
import com.paragon.responses.spec.*;
import java.io.IOException;
import java.util.List;
import net.jqwik.api.*;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the Responder class.
 *
 * <p>Tests cover: - Builder pattern functionality - HTTP request building and execution - Response
 * parsing and deserialization - Async operations - Error handling - Equality, hashCode, and
 * toString - Property-based tests for robustness
 */
class ResponderTest {

  private static final ObjectMapper objectMapper = ResponsesApiObjectMapper.create();
  private static final String TEST_API_KEY = "test-api-key-12345";

  private MockWebServer mockWebServer;
  private OkHttpClient okHttpClient;

  @BeforeEach
  void setUp() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    okHttpClient = new OkHttpClient();
  }

  @AfterEach
  void tearDown() throws IOException {
    mockWebServer.shutdown();
  }

  // ===== Builder Tests =====

  @Test
  void builder_createsResponderWithDefaultValues() {
    // Create a responder with minimal configuration
    Responder responder = Responder.builder().apiKey(TEST_API_KEY).build();

    assertNotNull(responder);
    assertNotNull(responder.toString());
  }

  @Test
  void builder_openAi_setsOpenAIProvider() {
    Responder responder = Responder.builder().openAi().apiKey(TEST_API_KEY).build();

    assertNotNull(responder);
    assertTrue(responder.toString().contains("OPENAI"));
  }

  @Test
  void builder_openRouter_setsOpenRouterProvider() {
    Responder responder = Responder.builder().openRouter().apiKey(TEST_API_KEY).build();

    assertNotNull(responder);
    assertTrue(responder.toString().contains("OPEN_ROUTER"));
  }

  @Test
  void builder_customProvider_setsProvider() {
    Responder responder =
        Responder.builder().provider(ResponsesAPIProvider.OPENAI).apiKey(TEST_API_KEY).build();

    assertNotNull(responder);
    assertTrue(responder.toString().contains("OPENAI"));
  }

  @Test
  void builder_customHttpClient_usesProvidedClient() {
    OkHttpClient customClient = new OkHttpClient.Builder().build();

    Responder responder = Responder.builder().httpClient(customClient).apiKey(TEST_API_KEY).build();

    assertNotNull(responder);
  }

  @Test
  void builder_customJsonSchemaProducer_usesProvidedProducer() {
    JsonSchemaProducer customProducer = new JacksonJsonSchemaProducer(new ObjectMapper());

    Responder responder =
        Responder.builder().jsonSchemaProducer(customProducer).apiKey(TEST_API_KEY).build();

    assertNotNull(responder);
  }

  @Test
  void builder_customObjectMapper_usesProvidedMapper() {
    ObjectMapper customMapper = new ObjectMapper();

    Responder responder =
        Responder.builder().objectMapper(customMapper).apiKey(TEST_API_KEY).build();

    assertNotNull(responder);
  }

  @Test
  void builder_apiKey_throwsNullPointerExceptionWhenNull() {
    assertThrows(NullPointerException.class, () -> Responder.builder().apiKey(null));
  }

  @Test
  void builder_httpClient_throwsNullPointerExceptionWhenNull() {
    assertThrows(NullPointerException.class, () -> Responder.builder().httpClient(null));
  }

  @Test
  void builder_jsonSchemaProducer_throwsNullPointerExceptionWhenNull() {
    assertThrows(NullPointerException.class, () -> Responder.builder().jsonSchemaProducer(null));
  }

  @Test
  void builder_provider_throwsNullPointerExceptionWhenNull() {
    assertThrows(NullPointerException.class, () -> Responder.builder().provider(null));
  }

  @Test
  void builder_objectMapper_throwsNullPointerExceptionWhenNull() {
    assertThrows(NullPointerException.class, () -> Responder.builder().objectMapper(null));
  }

  // ===== Respond Method Tests with MockWebServer =====

  @Test
  void respond_sendsRequestAndReturnsResponse() throws Exception {
    // Create a mock response
    Response mockResponse = createSampleResponse();
    String responseJson = objectMapper.writeValueAsString(mockResponse);

    // Enqueue the mock response
    mockWebServer.enqueue(
        new MockResponse()
            .setBody(responseJson)
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json"));

    // Create responder with mock URL
    Responder responder =
        Responder.builder()
            .httpClient(okHttpClient)
            .apiKey(TEST_API_KEY)
            .baseUrl(mockWebServer.url("/v1/responses"))
            .build();

    // Create payload
    CreateResponsePayload payload = createSamplePayload();

    // Execute
    Response future = responder.respond(payload);
    Response response = future;

    // Verify
    assertNotNull(response);
    assertEquals(mockResponse.id(), response.id());
    assertEquals(mockResponse.model(), response.model());
  }

  @Test
  void respond_handlesJsonProcessingException() throws Exception {
    // Enqueue invalid JSON response
    mockWebServer.enqueue(
        new MockResponse()
            .setBody("invalid json")
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json"));

    Responder responder =
        Responder.builder()
            .httpClient(okHttpClient)
            .apiKey(TEST_API_KEY)
            .baseUrl(mockWebServer.url("/v1/responses"))
            .build();

    CreateResponsePayload payload = createSamplePayload();

    // Execute and verify exception - API is synchronous now, so exceptions throw directly
    assertThrows(RuntimeException.class, () -> responder.respond(payload));
  }

  // ===== Parse Method Tests =====

  @Test
  void parse_extractsAndDeserializesOutputText() throws Exception {
    // Create a response with output text containing JSON
    TestData testData = new TestData("John Doe", 30, "test@example.com");
    String testDataJson = objectMapper.writeValueAsString(testData);

    OutputMessage<Void> outputMessage =
        new OutputMessage<>(
            List.of(new Text(testDataJson)), "msg-123", InputMessageStatus.COMPLETED, null);

    Response mockResponse =
        new Response(
            null,
            null,
            System.currentTimeMillis() / 1000,
            null,
            "resp-123",
            null,
            null,
            null,
            null,
            null,
            "gpt-4o",
            ResponseObject.RESPONSE,
            List.of(outputMessage),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            ResponseGenerationStatus.COMPLETED,
            null,
            null,
            null,
            null,
            null,
            null,
            null);

    String responseJson = objectMapper.writeValueAsString(mockResponse);
    mockWebServer.enqueue(
        new MockResponse()
            .setBody(responseJson)
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json"));

    Responder responder =
        Responder.builder()
            .httpClient(okHttpClient)
            .apiKey(TEST_API_KEY)
            .baseUrl(mockWebServer.url("/v1/responses"))
            .build();

    CreateResponsePayload.Structured<TestData> payload = createSamplePayloadForParsing();

    // Execute
    ParsedResponse<TestData> future = responder.respond(payload);
    ParsedResponse<TestData> parsedResponse = future;

    // Verify
    assertNotNull(parsedResponse);
    assertEquals("resp-123", parsedResponse.id());

    TestData parsed = parsedResponse.outputParsed();
    assertNotNull(parsed);
    assertEquals("John Doe", parsed.name);
    assertEquals(30, parsed.age);
    assertEquals("test@example.com", parsed.email);
  }

  @Test
  void parse_throwsExceptionWhenNoOutputTextFound() throws Exception {
    // Create a response without output text
    Response mockResponse =
        new Response(
            null,
            null,
            System.currentTimeMillis() / 1000,
            null,
            "resp-123",
            null,
            null,
            null,
            null,
            null,
            "gpt-4o",
            ResponseObject.RESPONSE,
            List.of(), // Empty output
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            ResponseGenerationStatus.COMPLETED,
            null,
            null,
            null,
            null,
            null,
            null,
            null);

    String responseJson = objectMapper.writeValueAsString(mockResponse);
    mockWebServer.enqueue(
        new MockResponse()
            .setBody(responseJson)
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json"));

    Responder responder =
        Responder.builder()
            .httpClient(okHttpClient)
            .apiKey(TEST_API_KEY)
            .baseUrl(mockWebServer.url("/v1/responses"))
            .build();

    CreateResponsePayload.Structured<TestData> payload = createSamplePayloadForParsing();

    // Verify exception - API is synchronous, throws directly
    RuntimeException exception =
        assertThrows(RuntimeException.class, () -> responder.respond(payload));
    assertTrue(exception.getMessage().contains("could not be parsed"));
  }

  @Test
  void parse_handlesInvalidJsonInOutput() throws Exception {
    // Create a response with invalid JSON in output text
    OutputMessage<Void> outputMessage =
        new OutputMessage<>(
            List.of(new Text("not valid json")), "msg-123", InputMessageStatus.COMPLETED, null);

    Response mockResponse =
        new Response(
            null,
            null,
            System.currentTimeMillis() / 1000,
            null,
            "resp-123",
            null,
            null,
            null,
            null,
            null,
            "gpt-4o",
            ResponseObject.RESPONSE,
            List.of(outputMessage),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            ResponseGenerationStatus.COMPLETED,
            null,
            null,
            null,
            null,
            null,
            null,
            null);

    String responseJson = objectMapper.writeValueAsString(mockResponse);
    mockWebServer.enqueue(
        new MockResponse()
            .setBody(responseJson)
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json"));

    Responder responder =
        Responder.builder()
            .httpClient(okHttpClient)
            .apiKey(TEST_API_KEY)
            .baseUrl(mockWebServer.url("/v1/responses"))
            .build();

    CreateResponsePayload.Structured<TestData> payload = createSamplePayloadForParsing();

    // Verify exception - API is synchronous, throws directly
    assertThrows(RuntimeException.class, () -> responder.respond(payload));
  }

  // ===== Equals, HashCode, and ToString Tests =====

  @Test
  void equals_sameInstance_returnsTrue() {
    Responder responder = Responder.builder().apiKey(TEST_API_KEY).build();

    assertEquals(responder, responder);
  }

  @Test
  void equals_nullObject_returnsFalse() {
    Responder responder = Responder.builder().apiKey(TEST_API_KEY).build();

    assertNotEquals(null, responder);
  }

  @Test
  void equals_differentClass_returnsFalse() {
    Responder responder = Responder.builder().apiKey(TEST_API_KEY).build();

    assertNotEquals("not a responder", responder);
  }

  @Test
  void equals_sameConfiguration_returnsTrue() {
    OkHttpClient sharedClient = new OkHttpClient();
    JsonSchemaProducer sharedProducer = new JacksonJsonSchemaProducer(new ObjectMapper());

    Responder responder1 =
        Responder.builder()
            .httpClient(sharedClient)
            .jsonSchemaProducer(sharedProducer)
            .apiKey(TEST_API_KEY)
            .openAi()
            .build();

    Responder responder2 =
        Responder.builder()
            .httpClient(sharedClient)
            .jsonSchemaProducer(sharedProducer)
            .apiKey(TEST_API_KEY)
            .openAi()
            .build();

    assertEquals(responder1, responder2);
  }

  @Test
  void equals_differentHttpClient_returnsFalse() {
    JsonSchemaProducer sharedProducer = new JacksonJsonSchemaProducer(new ObjectMapper());

    Responder responder1 =
        Responder.builder()
            .httpClient(new OkHttpClient())
            .jsonSchemaProducer(sharedProducer)
            .apiKey(TEST_API_KEY)
            .build();

    Responder responder2 =
        Responder.builder()
            .httpClient(new OkHttpClient())
            .jsonSchemaProducer(sharedProducer)
            .apiKey(TEST_API_KEY)
            .build();

    assertNotEquals(responder1, responder2);
  }

  @Test
  void hashCode_sameConfiguration_returnsSameHashCode() {
    OkHttpClient sharedClient = new OkHttpClient();
    JsonSchemaProducer sharedProducer = new JacksonJsonSchemaProducer(new ObjectMapper());

    Responder responder1 =
        Responder.builder()
            .httpClient(sharedClient)
            .jsonSchemaProducer(sharedProducer)
            .apiKey(TEST_API_KEY)
            .openAi()
            .build();

    Responder responder2 =
        Responder.builder()
            .httpClient(sharedClient)
            .jsonSchemaProducer(sharedProducer)
            .apiKey(TEST_API_KEY)
            .openAi()
            .build();

    assertEquals(responder1.hashCode(), responder2.hashCode());
  }

  @Test
  void toString_containsRelevantInformation() {
    Responder responder = Responder.builder().openAi().apiKey(TEST_API_KEY).build();

    String toString = responder.toString();

    assertNotNull(toString);
    assertTrue(toString.contains("Responder"));
    assertTrue(toString.contains("OPENAI"));
  }

  @Test
  void toString_consistentAcrossMultipleCalls() {
    Responder responder = Responder.builder().apiKey(TEST_API_KEY).build();

    String toString1 = responder.toString();
    String toString2 = responder.toString();

    assertEquals(toString1, toString2);
  }

  // ===== Property-Based Tests =====

  /** Property: Builder should never produce null Responder instances */
  @Property(tries = 50)
  void builder_neverProducesNull(@ForAll("providers") ResponsesAPIProvider provider) {
    Responder responder = Responder.builder().provider(provider).apiKey(TEST_API_KEY).build();

    assertNotNull(responder);
  }

  /** Property: toString should never return null or empty string */
  @Property(tries = 50)
  void toString_neverReturnsNullOrEmpty(@ForAll("providers") ResponsesAPIProvider provider) {
    Responder responder = Responder.builder().provider(provider).apiKey(TEST_API_KEY).build();

    String toString = responder.toString();

    assertNotNull(toString);
    assertFalse(toString.isEmpty());
  }

  /** Property: hashCode should be consistent across multiple calls */
  @Property(tries = 50)
  void hashCode_isConsistent(@ForAll("providers") ResponsesAPIProvider provider) {
    OkHttpClient sharedClient = new OkHttpClient();

    Responder responder =
        Responder.builder()
            .httpClient(sharedClient)
            .provider(provider)
            .apiKey(TEST_API_KEY)
            .build();

    int hash1 = responder.hashCode();
    int hash2 = responder.hashCode();

    assertEquals(hash1, hash2);
  }

  /** Property: Reflexivity - equals should return true when comparing to itself */
  @Property(tries = 50)
  void equals_reflexive(@ForAll("providers") ResponsesAPIProvider provider) {
    Responder responder = Responder.builder().provider(provider).apiKey(TEST_API_KEY).build();

    assertEquals(responder, responder);
  }

  /** Property: Symmetry - if a.equals(b) then b.equals(a) */
  @Property(tries = 50)
  void equals_symmetric(@ForAll("providers") ResponsesAPIProvider provider) {
    OkHttpClient sharedClient = new OkHttpClient();
    JsonSchemaProducer sharedProducer = new JacksonJsonSchemaProducer(new ObjectMapper());

    Responder a =
        Responder.builder()
            .httpClient(sharedClient)
            .jsonSchemaProducer(sharedProducer)
            .provider(provider)
            .apiKey(TEST_API_KEY)
            .build();

    Responder b =
        Responder.builder()
            .httpClient(sharedClient)
            .jsonSchemaProducer(sharedProducer)
            .provider(provider)
            .apiKey(TEST_API_KEY)
            .build();

    assertEquals(a.equals(b), b.equals(a));
  }

  // ===== Test Helpers =====

  private Response createSampleResponse() {
    return new Response(
        null,
        null,
        System.currentTimeMillis() / 1000,
        null,
        "resp-test-123",
        null,
        null,
        null,
        null,
        null,
        "gpt-4o",
        ResponseObject.RESPONSE,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        ResponseGenerationStatus.COMPLETED,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  private CreateResponsePayload createSamplePayload() {
    return new CreateResponsePayload(
        null,
        null,
        null,
        List.of(new DeveloperMessage(List.of(new Text("Test")), null)),
        "Test instructions",
        null,
        null,
        null,
        "gpt-4o",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  private CreateResponsePayload.Structured<TestData> createSamplePayloadForParsing() {
    return CreateResponsePayload.builder()
        .model("gpt-4o")
        .instructions("Test instructions")
        .addDeveloperMessage("Test", InputMessageStatus.COMPLETED)
        .withStructuredOutput(TestData.class)
        .build();
  }

  // ===== Arbitraries (Generators) for Property-Based Tests =====

  @Provide
  Arbitrary<ResponsesAPIProvider> providers() {
    return Arbitraries.of(ResponsesAPIProvider.values());
  }

  // ===== Test Data Classes =====

  /** Simple test data class for testing parsing functionality */
  private static class TestData {
    public String name;
    public int age;
    public String email;

    public TestData() {}

    public TestData(String name, int age, String email) {
      this.name = name;
      this.age = age;
      this.email = email;
    }
  }
}
