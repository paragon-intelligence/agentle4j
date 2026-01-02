package com.paragon.responses.spec;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for Reasoning record.
 * 
 * Tests cover:
 * - Record creation and accessors
 * - toString() formatting
 * - Nullable fields handling
 * - Record equality
 */
@DisplayName("Reasoning")
class ReasoningTest {

  // ═══════════════════════════════════════════════════════════════════════════
  // CONSTRUCTION TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Construction")
  class ConstructionTests {

    @Test
    @DisplayName("creates with all required fields")
    void createsWithAllRequiredFields() {
      Reasoning reasoning = new Reasoning(
          "reasoning-123",
          List.of(),
          null,
          null,
          null
      );

      assertNotNull(reasoning);
      assertEquals("reasoning-123", reasoning.id());
      assertTrue(reasoning.summary().isEmpty());
    }

    @Test
    @DisplayName("creates with all fields populated")
    void createsWithAllFieldsPopulated() {
      Reasoning reasoning = new Reasoning(
          "reasoning-456",
          List.of(),
          List.of(),
          "encrypted-content-xyz",
                    ReasoningStatus.COMPLETED
      );

      assertNotNull(reasoning);
      assertEquals("reasoning-456", reasoning.id());
      assertEquals("encrypted-content-xyz", reasoning.encryptedContent());
      assertEquals(          ReasoningStatus.COMPLETED, reasoning.status());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ACCESSOR TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Accessors")
  class AccessorTests {

    @Test
    @DisplayName("id() returns reasoning id")
    void idReturnsReasoningId() {
      Reasoning reasoning = new Reasoning("my-id", List.of(), null, null, null);
      assertEquals("my-id", reasoning.id());
    }

    @Test
    @DisplayName("summary() returns summary list")
    void summaryReturnsSummaryList() {
      List<ReasoningSummary> summaries = List.of();
      Reasoning reasoning = new Reasoning("id", summaries, null, null, null);
      assertEquals(summaries, reasoning.summary());
    }

    @Test
    @DisplayName("content() returns null when not set")
    void contentReturnsNullWhenNotSet() {
      Reasoning reasoning = new Reasoning("id", List.of(), null, null, null);
      assertNull(reasoning.content());
    }

    @Test
    @DisplayName("encryptedContent() returns null when not set")
    void encryptedContentReturnsNullWhenNotSet() {
      Reasoning reasoning = new Reasoning("id", List.of(), null, null, null);
      assertNull(reasoning.encryptedContent());
    }

    @Test
    @DisplayName("encryptedContent() returns value when set")
    void encryptedContentReturnsValueWhenSet() {
      Reasoning reasoning = new Reasoning("id", List.of(), null, "encrypted-123", null);
      assertEquals("encrypted-123", reasoning.encryptedContent());
    }

    @Test
    @DisplayName("status() returns null when not set")
    void statusReturnsNullWhenNotSet() {
      Reasoning reasoning = new Reasoning("id", List.of(), null, null, null);
      assertNull(reasoning.status());
    }

    @Test
    @DisplayName("status() returns completed when set")
    void statusReturnsCompletedWhenSet() {
      Reasoning reasoning = new Reasoning("id", List.of(), null, null, ReasoningStatus.COMPLETED);
      assertEquals(ReasoningStatus.COMPLETED, reasoning.status());
    }

    @Test
    @DisplayName("status() returns in_progress when set")
    void statusReturnsInProgressWhenSet() {
      Reasoning reasoning = new Reasoning("id", List.of(), null, null,           ReasoningStatus.IN_PROGRESS);
      assertEquals(          ReasoningStatus.IN_PROGRESS, reasoning.status());
    }

    @Test
    @DisplayName("status() returns incomplete when set")
    void statusReturnsIncompleteWhenSet() {
      Reasoning reasoning = new Reasoning("id", List.of(), null, null,           ReasoningStatus.INCOMPLETE);
      assertEquals(          ReasoningStatus.INCOMPLETE, reasoning.status());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // TOSTRING TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("toString()")
  class ToStringTests {

    @Test
    @DisplayName("toString contains id")
    void toStringContainsId() {
      Reasoning reasoning = new Reasoning("reasoning-789", List.of(), null, null, null);
      String str = reasoning.toString();

      assertTrue(str.contains("reasoning-789"));
    }

    @Test
    @DisplayName("toString contains null placeholders for optional fields")
    void toStringContainsNullPlaceholders() {
      Reasoning reasoning = new Reasoning("id", List.of(), null, null, null);
      String str = reasoning.toString();

      assertTrue(str.contains("null"));
    }

    @Test
    @DisplayName("toString contains encrypted content when set")
    void toStringContainsEncryptedContentWhenSet() {
      Reasoning reasoning = new Reasoning("id", List.of(), null, "secret-encrypted", null);
      String str = reasoning.toString();

      assertTrue(str.contains("secret-encrypted"));
    }

    @Test
    @DisplayName("toString contains XML-like format")
    void toStringContainsXmlLikeFormat() {
      Reasoning reasoning = new Reasoning("id", List.of(), null, null, ReasoningStatus.COMPLETED);
      String str = reasoning.toString();

      assertTrue(str.contains("<reasoning>"));
      assertTrue(str.contains("</reasoning>"));
      assertTrue(str.contains("<id>"));
      assertTrue(str.contains("</id>"));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // EQUALITY TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Equality")
  class EqualityTests {

    @Test
    @DisplayName("equal reasonings are equal")
    void equalReasoningsAreEqual() {
      Reasoning r1 = new Reasoning("id", List.of(), null, "enc", ReasoningStatus.COMPLETED);
      Reasoning r2 = new Reasoning("id", List.of(), null, "enc", ReasoningStatus.COMPLETED);

      assertEquals(r1, r2);
      assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    @DisplayName("different ids are not equal")
    void differentIdsAreNotEqual() {
      Reasoning r1 = new Reasoning("id1", List.of(), null, null, null);
      Reasoning r2 = new Reasoning("id2", List.of(), null, null, null);

      assertNotEquals(r1, r2);
    }

    @Test
    @DisplayName("different statuses are not equal")
    void differentStatusesAreNotEqual() {
      Reasoning r1 = new Reasoning("id", List.of(), null, null, ReasoningStatus.COMPLETED);
      Reasoning r2 = new Reasoning("id", List.of(), null, null, ReasoningStatus.IN_PROGRESS);

      assertNotEquals(r1, r2);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // REASONING STATUS TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("ReasoningStatus")
  class ReasoningStatusTests {

    @Test
    @DisplayName("all status values are available")
    void allStatusValuesAreAvailable() {
      assertNotNull(ReasoningStatus.IN_PROGRESS);
      assertNotNull(ReasoningStatus.COMPLETED);
      assertNotNull(ReasoningStatus.INCOMPLETE);
    }

    @Test
    @DisplayName("valueOf works for all statuses")
    void valueOfWorksForAllStatuses() {
      assertEquals(ReasoningStatus.IN_PROGRESS, ReasoningStatus.valueOf("IN_PROGRESS"));
      assertEquals(ReasoningStatus.COMPLETED, ReasoningStatus.valueOf("COMPLETED"));
      assertEquals(ReasoningStatus.INCOMPLETE, ReasoningStatus.valueOf("INCOMPLETE"));
    }
  }
}
