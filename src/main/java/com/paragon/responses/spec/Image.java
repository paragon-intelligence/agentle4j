package com.paragon.responses.spec;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * An image input to the model. Learn about <a
 * href="https://platform.openai.com/docs/guides/images-vision?api-mode=responses">image inputs</a>
 *
 * @param detail The detail level of the image to be sent to the model. One of {@code high}, {@code
 *     low}, or {@code auto}. Defaults to auto.
 * @param fileId The ID of the file to be sent to the model.
 * @param imageUrl The URL of the image to be sent to the model. A fully qualified URL or base64
 *     encoded image in a data URL.
 */
public record Image(@NonNull ImageDetail detail, @Nullable String fileId, @Nullable String imageUrl)
    implements MessageContent, FunctionToolCallOutputKind, CustomToolCallOutputKind {

  public static Image fromUrl(String imageUrl) {
    return new Image(ImageDetail.AUTO, null, imageUrl);
  }

  public static Image fromUrl(ImageDetail detail, String imageUrl) {
    return new Image(detail, null, imageUrl);
  }

  public static Image fromFileId(String fileId) {
    return new Image(ImageDetail.AUTO, fileId, null);
  }

  public static Image fromFileId(ImageDetail detail, String fileId) {
    return new Image(detail, fileId, null);
  }

  @Override
  public @NonNull String toString() {
    return "</input_image>";
  }
}
