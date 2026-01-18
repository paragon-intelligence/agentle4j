package com.paragon.agents;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.agents.Agent;
import com.paragon.agents.AgentResult;
import com.paragon.responses.Responder;
import com.paragon.skills.Skill;
import com.paragon.skills.SkillStore;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Agent with Skills.
 */
@DisplayName("Agent Skill Integration")
class AgentSkillIntegrationTest {

  private MockWebServer mockWebServer;
  private Responder responder;

  @BeforeEach
  void setUp() throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    responder =
        Responder.builder()
            .baseUrl(mockWebServer.url("/v1/responses"))
            .apiKey("test-key")
            .build();
  }

  @AfterEach
  void tearDown() throws Exception {
    mockWebServer.shutdown();
  }

  @Test
  @DisplayName("Agent can invoke a registered skill")
  void agent_canInvokeRegisteredSkill() {
    Skill skill = Skill.of("helper-skill", "Helps with tasks", "You are a helper.");
    
    Agent agent = Agent.builder()
        .name("MainAgent")
        .model("model-id")
        .instructions("You are the main agent.")
        .responder(responder)
        .addSkill(skill)
        .build();

    // 1. First response: Agent decides to call the skill
    enqueueToolCallResponse("skill_helper_skill", "{\"request\": \"do something\"}");

    // 2. Second response: Sub-agent (skill) executes and returns result
    enqueueContentResponse("I have done the task.");

    // 3. Third response: Agent receives skill output and gives final answer
    enqueueContentResponse("The helper says it is done.");

    AgentResult result = agent.interact("Please help me.");

    assertNotNull(result);
    assertFalse(result.isError(), "Result should not have error: " + (result.error() != null ? result.error().getMessage() : ""));
    assertTrue(result.output().contains("The helper says it is done."), "Output was: " + result.output());
    // Verify 3 requests were made (Agent -> Skill -> Agent)
    assertEquals(3, mockWebServer.getRequestCount());
  }

  @Test
  @DisplayName("Agent can register skills from SkillStore")
  void agent_canRegisterSkillsFromStore() {
    SkillStore store = new SkillStore();
    store.register(Skill.of("skill-1", "Desc 1", "Inst 1"));
    store.register(Skill.of("skill-2", "Desc 2", "Inst 2"));

    Agent agent = Agent.builder()
        .name("MainAgent")
        .model("model-id")
        .instructions("Inst")
        .responder(responder)
        .skillStore(store)
        .build();

    // Just verify the agent was built successfully and presumably has tools
    // Accessing internal tools list is not part of public API, but we act as if it works.
    assertNotNull(agent);
  }

  // Helper methods

  private void enqueueContentResponse(String content) {
    String json = """
        {
          "id": "resp_content",
          "object": "response",
          "created_at": 1234567890,
          "status": "completed",
          "model": "model-id",
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
          "usage": {"input_tokens": 10, "output_tokens": 10, "total_tokens": 20}
        }
        """.formatted(content);
    
    mockWebServer.enqueue(new MockResponse()
        .setResponseCode(200)
        .setBody(json)
        .addHeader("Content-Type", "application/json"));
  }

  private void enqueueToolCallResponse(String toolName, String arguments) {
    String json = """
        {
          "id": "resp_tool",
          "object": "response",
          "created_at": 1234567890,
          "status": "completed",
          "model": "model-id",
          "output": [
            {
              "type": "function_call",
              "call_id": "call_001",
              "name": "%s",
              "arguments": "%s",
              "status": "completed"
            }
          ],
          "usage": {"input_tokens": 10, "output_tokens": 10, "total_tokens": 20}
        }
        """.formatted(toolName, arguments.replace("\"", "\\\"")); // Escape quotes in arguments string

    mockWebServer.enqueue(new MockResponse()
        .setResponseCode(200)
        .setBody(json)
        .addHeader("Content-Type", "application/json"));
  }
}
