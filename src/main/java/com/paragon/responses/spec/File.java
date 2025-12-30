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

  /** Creates a file from a URL. */
  public static File fromUrl(String fileUrl) {
    return new File(null, null, fileUrl, null);
  }

  /** Creates a file from a URL with filename. */
  public static File fromUrl(String fileUrl, String filename) {
    return new File(null, null, fileUrl, filename);
  }

  /** Creates a file from a file ID. */
  public static File fromFileId(String fileId) {
    return new File(null, fileId, null, null);
  }

  /** Creates a file from a file ID with filename. */
  public static File fromFileId(String fileId, String filename) {
    return new File(null, fileId, null, filename);
  }

  /** Creates a file from base64-encoded data. */
  public static File fromBase64(String base64Data, String filename) {
    return new File(base64Data, null, null, filename);
  }

  public static File fromBase64(String base64Data) {
    return new File(base64Data, null, null, null);
  }

  @Override
  public @NonNull String toString() {
    return "</input_file>";
  }
}
