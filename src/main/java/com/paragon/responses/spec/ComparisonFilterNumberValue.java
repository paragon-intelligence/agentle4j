package com.paragon.responses.spec;

/**
 * Comparison filter number value
 *
 * @param value value number.
 */
public record ComparisonFilterNumberValue(Number value) implements ComparisonFilterValue {
  @Override
  public Object getValue() {
    return value;
  }
}
