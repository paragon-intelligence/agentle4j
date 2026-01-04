package com.paragon.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;
import java.util.List;
import org.jspecify.annotations.NonNull;

/**
 * Action to click on an element identified by a CSS selector.
 *
 * @param selector Query selector to find the element by
 * @param all If true, clicks all elements matched by the selector, not just the first one. Does not
 *     throw an error if no elements match the selector.
 */
public record Click(
    @JsonProperty("selector") @JsonPropertyDescription("Query selector to find the element by")
        @NonNull String selector,
    @JsonProperty("all")
        @JsonPropertyDescription(
            "Clicks all elements matched by the selector, not just the first one. "
                + "Does not throw an error if no elements match the selector.")
        boolean all)
    implements Action {

  /**
   * Creates a Click action with default values.
   *
   * @param selector Query selector to find the element by
   * @return A new Click instance
   */
  public static Click of(@NonNull String selector) {
    return new Click(selector, false);
  }

  /**
   * Creates a Click action that clicks all matching elements.
   *
   * @param selector Query selector to find the elements by
   * @return A new Click instance that clicks all matches
   */
  public static Click all(@NonNull String selector) {
    return new Click(selector, true);
  }

  @Override
  public void execute(Page page) {
    if (all) {
      List<ElementHandle> elements = page.querySelectorAll(selector);
      for (ElementHandle element : elements) {
        element.click();
      }
    } else {
      page.click(selector);
    }
  }
}
