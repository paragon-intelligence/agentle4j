package com.paragon.responses.spec;

/** The value to compare against the attribute key; supports string, number, or boolean types. */
public sealed interface ComparisonFilterValue
    permits ComparisonFilterStringValue,
        ComparisonFilterNumberValue,
        ComparisonFilterBooleanValue,
        ComparisonFilterArrayValue {
  Object getValue();
}
