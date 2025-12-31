package com.paragon.web;

import com.microsoft.playwright.Page;

/**
 * Action to scrape the current page content.
 *
 * <p>Returns a {@link ScrapeResult} containing the URL and HTML content of the page.
 */
public record Scrape() implements Action {

  /** The default scrape action instance. */
  private static final Scrape INSTANCE = new Scrape();

  /**
   * Returns the default Scrape action.
   *
   * @return The Scrape action instance
   */
  public static Scrape create() {
    return INSTANCE;
  }

  @Override
  public void execute(Page page) {
    // Scrape action - page content is available via page.url() and page.content()
  }
}
