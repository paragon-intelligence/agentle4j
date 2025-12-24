package com.paragon.responses.spec;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A file input to the model.
 *
 * @param fileData The base64-encoded data of the file to be sent to the model.
 * @param fileID The ID of the file to be sent to the model.
 * @param fileUrl The URL of the file to be sent to the model.
 * @param filename The name of the file to be sent to the model.
 */
public record File(
    @Nullable String fileData,
    @Nullable String fileID,
    @Nullable String fileUrl,
    @Nullable String filename)
    implements MessageContent, FunctionToolCallOutputKind, CustomToolCallOutputKind {
  @Override
  public @NonNull String toString() {
    return "</input_file>";
  }
}
