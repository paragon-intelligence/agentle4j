package com.paragon.responses.spec;

import org.jspecify.annotations.NonNull;

/**
 * A url source used in the search.
 *
 * @param url The URL of the source.
 */
public record UrlSource(@NonNull String url) implements Source {
  @Override
  public @NonNull String toString() {
    return String.format("<url_source>\n%s\n</url_source>", url);
  }
}
