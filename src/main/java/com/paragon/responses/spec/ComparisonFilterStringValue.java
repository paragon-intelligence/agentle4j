package com.paragon.responses.spec;

/**
 * Comparison filter string value
 *
 * @param value value string.
 */
public record ComparisonFilterStringValue(String value) implements ComparisonFilterValue {
  @Override
  public Object getValue() {
    return this.value;
  }
}
