package com.paragon.responses.spec;

import org.jspecify.annotations.NonNull;

/**
 * Action type "open_page" - Opens a specific URL from search results.
 *
 * @param url The URL opened by the model.s
 */
public record OpenPageAction(@NonNull String url) implements WebAction {
  @Override
  public @NonNull String toString() {
    return String.format(
        """
        <web_action:open_page_action>
            <url>%s</url>
        </web_action:open_page_action>
        """,
        url);
  }
}
