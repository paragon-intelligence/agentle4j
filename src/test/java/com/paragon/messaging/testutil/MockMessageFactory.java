package com.paragon.messaging.testutil;

import com.paragon.messaging.whatsapp.WebhookEvent;
import com.paragon.messaging.whatsapp.payload.*;
import java.time.Instant;
import java.util.Optional;

/** Factory for creating mock WhatsApp messages for testing. */
public class MockMessageFactory {

  public static InboundMessage createTextMessage(String from, String messageId, String text) {
    return new TextMessage(
        from,
        messageId,
        String.valueOf(Instant.now().getEpochSecond()),
        "text",
        null,
        new TextMessage.TextBody(text));
  }

  public static InboundMessage createTextMessage(String from, String text) {
    return createTextMessage(from, generateMessageId(), text);
  }

  public static InboundMessage createImageMessage(String from, String mediaId) {
    return new ImageMessage(
        from,
        generateMessageId(),
        String.valueOf(Instant.now().getEpochSecond()),
        "image",
        null,
        new MediaContent(mediaId, "image/jpeg", null, null, null, null, null, null));
  }

  public static InboundMessage createAudioMessage(String from, String mediaId) {
    return new AudioMessage(
        from,
        generateMessageId(),
        String.valueOf(Instant.now().getEpochSecond()),
        "audio",
        null,
        new MediaContent(mediaId, "audio/ogg", null, null, null, true, null, null));
  }

  public static InboundMessage createVideoMessage(String from, String mediaId) {
    return new VideoMessage(
        from,
        generateMessageId(),
        String.valueOf(Instant.now().getEpochSecond()),
        "video",
        null,
        new MediaContent(mediaId, "video/mp4", null, null, null, null, null, null));
  }

  public static InboundMessage createDocumentMessage(String from, String mediaId, String filename) {
    return new DocumentMessage(
        from,
        generateMessageId(),
        String.valueOf(Instant.now().getEpochSecond()),
        "document",
        null,
        new MediaContent(mediaId, "application/pdf", null, null, filename, null, null, null));
  }

  public static InboundMessage createLocationMessage(
      String from, double latitude, double longitude) {
    return new LocationMessage(
        from,
        generateMessageId(),
        String.valueOf(Instant.now().getEpochSecond()),
        "location",
        null,
        new LocationMessage.LocationContent(latitude, longitude, null, null, null));
  }

  public static InboundMessage createButtonReply(String from, String buttonText) {
    return new InteractiveMessage(
        from,
        generateMessageId(),
        String.valueOf(Instant.now().getEpochSecond()),
        "interactive",
        null,
        new ButtonReply("button_reply", new ReplyData(buttonText.toLowerCase(), buttonText, null)));
  }

  public static WebhookEvent createWebhookEvent(InboundMessage... messages) {
    // Create a simple IncomingMessageEvent based on the first message
    String messageId = messages.length > 0 ? messages[0].id() : generateMessageId();
    String senderId = messages.length > 0 ? messages[0].from() : "test-sender";

    // Determine message type and content from first message
    WebhookEvent.IncomingMessageType messageType = WebhookEvent.IncomingMessageType.TEXT;
    WebhookEvent.IncomingMessageContent content = new WebhookEvent.TextContent("test");

    if (messages.length > 0) {
      InboundMessage msg = messages[0];
      if (msg instanceof TextMessage tm) {
        messageType = WebhookEvent.IncomingMessageType.TEXT;
        content = new WebhookEvent.TextContent(tm.text.body);
      } else if (msg instanceof ImageMessage) {
        messageType = WebhookEvent.IncomingMessageType.IMAGE;
        content =
            new WebhookEvent.MediaContent(
                "media-id", "image/jpeg", Optional.empty(), Optional.empty());
      } else if (msg instanceof InteractiveMessage im) {
        if (im.buttonReply != null) {
          messageType = WebhookEvent.IncomingMessageType.BUTTON_REPLY;
          content = new WebhookEvent.ButtonReplyContent(im.buttonReply.id, im.buttonReply.title);
        } else if (im.listReply != null) {
          messageType = WebhookEvent.IncomingMessageType.LIST_REPLY;
          content =
              new WebhookEvent.ListReplyContent(
                  im.listReply.id, im.listReply.title, Optional.empty());
        }
      }
    }

    return new WebhookEvent.IncomingMessageEvent(
        messageId,
        senderId,
        Optional.of("Test User"),
        messageType,
        content,
        Instant.now(),
        Optional.empty());
  }

  private static String generateMessageId() {
    return "wamid." + System.currentTimeMillis() + "." + (int) (Math.random() * 10000);
  }

  private MockMessageFactory() {
    // Utility class
  }
}
