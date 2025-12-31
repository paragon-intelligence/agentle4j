package com.paragon.web;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.microsoft.playwright.Page;

/**
 * Base sealed interface for all web actions that can be performed on a page.
 *
 * <p>Actions are polymorphically serialized using the "type" property as the discriminator.
 * Each action type can be deserialized from JSON based on its type value.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = Click.class, name = "click"),
  @JsonSubTypes.Type(value = ExecuteJavascript.class, name = "execute_javascript"),
  @JsonSubTypes.Type(value = GeneratePdf.class, name = "pdf"),
  @JsonSubTypes.Type(value = PressAKey.class, name = "press"),
  @JsonSubTypes.Type(value = Scrape.class, name = "scrape"),
  @JsonSubTypes.Type(value = Screenshot.class, name = "screenshot"),
  @JsonSubTypes.Type(value = Scroll.class, name = "scroll"),
  @JsonSubTypes.Type(value = Wait.class, name = "wait"),
  @JsonSubTypes.Type(value = WriteText.class, name = "write")
})
public sealed interface Action
    permits Click, ExecuteJavascript, GeneratePdf, PressAKey, Scrape, Screenshot, Scroll, Wait, WriteText {

  /**
   * Executes this action on the given Playwright page.
   *
   * @param page The Playwright page to execute the action on
   */
  void execute(Page page);
}
