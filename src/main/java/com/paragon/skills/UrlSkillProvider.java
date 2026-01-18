package com.paragon.skills;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Loads skills from remote URLs.
 *
 * <p>This provider fetches SKILL.md files via HTTP/HTTPS. It supports both
 * direct URL loading and base URL + skill ID patterns.
 *
 * <h2>Security Warning</h2>
 *
 * <p><b>Only load skills from trusted sources.</b> Remote skills can contain
 * instructions that may execute tools or access data in unexpected ways.
 *
 * <h2>Usage Examples</h2>
 *
 * <pre>{@code
 * // Load from a direct URL
 * UrlSkillProvider provider = UrlSkillProvider.builder().build();
 * Skill skill = provider.loadFromUrl(
 *     URI.create("https://example.com/skills/pdf-processor/SKILL.md")
 * );
 *
 * // With base URL pattern
 * UrlSkillProvider provider = UrlSkillProvider.builder()
 *     .baseUrl("https://example.com/skills")
 *     .build();
 * Skill skill = provider.provide("pdf-processor");
 * // Fetches: https://example.com/skills/pdf-processor/SKILL.md
 * }</pre>
 *
 * @see SkillProvider
 * @since 1.0
 */
public final class UrlSkillProvider implements SkillProvider {

  private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

  private final @NonNull HttpClient httpClient;
  private final @NonNull SkillMarkdownParser parser;
  private final @Nullable String baseUrl;
  private final @NonNull Duration timeout;
  private final Map<String, Skill> cache = new ConcurrentHashMap<>();
  private final boolean cacheEnabled;

  private UrlSkillProvider(Builder builder) {
    this.httpClient = builder.httpClient != null 
        ? builder.httpClient 
        : HttpClient.newBuilder()
            .connectTimeout(builder.timeout)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    this.parser = new SkillMarkdownParser();
    this.baseUrl = builder.baseUrl;
    this.timeout = builder.timeout;
    this.cacheEnabled = builder.cacheEnabled;
  }

  /**
   * Creates a new builder for UrlSkillProvider.
   *
   * @return a new builder
   */
  public static @NonNull Builder builder() {
    return new Builder();
  }

  /**
   * Loads a skill from a direct URL.
   *
   * @param url the URL to the SKILL.md file
   * @return the parsed skill
   * @throws SkillProviderException if loading fails
   */
  public @NonNull Skill loadFromUrl(@NonNull URI url) {
    Objects.requireNonNull(url, "url cannot be null");

    String urlString = url.toString();

    // Check cache
    if (cacheEnabled && cache.containsKey(urlString)) {
      return cache.get(urlString);
    }

    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(url)
          .timeout(timeout)
          .header("Accept", "text/markdown, text/plain, */*")
          .GET()
          .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != 200) {
        throw new SkillProviderException(
            "Failed to fetch skill from URL: " + url + " (status: " + response.statusCode() + ")");
      }

      String content = response.body();
      if (content == null || content.isBlank()) {
        throw new SkillProviderException(url.toString(), "Empty response from URL");
      }

      Skill skill = parser.parse(content);

      // Cache the result
      if (cacheEnabled) {
        cache.put(urlString, skill);
      }

      return skill;
    } catch (IOException e) {
      throw new SkillProviderException("Network error fetching skill: " + url, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new SkillProviderException("Request interrupted: " + url, e);
    }
  }

  /**
   * Loads a skill from a URL string.
   *
   * @param url the URL string to the SKILL.md file
   * @return the parsed skill
   * @throws SkillProviderException if loading fails
   */
  public @NonNull Skill loadFromUrl(@NonNull String url) {
    return loadFromUrl(URI.create(url));
  }

  @Override
  public @NonNull Skill provide(@NonNull String skillId, @Nullable Map<String, String> filters) {
    Objects.requireNonNull(skillId, "skillId cannot be null");

    if (baseUrl == null) {
      // Treat skillId as a full URL
      return loadFromUrl(URI.create(skillId));
    }

    // Build URL from base + skillId
    String normalizedBase = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    String url = normalizedBase + skillId + "/SKILL.md";

    return loadFromUrl(URI.create(url));
  }

  @Override
  public boolean exists(@NonNull String skillId) {
    Objects.requireNonNull(skillId, "skillId cannot be null");

    try {
      provide(skillId);
      return true;
    } catch (SkillProviderException e) {
      return false;
    }
  }

  @Override
  public @NonNull Set<String> listSkillIds() {
    // URL providers cannot enumerate remote skills
    throw new UnsupportedOperationException(
        "UrlSkillProvider does not support listing skill IDs. "
        + "Use exists() or provide() with known skill IDs.");
  }

  /**
   * Clears the skill cache.
   */
  public void clearCache() {
    cache.clear();
  }

  /** Builder for UrlSkillProvider. */
  public static final class Builder {
    private @Nullable HttpClient httpClient;
    private @Nullable String baseUrl;
    private Duration timeout = DEFAULT_TIMEOUT;
    private boolean cacheEnabled = true;

    private Builder() {}

    /**
     * Sets a custom HttpClient.
     *
     * @param httpClient the HTTP client to use
     * @return this builder
     */
    public @NonNull Builder httpClient(@NonNull HttpClient httpClient) {
      this.httpClient = Objects.requireNonNull(httpClient);
      return this;
    }

    /**
     * Sets the base URL for skill lookups.
     *
     * <p>When set, skill IDs are resolved as: {baseUrl}/{skillId}/SKILL.md
     *
     * @param baseUrl the base URL
     * @return this builder
     */
    public @NonNull Builder baseUrl(@NonNull String baseUrl) {
      this.baseUrl = Objects.requireNonNull(baseUrl);
      return this;
    }

    /**
     * Sets the request timeout.
     *
     * <p>Default: 30 seconds
     *
     * @param timeout the timeout duration
     * @return this builder
     */
    public @NonNull Builder timeout(@NonNull Duration timeout) {
      this.timeout = Objects.requireNonNull(timeout);
      return this;
    }

    /**
     * Enables or disables caching.
     *
     * <p>Default: true (caching enabled)
     *
     * @param enabled whether to cache loaded skills
     * @return this builder
     */
    public @NonNull Builder cacheEnabled(boolean enabled) {
      this.cacheEnabled = enabled;
      return this;
    }

    /**
     * Builds the UrlSkillProvider.
     *
     * @return the configured provider
     */
    public @NonNull UrlSkillProvider build() {
      return new UrlSkillProvider(this);
    }
  }
}
