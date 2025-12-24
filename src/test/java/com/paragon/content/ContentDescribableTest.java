package com.paragon.content;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for ContentDescribable class.
 */
class ContentDescribableTest {

  @Test
  @DisplayName("ContentDescribable can be instantiated")
  void canBeInstantiated() {
    ContentDescribable describable = new ContentDescribable();
    assertNotNull(describable);
  }

  @Test
  @DisplayName("Multiple instances are independent")
  void multipleInstances() {
    ContentDescribable d1 = new ContentDescribable();
    ContentDescribable d2 = new ContentDescribable();

    assertNotNull(d1);
    assertNotNull(d2);
    assertNotSame(d1, d2);
  }
}
