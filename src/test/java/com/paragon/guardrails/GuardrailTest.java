package com.paragon.guardrails;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for Guardrail class. */
class GuardrailTest {

  @Test
  @DisplayName("Guardrail can be instantiated")
  void canBeInstantiated() {
    Guardrail guardrail = new Guardrail();
    assertNotNull(guardrail);
  }

  @Test
  @DisplayName("Multiple instances are independent")
  void multipleInstances() {
    Guardrail g1 = new Guardrail();
    Guardrail g2 = new Guardrail();

    assertNotNull(g1);
    assertNotNull(g2);
    assertNotSame(g1, g2);
  }
}
