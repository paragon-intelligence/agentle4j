package com.paragon.messaging.whatsapp.domain;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Sealed interface defining the structured output format for WhatsApp agents.
 *
 * <p>Agents communicating via WhatsApp should use {@code Agent.Structured<WhatsAppResponse>}
 * to ensure their outputs can be correctly mapped to WhatsApp message types.</p>
 *
 * @since 1.0
 */
public sealed interface WhatsAppResponse permits
        WhatsAppResponse.TextResponse,
        WhatsAppResponse.ImageResponse,
        WhatsAppResponse.MenuResponse,
        WhatsAppResponse.LinkResponse {

  /**
   * Simple text response.
   *
   * @param message The body text of the message
   */
  record TextResponse(@NonNull String message) implements WhatsAppResponse {
  }

  /**
   * Image response with optional caption.
   *
   * @param imageUrl The URL of the image to send
   * @param caption Optional caption for the image
   */
  record ImageResponse(@NonNull String imageUrl, @Nullable String caption) implements WhatsAppResponse {
  }

  /**
   * Menu response (interactive list or buttons) for choices.
   *
   * @param title The title of the menu
   * @param description The description or body text
   * @param options The list of options available to the user
   * @param footer Optional footer text
   */
  record MenuResponse(
          @NonNull String title,
          @NonNull String description,
          @NonNull List<MenuOption> options,
          @Nullable String footer
  ) implements WhatsAppResponse {
    public record MenuOption(@NonNull String id, @NonNull String title, @Nullable String description) {}
  }

  /**
   * Call-To-Action (CTA) Link response.
   *
   * @param text The body text explaining the link
   * @param buttonText The text to display on the button
   * @param url The URL to open
   */
  record LinkResponse(
          @NonNull String text,
          @NonNull String buttonText,
          @NonNull String url
  ) implements WhatsAppResponse {
  }
}
