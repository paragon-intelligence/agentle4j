package com.paragon.agents;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.paragon.responses.Responder;
import com.paragon.responses.ResponsesApiObjectMapper;
import com.paragon.responses.exception.AgentExecutionException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ReturnContract")
class ReturnContractTest {

  private MockWebServer mockWebServer;
  private Responder responder;

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
  @JsonSubTypes({
    @JsonSubTypes.Type(value = DirectAnswer.class, name = "local_direct"),
    @JsonSubTypes.Type(value = DirectEscalation.class, name = "local_escalation")
  })
  public sealed interface MainDirectOutput permits DirectAnswer, DirectEscalation {}

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
  @JsonSubTypes({
    @JsonSubTypes.Type(value = DirectAnswer.class, name = "local_direct"),
    @JsonSubTypes.Type(value = DirectEscalation.class, name = "local_escalation"),
    @JsonSubTypes.Type(value = ActivityOutput.class, name = "delegated_activity"),
    @JsonSubTypes.Type(value = RatOutput.class, name = "delegated_rat")
  })
  public sealed interface MainFinalOutput
      permits DirectAnswer, DirectEscalation, ActivityOutput, RatOutput {}

  public record DirectAnswer(@JsonProperty("kind") String kind, @JsonProperty("message") String message)
      implements MainDirectOutput, MainFinalOutput {
    @JsonCreator
    public DirectAnswer {}
  }

  public record DirectEscalation(@JsonProperty("kind") String kind, @JsonProperty("queue") String queue)
      implements MainDirectOutput, MainFinalOutput {
    @JsonCreator
    public DirectEscalation {}
  }

  public record ActivityOutput(
      @JsonProperty("kind") String kind, @JsonProperty("activityId") String activityId)
      implements MainFinalOutput {
    @JsonCreator
    public ActivityOutput {}
  }

  public record RatOutput(@JsonProperty("kind") String kind, @JsonProperty("ratId") String ratId)
      implements MainFinalOutput {
    @JsonCreator
    public RatOutput {}
  }

  @BeforeEach
  void setUp() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    responder =
        Responder.builder().baseUrl(mockWebServer.url("/v1/responses")).apiKey("test-key").build();
  }

  @AfterEach
  void tearDown() throws IOException {
    mockWebServer.shutdown();
  }

  @Test
  @DisplayName("returns uses only local schema in source agent request")
  void returnsUsesOnlyLocalSchemaInSourceAgentRequest() throws Exception {
    Agent mainAgent = createMainAgent();

    enqueueMessageResponse(
        """
        {"value":{"kind":"local_direct","message":"resolved locally"}}
        """);

    StructuredAgentResult<MainFinalOutput> result = mainAgent.returns(MainFinalOutput.class).interact("help");

    assertInstanceOf(DirectAnswer.class, result.parsed());
    assertEquals(OutputOrigin.LOCAL, result.outputOrigin());
    assertEquals("MainAgent", result.outputProducerName());
    assertEquals(List.of("MainAgent"), result.delegationPath());

    String requestBody = takeRequestBody();
    assertTrue(requestBody.contains("\"json_schema\""));
    assertTrue(requestBody.contains("local_direct"));
    assertTrue(requestBody.contains("local_escalation"));
    assertFalse(requestBody.contains("delegated_activity"));
    assertFalse(requestBody.contains("delegated_rat"));
  }

  @Test
  @DisplayName("handoff parses propagated output and marks result as delegated")
  void handoffParsesPropagatedOutputAndMarksResultAsDelegated() {
    Agent activitiesAgent = createPlainAgent("ActivitiesAgent");
    Agent mainAgent =
        createMainAgent(
            Handoff.to(activitiesAgent)
                .propagatedOutput(ActivityOutput.class)
                .build());

    enqueueHandoffResponse("transfer_to_activities_agent", "{\"message\":\"route to activities\"}");
    enqueueMessageResponse("{\"kind\":\"delegated_activity\",\"activityId\":\"act_123\"}");

    StructuredAgentResult<MainFinalOutput> result =
        mainAgent.returns(MainFinalOutput.class).interact("open activity");

    assertFalse(result.isError());
    assertInstanceOf(ActivityOutput.class, result.parsed());
    assertEquals("act_123", ((ActivityOutput) result.parsed()).activityId());
    assertEquals(OutputOrigin.DELEGATED, result.outputOrigin());
    assertEquals("ActivitiesAgent", result.outputProducerName());
    assertEquals(List.of("MainAgent", "ActivitiesAgent"), result.delegationPath());
    assertNotNull(result.handoffAgent());
    assertEquals("ActivitiesAgent", result.handoffAgent().name());
  }

  @Test
  @DisplayName("local output contract rejects propagated-only branch")
  void localOutputContractRejectsPropagatedOnlyBranch() {
    Agent mainAgent = createMainAgent();

    enqueueMessageResponse(
        """
        {"value":{"kind":"delegated_activity","activityId":"act_999"}}
        """);

    AgentResult result = mainAgent.interact("try to bypass");

    assertTrue(result.isError());
    assertInstanceOf(AgentExecutionException.class, result.error());
    assertEquals(OutputOrigin.LOCAL, result.outputOrigin());
  }

  @Test
  @DisplayName("router returns final contract without exposing it to classifier")
  void routerReturnsFinalContractWithoutExposingItToClassifier() throws Exception {
    Interactable.Structured<ActivityOutput> activitiesAgent =
        createStructuredActivityAgent("ActivitiesAgent");
    RouterAgent router =
        RouterAgent.builder()
            .name("MainRouter")
            .model("test-model")
            .responder(responder)
            .addRoute(activitiesAgent, "Handles activity workflows")
            .build();

    enqueueMessageResponse("1");
    enqueueMessageResponse("{\"kind\":\"delegated_activity\",\"activityId\":\"act_router\"}");

    StructuredAgentResult<MainFinalOutput> result = router.returns(MainFinalOutput.class).interact("route this");

    assertFalse(result.isError());
    assertInstanceOf(ActivityOutput.class, result.parsed());
    assertEquals(OutputOrigin.DELEGATED, result.outputOrigin());
    assertEquals("ActivitiesAgent", result.outputProducerName());
    assertEquals(List.of("MainRouter", "ActivitiesAgent"), result.delegationPath());

    String classifierRequestBody = takeRequestBody();
    assertFalse(classifierRequestBody.contains("\"json_schema\""));
    assertFalse(classifierRequestBody.contains("delegated_activity"));
    assertFalse(classifierRequestBody.contains("delegated_rat"));
  }

  private Agent createMainAgent(Handoff... handoffs) {
    Agent.Builder builder =
        Agent.builder()
            .name("MainAgent")
            .model("test-model")
            .instructions("Handle direct work or delegate terminally.")
            .responder(responder)
            .objectMapper(ResponsesApiObjectMapper.create())
            .outputType(MainDirectOutput.class);

    for (Handoff handoff : handoffs) {
      builder.addHandoff(handoff);
    }

    return builder.build();
  }

  private Agent createPlainAgent(String name) {
    return Agent.builder()
        .name(name)
        .model("test-model")
        .instructions("Return the delegated JSON payload.")
        .responder(responder)
        .objectMapper(ResponsesApiObjectMapper.create())
        .build();
  }

  private Interactable.Structured<ActivityOutput> createStructuredActivityAgent(String name) {
    return Agent.builder()
        .name(name)
        .model("test-model")
        .instructions("Return the delegated JSON payload.")
        .responder(responder)
        .objectMapper(ResponsesApiObjectMapper.create())
        .structured(ActivityOutput.class)
        .build();
  }

  private static String escapeForJsonString(String text) {
    return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "");
  }

  private void enqueueMessageResponse(String text) {
    String json =
        """
        {
          "id": "resp_001",
          "object": "response",
          "created_at": 1234567890,
          "status": "completed",
          "model": "test-model",
          "output": [
            {
              "type": "message",
              "id": "msg_001",
              "role": "assistant",
              "content": [
                {
                  "type": "output_text",
                  "text": "%s"
                }
              ]
            }
          ],
          "usage": {
            "input_tokens": 10,
            "output_tokens": 5,
            "total_tokens": 15
          }
        }
        """
            .formatted(escapeForJsonString(text));

    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setBody(json)
            .addHeader("Content-Type", "application/json"));
  }

  private void enqueueHandoffResponse(String handoffName, String arguments) {
    String json =
        """
        {
          "id": "resp_001",
          "object": "response",
          "created_at": 1234567890,
          "status": "completed",
          "model": "test-model",
          "output": [
            {
              "type": "function_call",
              "id": "fc_001",
              "call_id": "call_123",
              "name": "%s",
              "arguments": "%s"
            }
          ],
          "usage": {
            "input_tokens": 10,
            "output_tokens": 5,
            "total_tokens": 15
          }
        }
        """
            .formatted(handoffName, escapeForJsonString(arguments));

    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setBody(json)
            .addHeader("Content-Type", "application/json"));
  }

  private String takeRequestBody() throws Exception {
    RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
    assertNotNull(request);
    return request.getBody().readUtf8();
  }
}
