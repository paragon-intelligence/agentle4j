package com.paragon.responses.spec;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for UserLocation DTO (19 missed lines).
 */
@DisplayName("UserLocation DTO")
class UserLocationTest {

  @Nested
  @DisplayName("Constructor and Accessors")
  class ConstructorAndAccessors {

    @Test
    @DisplayName("constructor creates instance with all fields")
    void constructorCreatesInstance() {
      UserLocation loc = new UserLocation(
          "San Francisco",
          "US",
          "California",
          "America/Los_Angeles"
      );

      assertEquals("San Francisco", loc.city());
      assertEquals("US", loc.country());
      assertEquals("California", loc.region());
      assertEquals("America/Los_Angeles", loc.timezone());
    }

    @Test
    @DisplayName("city returns value")
    void cityReturnsValue() {
      UserLocation loc = createUserLocation();
      assertEquals("San Francisco", loc.city());
    }

    @Test
    @DisplayName("country returns value")
    void countryReturnsValue() {
      UserLocation loc = createUserLocation();
      assertEquals("US", loc.country());
    }

    @Test
    @DisplayName("region returns value")
    void regionReturnsValue() {
      UserLocation loc = createUserLocation();
      assertEquals("California", loc.region());
    }

    @Test
    @DisplayName("timezone returns value")
    void timezoneReturnsValue() {
      UserLocation loc = createUserLocation();
      assertEquals("America/Los_Angeles", loc.timezone());
    }

    @Test
    @DisplayName("all fields can be null")
    void allFieldsCanBeNull() {
      UserLocation loc = new UserLocation(null, null, null, null);
      assertNull(loc.city());
      assertNull(loc.country());
      assertNull(loc.region());
      assertNull(loc.timezone());
    }
  }

  @Nested
  @DisplayName("Equals and HashCode")
  class EqualsAndHashCode {

    @Test
    @DisplayName("equals returns true for same values")
    void equalsReturnsTrueForSame() {
      UserLocation loc1 = createUserLocation();
      UserLocation loc2 = createUserLocation();
      assertEquals(loc1, loc2);
    }

    @Test
    @DisplayName("equals returns false for different city")
    void equalsReturnsFalseForDifferentCity() {
      UserLocation loc1 = createUserLocation();
      UserLocation loc2 = new UserLocation("New York", "US", "California", "America/Los_Angeles");
      assertNotEquals(loc1, loc2);
    }

    @Test
    @DisplayName("hashCode is consistent")
    void hashCodeConsistent() {
      UserLocation loc1 = createUserLocation();
      UserLocation loc2 = createUserLocation();
      assertEquals(loc1.hashCode(), loc2.hashCode());
    }

    @Test
    @DisplayName("equals returns true for same instance")
    void equalsReturnsTrueForSameInstance() {
      UserLocation loc = createUserLocation();
      assertEquals(loc, loc);
    }

    @Test
    @DisplayName("equals returns false for null")
    void equalsReturnsFalseForNull() {
      UserLocation loc = createUserLocation();
      assertNotEquals(loc, null);
    }

    @Test
    @DisplayName("equals returns false for different class")
    void equalsReturnsFalseForDifferentClass() {
      UserLocation loc = createUserLocation();
      assertNotEquals(loc, "string");
    }
  }

  @Nested
  @DisplayName("ToString")
  class ToStringTests {

    @Test
    @DisplayName("toString contains all fields")
    void toStringContainsAllFields() {
      UserLocation loc = createUserLocation();
      String str = loc.toString();
      assertTrue(str.contains("San Francisco"));
      assertTrue(str.contains("US"));
      assertTrue(str.contains("California"));
      assertTrue(str.contains("America/Los_Angeles"));
    }

    @Test
    @DisplayName("toString contains class name")
    void toStringContainsClassName() {
      UserLocation loc = createUserLocation();
      String str = loc.toString();
      assertTrue(str.contains("UserLocation"));
    }
  }

  private UserLocation createUserLocation() {
    return new UserLocation(
        "San Francisco",
        "US",
        "California",
        "America/Los_Angeles"
    );
  }
}
