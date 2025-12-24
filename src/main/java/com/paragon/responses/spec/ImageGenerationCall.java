package com.paragon.responses.spec;

import java.util.Objects;
import org.jspecify.annotations.NonNull;

/** An image generation request made by the model. */
public final class ImageGenerationCall extends ToolCall implements Item, ResponseOutput {
  private final @NonNull String result;
  private final @NonNull String status;

  /**
   * @param id The unique ID of the image generation call.
   * @param result The generated image encoded in base64.
   * @param status The status of the image generation call.
   */
  public ImageGenerationCall(@NonNull String id, @NonNull String result, @NonNull String status) {
    super(id);
    this.result = result;
    this.status = status;
  }

  @Override
  public @NonNull String id() {
    return id;
  }

  public @NonNull String result() {
    return result;
  }

  public @NonNull String status() {
    return status;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    var that = (ImageGenerationCall) obj;
    return Objects.equals(this.id, that.id)
        && Objects.equals(this.result, that.result)
        && Objects.equals(this.status, that.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, result, status);
  }

  @Override
  public String toString() {
    return "ImageGenerationCall["
        + "id="
        + id
        + ", "
        + "result="
        + result
        + ", "
        + "status="
        + status
        + ']';
  }
}
