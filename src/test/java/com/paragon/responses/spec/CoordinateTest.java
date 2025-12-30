package com.paragon.responses.spec;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link Coordinate} record and its Flyweight Factory. */
class CoordinateTest {

  @BeforeEach
  void setUp() {
    // Clear the pool before each test for isolation
    Coordinate.Factory.clearPool();
  }

  @AfterEach
  void tearDown() {
    // Clean up after each test
    Coordinate.Factory.clearPool();
  }

  @Nested
  @DisplayName("Direct construction")
  class DirectConstructionTests {

    @Test
    @DisplayName("Coordinate can be created directly")
    void directConstruction() {
      Coordinate coord = new Coordinate(10, 20);

      assertEquals(10, coord.x());
      assertEquals(20, coord.y());
    }

    @Test
    @DisplayName("Coordinate with negative values")
    void negativeCoordinates() {
      Coordinate coord = new Coordinate(-5, -10);

      assertEquals(-5, coord.x());
      assertEquals(-10, coord.y());
    }

    @Test
    @DisplayName("Coordinate with zero values")
    void zeroCoordinates() {
      Coordinate coord = new Coordinate(0, 0);

      assertEquals(0, coord.x());
      assertEquals(0, coord.y());
    }
  }

  @Nested
  @DisplayName("toString method")
  class ToStringTests {

    @Test
    @DisplayName("toString returns formatted coordinate string")
    void toStringFormatting() {
      Coordinate coord = new Coordinate(15, 25);
      assertEquals("(15, 25)", coord.toString());
    }

    @Test
    @DisplayName("toString with negative values")
    void toStringNegative() {
      Coordinate coord = new Coordinate(-3, -7);
      assertEquals("(-3, -7)", coord.toString());
    }

    @Test
    @DisplayName("toString with zero values")
    void toStringZero() {
      Coordinate coord = new Coordinate(0, 0);
      assertEquals("(0, 0)", coord.toString());
    }
  }

  @Nested
  @DisplayName("Flyweight Factory")
  class FactoryTests {

    @Test
    @DisplayName("Factory returns Coordinate with correct values")
    void factoryCreatesCoordinate() {
      Coordinate coord = Coordinate.Factory.getCoordinate(100, 200);

      assertEquals(100, coord.x());
      assertEquals(200, coord.y());
    }

    @Test
    @DisplayName("Factory returns same instance for same coordinates (Flyweight pattern)")
    void factoryReusesSameInstance() {
      Coordinate coord1 = Coordinate.Factory.getCoordinate(10, 20);
      Coordinate coord2 = Coordinate.Factory.getCoordinate(10, 20);

      assertSame(
          coord1, coord2, "Factory should return the same instance for identical coordinates");
    }

    @Test
    @DisplayName("Factory returns different instances for different coordinates")
    void factoryCreatesDifferentInstances() {
      Coordinate coord1 = Coordinate.Factory.getCoordinate(10, 20);
      Coordinate coord2 = Coordinate.Factory.getCoordinate(30, 40);

      assertNotSame(coord1, coord2);
    }

    @Test
    @DisplayName("Pool size increases with unique coordinates")
    void poolSizeTracking() {
      assertEquals(0, Coordinate.Factory.getPoolSize());

      Coordinate.Factory.getCoordinate(1, 1);
      assertEquals(1, Coordinate.Factory.getPoolSize());

      Coordinate.Factory.getCoordinate(2, 2);
      assertEquals(2, Coordinate.Factory.getPoolSize());

      // Same coordinate should not increase pool size
      Coordinate.Factory.getCoordinate(1, 1);
      assertEquals(2, Coordinate.Factory.getPoolSize());
    }

    @Test
    @DisplayName("clearPool removes all cached coordinates")
    void clearPool() {
      Coordinate.Factory.getCoordinate(1, 1);
      Coordinate.Factory.getCoordinate(2, 2);
      assertEquals(2, Coordinate.Factory.getPoolSize());

      Coordinate.Factory.clearPool();
      assertEquals(0, Coordinate.Factory.getPoolSize());
    }

    @Test
    @DisplayName("After clearPool, same coordinates create new instances")
    void afterClearPoolNewInstances() {
      Coordinate coord1 = Coordinate.Factory.getCoordinate(5, 5);
      Coordinate.Factory.clearPool();
      Coordinate coord2 = Coordinate.Factory.getCoordinate(5, 5);

      // Both have same values
      assertEquals(coord1.x(), coord2.x());
      assertEquals(coord1.y(), coord2.y());

      // But are different instances
      assertNotSame(coord1, coord2);
    }

    @Test
    @DisplayName("Factory handles negative coordinates")
    void factoryNegativeCoordinates() {
      Coordinate neg1 = Coordinate.Factory.getCoordinate(-10, -20);
      Coordinate neg2 = Coordinate.Factory.getCoordinate(-10, -20);

      assertSame(neg1, neg2);
      assertEquals(-10, neg1.x());
      assertEquals(-20, neg1.y());
    }

    @Test
    @DisplayName("Different sign coordinates are stored separately")
    void differentSignsStoredSeparately() {
      Coordinate positive = Coordinate.Factory.getCoordinate(5, 10);
      Coordinate negative = Coordinate.Factory.getCoordinate(-5, -10);

      assertNotSame(positive, negative);
      assertEquals(2, Coordinate.Factory.getPoolSize());
    }
  }

  @Nested
  @DisplayName("Record equality")
  class EqualityTests {

    @Test
    @DisplayName("Equal coordinates have same hashCode")
    void equalHashCodes() {
      Coordinate coord1 = new Coordinate(10, 20);
      Coordinate coord2 = new Coordinate(10, 20);

      assertEquals(coord1, coord2);
      assertEquals(coord1.hashCode(), coord2.hashCode());
    }

    @Test
    @DisplayName("Different coordinates are not equal")
    void differentCoordinatesNotEqual() {
      Coordinate coord1 = new Coordinate(10, 20);
      Coordinate coord2 = new Coordinate(10, 21);

      assertNotEquals(coord1, coord2);
    }
  }
}
