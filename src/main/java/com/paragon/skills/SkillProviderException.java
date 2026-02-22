package com.paragon.skills;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Exception thrown when a skill cannot be loaded or parsed.
 *
 * <p>This exception is thrown by {@link SkillProvider} implementations when skill retrieval fails
 * due to I/O errors, parsing errors, or validation failures.
 *
 * @see SkillProvider
 * @since 1.0
 */
public class SkillProviderException extends RuntimeException {

  private final @Nullable String skillId;

  /**
   * Creates a new SkillProviderException.
   *
   * @param message the error message
   */
  public SkillProviderException(@NonNull String message) {
    super(message);
    this.skillId = null;
  }

  /**
   * Creates a new SkillProviderException with a cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   */
  public SkillProviderException(@NonNull String message, @Nullable Throwable cause) {
    super(message, cause);
    this.skillId = null;
  }

  /**
   * Creates a new SkillProviderException for a specific skill.
   *
   * @param skillId the skill identifier that failed
   * @param message the error message
   */
  public SkillProviderException(@NonNull String skillId, @NonNull String message) {
    super("Failed to load skill '" + skillId + "': " + message);
    this.skillId = skillId;
  }

  /**
   * Creates a new SkillProviderException for a specific skill with a cause.
   *
   * @param skillId the skill identifier that failed
   * @param message the error message
   * @param cause the underlying cause
   */
  public SkillProviderException(
      @NonNull String skillId, @NonNull String message, @Nullable Throwable cause) {
    super("Failed to load skill '" + skillId + "': " + message, cause);
    this.skillId = skillId;
  }

  /**
   * Returns the skill identifier that caused the exception, if available.
   *
   * @return the skill ID, or null if not applicable
   */
  public @Nullable String skillId() {
    return skillId;
  }
}
