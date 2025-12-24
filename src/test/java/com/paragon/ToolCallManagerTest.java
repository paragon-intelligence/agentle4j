package com.paragon;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for ToolCallManager class.
 */
class ToolCallManagerTest {

  @Test
  @DisplayName("ToolCallManager can be instantiated")
  void canBeInstantiated() {
    ToolCallManager manager = new ToolCallManager();
    assertNotNull(manager);
  }

  @Test
  @DisplayName("Multiple instances are independent")
  void multipleInstances() {
    ToolCallManager m1 = new ToolCallManager();
    ToolCallManager m2 = new ToolCallManager();

    assertNotNull(m1);
    assertNotNull(m2);
    assertNotSame(m1, m2);
  }
}
