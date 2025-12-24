package com.paragon.responses.spec;

import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Action type "search" - Performs a web search query.
 *
 * @param query The search query.
 * @param sources The sources used in the search.
 */
public record SearchAction(@NonNull String query, @Nullable List<Source> sources)
    implements WebAction {
  @Override
  public @NonNull String toString() {
    return String.format(
        """
        <web_action:search_action>
            <query>%s</query>
            <sources>%s</sources>
        </web_action:search_action>
        """,
        query, sources != null ? sources : "null");
  }
}
