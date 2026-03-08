package com.paragon.harness.tools;

import com.paragon.harness.RunReportExporter;
import com.paragon.responses.annotations.FunctionMetadata;
import com.paragon.responses.spec.FunctionTool;
import com.paragon.responses.spec.FunctionToolCallOutput;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A {@link FunctionTool} that reads a batch of {@link com.paragon.harness.AgentRunReport}
 * files and returns a structured failure summary for analysis.
 *
 * <p>Feed this tool to a meta-agent to automate harness improvement recommendations:
 *
 * <pre>{@code
 * RunReportExporter exporter = RunReportExporter.create(Path.of("./reports"));
 *
 * Agent metaAgent = Agent.builder()
 *     .name("HarnessImprover")
 *     .instructions("""
 *         You are a harness improvement agent. Use the analyze_failures tool to read
 *         recent agent run reports and suggest concrete improvements to the harness
 *         configuration (guardrails, retry limits, tool timeouts, etc.).
 *         """)
 *     .addTool(new FailureAnalysisTool(exporter))
 *     .build();
 *
 * metaAgent.interact("Analyze recent failures and suggest improvements");
 * }</pre>
 *
 * @since 1.0
 */
@FunctionMetadata(
    name = "analyze_failures",
    description =
        "Read recent agent run reports and return a structured failure summary. "
            + "Use this to understand patterns in agent failures and improve the harness.")
public final class FailureAnalysisTool extends FunctionTool<FailureAnalysisTool.AnalysisRequest> {

  /** Parameters for the failure analysis. */
  public record AnalysisRequest(
      @Nullable Integer maxReports // optional limit on how many reports to analyze
  ) {}

  private final RunReportExporter exporter;

  /**
   * Creates a FailureAnalysisTool backed by the given exporter.
   *
   * @param exporter the exporter to read reports from
   */
  public FailureAnalysisTool(@NonNull RunReportExporter exporter) {
    this.exporter = Objects.requireNonNull(exporter, "exporter cannot be null");
  }

  @Override
  public @Nullable FunctionToolCallOutput call(@Nullable AnalysisRequest params) {
    List<String> allReports = exporter.loadAll();
    if (allReports.isEmpty()) {
      return FunctionToolCallOutput.success("No agent run reports found for analysis.");
    }

    int limit = (params != null && params.maxReports() != null && params.maxReports() > 0)
        ? params.maxReports()
        : allReports.size();

    List<String> reportsToAnalyze = allReports.subList(
        Math.max(0, allReports.size() - limit), allReports.size());

    // Build a structured summary for the LLM to analyze
    StringBuilder summary = new StringBuilder();
    summary.append("## Agent Run Reports (").append(reportsToAnalyze.size()).append(" reports)\n\n");

    int successCount = 0;
    int failureCount = 0;
    for (int i = 0; i < reportsToAnalyze.size(); i++) {
      String report = reportsToAnalyze.get(i);
      summary.append("### Report ").append(i + 1).append("\n");
      summary.append("```json\n").append(report).append("\n```\n\n");
      // Simple heuristic: check if success field appears as true/false
      if (report.contains("\"success\": true")) successCount++;
      else failureCount++;
    }

    summary.append("---\n");
    summary.append("Summary: ").append(successCount).append(" succeeded, ")
        .append(failureCount).append(" failed out of ")
        .append(reportsToAnalyze.size()).append(" analyzed.\n");

    return FunctionToolCallOutput.success(summary.toString());
  }
}
