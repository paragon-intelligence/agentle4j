package com.paragon.agents;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.responses.Responder;
import com.paragon.responses.spec.FunctionToolCallOutput;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for agent DTOs: ToolExecution, GuardrailResult, MemoryEntry. */
@DisplayName("Agent DTO Tests")
class AgentDtoTest {

  // ═══════════════════════════════════════════════════════════════════════════
  // TOOL EXECUTION
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("ToolExecution")
  class ToolExecutionTests {

    @Test
    @DisplayName("creates with valid parameters")
    void createsWithValidParams() {
      var output = FunctionToolCallOutput.success("result");
      var exec =
          new ToolExecution(
              "my_tool", "call_123", "{\"arg\": \"value\"}", output, Duration.ofMillis(150));

      assertEquals("my_tool", exec.toolName());
      assertEquals("call_123", exec.callId());
      assertEquals("{\"arg\": \"value\"}", exec.arguments());
      assertEquals(output, exec.output());
      assertEquals(Duration.ofMillis(150), exec.duration());
    }

    @Test
    @DisplayName("isSuccess returns true when output status is COMPLETED")
    void isSuccessWhenCompleted() {
      var output = FunctionToolCallOutput.success("result");
      var exec = new ToolExecution("tool", "id", "{}", output, Duration.ofMillis(10));

      assertTrue(exec.isSuccess());
    }

    @Test
    @DisplayName("isSuccess returns false when output status is IN_PROGRESS")
    void isNotSuccessWhenInProgress() {
      var output = FunctionToolCallOutput.inProgress("still working");
      var exec = new ToolExecution("tool", "id", "{}", output, Duration.ofMillis(10));

      assertFalse(exec.isSuccess());
    }

    @Test
    @DisplayName("isSuccess returns false when output is error")
    void isNotSuccessWhenError() {
      var output = FunctionToolCallOutput.error("failed");
      var exec = new ToolExecution("tool", "id", "{}", output, Duration.ofMillis(10));

      assertFalse(exec.isSuccess());
    }

    @Test
    @DisplayName("throws on null toolName")
    void throwsOnNullToolName() {
      var output = FunctionToolCallOutput.success("result");
      assertThrows(
          IllegalArgumentException.class,
          () -> new ToolExecution(null, "id", "{}", output, Duration.ofMillis(10)));
    }

    @Test
    @DisplayName("throws on blank toolName")
    void throwsOnBlankToolName() {
      var output = FunctionToolCallOutput.success("result");
      assertThrows(
          IllegalArgumentException.class,
          () -> new ToolExecution("  ", "id", "{}", output, Duration.ofMillis(10)));
    }

    @Test
    @DisplayName("throws on null callId")
    void throwsOnNullCallId() {
      var output = FunctionToolCallOutput.success("result");
      assertThrows(
          IllegalArgumentException.class,
          () -> new ToolExecution("tool", null, "{}", output, Duration.ofMillis(10)));
    }

    @Test
    @DisplayName("throws on null arguments")
    void throwsOnNullArguments() {
      var output = FunctionToolCallOutput.success("result");
      assertThrows(
          IllegalArgumentException.class,
          () -> new ToolExecution("tool", "id", null, output, Duration.ofMillis(10)));
    }

    @Test
    @DisplayName("throws on null output")
    void throwsOnNullOutput() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new ToolExecution("tool", "id", "{}", null, Duration.ofMillis(10)));
    }

    @Test
    @DisplayName("throws on null duration")
    void throwsOnNullDuration() {
      var output = FunctionToolCallOutput.success("result");
      assertThrows(
          IllegalArgumentException.class,
          () -> new ToolExecution("tool", "id", "{}", output, null));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // GUARDRAIL RESULT
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("GuardrailResult")
  class GuardrailResultTests {

    @Test
    @DisplayName("passed() returns Passed instance")
    void passedReturnsPassed() {
      var result = GuardrailResult.passed();
      assertInstanceOf(GuardrailResult.Passed.class, result);
    }

    @Test
    @DisplayName("passed() returns same singleton instance")
    void passedReturnsSingleton() {
      var result1 = GuardrailResult.passed();
      var result2 = GuardrailResult.passed();
      assertSame(result1, result2);
    }

    @Test
    @DisplayName("isPassed returns true for Passed")
    void isPassedTrue() {
      var result = GuardrailResult.passed();
      assertTrue(result.isPassed());
      assertFalse(result.isFailed());
    }

    @Test
    @DisplayName("failed() returns Failed instance with reason")
    void failedReturnsWithReason() {
      var result = GuardrailResult.failed("Content is inappropriate");
      assertInstanceOf(GuardrailResult.Failed.class, result);
      assertEquals("Content is inappropriate", ((GuardrailResult.Failed) result).reason());
    }

    @Test
    @DisplayName("isFailed returns true for Failed")
    void isFailedTrue() {
      var result = GuardrailResult.failed("reason");
      assertTrue(result.isFailed());
      assertFalse(result.isPassed());
    }

    @Test
    @DisplayName("Failed throws on null reason")
    void failedThrowsOnNullReason() {
      assertThrows(IllegalArgumentException.class, () -> GuardrailResult.failed(null));
    }

    @Test
    @DisplayName("Failed throws on blank reason")
    void failedThrowsOnBlankReason() {
      assertThrows(IllegalArgumentException.class, () -> GuardrailResult.failed("   "));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // MEMORY ENTRY
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("MemoryEntry")
  class MemoryEntryTests {

    @Test
    @DisplayName("of(content) creates entry with auto-generated id")
    void ofCreatesWithAutoId() {
      var before = Instant.now();
      var entry = MemoryEntry.of("User prefers dark mode");
      var after = Instant.now();

      assertNotNull(entry.id());
      assertFalse(entry.id().isBlank());
      assertEquals("User prefers dark mode", entry.content());
      assertTrue(entry.metadata().isEmpty());
      assertTrue(entry.timestamp().isAfter(before.minusSeconds(1)));
      assertTrue(entry.timestamp().isBefore(after.plusSeconds(1)));
    }

    @Test
    @DisplayName("of(content, metadata) creates entry with metadata")
    void ofCreatesWithMetadata() {
      var metadata = Map.<String, Object>of("source", "chat", "priority", 1);
      var entry = MemoryEntry.of("Important fact", metadata);

      assertEquals("Important fact", entry.content());
      assertEquals("chat", entry.metadata().get("source"));
      assertEquals(1, entry.metadata().get("priority"));
    }

    @Test
    @DisplayName("withId creates entry with specific id")
    void withIdCreatesWithSpecificId() {
      var entry = MemoryEntry.withId("custom-id-123", "Some content");

      assertEquals("custom-id-123", entry.id());
      assertEquals("Some content", entry.content());
      assertTrue(entry.metadata().isEmpty());
    }

    @Test
    @DisplayName("toPromptFormat returns formatted string")
    void toPromptFormatReturnsFormatted() {
      var entry = MemoryEntry.of("Test memory");
      var formatted = entry.toPromptFormat();

      assertTrue(formatted.contains("Test memory"));
      assertTrue(formatted.startsWith("["));
      assertTrue(formatted.contains("]"));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // HANDOFF
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Handoff")
  class HandoffTests {

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

    private Agent createTestAgent(String name, String instructions) {
      return Agent.builder()
          .name(name)
          .instructions(instructions)
          .model("test-model")
          .responder(responder)
          .build();
    }

    @Test
    @DisplayName("to() creates builder with target agent")
    void toCreatesBuilder() throws Exception {
      var targetAgent = createTestAgent("Support", "Handle support issues");
      var handoff = Handoff.to(targetAgent).build();

      assertNotNull(handoff);
      assertEquals(targetAgent, handoff.targetAgent());
    }

    @Test
    @DisplayName("default name is transfer_to_[agent_name]")
    @SuppressWarnings("unused")
    void defaultNameFormat() throws Exception {
      var targetAgent = createTestAgent("CustomerSupport", "Handle support");
      var handoff = Handoff.to(targetAgent).build();

      assertEquals("transfer_to_customer_support", handoff.name());
    }

    @Test
    @DisplayName("default description includes agent instructions")
    void defaultDescriptionIncludesInstructions() throws Exception {
      var targetAgent = createTestAgent("Sales", "Handle sales inquiries");
      var handoff = Handoff.to(targetAgent).build();

      assertTrue(handoff.description().contains("Sales"));
      assertTrue(handoff.description().contains("Handle sales inquiries"));
    }

    @Test
    @DisplayName("withName sets custom name")
    void withNameSetsCustomName() throws Exception {
      var targetAgent = createTestAgent("Support", "Handle support");
      var handoff = Handoff.to(targetAgent).withName("escalate_issue").build();

      assertEquals("escalate_issue", handoff.name());
    }

    @Test
    @DisplayName("withDescription sets custom description")
    void withDescriptionSetsCustom() throws Exception {
      var targetAgent = createTestAgent("Support", "Handle support");
      var handoff =
          Handoff.to(targetAgent).withDescription("Escalate to support for complex issues").build();

      assertEquals("Escalate to support for complex issues", handoff.description());
    }

    @Test
    @DisplayName("asTool returns FunctionTool")
    void asToolReturnsFunctionTool() throws Exception {
      var targetAgent = createTestAgent("Support", "Handle support");
      var handoff = Handoff.to(targetAgent).build();
      var tool = handoff.asTool();

      assertNotNull(tool);
      assertEquals(handoff.name(), tool.getName());
      assertEquals(handoff.description(), tool.getDescription());
    }

    @Test
    @DisplayName("throws on null target agent")
    void throwsOnNullTargetAgent() {
      assertThrows(NullPointerException.class, () -> Handoff.to(null));
    }
  }
}
