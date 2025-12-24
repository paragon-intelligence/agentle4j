package com.paragon.responses.spec;

import org.jspecify.annotations.NonNull;

/**
 * A filter used to compare a specified attribute key to a given value using a defined comparison
 * operation.
 *
 * @param key The key to compare against the value.
 * @param type Specifies the comparison operator: {@code eq}, {@code ne}, {@code gt}, {@code
 *     gte},{@code lt}, {@code lte},{@code in}, {@code nin}.
 *     <ul>
 *       <li>{eq}: equals
 *       <li>{ne}: not equal
 *       <li>{gt}: greater than
 *       <li>{gte}: greater than or equal
 *       <li>{lt}: less than
 *       <li>{lte}: less than or equal
 *       <li>{in}: in
 *       <li>{nin}: not in
 *     </ul>
 *
 * @param value The value to compare against the attribute key; supports string, number, or boolean
 *     types.
 */
public record ComparisonFilter(
    @NonNull String key, @NonNull ComparisonFilterType type, @NonNull ComparisonFilterValue value)
    implements FileSearchFilter {}
