package com.paragon.agents;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.paragon.responses.Responder;

import okhttp3.mockwebserver.MockWebServer;

/**
 * Comprehensive tests for Handoff.
 *
 * <p>Tests cover:
 * - Builder pattern
 * - Agent reference
 * - Description/name handling
 */
@DisplayName("Handoff")
class HandoffTest {

  private MockWebServer mockWebServer;
  private Responder responder;

  @BeforeEach
  void setUp() throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    responder = Responder.builder()
        .baseUrl(mockWebServer.url("/v1/responses"))
        .apiKey("test-key")
        .build();
  }

  void tearDown() throws Exception {
    mockWebServer.shutdown();
  }

  @Nested
  @DisplayName("Builder Pattern")
  class BuilderPattern {

    @Test
    @DisplayName("to() creates builder targeting agent")
    void to_createsBuilderTargetingAgent() {
      Agent target = createTestAgent("BillingAgent");

      Handoff.Builder builder = Handoff.to(target);

      assertNotNull(builder);
    }

    @Test
    @DisplayName("build() creates handoff with defaults")
    void build_createsHandoffWithDefaults() {
      Agent target = createTestAgent("BillingAgent");

      Handoff handoff = Handoff.to(target).build();

      assertNotNull(handoff);
      assertEquals(target, handoff.targetAgent());
    }

    @Test
    @DisplayName("withDescription() sets description")
    void withDescription_setsDescription() {
      Agent target = createTestAgent("Target");

      Handoff handoff = Handoff.to(target)
          .withDescription("when user asks about billing")
          .build();

      assertEquals("when user asks about billing", handoff.description());
    }

    @Test
    @DisplayName("withName() sets custom tool name")
    void withName_setsCustomToolName() {
      Agent target = createTestAgent("Target");

      Handoff handoff = Handoff.to(target)
          .withName("custom_transfer")
          .build();

      assertEquals("custom_transfer", handoff.name());
    }

    @Test
    @DisplayName("to() throws when target agent is null")
    void to_throwsWhenTargetNull() {
      assertThrows(NullPointerException.class, () -> Handoff.to(null));
    }
  }

  @Nested
  @DisplayName("Handoff Properties")
  class HandoffProperties {

    @Test
    @DisplayName("targetAgent() returns the target agent")
    void targetAgent_returnsTarget() {
      Agent target = createTestAgent("Target");
      Handoff handoff = Handoff.to(target).build();

      assertSame(target, handoff.targetAgent());
    }

    @Test
    @DisplayName("description() returns the description")
    void description_returnsDescription() {
      Agent target = createTestAgent("Target");
      Handoff handoff = Handoff.to(target)
          .withDescription("billing issues")
          .build();

      assertEquals("billing issues", handoff.description());
    }

    @Test
    @DisplayName("name() returns default snake_case tool name")
    void name_returnsDefaultSnakeCaseName() {
      Agent target = createTestAgent("BillingSupport");
      Handoff handoff = Handoff.to(target).build();

      assertEquals("transfer_to_billing_support", handoff.name());
    }

    @Test
    @DisplayName("asTool() returns FunctionTool")
    void asTool_returnsFunctionTool() {
      Agent target = createTestAgent("Target");
      Handoff handoff = Handoff.to(target).build();

      var tool = handoff.asTool();

      assertNotNull(tool);
    }
  }

  @Nested
  @DisplayName("Default Values")
  class DefaultValues {

    @Test
    @DisplayName("default description includes agent name and instructions")
    void defaultDescription_includesAgentInfo() {
      Agent target = createTestAgent("SupportAgent");
      Handoff handoff = Handoff.to(target).build();

      assertTrue(handoff.description().contains("SupportAgent"));
    }

    @Test
    @DisplayName("default name follows transfer_to_[agent_name] pattern")
    void defaultName_followsPattern() {
      Agent target = createTestAgent("BillingAgent");
      Handoff handoff = Handoff.to(target).build();

      assertTrue(handoff.name().startsWith("transfer_to_"));
    }
  }

  // Helper methods

  private Agent createTestAgent(String name) {
    return Agent.builder()
        .name(name)
        .model("test-model")
        .instructions("Test instructions")
        .responder(responder)
        .build();
  }
}
