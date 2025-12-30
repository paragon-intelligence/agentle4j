package com.paragon.parsing;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public record MarkdownResult(
    @JsonPropertyDescription(
            "A list of markdown representations of each page of the document, if more than one"
                + " page.")
        @NonNull List<String> markdowns) {
  @NotNull
  @Override
  public String toString() {
    var sb = new StringBuilder();
    for (String markdown : markdowns) {
      sb.append(markdown);
    }
    return sb.toString();
  }
}
