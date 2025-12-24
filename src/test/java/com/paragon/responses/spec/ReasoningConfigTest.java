package com.paragon.responses.spec;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ReasoningConfig} record.
 */
class ReasoningConfigTest {

  @Nested
  @DisplayName("Construction")
  class ConstructionTests {

    @Test
    @DisplayName("ReasoningConfig can be created with both values")
    void createWithBothValues() {
      ReasoningConfig config = new ReasoningConfig(ReasoningEffort.HIGH, ReasoningSummaryKind.AUTO);

      assertEquals(ReasoningEffort.HIGH, config.effort());
      assertEquals(ReasoningSummaryKind.AUTO, config.summary());
    }

    @Test
    @DisplayName("ReasoningConfig can be created with null effort")
    void createWithNullEffort() {
      ReasoningConfig config = new ReasoningConfig(null, ReasoningSummaryKind.DETAILED);

      assertNull(config.effort());
      assertEquals(ReasoningSummaryKind.DETAILED, config.summary());
    }

    @Test
    @DisplayName("ReasoningConfig can be created with null summary")
    void createWithNullSummary() {
      ReasoningConfig config = new ReasoningConfig(ReasoningEffort.MEDIUM, null);

      assertEquals(ReasoningEffort.MEDIUM, config.effort());
      assertNull(config.summary());
    }

    @Test
    @DisplayName("ReasoningConfig can be created with both null values")
    void createWithBothNull() {
      ReasoningConfig config = new ReasoningConfig(null, null);

      assertNull(config.effort());
      assertNull(config.summary());
    }
  }

  @Nested
  @DisplayName("Record equality")
  class EqualityTests {

    @Test
    @DisplayName("Equal configs have same hashCode")
    void equalHashCodes() {
      ReasoningConfig config1 = new ReasoningConfig(ReasoningEffort.LOW, ReasoningSummaryKind.CONCISE);
      ReasoningConfig config2 = new ReasoningConfig(ReasoningEffort.LOW, ReasoningSummaryKind.CONCISE);

      assertEquals(config1, config2);
      assertEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    @DisplayName("Different configs are not equal")
    void differentConfigsNotEqual() {
      ReasoningConfig config1 = new ReasoningConfig(ReasoningEffort.HIGH, ReasoningSummaryKind.AUTO);
      ReasoningConfig config2 = new ReasoningConfig(ReasoningEffort.LOW, ReasoningSummaryKind.AUTO);

      assertNotEquals(config1, config2);
    }

    @Test
    @DisplayName("Configs with different summary are not equal")
    void differentSummaryNotEqual() {
      ReasoningConfig config1 = new ReasoningConfig(ReasoningEffort.HIGH, ReasoningSummaryKind.AUTO);
      ReasoningConfig config2 = new ReasoningConfig(ReasoningEffort.HIGH, ReasoningSummaryKind.CONCISE);

      assertNotEquals(config1, config2);
    }

    @Test
    @DisplayName("Null values in comparison")
    void nullValueComparison() {
      ReasoningConfig withNull = new ReasoningConfig(null, null);
      ReasoningConfig withValues = new ReasoningConfig(ReasoningEffort.HIGH, ReasoningSummaryKind.AUTO);

      assertNotEquals(withNull, withValues);
    }

    @Test
    @DisplayName("Both null configs are equal")
    void bothNullEqual() {
      ReasoningConfig config1 = new ReasoningConfig(null, null);
      ReasoningConfig config2 = new ReasoningConfig(null, null);

      assertEquals(config1, config2);
      assertEquals(config1.hashCode(), config2.hashCode());
    }
  }

  @Nested
  @DisplayName("Enum values coverage")
  class EnumCoverageTests {

    @Test
    @DisplayName("All ReasoningEffort values can be used")
    void allEffortValues() {
      for (ReasoningEffort effort : ReasoningEffort.values()) {
        ReasoningConfig config = new ReasoningConfig(effort, null);
        assertEquals(effort, config.effort());
      }
    }

    @Test
    @DisplayName("All ReasoningSummaryKind values can be used")
    void allSummaryValues() {
      for (ReasoningSummaryKind summary : ReasoningSummaryKind.values()) {
        ReasoningConfig config = new ReasoningConfig(null, summary);
        assertEquals(summary, config.summary());
      }
    }
  }
}
