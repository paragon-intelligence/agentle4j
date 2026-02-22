package com.paragon.responses.spec;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Comprehensive tests for ComputerUseAction implementations. */
@DisplayName("Computer Use Actions Tests")
class ComputerUseActionsTest {

  // =========================================================================
  // ClickAction Record
  // =========================================================================
  @Nested
  @DisplayName("ClickAction")
  class ClickActionTest {

    @Test
    @DisplayName("should create with left button and coordinate")
    void shouldCreateWithLeftButton() {
      Coordinate coord = Coordinate.Factory.getCoordinate(100, 200);
      ClickAction action = new ClickAction(ClickButton.LEFT, coord);

      assertEquals(ClickButton.LEFT, action.button());
      assertEquals(coord, action.coordinate());
    }

    @Test
    @DisplayName("should create with right button")
    void shouldCreateWithRightButton() {
      Coordinate coord = Coordinate.Factory.getCoordinate(50, 75);
      ClickAction action = new ClickAction(ClickButton.RIGHT, coord);

      assertEquals(ClickButton.RIGHT, action.button());
    }

    @Test
    @DisplayName("toString should contain button and coordinate")
    void toStringShouldContainButtonAndCoordinate() {
      Coordinate coord = Coordinate.Factory.getCoordinate(100, 200);
      ClickAction action = new ClickAction(ClickButton.LEFT, coord);

      String str = action.toString();
      assertTrue(str.contains("click"));
      assertTrue(str.contains("button"));
    }

    @Test
    @DisplayName("should implement ComputerUseAction")
    void shouldImplementComputerUseAction() {
      Coordinate coord = Coordinate.Factory.getCoordinate(0, 0);
      ClickAction action = new ClickAction(ClickButton.LEFT, coord);

      assertInstanceOf(ComputerUseAction.class, action);
    }
  }

  // =========================================================================
  // ScreenshotAction Record
  // =========================================================================
  @Nested
  @DisplayName("ScreenshotAction")
  class ScreenshotActionTest {

    @Test
    @DisplayName("should create with no args")
    void shouldCreateWithNoArgs() {
      ScreenshotAction action = new ScreenshotAction();
      assertNotNull(action);
    }

    @Test
    @DisplayName("toString should contain screenshot")
    void toStringShouldContainScreenshot() {
      ScreenshotAction action = new ScreenshotAction();
      assertTrue(action.toString().contains("screenshot"));
    }

    @Test
    @DisplayName("should implement ComputerUseAction")
    void shouldImplementComputerUseAction() {
      ScreenshotAction action = new ScreenshotAction();
      assertInstanceOf(ComputerUseAction.class, action);
    }
  }

  // =========================================================================
  // KeyPressAction Record
  // =========================================================================
  @Nested
  @DisplayName("KeyPressAction")
  class KeyPressActionTest {

    @Test
    @DisplayName("should create with single key")
    void shouldCreateWithSingleKey() {
      KeyPressAction action = new KeyPressAction(List.of("Enter"));
      assertEquals(1, action.keys().size());
      assertEquals("Enter", action.keys().get(0));
    }

    @Test
    @DisplayName("should create with key combination")
    void shouldCreateWithKeyCombination() {
      KeyPressAction action = new KeyPressAction(List.of("Ctrl", "C"));
      assertEquals(2, action.keys().size());
      assertTrue(action.keys().contains("Ctrl"));
      assertTrue(action.keys().contains("C"));
    }

    @Test
    @DisplayName("toString should contain keys")
    void toStringShouldContainKeys() {
      KeyPressAction action = new KeyPressAction(List.of("Escape"));
      String str = action.toString();
      assertTrue(str.contains("keypress"));
      assertTrue(str.contains("Escape"));
    }
  }

  // =========================================================================
  // DoubleClickAction Record
  // =========================================================================
  @Nested
  @DisplayName("DoubleClickAction")
  class DoubleClickActionTest {

    @Test
    @DisplayName("should create with coordinate")
    void shouldCreateWithCoordinate() {
      Coordinate coord = Coordinate.Factory.getCoordinate(300, 400);
      DoubleClickAction action = new DoubleClickAction(coord);

      assertEquals(coord, action.coordinate());
    }

    @Test
    @DisplayName("should implement ComputerUseAction")
    void shouldImplementComputerUseAction() {
      Coordinate coord = Coordinate.Factory.getCoordinate(0, 0);
      DoubleClickAction action = new DoubleClickAction(coord);
      assertInstanceOf(ComputerUseAction.class, action);
    }
  }

  // =========================================================================
  // MoveAction Record
  // =========================================================================
  @Nested
  @DisplayName("MoveAction")
  class MoveActionTest {

    @Test
    @DisplayName("should create with coordinate")
    void shouldCreateWithCoordinate() {
      Coordinate coord = Coordinate.Factory.getCoordinate(500, 600);
      MoveAction action = new MoveAction(coord);

      assertEquals(coord, action.coordinate());
    }
  }

  // =========================================================================
  // TypeAction Record
  // =========================================================================
  @Nested
  @DisplayName("TypeAction")
  class TypeActionTest {

    @Test
    @DisplayName("should create with text")
    void shouldCreateWithText() {
      TypeAction action = new TypeAction("Hello, World!");
      assertEquals("Hello, World!", action.text());
    }

    @Test
    @DisplayName("should handle empty text")
    void shouldHandleEmptyText() {
      TypeAction action = new TypeAction("");
      assertEquals("", action.text());
    }
  }

  // =========================================================================
  // ScrollAction Record
  // =========================================================================
  @Nested
  @DisplayName("ScrollAction")
  class ScrollActionTest {

    @Test
    @DisplayName("should create scroll action")
    void shouldCreateScrollAction() {
      Coordinate coord = Coordinate.Factory.getCoordinate(100, 100);
      ScrollAction action = new ScrollAction(0, -100, coord);

      assertEquals(coord, action.coordinate());
      assertEquals(0, action.scrollX());
      assertEquals(-100, action.scrollY());
    }
  }

  // =========================================================================
  // WaitAction Record
  // =========================================================================
  @Nested
  @DisplayName("WaitAction")
  class WaitActionTest {

    @Test
    @DisplayName("should create wait action")
    void shouldCreateWaitAction() {
      WaitAction action = new WaitAction();
      assertNotNull(action);
    }
  }

  // =========================================================================
  // Coordinate Factory
  // =========================================================================
  @Nested
  @DisplayName("Coordinate Factory")
  class CoordinateFactoryTest {

    @Test
    @DisplayName("should return same instance for same coordinates")
    void shouldReturnSameInstanceForSameCoordinates() {
      Coordinate coord1 = Coordinate.Factory.getCoordinate(10, 20);
      Coordinate coord2 = Coordinate.Factory.getCoordinate(10, 20);
      assertSame(coord1, coord2);
    }

    @Test
    @DisplayName("should return different instances for different coordinates")
    void shouldReturnDifferentInstancesForDifferentCoordinates() {
      Coordinate coord1 = Coordinate.Factory.getCoordinate(10, 20);
      Coordinate coord2 = Coordinate.Factory.getCoordinate(30, 40);
      assertNotSame(coord1, coord2);
    }

    @Test
    @DisplayName("toString should show x and y")
    void toStringShouldShowXAndY() {
      Coordinate coord = Coordinate.Factory.getCoordinate(100, 200);
      String str = coord.toString();
      assertTrue(str.contains("100"));
      assertTrue(str.contains("200"));
    }
  }
}
