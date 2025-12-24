package com.paragon.responses.spec;

/**
 * Comparison filter boolean value
 *
 * @param value value list.
 */
public record ComparisonFilterBooleanValue(Boolean value) implements ComparisonFilterValue {
  @Override
  public Object getValue() {
    return value;
  }
}
