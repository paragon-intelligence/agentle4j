package com.paragon.messaging.testutil;

import com.paragon.messaging.whatsapp.payload.*;
import java.time.Instant;
import java.util.List;

/**
 * Factory for creating mock WhatsApp messages for testing.
 */
public class MockMessageFactory {

    public static InboundMessage createTextMessage(String from, String messageId, String text) {
        return InboundMessage.builder()
                .from(from)
                .id(messageId)
                .timestamp(Instant.now().getEpochSecond())
                .type("text")
                .text(TextPayload.builder().body(text).build())
                .build();
    }

    public static InboundMessage createTextMessage(String from, String text) {
        return createTextMessage(from, generateMessageId(), text);
    }

    public static InboundMessage createImageMessage(String from, String mediaId) {
        return InboundMessage.builder()
                .from(from)
                .id(generateMessageId())
                .timestamp(Instant.now().getEpochSecond())
                .type("image")
                .image(ImagePayload.builder()
                        .id(mediaId)
                        .mimeType("image/jpeg")
                        .build())
                .build();
    }

    public static InboundMessage createAudioMessage(String from, String mediaId) {
        return InboundMessage.builder()
                .from(from)
                .id(generateMessageId())
                .timestamp(Instant.now().getEpochSecond())
                .type("audio")
                .audio(AudioPayload.builder()
                        .id(mediaId)
                        .mimeType("audio/ogg")
                        .build())
                .build();
    }

    public static InboundMessage createVideoMessage(String from, String mediaId) {
        return InboundMessage.builder()
                .from(from)
                .id(generateMessageId())
                .timestamp(Instant.now().getEpochSecond())
                .type("video")
                .video(VideoPayload.builder()
                        .id(mediaId)
                        .mimeType("video/mp4")
                        .build())
                .build();
    }

    public static InboundMessage createDocumentMessage(String from, String mediaId, String filename) {
        return InboundMessage.builder()
                .from(from)
                .id(generateMessageId())
                .timestamp(Instant.now().getEpochSecond())
                .type("document")
                .document(DocumentPayload.builder()
                        .id(mediaId)
                        .filename(filename)
                        .mimeType("application/pdf")
                        .build())
                .build();
    }

    public static InboundMessage createLocationMessage(String from, double latitude, double longitude) {
        return InboundMessage.builder()
                .from(from)
                .id(generateMessageId())
                .timestamp(Instant.now().getEpochSecond())
                .type("location")
                .location(LocationPayload.builder()
                        .latitude(latitude)
                        .longitude(longitude)
                        .build())
                .build();
    }

    public static InboundMessage createButtonReply(String from, String buttonText) {
        return InboundMessage.builder()
                .from(from)
                .id(generateMessageId())
                .timestamp(Instant.now().getEpochSecond())
                .type("button")
                .button(ButtonReply.builder()
                        .text(buttonText)
                        .payload(buttonText.toLowerCase())
                        .build())
                .build();
    }

    public static WebhookEvent createWebhookEvent(InboundMessage... messages) {
        ChangeValue value = ChangeValue.builder()
                .messagingProduct("whatsapp")
                .metadata(Metadata.builder()
                        .displayPhoneNumber("1234567890")
                        .phoneNumberId("phone-id-123")
                        .build())
                .messages(List.of(messages))
                .build();

        Change change = Change.builder()
                .value(value)
                .field("messages")
                .build();

        Entry entry = Entry.builder()
                .id("entry-123")
                .changes(List.of(change))
                .build();

        return WebhookEvent.builder()
                .object("whatsapp_business_account")
                .entry(List.of(entry))
                .build();
    }

    private static String generateMessageId() {
        return "wamid." + System.currentTimeMillis() + "." + (int) (Math.random() * 10000);
    }

    private MockMessageFactory() {
        // Utility class
    }
}
