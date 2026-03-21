package com.paragon.harness;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.agents.AgentResult;
import com.paragon.agents.AgenticContext;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("RunReportExporter")
class RunReportExporterTest {

  @TempDir Path tempDir;
  RunReportExporter exporter;

  @BeforeEach
  void setUp() {
    exporter = RunReportExporter.create(tempDir);
  }

  @Test
  @DisplayName("exports report to disk")
  void exportsReportToDisk() {
    AgenticContext ctx = AgenticContext.create();
    AgentResult result = AgentResult.error(new RuntimeException("oops"), ctx, 1);
    AgentRunReport report =
        AgentRunReport.from(
            "TestAgent", result, Instant.now().minusSeconds(1), Instant.now(), 0, 0);

    Path written = exporter.export(report);

    assertNotNull(written);
    assertTrue(written.toFile().exists());
    assertTrue(written.getFileName().toString().contains("TestAgent"));
    assertTrue(written.getFileName().toString().endsWith(".json"));
  }

  @Test
  @DisplayName("loadAll returns all exported reports as JSON strings")
  void loadAllReturnsReports() {
    AgenticContext ctx = AgenticContext.create();
    AgentResult result = AgentResult.error(new RuntimeException("x"), ctx, 1);
    AgentRunReport r1 = AgentRunReport.from("A", result, Instant.now(), Instant.now(), 0, 0);
    AgentRunReport r2 = AgentRunReport.from("B", result, Instant.now(), Instant.now(), 0, 0);

    exporter.export(r1);
    exporter.export(r2);

    List<String> all = exporter.loadAll();
    assertEquals(2, all.size());
    assertTrue(all.stream().allMatch(s -> s.startsWith("{")));
  }

  @Test
  @DisplayName("count() returns number of stored reports")
  void countReturnsCorrectNumber() {
    assertEquals(0, exporter.count());

    AgenticContext ctx = AgenticContext.create();
    AgentResult result = AgentResult.error(new RuntimeException("x"), ctx, 1);
    AgentRunReport report = AgentRunReport.from("A", result, Instant.now(), Instant.now(), 0, 0);
    exporter.export(report);

    assertEquals(1, exporter.count());
  }
}
