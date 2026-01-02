package com.paragon.agents;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.responses.Responder;
import com.paragon.responses.spec.*;
import java.util.List;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;

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
    @DisplayName("success creates success result")
    void successCreatesSuccessResult() {
      AgentContext context = AgentContext.create();
      Response response = createMinimalResponse();
      List<ToolExecution> executions = List.of();

      AgentResult result = AgentResult.success("Output text", response, context, executions, 2);

      assertTrue(result.isSuccess());
      assertFalse(result.isError());
      assertFalse(result.isHandoff());
      assertEquals("Output text", result.output());
      assertEquals(response, result.finalResponse());
      assertEquals(2, result.turnsUsed());
    }

    @Test
    @DisplayName("successWithParsed creates success result with parsed object")
    void successWithParsedCreatesSuccessResult() {
      AgentContext context = AgentContext.create();
      Response response = createMinimalResponse();
      List<ToolExecution> executions = List.of();
      TestPerson parsed = new TestPerson("John", 30);

      AgentResult result = AgentResult.successWithParsed(
          "{\"name\":\"John\",\"age\":30}", parsed, response, context, executions, 1);

      assertTrue(result.isSuccess());
      assertEquals("{\"name\":\"John\",\"age\":30}", result.output());
      assertNotNull(result.parsed());
      assertEquals("John", ((TestPerson) result.parsed()).name());
    }

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
      assertTrue(result.error().getMessage().contains("Invalid input"));
    }

    @Test
    @DisplayName("paused creates paused result")
    void pausedCreatesPausedResult() {
      AgentContext context = AgentContext.create();
      FunctionToolCall pendingCall = new FunctionToolCall("{}", "call-123", "test_tool", null, null);
      AgentRunState state = AgentRunState.pendingApproval(
          "TestAgent", context, pendingCall, null, List.of(), 1);

      AgentResult result = AgentResult.paused(state, context);

      assertTrue(result.isPaused());
      // Note: isPaused also returns true for isSuccess since error is null
      assertTrue(result.isSuccess());
      assertFalse(result.isError());
      assertNotNull(result.pausedState());
      assertEquals(state, result.pausedState());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // STATUS CHECKS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Status Checks")
  class StatusChecks {

    @Test
    @DisplayName("isSuccess returns true for success result")
    void isSuccessReturnsTrueForSuccess() {
      AgentContext context = AgentContext.create();
      Response response = createMinimalResponse();

      AgentResult result = AgentResult.success("output", response, context, List.of(), 1);

      assertTrue(result.isSuccess());
      assertFalse(result.isError());
      assertFalse(result.isHandoff());
      assertFalse(result.isPaused());
    }

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

    @Test
    @DisplayName("isPaused returns true for paused result")
    void isPausedReturnsTrueForPaused() {
      AgentContext context = AgentContext.create();
      FunctionToolCall call = new FunctionToolCall("{}", "call-1", "tool", null, null);
      AgentRunState state = AgentRunState.pendingApproval("Agent", context, call, null, List.of(), 1);

      AgentResult result = AgentResult.paused(state, context);

      assertTrue(result.isPaused());
      // Note: isPaused also returns true for isSuccess since error is null
      assertTrue(result.isSuccess());
      assertFalse(result.isError());
    }

    @Test
    @DisplayName("isPaused returns false for success result")
    void isPausedReturnsFalseForSuccess() {
      AgentContext context = AgentContext.create();
      Response response = createMinimalResponse();

      AgentResult result = AgentResult.success("output", response, context, List.of(), 1);

      assertFalse(result.isPaused());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ACCESSORS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Accessors")
  class Accessors {

    @Test
    @DisplayName("output returns text for success")
    void outputReturnsTextForSuccess() {
      AgentContext context = AgentContext.create();
      Response response = createMinimalResponse();

      AgentResult result = AgentResult.success("Hello world", response, context, List.of(), 1);

      assertEquals("Hello world", result.output());
    }

    @Test
    @DisplayName("finalResponse returns response for success")
    void finalResponseReturnsResponseForSuccess() {
      AgentContext context = AgentContext.create();
      Response response = createMinimalResponse();

      AgentResult result = AgentResult.success("output", response, context, List.of(), 1);

      assertNotNull(result.finalResponse());
      assertEquals(response, result.finalResponse());
    }

    @Test
    @DisplayName("toolExecutions returns executions")
    void toolExecutionsReturnsExecutions() {
      AgentContext context = AgentContext.create();
      Response response = createMinimalResponse();
      ToolExecution exec = new ToolExecution("tool", "call-1", "{}", 
          FunctionToolCallOutput.success("output"), java.time.Duration.ofMillis(100));
      List<ToolExecution> executions = List.of(exec);

      AgentResult result = AgentResult.success("output", response, context, executions, 1);

      assertEquals(1, result.toolExecutions().size());
      assertEquals("tool", result.toolExecutions().get(0).toolName());
    }

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
    @DisplayName("pausedState returns state for paused result")
    void pausedStateReturnsStateForPaused() {
      AgentContext context = AgentContext.create();
      FunctionToolCall call = new FunctionToolCall("{}", "call-1", "tool", null, null);
      AgentRunState state = AgentRunState.pendingApproval("Agent", context, call, null, List.of(), 1);

      AgentResult result = AgentResult.paused(state, context);

      assertNotNull(result.pausedState());
      assertEquals(state, result.pausedState());
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
    @DisplayName("parsed returns object for successWithParsed")
    void parsedReturnsObjectForSuccessWithParsed() {
      AgentContext context = AgentContext.create();
      Response response = createMinimalResponse();
      TestPerson person = new TestPerson("Jane", 25);

      AgentResult result = AgentResult.successWithParsed("{}", person, response, context, List.of(), 1);

      assertNotNull(result.parsed());
      TestPerson parsed = (TestPerson) result.parsed();
      assertEquals("Jane", parsed.name());
      assertEquals(25, parsed.age());
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
    @DisplayName("history includes context messages for success")
    void historyIncludesContextMessages() {
      AgentContext context = AgentContext.create();
      context.addInput(Message.user("Hello"));
      Response response = createMinimalResponse();

      AgentResult result = AgentResult.success("output", response, context, List.of(), 1);

      assertFalse(result.history().isEmpty());
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

  // ═══════════════════════════════════════════════════════════════════════════
  // HELPERS
  // ═══════════════════════════════════════════════════════════════════════════

  private Response createMinimalResponse() {
    return new Response(
        null, null, System.currentTimeMillis() / 1000, null, "resp_123", null, null,
        null, null, null, "gpt-4o", ResponseObject.RESPONSE,
        List.of(new OutputMessage<Void>(List.of(Text.valueOf("Test")), "msg_1", 
            InputMessageStatus.COMPLETED, null)),
        null, null, null, null, null, null, null,
        ResponseGenerationStatus.COMPLETED, null, null, null, null, null, null, null);
  }

  public record TestPerson(String name, int age) {}
}

