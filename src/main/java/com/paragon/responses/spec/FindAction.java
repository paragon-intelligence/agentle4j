package com.paragon.responses.spec;

import org.jspecify.annotations.NonNull;

/**
 * Action type "find": Searches for a pattern within a loaded page.
 *
 * @param pattern The pattern or text to search for within the page.
 * @param url The URL of the page searched for the pattern.
 */
public record FindAction(@NonNull String pattern, @NonNull String url) implements WebAction {
  @Override
  public @NonNull String toString() {
    return String.format(
        """
        <web_action:find_action>
            <pattern>%s</pattern>
            <url>%s</url>
        </web_action:find_action>
        """,
        pattern, url);
  }
}
