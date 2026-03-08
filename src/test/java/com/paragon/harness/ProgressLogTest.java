package com.paragon.harness;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ProgressLog")
class ProgressLogTest {

  @Nested
  @DisplayName("append")
  class Append {

    @Test
    @DisplayName("appends entries and returns them via all()")
    void appendsEntries() {
      ProgressLog log = ProgressLog.create();
      log.append("Step 1", ProgressLog.Status.DONE, null);
      log.append("Step 2", ProgressLog.Status.IN_PROGRESS, "working on it");

      List<ProgressLog.Entry> all = log.all();
      assertEquals(2, all.size());
      assertEquals("Step 1", all.get(0).description());
      assertEquals(ProgressLog.Status.DONE, all.get(0).status());
      assertEquals("Step 2", all.get(1).description());
      assertEquals("working on it", all.get(1).notes());
    }

    @Test
    @DisplayName("returned entry has auto-generated id and timestamp")
    void entryHasIdAndTimestamp() {
      ProgressLog log = ProgressLog.create();
      ProgressLog.Entry entry = log.append("test", ProgressLog.Status.DONE, null);

      assertNotNull(entry.id());
      assertFalse(entry.id().isBlank());
      assertNotNull(entry.timestamp());
    }
  }

  @Nested
  @DisplayName("byStatus")
  class ByStatus {

    @Test
    @DisplayName("filters correctly by each status")
    void filtersByStatus() {
      ProgressLog log = ProgressLog.create();
      log.append("done task", ProgressLog.Status.DONE, null);
      log.append("failing task", ProgressLog.Status.FAILED, "error occurred");
      log.append("active task", ProgressLog.Status.IN_PROGRESS, null);

      assertEquals(1, log.byStatus(ProgressLog.Status.DONE).size());
      assertEquals(1, log.byStatus(ProgressLog.Status.FAILED).size());
      assertEquals(1, log.byStatus(ProgressLog.Status.IN_PROGRESS).size());
    }

    @Test
    @DisplayName("returns empty list when no entries match")
    void returnsEmptyWhenNoMatch() {
      ProgressLog log = ProgressLog.create();
      log.append("done", ProgressLog.Status.DONE, null);

      assertTrue(log.byStatus(ProgressLog.Status.FAILED).isEmpty());
    }
  }

  @Nested
  @DisplayName("toSummary")
  class ToSummary {

    @Test
    @DisplayName("returns empty message for empty log")
    void emptyLogSummary() {
      ProgressLog log = ProgressLog.create();
      assertTrue(log.toSummary().contains("empty"));
    }

    @Test
    @DisplayName("summary includes all entries")
    void summaryIncludesEntries() {
      ProgressLog log = ProgressLog.create();
      log.append("Feature A", ProgressLog.Status.DONE, "shipped");
      log.append("Feature B", ProgressLog.Status.IN_PROGRESS, null);

      String summary = log.toSummary();
      assertTrue(summary.contains("Feature A"));
      assertTrue(summary.contains("DONE"));
      assertTrue(summary.contains("Feature B"));
      assertTrue(summary.contains("IN_PROGRESS"));
      assertTrue(summary.contains("shipped"));
    }
  }

  @Test
  @DisplayName("from() pre-populates entries")
  void fromPrePopulatesEntries() {
    ProgressLog.Entry e = ProgressLog.Entry.of("pre-existing", ProgressLog.Status.DONE, null);
    ProgressLog log = ProgressLog.from(List.of(e));

    assertEquals(1, log.size());
    assertEquals("pre-existing", log.all().get(0).description());
  }
}
