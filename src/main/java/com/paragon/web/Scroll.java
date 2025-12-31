package com.paragon.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.microsoft.playwright.Page;
import org.jspecify.annotations.NonNull;

import java.util.Map;

/**
 * Action to scroll the page or a specific element.
 *
 * @param direction The direction to scroll (up, down, left, right)
 * @param amount    The amount to scroll in pixels (0-1000)
 * @param selector  Query selector for the element to scroll
 */
public record Scroll(
    @JsonProperty("direction")
    @JsonPropertyDescription("The direction to scroll.")
    @NonNull ScrollDirection direction,
    @JsonProperty("amount")
    @JsonPropertyDescription("The amount to scroll in pixels.")
    int amount,
    @JsonProperty("selector")
    @JsonPropertyDescription("Query selector for the element to scroll.")
    @NonNull String selector)
    implements Action {

  /**
   * Creates a Scroll action.
   *
   * @param selector  Query selector for the element to scroll
   * @param direction The direction to scroll
   * @param amount    The amount to scroll in pixels (0-1000)
   * @return A new Scroll instance
   */
  public static Scroll of(@NonNull String selector, @NonNull ScrollDirection direction, int amount) {
    validateAmount(amount);
    return new Scroll(direction, amount, selector);
  }

  /**
   * Creates a Scroll action that scrolls down.
   *
   * @param selector Query selector for the element to scroll
   * @param amount   The amount to scroll in pixels
   * @return A new Scroll instance that scrolls down
   */
  public static Scroll down(@NonNull String selector, int amount) {
    return of(selector, ScrollDirection.DOWN, amount);
  }

  /**
   * Creates a Scroll action that scrolls up.
   *
   * @param selector Query selector for the element to scroll
   * @param amount   The amount to scroll in pixels
   * @return A new Scroll instance that scrolls up
   */
  public static Scroll up(@NonNull String selector, int amount) {
    return of(selector, ScrollDirection.UP, amount);
  }

  /**
   * Creates a Scroll action that scrolls left.
   *
   * @param selector Query selector for the element to scroll
   * @param amount   The amount to scroll in pixels
   * @return A new Scroll instance that scrolls left
   */
  public static Scroll left(@NonNull String selector, int amount) {
    return of(selector, ScrollDirection.LEFT, amount);
  }

  /**
   * Creates a Scroll action that scrolls right.
   *
   * @param selector Query selector for the element to scroll
   * @param amount   The amount to scroll in pixels
   * @return A new Scroll instance that scrolls right
   */
  public static Scroll right(@NonNull String selector, int amount) {
    return of(selector, ScrollDirection.RIGHT, amount);
  }

  private static void validateAmount(int amount) {
    if (amount < 0 || amount > 1000) {
      throw new IllegalArgumentException("Amount must be between 0 and 1000");
    }
  }

  @Override
  public void execute(Page page) {
    int deltaX = 0;
    int deltaY = 0;

    switch (direction) {
      case DOWN -> deltaY = amount;
      case UP -> deltaY = -amount;
      case RIGHT -> deltaX = amount;
      case LEFT -> deltaX = -amount;
    }

    page.evaluate("""
        (args) => {
            const element = document.querySelector(args.selector);
            if (element) {
                element.scrollBy(args.deltaX, args.deltaY);
            }
        }
        """, Map.of("selector", selector, "deltaX", deltaX, "deltaY", deltaY));
  }
}
