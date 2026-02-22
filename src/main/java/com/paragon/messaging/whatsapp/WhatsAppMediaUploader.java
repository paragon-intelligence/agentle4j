package com.paragon.messaging.whatsapp;

import java.io.IOException;
import java.time.Instant;
import java.util.Objects;
import okhttp3.*;
import org.jspecify.annotations.NonNull;

/**
 * Service for uploading media files to WhatsApp Cloud API.
 *
 * <p>Handles uploading audio, images, videos, and documents to WhatsApp's media endpoint and
 * returns media IDs that can be used in message sending.
 *
 * <p><b>Virtual Thread Friendly:</b> Uses synchronous OkHttp calls which work well with virtual
 * threads. All I/O operations are blocking but non-blocking at the platform thread level.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * OkHttpClient httpClient = new OkHttpClient.Builder()
 *     .connectTimeout(10, TimeUnit.SECONDS)
 *     .build();
 *
 * WhatsAppMediaUploader uploader = new WhatsAppMediaUploader(
 *     phoneNumberId,
 *     accessToken,
 *     httpClient
 * );
 *
 * byte[] audioData = ttsProvider.synthesize(text, config);
 * MediaUploadResponse response = uploader.uploadAudio(audioData, "audio/ogg; codecs=opus");
 *
 * // Use response.mediaId() in message
 * }</pre>
 *
 * @author Agentle Team
 * @since 2.1
 */
public class WhatsAppMediaUploader {

  private static final String API_VERSION = "v22.0";
  private static final String BASE_URL = "https://graph.facebook.com/" + API_VERSION;
  private static final long MAX_AUDIO_SIZE_BYTES = 16 * 1024 * 1024; // 16MB
  private static final long MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024; // 5MB
  private static final long MAX_VIDEO_SIZE_BYTES = 16 * 1024 * 1024; // 16MB
  private static final long MAX_DOCUMENT_SIZE_BYTES = 100 * 1024 * 1024; // 100MB

  private final String phoneNumberId;
  private final String accessToken;
  private final OkHttpClient httpClient;

  /**
   * Creates a new WhatsApp media uploader.
   *
   * @param phoneNumberId WhatsApp Business phone number ID
   * @param accessToken WhatsApp Business API access token
   * @param httpClient configured OkHttp client (should be shared/reused)
   */
  public WhatsAppMediaUploader(
      @NonNull String phoneNumberId,
      @NonNull String accessToken,
      @NonNull OkHttpClient httpClient) {
    this.phoneNumberId = Objects.requireNonNull(phoneNumberId, "phoneNumberId cannot be null");
    this.accessToken = Objects.requireNonNull(accessToken, "accessToken cannot be null");
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
  }

  /**
   * Uploads audio data to WhatsApp Media API.
   *
   * <p>WhatsApp supports the following audio formats:
   *
   * <ul>
   *   <li>AAC - audio/aac
   *   <li>AMR - audio/amr
   *   <li>MP3 - audio/mpeg
   *   <li>MP4 Audio - audio/mp4
   *   <li>OGG Opus - audio/ogg (recommended for TTS)
   * </ul>
   *
   * @param audioBytes audio file content
   * @param mimeType MIME type (e.g., "audio/ogg; codecs=opus")
   * @return upload response with media ID
   * @throws MediaUploadException if upload fails
   */
  public @NonNull MediaUploadResponse uploadAudio(
      @NonNull byte[] audioBytes, @NonNull String mimeType) throws MediaUploadException {

    Objects.requireNonNull(audioBytes, "audioBytes cannot be null");
    Objects.requireNonNull(mimeType, "mimeType cannot be null");

    if (audioBytes.length > MAX_AUDIO_SIZE_BYTES) {
      throw new MediaUploadException(
          "Audio file too large: "
              + audioBytes.length
              + " bytes (max "
              + MAX_AUDIO_SIZE_BYTES
              + ")");
    }

    return uploadMedia(audioBytes, mimeType, "audio", determineFileExtension(mimeType));
  }

  /**
   * Uploads image data to WhatsApp Media API.
   *
   * @param imageBytes image file content
   * @param mimeType MIME type (e.g., "image/jpeg", "image/png")
   * @return upload response with media ID
   * @throws MediaUploadException if upload fails
   */
  public @NonNull MediaUploadResponse uploadImage(
      @NonNull byte[] imageBytes, @NonNull String mimeType) throws MediaUploadException {

    Objects.requireNonNull(imageBytes, "imageBytes cannot be null");
    Objects.requireNonNull(mimeType, "mimeType cannot be null");

    if (imageBytes.length > MAX_IMAGE_SIZE_BYTES) {
      throw new MediaUploadException(
          "Image file too large: "
              + imageBytes.length
              + " bytes (max "
              + MAX_IMAGE_SIZE_BYTES
              + ")");
    }

    return uploadMedia(imageBytes, mimeType, "image", determineFileExtension(mimeType));
  }

  /**
   * Core upload method that handles multipart/form-data upload.
   *
   * @param fileBytes file content
   * @param mimeType MIME type
   * @param type media type for API (audio, image, video, document)
   * @param extension file extension
   * @return upload response with media ID
   * @throws MediaUploadException if upload fails
   */
  private MediaUploadResponse uploadMedia(
      byte[] fileBytes, String mimeType, String type, String extension)
      throws MediaUploadException {

    // Build multipart request body
    RequestBody fileBody = RequestBody.create(fileBytes, MediaType.parse(mimeType));

    MultipartBody requestBody =
        new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("messaging_product", "whatsapp")
            .addFormDataPart("type", type)
            .addFormDataPart("file", "media." + extension, fileBody)
            .build();

    // Build HTTP request
    Request request =
        new Request.Builder()
            .url(BASE_URL + "/" + phoneNumberId + "/media")
            .addHeader("Authorization", "Bearer " + accessToken)
            .post(requestBody)
            .build();

    // Execute request (blocking, but virtual-thread friendly)
    try (Response response = httpClient.newCall(request).execute()) {
      return parseUploadResponse(response);
    } catch (IOException e) {
      throw new MediaUploadException("Media upload failed: " + e.getMessage(), e);
    }
  }

  /**
   * Parses the upload response from WhatsApp API.
   *
   * @param response HTTP response
   * @return parsed upload response
   * @throws MediaUploadException if parsing fails or API returned error
   */
  private MediaUploadResponse parseUploadResponse(Response response) throws MediaUploadException {
    int statusCode = response.code();
    String body;

    try {
      ResponseBody responseBody = response.body();
      body = responseBody != null ? responseBody.string() : "";
    } catch (IOException e) {
      throw new MediaUploadException("Failed to read response body", e);
    }

    if (statusCode >= 200 && statusCode < 300) {
      // Parse successful response
      // Expected format: {"id":"media-id"}
      String mediaId = extractMediaId(body);
      if (mediaId == null || mediaId.isEmpty()) {
        throw new MediaUploadException("No media ID in response: " + body);
      }

      return new MediaUploadResponse(mediaId, Instant.now());
    } else {
      throw new MediaUploadException(
          "Media upload failed with status " + statusCode + ": " + body, statusCode);
    }
  }

  /**
   * Extracts media ID from JSON response (simplified JSON parsing).
   *
   * @param responseBody JSON response body
   * @return media ID or null if not found
   */
  private String extractMediaId(String responseBody) {
    // Simple JSON parsing (in production, use Jackson/Gson)
    int idStart = responseBody.indexOf("\"id\":\"");
    if (idStart > -1) {
      idStart += 6; // Skip `"id":"`
      int idEnd = responseBody.indexOf("\"", idStart);
      if (idEnd > idStart) {
        return responseBody.substring(idStart, idEnd);
      }
    }
    return null;
  }

  /**
   * Determines file extension from MIME type.
   *
   * @param mimeType MIME type
   * @return file extension (without dot)
   */
  private String determineFileExtension(String mimeType) {
    return switch (mimeType.toLowerCase()) {
      case "audio/ogg", "audio/ogg; codecs=opus" -> "ogg";
      case "audio/mpeg", "audio/mp3" -> "mp3";
      case "audio/aac" -> "aac";
      case "audio/amr" -> "amr";
      case "audio/mp4" -> "m4a";
      case "image/jpeg" -> "jpg";
      case "image/png" -> "png";
      case "image/webp" -> "webp";
      case "video/mp4" -> "mp4";
      case "video/3gpp" -> "3gp";
      default -> "bin";
    };
  }

  /**
   * Response from media upload.
   *
   * @param mediaId the media ID assigned by WhatsApp
   * @param timestamp when the upload completed
   */
  public record MediaUploadResponse(@NonNull String mediaId, @NonNull Instant timestamp) {}

  /** Exception thrown when media upload fails. */
  public static class MediaUploadException extends RuntimeException {
    private final int statusCode;

    public MediaUploadException(String message) {
      super(message);
      this.statusCode = -1;
    }

    public MediaUploadException(String message, Throwable cause) {
      super(message, cause);
      this.statusCode = -1;
    }

    public MediaUploadException(String message, int statusCode) {
      super(message);
      this.statusCode = statusCode;
    }

    public int getStatusCode() {
      return statusCode;
    }
  }
}
