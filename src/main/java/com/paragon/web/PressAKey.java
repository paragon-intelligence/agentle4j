package com.paragon.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.microsoft.playwright.Page;
import org.jspecify.annotations.NonNull;

/**
 * Action to press a key on the keyboard.
 *
 * @param key The key to press (e.g., "Enter", "Space", "ArrowUp", "ArrowDown", "ArrowLeft",
 *     "ArrowRight")
 */
public record PressAKey(
    @JsonProperty("key") @JsonPropertyDescription("The key to press.") @NonNull String key)
    implements Action {

  /**
   * Creates a PressAKey action.
   *
   * @param key The key to press
   * @return A new PressAKey instance
   */
  public static PressAKey of(@NonNull String key) {
    return new PressAKey(key);
  }

  /** Presses the Enter key. */
  public static PressAKey enter() {
    return of("Enter");
  }

  /** Presses the Space key. */
  public static PressAKey space() {
    return of("Space");
  }

  /** Presses the Tab key. */
  public static PressAKey tab() {
    return of("Tab");
  }

  /** Presses the Escape key. */
  public static PressAKey escape() {
    return of("Escape");
  }

  /** Presses the Backspace key. */
  public static PressAKey backspace() {
    return of("Backspace");
  }

  /** Presses the ArrowUp key. */
  public static PressAKey arrowUp() {
    return of("ArrowUp");
  }

  /** Presses the ArrowDown key. */
  public static PressAKey arrowDown() {
    return of("ArrowDown");
  }

  /** Presses the ArrowLeft key. */
  public static PressAKey arrowLeft() {
    return of("ArrowLeft");
  }

  /** Presses the ArrowRight key. */
  public static PressAKey arrowRight() {
    return of("ArrowRight");
  }

  @Override
  public void execute(Page page) {
    page.keyboard().press(key);
  }
}
