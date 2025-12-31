package com.paragon.agents;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for AgentResult factory methods and accessors. */
@DisplayName("AgentResult")
class AgentResultTest {

  // ═══════════════════════════════════════════════════════════════════════════
  // FACTORY METHODS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Factory Methods")
  class FactoryMethods {

    @Test
    @DisplayName("error creates error result")
    void errorCreatesErrorResult() {
      AgentContext context = AgentContext.create();
      RuntimeException ex = new RuntimeException("Test error");

      AgentResult result = AgentResult.error(ex, context, 1);

      assertTrue(result.isError());
      assertFalse(result.isSuccess());
      assertEquals(ex, result.error());
      assertEquals(1, result.turnsUsed());
    }

    @Test
    @DisplayName("guardrailFailed creates guardrail error result")
    void guardrailFailedCreatesGuardrailError() {
      AgentContext context = AgentContext.create();

      AgentResult result = AgentResult.guardrailFailed("Invalid input", context);

      assertTrue(result.isError());
      assertNotNull(result.error());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // STATUS CHECKS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Status Checks")
  class StatusChecks {

    @Test
    @DisplayName("isError returns true for error result")
    void isErrorReturnsTrueForError() {
      AgentContext context = AgentContext.create();
      AgentResult errorResult = AgentResult.error(new RuntimeException(), context, 1);

      assertTrue(errorResult.isError());
      assertFalse(errorResult.isSuccess());
    }

    @Test
    @DisplayName("isSuccess returns false for error result")
    void isSuccessReturnsFalseForError() {
      AgentContext context = AgentContext.create();
      AgentResult errorResult = AgentResult.error(new RuntimeException(), context, 1);

      assertFalse(errorResult.isSuccess());
    }

    @Test
    @DisplayName("isHandoff returns false for error result")
    void isHandoffReturnsFalseForError() {
      AgentContext context = AgentContext.create();
      AgentResult errorResult = AgentResult.error(new RuntimeException(), context, 1);

      assertFalse(errorResult.isHandoff());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ACCESSORS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Accessors")
  class Accessors {

    @Test
    @DisplayName("error returns null for guardrail failure")
    void errorReturnsExceptionForGuardrail() {
      AgentContext context = AgentContext.create();
      AgentResult result = AgentResult.guardrailFailed("Test", context);

      assertNotNull(result.error());
    }

    @Test
    @DisplayName("pausedState returns null for error result")
    void pausedStateReturnsNullForError() {
      AgentContext context = AgentContext.create();
      AgentResult errorResult = AgentResult.error(new RuntimeException(), context, 1);

      assertNull(errorResult.pausedState());
    }

    @Test
    @DisplayName("handoffAgent returns null for error result")
    void handoffAgentReturnsNullForError() {
      AgentContext context = AgentContext.create();
      AgentResult errorResult = AgentResult.error(new RuntimeException(), context, 1);

      assertNull(errorResult.handoffAgent());
    }

    @Test
    @DisplayName("parsed returns null for error result")
    void parsedReturnsNullForError() {
      AgentContext context = AgentContext.create();
      AgentResult errorResult = AgentResult.error(new RuntimeException(), context, 1);

      assertNull(errorResult.parsed());
    }

    @Test
    @DisplayName("toolExecutions returns empty list for error")
    void toolExecutionsReturnsEmptyForError() {
      AgentContext context = AgentContext.create();
      AgentResult errorResult = AgentResult.error(new RuntimeException(), context, 1);

      assertNotNull(errorResult.toolExecutions());
      assertTrue(errorResult.toolExecutions().isEmpty());
    }

    @Test
    @DisplayName("turnsUsed returns correct value")
    void turnsUsedReturnsCorrectValue() {
      AgentContext context = AgentContext.create();
      AgentResult errorResult = AgentResult.error(new RuntimeException(), context, 5);

      assertEquals(5, errorResult.turnsUsed());
    }

    @Test
    @DisplayName("history returns list")
    void historyReturnsList() {
      AgentContext context = AgentContext.create();
      AgentResult errorResult = AgentResult.error(new RuntimeException(), context, 1);

      assertNotNull(errorResult.history());
    }

    @Test
    @DisplayName("finalResponse returns null for error")
    void finalResponseReturnsNullForError() {
      AgentContext context = AgentContext.create();
      AgentResult errorResult = AgentResult.error(new RuntimeException(), context, 1);

      assertNull(errorResult.finalResponse());
    }

    @Test
    @DisplayName("output returns null for error")
    void outputReturnsNullForError() {
      AgentContext context = AgentContext.create();
      AgentResult errorResult = AgentResult.error(new RuntimeException(), context, 1);

      assertNull(errorResult.output());
    }
  }
}
