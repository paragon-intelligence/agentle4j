package com.paragon.responses.spec;

import java.util.List;

/**
 * Comparison filter array value
 *
 * @param value value list.
 */
public record ComparisonFilterArrayValue(List<?> value) implements ComparisonFilterValue {
  public ComparisonFilterArrayValue {
    value = List.copyOf(value);
  }

  @Override
  public Object getValue() {
    return value;
  }
}
