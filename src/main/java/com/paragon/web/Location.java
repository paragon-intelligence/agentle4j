package com.paragon.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Represents a geographic location for geo-targeting requests.
 *
 * @param country  The country code (e.g., "US", "GB", "BR")
 * @param language Optional language code (e.g., "en", "pt")
 */
public record Location(
    @JsonProperty("country")
    @JsonPropertyDescription("The country code (e.g., 'US', 'GB', 'BR')")
    @NonNull String country,
    @JsonProperty("language")
    @JsonPropertyDescription("The language code (e.g., 'en', 'pt')")
    @Nullable String language) {

  /**
   * Creates a Location with just a country code.
   *
   * @param country The country code
   * @return A new Location instance
   */
  public static Location of(@NonNull String country) {
    return new Location(country, null);
  }

  /**
   * Creates a Location with country and language codes.
   *
   * @param country  The country code
   * @param language The language code
   * @return A new Location instance
   */
  public static Location of(@NonNull String country, @NonNull String language) {
    return new Location(country, language);
  }
}
