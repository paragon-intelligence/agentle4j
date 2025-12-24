package com.paragon.responses.spec;

import java.util.List;
import org.jspecify.annotations.NonNull;

/**
 * Combine multiple filters using {@code and} or {@code or}.
 *
 * @param filters Array of filters to combine. Items can be {@link ComparisonFilter} or {@link
 *     CompoundFilter}.
 */
public record CompoundFilter(@NonNull List<ComparisonFilter> filters) implements FileSearchFilter {
  public CompoundFilter {
    filters = List.copyOf(filters);
  }
}
