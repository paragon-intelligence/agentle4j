package com.paragon.web;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for Scroll action.
 *
 * <p>Tests cover: - Factory methods (of, down, up, left, right) - Amount validation - Record
 * accessors - Direction values
 */
@DisplayName("Scroll")
class ScrollTest {

  // ═══════════════════════════════════════════════════════════════════════════
  // FACTORY METHOD TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Factory Methods")
  class FactoryMethodTests {

    @Test
    @DisplayName("of() creates scroll with specified direction and amount")
    void ofCreatesScrollWithSpecifiedValues() {
      Scroll scroll = Scroll.of("body", ScrollDirection.DOWN, 100);

      assertEquals(ScrollDirection.DOWN, scroll.direction());
      assertEquals(100, scroll.amount());
      assertEquals("body", scroll.selector());
    }

    @Test
    @DisplayName("down() creates downward scroll")
    void downCreatesDownwardScroll() {
      Scroll scroll = Scroll.down("#content", 200);

      assertEquals(ScrollDirection.DOWN, scroll.direction());
      assertEquals(200, scroll.amount());
      assertEquals("#content", scroll.selector());
    }

    @Test
    @DisplayName("up() creates upward scroll")
    void upCreatesUpwardScroll() {
      Scroll scroll = Scroll.up(".container", 300);

      assertEquals(ScrollDirection.UP, scroll.direction());
      assertEquals(300, scroll.amount());
      assertEquals(".container", scroll.selector());
    }

    @Test
    @DisplayName("left() creates leftward scroll")
    void leftCreatesLeftwardScroll() {
      Scroll scroll = Scroll.left("[data-scroll]", 400);

      assertEquals(ScrollDirection.LEFT, scroll.direction());
      assertEquals(400, scroll.amount());
      assertEquals("[data-scroll]", scroll.selector());
    }

    @Test
    @DisplayName("right() creates rightward scroll")
    void rightCreatesRightwardScroll() {
      Scroll scroll = Scroll.right("div.scroll-area", 500);

      assertEquals(ScrollDirection.RIGHT, scroll.direction());
      assertEquals(500, scroll.amount());
      assertEquals("div.scroll-area", scroll.selector());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // VALIDATION TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Validation")
  class ValidationTests {

    @Test
    @DisplayName("rejects negative amount")
    void rejectsNegativeAmount() {
      assertThrows(
          IllegalArgumentException.class, () -> Scroll.of("body", ScrollDirection.DOWN, -1));
    }

    @Test
    @DisplayName("rejects amount over 1000")
    void rejectsAmountOver1000() {
      assertThrows(
          IllegalArgumentException.class, () -> Scroll.of("body", ScrollDirection.DOWN, 1001));
    }

    @Test
    @DisplayName("accepts amount of 0")
    void acceptsAmountOfZero() {
      Scroll scroll = Scroll.down("body", 0);
      assertEquals(0, scroll.amount());
    }

    @Test
    @DisplayName("accepts amount of 1000")
    void acceptsAmountOf1000() {
      Scroll scroll = Scroll.down("body", 1000);
      assertEquals(1000, scroll.amount());
    }

    @Test
    @DisplayName("accepts amount in valid range")
    void acceptsAmountInValidRange() {
      Scroll scroll = Scroll.down("body", 500);
      assertEquals(500, scroll.amount());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ACCESSOR TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Accessors")
  class AccessorTests {

    @Test
    @DisplayName("direction() returns correct direction")
    void directionReturnsCorrectDirection() {
      Scroll scroll = Scroll.of("body", ScrollDirection.LEFT, 100);
      assertEquals(ScrollDirection.LEFT, scroll.direction());
    }

    @Test
    @DisplayName("amount() returns correct amount")
    void amountReturnsCorrectAmount() {
      Scroll scroll = Scroll.of("body", ScrollDirection.DOWN, 750);
      assertEquals(750, scroll.amount());
    }

    @Test
    @DisplayName("selector() returns correct selector")
    void selectorReturnsCorrectSelector() {
      Scroll scroll = Scroll.of("#my-element", ScrollDirection.RIGHT, 250);
      assertEquals("#my-element", scroll.selector());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // DIRECTION ENUM TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("ScrollDirection")
  class ScrollDirectionTests {

    @Test
    @DisplayName("all directions are available")
    void allDirectionsAreAvailable() {
      assertNotNull(ScrollDirection.UP);
      assertNotNull(ScrollDirection.DOWN);
      assertNotNull(ScrollDirection.LEFT);
      assertNotNull(ScrollDirection.RIGHT);
    }

    @Test
    @DisplayName("valueOf works for all directions")
    void valueOfWorksForAllDirections() {
      assertEquals(ScrollDirection.UP, ScrollDirection.valueOf("UP"));
      assertEquals(ScrollDirection.DOWN, ScrollDirection.valueOf("DOWN"));
      assertEquals(ScrollDirection.LEFT, ScrollDirection.valueOf("LEFT"));
      assertEquals(ScrollDirection.RIGHT, ScrollDirection.valueOf("RIGHT"));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // RECORD EQUALITY TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Record Equality")
  class RecordEqualityTests {

    @Test
    @DisplayName("equal scrolls are equal")
    void equalScrollsAreEqual() {
      Scroll scroll1 = Scroll.down("body", 100);
      Scroll scroll2 = Scroll.down("body", 100);

      assertEquals(scroll1, scroll2);
      assertEquals(scroll1.hashCode(), scroll2.hashCode());
    }

    @Test
    @DisplayName("different directions are not equal")
    void differentDirectionsAreNotEqual() {
      Scroll scroll1 = Scroll.down("body", 100);
      Scroll scroll2 = Scroll.up("body", 100);

      assertNotEquals(scroll1, scroll2);
    }

    @Test
    @DisplayName("different amounts are not equal")
    void differentAmountsAreNotEqual() {
      Scroll scroll1 = Scroll.down("body", 100);
      Scroll scroll2 = Scroll.down("body", 200);

      assertNotEquals(scroll1, scroll2);
    }

    @Test
    @DisplayName("different selectors are not equal")
    void differentSelectorsAreNotEqual() {
      Scroll scroll1 = Scroll.down("body", 100);
      Scroll scroll2 = Scroll.down("#content", 100);

      assertNotEquals(scroll1, scroll2);
    }

    @Test
    @DisplayName("toString contains all fields")
    void toStringContainsAllFields() {
      Scroll scroll = Scroll.down("#element", 150);
      String str = scroll.toString();

      assertTrue(str.contains("DOWN") || str.contains("down"));
      assertTrue(str.contains("150"));
      assertTrue(str.contains("#element"));
    }
  }
}
