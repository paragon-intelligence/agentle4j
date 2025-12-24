package com.paragon.responses.spec;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for Computer Use Action records: ClickAction, DragAction, TypeAction, WaitAction.
 */
class ComputerUseActionTest {

  @Nested
  @DisplayName("ClickAction")
  class ClickActionTests {

    @Test
    @DisplayName("can be created with button and coordinate")
    void creation() {
      Coordinate coord = new Coordinate(100, 200);
      ClickAction action = new ClickAction(ClickButton.LEFT, coord);

      assertEquals(ClickButton.LEFT, action.button());
      assertEquals(coord, action.coordinate());
    }

    @Test
    @DisplayName("toString returns formatted click action")
    void toStringFormat() {
      Coordinate coord = new Coordinate(50, 75);
      ClickAction action = new ClickAction(ClickButton.RIGHT, coord);

      String result = action.toString();

      assertTrue(result.contains("<computer_use_action:click>"));
      assertTrue(result.contains("<button>RIGHT</button>"));
      assertTrue(result.contains("<coordinate>(50, 75)</coordinate>"));
      assertTrue(result.contains("</computer_use_action:click>"));
    }

    @Test
    @DisplayName("implements ComputerUseAction")
    void implementsInterface() {
      ClickAction action = new ClickAction(ClickButton.LEFT, new Coordinate(0, 0));
      assertTrue(action instanceof ComputerUseAction);
    }

    @Test
    @DisplayName("all button types work")
    void allButtonTypes() {
      Coordinate coord = new Coordinate(0, 0);

      for (ClickButton button : ClickButton.values()) {
        ClickAction action = new ClickAction(button, coord);
        assertEquals(button, action.button());
        assertTrue(action.toString().contains(button.name()));
      }
    }

    @Test
    @DisplayName("equality works correctly")
    void equality() {
      Coordinate coord = new Coordinate(10, 20);
      ClickAction action1 = new ClickAction(ClickButton.LEFT, coord);
      ClickAction action2 = new ClickAction(ClickButton.LEFT, coord);
      ClickAction action3 = new ClickAction(ClickButton.RIGHT, coord);

      assertEquals(action1, action2);
      assertEquals(action1.hashCode(), action2.hashCode());
      assertNotEquals(action1, action3);
    }
  }

  @Nested
  @DisplayName("DragAction")
  class DragActionTests {

    @Test
    @DisplayName("can be created with path")
    void creation() {
      List<Coordinate> path =
          List.of(new Coordinate(0, 0), new Coordinate(100, 100), new Coordinate(200, 50));
      DragAction action = new DragAction(path);

      assertEquals(path, action.path());
      assertEquals(3, action.path().size());
    }

    @Test
    @DisplayName("toString returns formatted drag action")
    void toStringFormat() {
      List<Coordinate> path = List.of(new Coordinate(10, 20), new Coordinate(30, 40));
      DragAction action = new DragAction(path);

      String result = action.toString();

      assertTrue(result.contains("<computer_use_action:drag>"));
      assertTrue(result.contains("<path>"));
      assertTrue(result.contains("</path>"));
      assertTrue(result.contains("</computer_use_action:drag>"));
    }

    @Test
    @DisplayName("implements ComputerUseAction")
    void implementsInterface() {
      DragAction action = new DragAction(List.of());
      assertTrue(action instanceof ComputerUseAction);
    }

    @Test
    @DisplayName("empty path is valid")
    void emptyPath() {
      DragAction action = new DragAction(List.of());
      assertTrue(action.path().isEmpty());
    }

    @Test
    @DisplayName("equality works correctly")
    void equality() {
      List<Coordinate> path = List.of(new Coordinate(5, 5));
      DragAction action1 = new DragAction(path);
      DragAction action2 = new DragAction(path);
      DragAction action3 = new DragAction(List.of(new Coordinate(10, 10)));

      assertEquals(action1, action2);
      assertEquals(action1.hashCode(), action2.hashCode());
      assertNotEquals(action1, action3);
    }
  }

  @Nested
  @DisplayName("TypeAction")
  class TypeActionTests {

    @Test
    @DisplayName("can be created with text")
    void creation() {
      TypeAction action = new TypeAction("Hello World");
      assertEquals("Hello World", action.text());
    }

    @Test
    @DisplayName("toString returns formatted type action")
    void toStringFormat() {
      TypeAction action = new TypeAction("test input");

      String result = action.toString();

      assertTrue(result.contains("<computer_use_action:type>"));
      assertTrue(result.contains("<text>test input</text>"));
      assertTrue(result.contains("</computer_use_action:type>"));
    }

    @Test
    @DisplayName("implements ComputerUseAction")
    void implementsInterface() {
      TypeAction action = new TypeAction("test");
      assertTrue(action instanceof ComputerUseAction);
    }

    @Test
    @DisplayName("empty text is valid")
    void emptyText() {
      TypeAction action = new TypeAction("");
      assertEquals("", action.text());
    }

    @Test
    @DisplayName("special characters in text")
    void specialCharacters() {
      String specialText = "<script>alert('xss')</script>";
      TypeAction action = new TypeAction(specialText);

      assertEquals(specialText, action.text());
      assertTrue(action.toString().contains(specialText));
    }

    @Test
    @DisplayName("equality works correctly")
    void equality() {
      TypeAction action1 = new TypeAction("hello");
      TypeAction action2 = new TypeAction("hello");
      TypeAction action3 = new TypeAction("world");

      assertEquals(action1, action2);
      assertEquals(action1.hashCode(), action2.hashCode());
      assertNotEquals(action1, action3);
    }
  }

  @Nested
  @DisplayName("WaitAction")
  class WaitActionTests {

    @Test
    @DisplayName("can be created without parameters")
    void creation() {
      WaitAction action = new WaitAction();
      assertNotNull(action);
    }

    @Test
    @DisplayName("toString returns formatted wait action")
    void toStringFormat() {
      WaitAction action = new WaitAction();
      assertEquals("<computer_use_action:wait />", action.toString());
    }

    @Test
    @DisplayName("implements ComputerUseAction")
    void implementsInterface() {
      WaitAction action = new WaitAction();
      assertTrue(action instanceof ComputerUseAction);
    }

    @Test
    @DisplayName("all instances are equal")
    void equality() {
      WaitAction action1 = new WaitAction();
      WaitAction action2 = new WaitAction();

      assertEquals(action1, action2);
      assertEquals(action1.hashCode(), action2.hashCode());
    }
  }

  @Nested
  @DisplayName("Polymorphism")
  class PolymorphismTests {

    @Test
    @DisplayName("all actions can be treated as ComputerUseAction")
    void polymorphicUsage() {
      List<ComputerUseAction> actions =
          List.of(
              new ClickAction(ClickButton.LEFT, new Coordinate(0, 0)),
              new DragAction(List.of(new Coordinate(10, 10))),
              new TypeAction("hello"),
              new WaitAction());

      assertEquals(4, actions.size());

      for (ComputerUseAction action : actions) {
        assertNotNull(action.toString());
      }
    }
  }
}
