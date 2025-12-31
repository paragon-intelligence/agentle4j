package com.paragon.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.microsoft.playwright.Page;
import org.jspecify.annotations.NonNull;

/**
 * Action to wait for an element to be visible.
 *
 * @param milliseconds Number of milliseconds to wait (timeout)
 * @param selector     Query selector to find the element by
 */
public record Wait(
    @JsonProperty("milliseconds")
    @JsonPropertyDescription("Number of milliseconds to wait")
    int milliseconds,
    @JsonProperty("selector")
    @JsonPropertyDescription("Query selector to find the element by")
    @NonNull String selector)
    implements Action {

  /**
   * Creates a Wait action.
   *
   * @param selector     Query selector for the element to wait for
   * @param milliseconds Timeout in milliseconds
   * @return A new Wait instance
   */
  public static Wait forSelector(@NonNull String selector, int milliseconds) {
    return new Wait(milliseconds, selector);
  }

  /**
   * Creates a Wait action with a default timeout of 30 seconds.
   *
   * @param selector Query selector for the element to wait for
   * @return A new Wait instance with 30 second timeout
   */
  public static Wait forSelector(@NonNull String selector) {
    return forSelector(selector, 30000);
  }

  @Override
  public void execute(Page page) {
    page.waitForSelector(selector, new Page.WaitForSelectorOptions().setTimeout(milliseconds));
  }
}
