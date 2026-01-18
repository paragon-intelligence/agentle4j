package com.paragon.skills;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.agents.AgentResult;
import com.paragon.responses.Responder;
import com.paragon.responses.spec.FunctionToolCallOutput;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the SkillTool class.
 */
@DisplayName("SkillTool")
class SkillToolTest {

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

  @Nested
  @DisplayName("Execution")
  class Execution {

    @Test
    @DisplayName("call() creates sub-agent and returns output")
    void call_createsSubAgentAndReturnsOutput() {
      Skill skill = Skill.of("test-skill", "Description", "Instructions");
      SkillTool tool = new SkillTool(skill, responder, "test-model", SkillTool.Config.defaults());

      // Corrected Mock response structure based on AgentTest.java
      String json = """
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
                    "text": "Skill executed successfully"
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
          """;

      mockWebServer.enqueue(
          new MockResponse()
              .setResponseCode(200)
              .setBody(json)
              .addHeader("Content-Type", "application/json"));

      SkillTool.SkillParams params = new SkillTool.SkillParams("Execute task");
      FunctionToolCallOutput output = tool.call(params);

      assertNotNull(output);
      assertTrue(output.toString().contains("Skill executed successfully"));
    }
  }

  @Nested
  @DisplayName("Configuration")
  class Configuration {

    @Test
    @DisplayName("defaults() uses secure defaults")
    void defaults_usesSecureDefaults() {
      SkillTool.Config config = SkillTool.Config.defaults();

      assertFalse(config.shareState());
      assertFalse(config.shareHistory());
      assertEquals(5, config.maxTurns());
    }

    @Test
    @DisplayName("builder allows custom configuration")
    void builder_allowsCustomConfiguration() {
      SkillTool.Config config = SkillTool.Config.builder()
          .shareState(true)
          .shareHistory(true)
          .maxTurns(5)
          .build();

      assertTrue(config.shareState());
      assertTrue(config.shareHistory());
      assertEquals(5, config.maxTurns());
    }
  }
}
