package com.paragon.responses.spec;

/**
 * Specifies the comparison operator: {@code eq}, {@code ne}, {@code gt}, {@code gte},{@code lt},
 * {@code lte},{@code in}, {@code nin}.
 *
 * <ul>
 *   <li>{@code eq}: equals
 *   <li>{@code ne}: not equal
 *   <li>{@code gt}: greater than
 *   <li>{@code gte}: greater than or equal
 *   <li>{@code lt}: less than
 *   <li>{@code lte}: less than or equal
 *   <li>{@code in}: in
 *   <li>{@code nin}: not in
 * </ul>
 */
public enum ComparisonFilterType {
  EQ,
  NE,
  GT,
  GTE,
  LT,
  LTE,
  IN,
  NIN
}
