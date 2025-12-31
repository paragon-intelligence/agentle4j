package com.paragon.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NonNull;

/**
 * Result of a Scrape action containing the URL and HTML content.
 *
 * @param url  The URL of the scraped page
 * @param html The HTML content of the scraped page
 */
public record ScrapeResult(
    @JsonProperty("url") @NonNull String url,
    @JsonProperty("html") @NonNull String html) {}
