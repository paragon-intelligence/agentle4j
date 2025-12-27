package com.paragon.responses.exception;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Exception thrown when required configuration is missing or invalid.
 *
 * <p>This exception is thrown during builder validation and is not retryable.
 *
 * <p>Example usage:
 * <pre>{@code
 * try {
 *     Responder responder = Responder.builder()
 *         .openRouter()
 *         // Missing apiKey!
 *         .build();
 * } catch (ConfigurationException e) {
 *     log.error("Config error: {}", e.suggestion());
 * }
 * }</pre>
 */
public class ConfigurationException extends AgentleException {

  private final @Nullable String configKey;

  /**
   * Creates a new ConfigurationException.
   *
   * @param message the error message
   * @param configKey the configuration key that is missing or invalid
   * @param suggestion optional resolution hint
   */
  public ConfigurationException(
      @NonNull String message,
      @Nullable String configKey,
      @Nullable String suggestion) {
    super(ErrorCode.MISSING_CONFIGURATION, message, suggestion, false);
    this.configKey = configKey;
  }

  /**
   * Creates a missing configuration exception.
   *
   * @param configKey the missing configuration key
   * @return a new ConfigurationException
   */
  public static ConfigurationException missing(@NonNull String configKey) {
    return new ConfigurationException(
        "Required configuration '" + configKey + "' is missing",
        configKey,
        "Set " + configKey + " before calling build()");
  }

  /**
   * Creates an invalid configuration exception.
   *
   * @param configKey the invalid configuration key
   * @param reason why the value is invalid
   * @return a new ConfigurationException
   */
  public static ConfigurationException invalid(@NonNull String configKey, @NonNull String reason) {
    return new ConfigurationException(
        "Invalid configuration '" + configKey + "': " + reason,
        configKey,
        "Check the value of " + configKey);
  }

  /**
   * Returns the configuration key that caused the error.
   *
   * @return the config key, or null if not specific to a key
   */
  public @Nullable String configKey() {
    return configKey;
  }
}
