package com.paragon.responses.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link GuardrailException}. */
@DisplayName("GuardrailException")
class GuardrailExceptionTest {

  @Nested
  @DisplayName("Constructor")
  class Constructor {

    @Test
    @DisplayName("should create with all fields")
    void shouldCreateWithAllFields() {
      GuardrailException e =
          new GuardrailException(
              "PII Filter", GuardrailException.ViolationType.INPUT, "Contains SSN");

      assertEquals("PII Filter", e.guardrailName());
      assertEquals(GuardrailException.ViolationType.INPUT, e.violationType());
      assertEquals("Contains SSN", e.reason());
      assertEquals(AgentleException.ErrorCode.GUARDRAIL_VIOLATED, e.code());
      assertFalse(e.isRetryable());
    }

    @Test
    @DisplayName("should create with null guardrail name")
    void shouldCreateWithNullName() {
      GuardrailException e =
          new GuardrailException(null, GuardrailException.ViolationType.OUTPUT, "Inappropriate");

      assertNull(e.guardrailName());
      assertEquals(GuardrailException.ViolationType.OUTPUT, e.violationType());
      assertTrue(e.getMessage().contains("blocked output"));
    }

    @Test
    @DisplayName("should have appropriate suggestion for INPUT violation")
    void shouldHaveSuggestionForInput() {
      GuardrailException e =
          new GuardrailException(
              "test", GuardrailException.ViolationType.INPUT, "blocked");

      assertTrue(e.suggestion().toLowerCase().contains("rephrase"));
    }

    @Test
    @DisplayName("should have appropriate suggestion for OUTPUT violation")
    void shouldHaveSuggestionForOutput() {
      GuardrailException e =
          new GuardrailException(
              "test", GuardrailException.ViolationType.OUTPUT, "blocked");

      assertTrue(e.suggestion().toLowerCase().contains("blocked"));
    }
  }

  @Nested
  @DisplayName("Factory methods")
  class FactoryMethods {

    @Test
    @DisplayName("inputViolation should create INPUT type")
    void inputViolationShouldCreateInputType() {
      GuardrailException e = GuardrailException.inputViolation("Profanity detected");

      assertEquals(GuardrailException.ViolationType.INPUT, e.violationType());
      assertNull(e.guardrailName());
      assertEquals("Profanity detected", e.reason());
    }

    @Test
    @DisplayName("inputViolation with name should include name")
    void inputViolationWithNameShouldIncludeName() {
      GuardrailException e =
          GuardrailException.inputViolation("Content Filter", "Inappropriate content");

      assertEquals(GuardrailException.ViolationType.INPUT, e.violationType());
      assertEquals("Content Filter", e.guardrailName());
      assertEquals("Inappropriate content", e.reason());
      assertTrue(e.getMessage().contains("Content Filter"));
    }

    @Test
    @DisplayName("outputViolation should create OUTPUT type")
    void outputViolationShouldCreateOutputType() {
      GuardrailException e = GuardrailException.outputViolation("Response too long");

      assertEquals(GuardrailException.ViolationType.OUTPUT, e.violationType());
      assertNull(e.guardrailName());
      assertEquals("Response too long", e.reason());
    }

    @Test
    @DisplayName("outputViolation with name should include name")
    void outputViolationWithNameShouldIncludeName() {
      GuardrailException e =
          GuardrailException.outputViolation("Length Guard", "Exceeds 5000 chars");

      assertEquals(GuardrailException.ViolationType.OUTPUT, e.violationType());
      assertEquals("Length Guard", e.guardrailName());
      assertEquals("Exceeds 5000 chars", e.reason());
    }
  }

  @Nested
  @DisplayName("ViolationType enum")
  class ViolationTypeEnum {

    @Test
    @DisplayName("should have INPUT and OUTPUT values")
    void shouldHaveInputAndOutput() {
      GuardrailException.ViolationType[] types = GuardrailException.ViolationType.values();

      assertEquals(2, types.length);
      assertNotNull(GuardrailException.ViolationType.INPUT);
      assertNotNull(GuardrailException.ViolationType.OUTPUT);
    }
  }

  @Nested
  @DisplayName("Inheritance")
  class Inheritance {

    @Test
    @DisplayName("should extend AgentleException")
    void shouldExtendAgentleException() {
      GuardrailException e =
          new GuardrailException(null, GuardrailException.ViolationType.INPUT, "test");

      assertInstanceOf(AgentleException.class, e);
    }
  }
}
