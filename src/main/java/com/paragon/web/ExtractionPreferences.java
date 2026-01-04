package com.paragon.web;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Configuration options for web content extraction. Use the builder pattern to construct instances.
 */
public final class ExtractionPreferences {

  private final boolean mobile;
  private final boolean blockAds;
  private final boolean removeBase64Images;
  private final boolean onlyMainContent;
  private final boolean skipTlsVerification;
  private final int timeoutMs;
  private final int waitForMs;
  private final @Nullable GeoLocation location;
  private final @Nullable Map<String, String> headers;
  private final @Nullable List<String> includeTags;
  private final @Nullable List<String> excludeTags;
  private final @Nullable ProxyMode proxy;

  private ExtractionPreferences(Builder builder) {
    this.mobile = builder.mobile;
    this.blockAds = builder.blockAds;
    this.removeBase64Images = builder.removeBase64Images;
    this.onlyMainContent = builder.onlyMainContent;
    this.skipTlsVerification = builder.skipTlsVerification;
    this.timeoutMs = builder.timeoutMs;
    this.waitForMs = builder.waitForMs;
    this.location = builder.location;
    this.headers = builder.headers;
    this.includeTags = builder.includeTags;
    this.excludeTags = builder.excludeTags;
    this.proxy = builder.proxy;
  }

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull ExtractionPreferences defaults() {
    return builder().build();
  }

  // Getters
  public boolean mobile() {
    return mobile;
  }

  public boolean blockAds() {
    return blockAds;
  }

  public boolean removeBase64Images() {
    return removeBase64Images;
  }

  public boolean onlyMainContent() {
    return onlyMainContent;
  }

  public boolean skipTlsVerification() {
    return skipTlsVerification;
  }

  public int timeoutMs() {
    return timeoutMs;
  }

  public int waitForMs() {
    return waitForMs;
  }

  public @Nullable GeoLocation location() {
    return location;
  }

  public @Nullable Map<String, String> headers() {
    return headers;
  }

  public @Nullable List<String> includeTags() {
    return includeTags;
  }

  public @Nullable List<String> excludeTags() {
    return excludeTags;
  }

  public @Nullable ProxyMode proxy() {
    return proxy;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ExtractionPreferences that)) return false;
    return mobile == that.mobile
        && blockAds == that.blockAds
        && removeBase64Images == that.removeBase64Images
        && onlyMainContent == that.onlyMainContent
        && skipTlsVerification == that.skipTlsVerification
        && timeoutMs == that.timeoutMs
        && waitForMs == that.waitForMs
        && Objects.equals(location, that.location)
        && Objects.equals(headers, that.headers)
        && Objects.equals(includeTags, that.includeTags)
        && Objects.equals(excludeTags, that.excludeTags)
        && proxy == that.proxy;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        mobile,
        blockAds,
        removeBase64Images,
        onlyMainContent,
        skipTlsVerification,
        timeoutMs,
        waitForMs,
        location,
        headers,
        includeTags,
        excludeTags,
        proxy);
  }

  @Override
  public String toString() {
    return "ExtractionPreferences{"
        + "mobile="
        + mobile
        + ", blockAds="
        + blockAds
        + ", removeBase64Images="
        + removeBase64Images
        + ", onlyMainContent="
        + onlyMainContent
        + ", skipTlsVerification="
        + skipTlsVerification
        + ", timeoutMs="
        + timeoutMs
        + ", waitForMs="
        + waitForMs
        + ", location="
        + location
        + ", headers="
        + headers
        + ", includeTags="
        + includeTags
        + ", excludeTags="
        + excludeTags
        + ", proxy="
        + proxy
        + '}';
  }

  public enum ProxyMode {
    BASIC,
    STEALTH
  }

  public record GeoLocation(double latitude, double longitude) {}

  public static final class Builder {
    private boolean mobile = false;
    private boolean blockAds = false;
    private boolean removeBase64Images = false;
    private boolean onlyMainContent = false;
    private boolean skipTlsVerification = false;
    private int timeoutMs = 30_000;
    private int waitForMs = 0;
    private @Nullable GeoLocation location;
    private @Nullable Map<String, String> headers;
    private @Nullable List<String> includeTags;
    private @Nullable List<String> excludeTags;
    private @Nullable ProxyMode proxy;

    private Builder() {}

    public @NonNull Builder mobile(boolean mobile) {
      this.mobile = mobile;
      return this;
    }

    public @NonNull Builder blockAds(boolean blockAds) {
      this.blockAds = blockAds;
      return this;
    }

    public @NonNull Builder removeBase64Images(boolean removeBase64Images) {
      this.removeBase64Images = removeBase64Images;
      return this;
    }

    public @NonNull Builder onlyMainContent(boolean onlyMainContent) {
      this.onlyMainContent = onlyMainContent;
      return this;
    }

    public @NonNull Builder skipTlsVerification(boolean skipTlsVerification) {
      this.skipTlsVerification = skipTlsVerification;
      return this;
    }

    public @NonNull Builder timeoutMs(int timeoutMs) {
      if (timeoutMs < 0) {
        throw new IllegalArgumentException("timeoutMs must be non-negative");
      }
      this.timeoutMs = timeoutMs;
      return this;
    }

    public @NonNull Builder waitForMs(int waitForMs) {
      if (waitForMs < 0) {
        throw new IllegalArgumentException("waitForMs must be non-negative");
      }
      this.waitForMs = waitForMs;
      return this;
    }

    public @NonNull Builder location(@Nullable GeoLocation location) {
      this.location = location;
      return this;
    }

    public @NonNull Builder location(double latitude, double longitude) {
      this.location = new GeoLocation(latitude, longitude);
      return this;
    }

    public @NonNull Builder headers(@Nullable Map<String, String> headers) {
      this.headers = headers;
      return this;
    }

    public @NonNull Builder includeTags(@Nullable List<String> includeTags) {
      this.includeTags = includeTags;
      return this;
    }

    public @NonNull Builder excludeTags(@Nullable List<String> excludeTags) {
      this.excludeTags = excludeTags;
      return this;
    }

    public @NonNull Builder proxy(@Nullable ProxyMode proxy) {
      this.proxy = proxy;
      return this;
    }

    public @NonNull ExtractionPreferences build() {
      return new ExtractionPreferences(this);
    }
  }
}
