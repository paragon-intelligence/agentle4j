package com.paragon.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.microsoft.playwright.Page;
import org.jspecify.annotations.NonNull;

/**
 * Action to execute JavaScript code on the current page.
 *
 * @param script The JavaScript code to execute
 */
public record ExecuteJavascript(
    @JsonProperty("script") @JsonPropertyDescription("The JavaScript code to execute.")
        @NonNull String script)
    implements Action {

  /**
   * Creates an ExecuteJavascript action.
   *
   * @param script The JavaScript code to execute
   * @return A new ExecuteJavascript instance
   */
  public static ExecuteJavascript of(@NonNull String script) {
    return new ExecuteJavascript(script);
  }

  @Override
  public void execute(Page page) {
    page.evaluate(script);
  }
}
