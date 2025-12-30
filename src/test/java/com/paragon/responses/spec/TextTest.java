package com.paragon.responses.spec;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for Text record. */
@DisplayName("Text Tests")
class TextTest {

  @Nested
  @DisplayName("Factory Methods")
  class FactoryMethods {

    @Test
    @DisplayName("valueOf creates text")
    void valueOfCreates() {
      Text text = Text.valueOf("Hello, World!");

      assertEquals("Hello, World!", text.text());
    }

    @Test
    @DisplayName("constructor creates text")
    void constructorCreates() {
      Text text = new Text("Direct construction");

      assertEquals("Direct construction", text.text());
    }
  }

  @Nested
  @DisplayName("ToString")
  class ToStringTests {

    @Test
    @DisplayName("toString returns text value")
    void toStringReturnsText() {
      Text text = Text.valueOf("Test message");

      assertEquals("Test message", text.toString());
    }
  }

  @Nested
  @DisplayName("Equality")
  class EqualityTests {

    @Test
    @DisplayName("equals works for same text")
    void equalsWorks() {
      Text t1 = Text.valueOf("Same");
      Text t2 = Text.valueOf("Same");

      assertEquals(t1, t2);
    }

    @Test
    @DisplayName("not equals for different text")
    void notEquals() {
      Text t1 = Text.valueOf("First");
      Text t2 = Text.valueOf("Second");

      assertNotEquals(t1, t2);
    }
  }
}
