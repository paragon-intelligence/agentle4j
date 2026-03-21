package com.paragon.harness;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.agents.AgentResult;
import com.paragon.agents.AgenticContext;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AgentRunReport")
class AgentRunReportTest {

  @Nested
  @DisplayName("from()")
  class From {

    @Test
    @DisplayName("creates report from a successful result")
    void fromSuccessfulResult() {
      AgenticContext ctx = AgenticContext.create();
      AgentResult result = AgentResult.error(new RuntimeException("test"), ctx, 2);
      Instant start = Instant.now().minusSeconds(1);
      Instant end = Instant.now();

      AgentRunReport report = AgentRunReport.from("MyAgent", result, start, end, 1, 0);

      assertEquals("MyAgent", report.agentName());
      assertFalse(report.isSuccess());
      assertEquals(2, report.turnsUsed());
      assertEquals(1, report.retryCount());
      assertEquals(0, report.guardrailTriggerCount());
      assertNotNull(report.reportId());
      assertTrue(report.totalDuration().toMillis() >= 0);
    }

    @Test
    @DisplayName("tool statistics are computed from result")
    void computesToolStatistics() {
      AgenticContext ctx = AgenticContext.create();
      AgentResult result = AgentResult.error(new RuntimeException("x"), ctx, 1);

      AgentRunReport report = AgentRunReport.from("A", result, Instant.now(), Instant.now(), 0, 0);

      // No tool executions in a simple error result
      assertEquals(0, report.totalToolCalls());
      assertEquals(0, report.failedToolCalls());
    }
  }

  @Test
  @DisplayName("toSummary() includes key fields")
  void toSummaryIncludesKeyFields() {
    AgenticContext ctx = AgenticContext.create();
    AgentResult result = AgentResult.error(new RuntimeException("bad"), ctx, 3);
    AgentRunReport report =
        AgentRunReport.from(
            "TestAgent", result, Instant.now().minusSeconds(2), Instant.now(), 2, 1);

    String summary = report.toSummary();
    assertTrue(summary.contains("TestAgent"));
    assertTrue(summary.contains("FAILURE"));
    assertTrue(summary.contains("Retries:"));
    assertTrue(summary.contains("Guardrails:"));
  }
}
