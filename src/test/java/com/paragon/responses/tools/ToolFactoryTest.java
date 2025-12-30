package com.paragon.responses.tools;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for ToolFactory class. */
class ToolFactoryTest {

  @Test
  @DisplayName("ToolFactory can be instantiated")
  void canBeInstantiated() {
    ToolFactory factory = new ToolFactory();
    assertNotNull(factory);
  }

  @Test
  @DisplayName("Multiple instances are independent")
  void multipleInstances() {
    ToolFactory f1 = new ToolFactory();
    ToolFactory f2 = new ToolFactory();

    assertNotNull(f1);
    assertNotNull(f2);
    assertNotSame(f1, f2);
  }
}
