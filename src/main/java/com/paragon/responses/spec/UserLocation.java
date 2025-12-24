package com.paragon.responses.spec;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** The approximate location of the user. */
public final class UserLocation {
  private final @Nullable String city;
  private final @Nullable String country;
  private final @Nullable String region;
  private final @Nullable String timezone;

  /**
   * @param city Free text input for the city of the user, e.g. {@code San Francisco}.
   * @param country The two-letter <a href="https://en.wikipedia.org/wiki/ISO_3166-1">ISO country
   *     code</a> of the user, e.g. US.
   * @param region Free text input for the region of the user, e.g. {@code California}.
   * @param timezone The <a href="https://timeapi.io/documentation/iana-timezones">IANA timezone</a>
   *     of the user, e.g. {@code America/Los_Angeles}.
   */
  public UserLocation(
      @Nullable String city,
      @Nullable String country,
      @Nullable String region,
      @Nullable String timezone) {
    this.city = city;
    this.country = country;
    this.region = region;
    this.timezone = timezone;
  }

  public @Nullable String city() {
    return city;
  }

  public @Nullable String country() {
    return country;
  }

  public @Nullable String region() {
    return region;
  }

  public @Nullable String timezone() {
    return timezone;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    var that = (UserLocation) obj;
    return Objects.equals(this.city, that.city)
        && Objects.equals(this.country, that.country)
        && Objects.equals(this.region, that.region)
        && Objects.equals(this.timezone, that.timezone);
  }

  @Override
  public int hashCode() {
    return Objects.hash(city, country, region, timezone);
  }

  @Override
  public String toString() {
    return "UserLocation["
        + "city="
        + city
        + ", "
        + "country="
        + country
        + ", "
        + "region="
        + region
        + ", "
        + "timezone="
        + timezone
        + ']';
  }
}
