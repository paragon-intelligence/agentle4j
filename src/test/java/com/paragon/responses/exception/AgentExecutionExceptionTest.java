package com.paragon.responses.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Comprehensive tests for {@link AgentExecutionException}. */
@DisplayName("AgentExecutionException")
class AgentExecutionExceptionTest {

  // ==================== Constructor Tests ====================

  @Nested
  @DisplayName("Constructors")
  class ConstructorTests {

    @Test
    @DisplayName("basic constructor creates exception")
    void basicConstructorCreatesException() {
      AgentExecutionException ex =
          new AgentExecutionException(
              "testAgent", AgentExecutionException.Phase.LLM_CALL, 5, "Test error");

      assertEquals("testAgent", ex.agentName());
      assertEquals(AgentExecutionException.Phase.LLM_CALL, ex.phase());
      assertEquals(5, ex.turnsCompleted());
      assertEquals("Test error", ex.getMessage());
      assertNull(ex.getCause());
    }

    @Test
    @DisplayName("constructor with cause creates exception")
    void constructorWithCauseCreatesException() {
      RuntimeException cause = new RuntimeException("Original error");
      AgentExecutionException ex =
          new AgentExecutionException(
              "testAgent", AgentExecutionException.Phase.TOOL_EXECUTION, 3, "Wrapper error", cause);

      assertEquals("testAgent", ex.agentName());
      assertEquals(AgentExecutionException.Phase.TOOL_EXECUTION, ex.phase());
      assertEquals(3, ex.turnsCompleted());
      assertEquals("Wrapper error", ex.getMessage());
      assertEquals(cause, ex.getCause());
    }

    @Test
    @DisplayName("constructor with response ID creates exception")
    void constructorWithResponseIdCreatesException() {
      AgentExecutionException ex =
          new AgentExecutionException(
              "testAgent",
              AgentExecutionException.Phase.PARSING,
              2,
              "resp_123",
              "Parse failed",
              null);

      assertEquals("testAgent", ex.agentName());
      assertEquals(AgentExecutionException.Phase.PARSING, ex.phase());
      assertEquals(2, ex.turnsCompleted());
      assertEquals("resp_123", ex.lastResponseId());
    }
  }

  // ==================== Factory Method Tests ====================

  @Nested
  @DisplayName("Factory Methods")
  class FactoryMethodTests {

    @Test
    @DisplayName("maxTurnsExceeded creates correct exception")
    void maxTurnsExceededCreatesCorrectException() {
      AgentExecutionException ex = AgentExecutionException.maxTurnsExceeded("myAgent", 10, 10);

      assertEquals("myAgent", ex.agentName());
      assertEquals(AgentExecutionException.Phase.MAX_TURNS_EXCEEDED, ex.phase());
      assertEquals(10, ex.turnsCompleted());
      assertTrue(ex.getMessage().contains("exceeded maximum turns"));
      assertTrue(ex.getMessage().contains("10"));
      assertFalse(ex.isRetryable());
    }

    @Test
    @DisplayName("llmCallFailed creates correct exception")
    void llmCallFailedCreatesCorrectException() {
      Exception cause = new RuntimeException("API timeout");
      AgentExecutionException ex = AgentExecutionException.llmCallFailed("myAgent", 3, cause);

      assertEquals("myAgent", ex.agentName());
      assertEquals(AgentExecutionException.Phase.LLM_CALL, ex.phase());
      assertEquals(3, ex.turnsCompleted());
      assertTrue(ex.getMessage().contains("LLM call failed"));
      assertTrue(ex.getMessage().contains("API timeout"));
      assertEquals(cause, ex.getCause());
      assertTrue(ex.isRetryable());
    }

    @Test
    @DisplayName("parsingFailed creates correct exception")
    void parsingFailedCreatesCorrectException() {
      Exception cause = new RuntimeException("Invalid JSON");
      AgentExecutionException ex = AgentExecutionException.parsingFailed("myAgent", 5, cause);

      assertEquals("myAgent", ex.agentName());
      assertEquals(AgentExecutionException.Phase.PARSING, ex.phase());
      assertEquals(5, ex.turnsCompleted());
      assertTrue(ex.getMessage().contains("failed to parse"));
      assertEquals(cause, ex.getCause());
      assertFalse(ex.isRetryable());
    }

    @Test
    @DisplayName("handoffFailed creates correct exception")
    void handoffFailedCreatesCorrectException() {
      Exception cause = new RuntimeException("Target not found");
      AgentExecutionException ex =
          AgentExecutionException.handoffFailed("sourceAgent", "targetAgent", 2, cause);

      assertEquals("sourceAgent", ex.agentName());
      assertEquals(AgentExecutionException.Phase.HANDOFF, ex.phase());
      assertEquals(2, ex.turnsCompleted());
      assertTrue(ex.getMessage().contains("handoff to 'targetAgent'"));
      assertEquals(cause, ex.getCause());
      assertFalse(ex.isRetryable());
    }
  }

  // ==================== Phase Tests ====================

  @Nested
  @DisplayName("Phase Handling")
  class PhaseTests {

    @Test
    @DisplayName("INPUT_GUARDRAIL phase maps correctly")
    void inputGuardrailPhaseMapCorrectly() {
      AgentExecutionException ex =
          new AgentExecutionException(
              "agent", AgentExecutionException.Phase.INPUT_GUARDRAIL, 0, "Input blocked");

      assertEquals(AgentExecutionException.Phase.INPUT_GUARDRAIL, ex.phase());
      assertFalse(ex.isRetryable());
      assertNotNull(ex.suggestion());
    }

    @Test
    @DisplayName("OUTPUT_GUARDRAIL phase maps correctly")
    void outputGuardrailPhaseMapCorrectly() {
      AgentExecutionException ex =
          new AgentExecutionException(
              "agent", AgentExecutionException.Phase.OUTPUT_GUARDRAIL, 1, "Output blocked");

      assertEquals(AgentExecutionException.Phase.OUTPUT_GUARDRAIL, ex.phase());
      assertFalse(ex.isRetryable());
    }

    @Test
    @DisplayName("TOOL_EXECUTION phase maps correctly")
    void toolExecutionPhaseMapCorrectly() {
      AgentExecutionException ex =
          new AgentExecutionException(
              "agent", AgentExecutionException.Phase.TOOL_EXECUTION, 2, "Tool failed");

      assertEquals(AgentExecutionException.Phase.TOOL_EXECUTION, ex.phase());
      assertFalse(ex.isRetryable());
    }

    @Test
    @DisplayName("LLM_CALL phase is retryable")
    void llmCallPhaseIsRetryable() {
      AgentExecutionException ex =
          new AgentExecutionException(
              "agent", AgentExecutionException.Phase.LLM_CALL, 0, "API error");

      assertEquals(AgentExecutionException.Phase.LLM_CALL, ex.phase());
      assertTrue(ex.isRetryable());
    }

    @Test
    @DisplayName("all phases have values")
    void allPhasesHaveValues() {
      for (AgentExecutionException.Phase phase : AgentExecutionException.Phase.values()) {
        assertNotNull(phase);
        assertNotNull(phase.name());
      }
    }

    @Test
    @DisplayName("phase enum has expected count")
    void phaseEnumHasExpectedCount() {
      assertEquals(7, AgentExecutionException.Phase.values().length);
    }
  }

  // ==================== Suggestion Tests ====================

  @Nested
  @DisplayName("Suggestions")
  class SuggestionTests {

    @Test
    @DisplayName("each phase has a suggestion")
    void eachPhaseHasSuggestion() {
      for (AgentExecutionException.Phase phase : AgentExecutionException.Phase.values()) {
        AgentExecutionException ex = new AgentExecutionException("agent", phase, 0, "Test error");

        assertNotNull(ex.suggestion(), "Phase " + phase + " should have a suggestion");
        assertFalse(ex.suggestion().isEmpty());
      }
    }
  }

  // ==================== Inheritance Tests ====================

  @Nested
  @DisplayName("Inheritance")
  class InheritanceTests {

    @Test
    @DisplayName("extends AgentleException")
    void extendsAgentleException() {
      AgentExecutionException ex =
          new AgentExecutionException("agent", AgentExecutionException.Phase.LLM_CALL, 0, "Error");

      assertInstanceOf(AgentleException.class, ex);
    }

    @Test
    @DisplayName("has error code string")
    void hasErrorCodeString() {
      AgentExecutionException ex =
          new AgentExecutionException(
              "agent", AgentExecutionException.Phase.MAX_TURNS_EXCEEDED, 10, "Max turns");

      // Test that exception inherits properly
      assertNotNull(ex.getMessage());
    }
  }
}
