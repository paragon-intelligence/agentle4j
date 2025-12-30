package com.paragon.responses.spec;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for filter-related records: ComparisonFilter, CompoundFilter, and ComparisonFilterType. */
class FilterTest {

  @Nested
  @DisplayName("ComparisonFilter record")
  class ComparisonFilterTests {

    @Test
    @DisplayName("ComparisonFilter can be created with string value")
    void createWithStringValue() {
      ComparisonFilterValue value = new ComparisonFilterStringValue("test-value");
      ComparisonFilter filter = new ComparisonFilter("status", ComparisonFilterType.EQ, value);

      assertEquals("status", filter.key());
      assertEquals(ComparisonFilterType.EQ, filter.type());
      assertEquals(value, filter.value());
    }

    @Test
    @DisplayName("ComparisonFilter can be created with number value")
    void createWithNumberValue() {
      ComparisonFilterValue value = new ComparisonFilterNumberValue(42);
      ComparisonFilter filter = new ComparisonFilter("age", ComparisonFilterType.GT, value);

      assertEquals("age", filter.key());
      assertEquals(ComparisonFilterType.GT, filter.type());
      assertEquals(value, filter.value());
    }

    @Test
    @DisplayName("ComparisonFilter can be created with boolean value")
    void createWithBooleanValue() {
      ComparisonFilterValue value = new ComparisonFilterBooleanValue(true);
      ComparisonFilter filter = new ComparisonFilter("active", ComparisonFilterType.EQ, value);

      assertEquals("active", filter.key());
      assertEquals(ComparisonFilterType.EQ, filter.type());
      assertEquals(value, filter.value());
    }

    @Test
    @DisplayName("ComparisonFilter implements FileSearchFilter")
    void implementsFileSearchFilter() {
      ComparisonFilter filter =
          new ComparisonFilter(
              "key", ComparisonFilterType.NE, new ComparisonFilterStringValue("value"));
      assertTrue(filter instanceof FileSearchFilter);
    }

    @Test
    @DisplayName("Equal filters are equal")
    void equality() {
      ComparisonFilterValue value = new ComparisonFilterStringValue("test");
      ComparisonFilter filter1 = new ComparisonFilter("key", ComparisonFilterType.EQ, value);
      ComparisonFilter filter2 = new ComparisonFilter("key", ComparisonFilterType.EQ, value);

      assertEquals(filter1, filter2);
      assertEquals(filter1.hashCode(), filter2.hashCode());
    }
  }

  @Nested
  @DisplayName("CompoundFilter record")
  class CompoundFilterTests {

    @Test
    @DisplayName("CompoundFilter can be created with list of filters")
    void createWithFilters() {
      ComparisonFilter filter1 =
          new ComparisonFilter(
              "status", ComparisonFilterType.EQ, new ComparisonFilterStringValue("active"));
      ComparisonFilter filter2 =
          new ComparisonFilter(
              "age", ComparisonFilterType.GTE, new ComparisonFilterNumberValue(18));

      CompoundFilter compound = new CompoundFilter(List.of(filter1, filter2));

      assertEquals(2, compound.filters().size());
      assertEquals(filter1, compound.filters().get(0));
      assertEquals(filter2, compound.filters().get(1));
    }

    @Test
    @DisplayName("CompoundFilter creates defensive copy of list")
    void defensiveCopy() {
      ComparisonFilter filter =
          new ComparisonFilter(
              "key", ComparisonFilterType.EQ, new ComparisonFilterStringValue("value"));
      List<ComparisonFilter> originalList = new java.util.ArrayList<>();
      originalList.add(filter);

      CompoundFilter compound = new CompoundFilter(originalList);

      // Modifying original list should not affect the compound filter
      originalList.clear();

      assertEquals(1, compound.filters().size());
    }

    @Test
    @DisplayName("CompoundFilter filters list is immutable")
    void immutableFilters() {
      ComparisonFilter filter =
          new ComparisonFilter(
              "key", ComparisonFilterType.EQ, new ComparisonFilterStringValue("value"));

      CompoundFilter compound = new CompoundFilter(List.of(filter));

      assertThrows(UnsupportedOperationException.class, () -> compound.filters().add(filter));
    }

    @Test
    @DisplayName("CompoundFilter implements FileSearchFilter")
    void implementsFileSearchFilter() {
      CompoundFilter compound = new CompoundFilter(List.of());
      assertTrue(compound instanceof FileSearchFilter);
    }

    @Test
    @DisplayName("Equal compound filters are equal")
    void equality() {
      ComparisonFilter filter =
          new ComparisonFilter(
              "key", ComparisonFilterType.IN, new ComparisonFilterStringValue("value"));

      CompoundFilter compound1 = new CompoundFilter(List.of(filter));
      CompoundFilter compound2 = new CompoundFilter(List.of(filter));

      assertEquals(compound1, compound2);
      assertEquals(compound1.hashCode(), compound2.hashCode());
    }
  }

  @Nested
  @DisplayName("ComparisonFilterType enum")
  class ComparisonFilterTypeTests {

    @Test
    @DisplayName("All comparison types exist")
    void allTypesExist() {
      assertEquals(8, ComparisonFilterType.values().length);

      assertNotNull(ComparisonFilterType.valueOf("EQ"));
      assertNotNull(ComparisonFilterType.valueOf("NE"));
      assertNotNull(ComparisonFilterType.valueOf("GT"));
      assertNotNull(ComparisonFilterType.valueOf("GTE"));
      assertNotNull(ComparisonFilterType.valueOf("LT"));
      assertNotNull(ComparisonFilterType.valueOf("LTE"));
      assertNotNull(ComparisonFilterType.valueOf("IN"));
      assertNotNull(ComparisonFilterType.valueOf("NIN"));
    }

    @Test
    @DisplayName("Filter types can be used in filters")
    void typesUsableInFilters() {
      ComparisonFilterValue value = new ComparisonFilterNumberValue(100);

      for (ComparisonFilterType type : ComparisonFilterType.values()) {
        ComparisonFilter filter = new ComparisonFilter("amount", type, value);
        assertEquals(type, filter.type());
      }
    }
  }

  @Nested
  @DisplayName("ComparisonFilterValue implementations")
  class ComparisonFilterValueTests {

    @Test
    @DisplayName("ComparisonFilterStringValue stores string")
    void stringValue() {
      ComparisonFilterStringValue value = new ComparisonFilterStringValue("hello");
      assertEquals("hello", value.value());
    }

    @Test
    @DisplayName("ComparisonFilterNumberValue stores number")
    void numberValue() {
      ComparisonFilterNumberValue value = new ComparisonFilterNumberValue(3.14);
      assertEquals(3.14, value.value().doubleValue());
    }

    @Test
    @DisplayName("ComparisonFilterBooleanValue stores boolean")
    void booleanValue() {
      ComparisonFilterBooleanValue trueValue = new ComparisonFilterBooleanValue(true);
      ComparisonFilterBooleanValue falseValue = new ComparisonFilterBooleanValue(false);

      assertTrue(trueValue.value());
      assertFalse(falseValue.value());
    }

    @Test
    @DisplayName("All value types implement ComparisonFilterValue")
    void allImplementInterface() {
      assertTrue(new ComparisonFilterStringValue("test") instanceof ComparisonFilterValue);
      assertTrue(new ComparisonFilterNumberValue(42) instanceof ComparisonFilterValue);
      assertTrue(new ComparisonFilterBooleanValue(true) instanceof ComparisonFilterValue);
    }
  }
}
