package com.paragon.responses.spec;

import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class ResponseFailedException extends RuntimeException {
  private final @NonNull ResponseError error;
  private final @Nullable Map<String, String> metadata;

  public ResponseFailedException(
      @NonNull ResponseError error, @Nullable Map<String, String> metadata) {
    super(error.toString());
    this.error = error;
    this.metadata = metadata;
  }

  public @NonNull ResponseError error() {
    return error;
  }

  public @Nullable Map<String, String> metadata() {
    return metadata;
  }
}
