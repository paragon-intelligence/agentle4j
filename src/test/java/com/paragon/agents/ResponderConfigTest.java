package com.paragon.agents;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.responses.spec.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for ResponderConfig record. */
@DisplayName("ResponderConfig Tests")
class ResponderConfigTest {

  @Nested
  @DisplayName("Record")
  class RecordTests {

    @Test
    @DisplayName("creates with all null values")
    void createsWithAllNull() {
      ResponderConfig config =
          new ResponderConfig(
              null, null, null, null, null, null, null, null, null, null, null, null, null, null,
              null, null, null, null, null, null, null, null);

      assertNotNull(config);
      assertNull(config.model());
      assertNull(config.instructions());
      assertNull(config.temperature());
    }

    @Test
    @DisplayName("creates with model and temperature")
    void createsWithModelAndTemperature() {
      ResponderConfig config =
          new ResponderConfig(
              null, null, null, null, null, "gpt-4", null, null, null, null, null, null, null, null,
              0.7, null, null, null, null, null, null, null);

      assertEquals("gpt-4", config.model());
      assertEquals(0.7, config.temperature());
    }

    @Test
    @DisplayName("creates with maxOutputTokens")
    void createsWithMaxOutputTokens() {
      ResponderConfig config =
          new ResponderConfig(
              null, null, 1000, null, null, null, null, null, null, null, null, null, null, null,
              null, null, null, null, null, null, null, null);

      assertEquals(1000, config.maxOutputTokens());
    }

    @Test
    @DisplayName("creates with metadata map")
    void createsWithMetadata() {
      Map<String, String> metadata = Map.of("key", "value");
      ResponderConfig config =
          new ResponderConfig(
              null, null, null, null, metadata, null, null, null, null, null, null, null, null,
              null, null, null, null, null, null, null, null, null);

      assertEquals(metadata, config.metadata());
      assertEquals("value", config.metadata().get("key"));
    }

    @Test
    @DisplayName("creates with include list")
    void createsWithInclude() {
      List<OutputDataInclude> includes = List.of(OutputDataInclude.REASONING_ENCRYPTED_CONTENT);
      ResponderConfig config =
          new ResponderConfig(
              includes, null, null, null, null, null, null, null, null, null, null, null, null,
              null, null, null, null, null, null, null, null, null);

      assertEquals(1, config.include().size());
    }

    @Test
    @DisplayName("different configs not equal")
    void differentConfigsNotEqual() {
      ResponderConfig config1 =
          new ResponderConfig(
              null, null, null, null, null, "model-a", null, null, null, null, null, null, null,
              null, null, null, null, null, null, null, null, null);
      ResponderConfig config2 =
          new ResponderConfig(
              null, null, null, null, null, "model-b", null, null, null, null, null, null, null,
              null, null, null, null, null, null, null, null, null);

      assertNotEquals(config1, config2);
    }

    @Test
    @DisplayName("equal configs are equal")
    void equalConfigsAreEqual() {
      ResponderConfig config1 =
          new ResponderConfig(
              null,
              "instructions",
              100,
              null,
              null,
              "gpt-4",
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              0.5,
              null,
              null,
              null,
              null,
              null,
              null,
              null);
      ResponderConfig config2 =
          new ResponderConfig(
              null,
              "instructions",
              100,
              null,
              null,
              "gpt-4",
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              0.5,
              null,
              null,
              null,
              null,
              null,
              null,
              null);

      assertEquals(config1, config2);
    }
  }
}
