package com.paragon.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.microsoft.playwright.Page;
import org.jspecify.annotations.NonNull;

/**
 * Action to write text into an input field, text area, or contenteditable element.
 *
 * <p>Note: You must first focus the element using a 'click' action before writing. The text will be
 * typed character by character to simulate keyboard input.
 *
 * @param text Text to write into the element
 */
public record WriteText(
    @JsonProperty("text") @JsonPropertyDescription("Text to write into the element.")
        @NonNull String text)
    implements Action {

  /**
   * Creates a WriteText action.
   *
   * @param text The text to write
   * @return A new WriteText instance
   */
  public static WriteText of(@NonNull String text) {
    return new WriteText(text);
  }

  @Override
  public void execute(Page page) {
    page.keyboard().type(text);
  }
}
