package com.paragon.telemetry.otel;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for OtelAttributeValue.
 * 
 * Tests cover:
 * - Factory methods (ofString, ofBool, ofInt, ofDouble, of)
 * - Type inference in of() method
 * - Record accessors
 * - Edge cases
 */
@DisplayName("OtelAttributeValue")
class OtelAttributeValueTest {

  // ═══════════════════════════════════════════════════════════════════════════
  // FACTORY METHOD TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Factory Methods")
  class FactoryMethodTests {

    @Test
    @DisplayName("ofString creates string value")
    void ofStringCreatesStringValue() {
      OtelAttributeValue value = OtelAttributeValue.ofString("test");

      assertEquals("test", value.stringValue());
      assertNull(value.boolValue());
      assertNull(value.intValue());
      assertNull(value.doubleValue());
    }

    @Test
    @DisplayName("ofBool creates boolean value with true")
    void ofBoolCreatesBooleanValueTrue() {
      OtelAttributeValue value = OtelAttributeValue.ofBool(true);

      assertTrue(value.boolValue());
      assertNull(value.stringValue());
      assertNull(value.intValue());
      assertNull(value.doubleValue());
    }

    @Test
    @DisplayName("ofBool creates boolean value with false")
    void ofBoolCreatesBooleanValueFalse() {
      OtelAttributeValue value = OtelAttributeValue.ofBool(false);

      assertFalse(value.boolValue());
    }

    @Test
    @DisplayName("ofInt creates integer value")
    void ofIntCreatesIntegerValue() {
      OtelAttributeValue value = OtelAttributeValue.ofInt(42L);

      assertEquals(42L, value.intValue());
      assertNull(value.stringValue());
      assertNull(value.boolValue());
      assertNull(value.doubleValue());
    }

    @Test
    @DisplayName("ofDouble creates double value")
    void ofDoubleCreatesDoubleValue() {
      OtelAttributeValue value = OtelAttributeValue.ofDouble(3.14);

      assertEquals(3.14, value.doubleValue());
      assertNull(value.stringValue());
      assertNull(value.boolValue());
      assertNull(value.intValue());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // TYPE INFERENCE TESTS - of() method
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Type Inference (of method)")
  class TypeInferenceTests {

    @Test
    @DisplayName("of(null) returns empty string value")
    void ofNullReturnsEmptyStringValue() {
      OtelAttributeValue value = OtelAttributeValue.of(null);

      assertEquals("", value.stringValue());
    }

    @Test
    @DisplayName("of(String) returns string value")
    void ofStringReturnsStringValue() {
      OtelAttributeValue value = OtelAttributeValue.of("hello");

      assertEquals("hello", value.stringValue());
    }

    @Test
    @DisplayName("of(Boolean true) returns bool value")
    void ofBooleanTrueReturnsBoolValue() {
      OtelAttributeValue value = OtelAttributeValue.of(Boolean.TRUE);

      assertTrue(value.boolValue());
    }

    @Test
    @DisplayName("of(Boolean false) returns bool value")
    void ofBooleanFalseReturnsBoolValue() {
      OtelAttributeValue value = OtelAttributeValue.of(Boolean.FALSE);

      assertFalse(value.boolValue());
    }

    @Test
    @DisplayName("of(Long) returns int value")
    void ofLongReturnsIntValue() {
      OtelAttributeValue value = OtelAttributeValue.of(100L);

      assertEquals(100L, value.intValue());
    }

    @Test
    @DisplayName("of(Integer) converts to int value")
    void ofIntegerConvertsToIntValue() {
      OtelAttributeValue value = OtelAttributeValue.of(Integer.valueOf(50));

      assertEquals(50L, value.intValue());
    }

    @Test
    @DisplayName("of(Double) returns double value")
    void ofDoubleReturnsDoubleValue() {
      OtelAttributeValue value = OtelAttributeValue.of(Double.valueOf(2.71));

      assertEquals(2.71, value.doubleValue());
    }

    @Test
    @DisplayName("of(Float) converts to double value")
    void ofFloatConvertsToDoubleValue() {
      OtelAttributeValue value = OtelAttributeValue.of(Float.valueOf(1.5f));

      assertEquals(1.5, value.doubleValue(), 0.001);
    }

    @Test
    @DisplayName("of(Number - Short) converts to double value")
    void ofShortConvertsToDoubleValue() {
      OtelAttributeValue value = OtelAttributeValue.of(Short.valueOf((short) 10));

      assertEquals(10.0, value.doubleValue());
    }

    @Test
    @DisplayName("of(arbitrary object) converts to string value")
    void ofObjectConvertsToStringValue() {
      Object customObj = new Object() {
        @Override
        public String toString() {
          return "custom-to-string";
        }
      };
      OtelAttributeValue value = OtelAttributeValue.of(customObj);

      assertEquals("custom-to-string", value.stringValue());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ACCESSOR TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Accessors")
  class AccessorTests {

    @Test
    @DisplayName("stringValue returns null when not set")
    void stringValueReturnsNullWhenNotSet() {
      OtelAttributeValue value = OtelAttributeValue.ofBool(true);
      assertNull(value.stringValue());
    }

    @Test
    @DisplayName("boolValue returns null when not set")
    void boolValueReturnsNullWhenNotSet() {
      OtelAttributeValue value = OtelAttributeValue.ofString("test");
      assertNull(value.boolValue());
    }

    @Test
    @DisplayName("intValue returns null when not set")
    void intValueReturnsNullWhenNotSet() {
      OtelAttributeValue value = OtelAttributeValue.ofString("test");
      assertNull(value.intValue());
    }

    @Test
    @DisplayName("doubleValue returns null when not set")
    void doubleValueReturnsNullWhenNotSet() {
      OtelAttributeValue value = OtelAttributeValue.ofString("test");
      assertNull(value.doubleValue());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // EQUALITY TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Equality")
  class EqualityTests {

    @Test
    @DisplayName("equal values are equal")
    void equalValuesAreEqual() {
      OtelAttributeValue v1 = OtelAttributeValue.ofString("test");
      OtelAttributeValue v2 = OtelAttributeValue.ofString("test");

      assertEquals(v1, v2);
      assertEquals(v1.hashCode(), v2.hashCode());
    }

    @Test
    @DisplayName("different types are not equal")
    void differentTypesAreNotEqual() {
      OtelAttributeValue v1 = OtelAttributeValue.ofString("42");
      OtelAttributeValue v2 = OtelAttributeValue.ofInt(42);

      assertNotEquals(v1, v2);
    }

    @Test
    @DisplayName("different values are not equal")
    void differentValuesAreNotEqual() {
      OtelAttributeValue v1 = OtelAttributeValue.ofString("a");
      OtelAttributeValue v2 = OtelAttributeValue.ofString("b");

      assertNotEquals(v1, v2);
    }
  }
}
