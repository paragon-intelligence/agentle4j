package com.paragon.messaging.whatsapp.payload;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link InboundMessage} and its implementations.
 */
@DisplayName("InboundMessage")
class InboundMessageTest {

    private ObjectMapper objectMapper;
    private Validator validator;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Nested
    @DisplayName("TextMessage Deserialization")
    class TextMessageTests {

        @Test
        @DisplayName("deserializes text message from JSON")
        void deserializesTextMessage() throws JsonProcessingException {
            String json = """
                    {
                      "from": "5511999999999",
                      "id": "wamid.ABC123",
                      "timestamp": "1234567890",
                      "type": "text",
                      "text": {
                        "body": "Hello, World!"
                      }
                    }
                    """;

            InboundMessage message = objectMapper.readValue(json, InboundMessage.class);

            assertInstanceOf(TextMessage.class, message);
            TextMessage textMessage = (TextMessage) message;
            assertEquals("Hello, World!", textMessage.text.body);
            assertEquals("5511999999999", textMessage.from());
            assertEquals("wamid.ABC123", textMessage.id());
        }

        @Test
        @DisplayName("extracts text content from text message")
        void extractsTextContent() throws JsonProcessingException {
            String json = """
                    {
                      "from": "5511999999999",
                      "id": "wamid.ABC123",
                      "timestamp": "1234567890",
                      "type": "text",
                      "text": {
                        "body": "Test message"
                      }
                    }
                    """;

            InboundMessage message = objectMapper.readValue(json, InboundMessage.class);

            assertEquals("Test message", message.extractTextContent());
        }

        @Test
        @DisplayName("deserializes text message with reply context")
        void deserializesWithContext() throws JsonProcessingException {
            String json = """
                    {
                      "from": "5511999999999",
                      "id": "wamid.NEW",
                      "timestamp": "1234567890",
                      "type": "text",
                      "context": {
                        "id": "wamid.ORIGINAL"
                      },
                      "text": {
                        "body": "This is a reply"
                      }
                    }
                    """;

            InboundMessage message = objectMapper.readValue(json, InboundMessage.class);

            assertTrue(message.isReply());
            assertEquals("wamid.ORIGINAL", message.repliedToMessageId());
        }
    }

    @Nested
    @DisplayName("Image Message Deserialization")
    class ImageMessageTests {

        @Test
        @DisplayName("deserializes image message")
        void deserializesImageMessage() throws JsonProcessingException {
            String json = """
                    {
                      "from": "5511999999999",
                      "id": "wamid.IMG123",
                      "timestamp": "1234567890",
                      "type": "image",
                      "image": {
                        "id": "media123",
                        "mime_type": "image/jpeg",
                        "sha256": "hash123"
                      }
                    }
                    """;

            InboundMessage message = objectMapper.readValue(json, InboundMessage.class);

            assertInstanceOf(ImageMessage.class, message);
            assertEquals("image", message.type());
        }

        @Test
        @DisplayName("deserializes image with caption")
        void deserializesWithCaption() throws JsonProcessingException {
            String json = """
                    {
                      "from": "5511999999999",
                      "id": "wamid.IMG123",
                      "timestamp": "1234567890",
                      "type": "image",
                      "image": {
                        "id": "media123",
                        "caption": "Check this out!",
                        "mime_type": "image/jpeg"
                      }
                    }
                    """;

            InboundMessage message = objectMapper.readValue(json, InboundMessage.class);
            ImageMessage imageMessage = (ImageMessage) message;

            String content = message.extractTextContent();
            assertTrue(content.contains("Image"));
            assertTrue(content.contains("Check this out!"));
        }

        @Test
        @DisplayName("extracts placeholder for image without caption")
        void extractsPlaceholder() throws JsonProcessingException {
            String json = """
                    {
                      "from": "5511999999999",
                      "id": "wamid.IMG123",
                      "timestamp": "1234567890",
                      "type": "image",
                      "image": {
                        "id": "media123",
                        "mime_type": "image/jpeg"
                      }
                    }
                    """;

            InboundMessage message = objectMapper.readValue(json, InboundMessage.class);

            assertEquals("[Image]", message.extractTextContent());
        }
    }

    @Nested
    @DisplayName("Interactive Message Deserialization")
    class InteractiveMessageTests {

        @Test
        @DisplayName("deserializes button reply")
        void deserializesButtonReply() throws JsonProcessingException {
            String json = """
                    {
                      "from": "5511999999999",
                      "id": "wamid.INT123",
                      "timestamp": "1234567890",
                      "type": "interactive",
                      "interactive": {
                        "type": "button_reply",
                        "button_reply": {
                          "id": "yes",
                          "title": "Yes"
                        }
                      }
                    }
                    """;

            InboundMessage message = objectMapper.readValue(json, InboundMessage.class);

            assertInstanceOf(InteractiveMessage.class, message);
            String content = message.extractTextContent();
            assertTrue(content.contains("Button"));
            assertTrue(content.contains("Yes"));
        }

        @Test
        @DisplayName("deserializes list reply")
        void deserializesListReply() throws JsonProcessingException {
            String json = """
                    {
                      "from": "5511999999999",
                      "id": "wamid.INT123",
                      "timestamp": "1234567890",
                      "type": "interactive",
                      "interactive": {
                        "type": "list_reply",
                        "list_reply": {
                          "id": "item1",
                          "title": "Pizza",
                          "description": "Margherita Pizza"
                        }
                      }
                    }
                    """;

            InboundMessage message = objectMapper.readValue(json, InboundMessage.class);

            String content = message.extractTextContent();
            assertTrue(content.contains("List"));
            assertTrue(content.contains("Pizza"));
        }
    }

    @Nested
    @DisplayName("Location Message Deserialization")
    class LocationMessageTests {

        @Test
        @DisplayName("deserializes location message")
        void deserializesLocation() throws JsonProcessingException {
            String json = """
                    {
                      "from": "5511999999999",
                      "id": "wamid.LOC123",
                      "timestamp": "1234567890",
                      "type": "location",
                      "location": {
                        "latitude": -23.550520,
                        "longitude": -46.633308,
                        "name": "São Paulo",
                        "address": "São Paulo, Brazil"
                      }
                    }
                    """;

            InboundMessage message = objectMapper.readValue(json, InboundMessage.class);

            assertInstanceOf(LocationMessage.class, message);
            LocationMessage locationMessage = (LocationMessage) message;
            assertEquals(-23.550520, locationMessage.latitude);
            assertEquals(-46.633308, locationMessage.longitude);
        }

        @Test
        @DisplayName("extracts location content")
        void extractsLocationContent() throws JsonProcessingException {
            String json = """
                    {
                      "from": "5511999999999",
                      "id": "wamid.LOC123",
                      "timestamp": "1234567890",
                      "type": "location",
                      "location": {
                        "latitude": -23.550520,
                        "longitude": -46.633308
                      }
                    }
                    """;

            InboundMessage message = objectMapper.readValue(json, InboundMessage.class);

            String content = message.extractTextContent();
            assertTrue(content.contains("Location"));
            assertTrue(content.contains("-23.550520"));
            assertTrue(content.contains("-46.633308"));
        }
    }

    @Nested
    @DisplayName("Reaction Message Deserialization")
    class ReactionMessageTests {

        @Test
        @DisplayName("deserializes reaction message")
        void deserializesReaction() throws JsonProcessingException {
            String json = """
                    {
                      "from": "5511999999999",
                      "id": "wamid.REACT123",
                      "timestamp": "1234567890",
                      "type": "reaction",
                      "reaction": {
                        "message_id": "wamid.ORIGINAL",
                        "emoji": "👍"
                      }
                    }
                    """;

            InboundMessage message = objectMapper.readValue(json, InboundMessage.class);

            assertInstanceOf(ReactionMessage.class, message);
            assertEquals("👍", message.extractTextContent());
        }
    }

    @Nested
    @DisplayName("Audio Message Deserialization")
    class AudioMessageTests {

        @Test
        @DisplayName("deserializes audio message")
        void deserializesAudio() throws JsonProcessingException {
            String json = """
                    {
                      "from": "5511999999999",
                      "id": "wamid.AUD123",
                      "timestamp": "1234567890",
                      "type": "audio",
                      "audio": {
                        "id": "media123",
                        "mime_type": "audio/ogg"
                      }
                    }
                    """;

            InboundMessage message = objectMapper.readValue(json, InboundMessage.class);

            assertInstanceOf(AudioMessage.class, message);
            assertEquals("[Audio message]", message.extractTextContent());
        }
    }

    @Nested
    @DisplayName("Validation")
    class ValidationTests {

        @Test
        @DisplayName("validates from field not blank")
        void validatesFromNotBlank() {
            TextMessage message = new TextMessage(
                    "",
                    "wamid.123",
                    "1234567890",
                    "text",
                    null,
                    new TextMessage.TextBody("Hello"));

            Set<ConstraintViolation<TextMessage>> violations = validator.validate(message);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("validates from field phone pattern")
        void validatesFromPattern() {
            TextMessage message = new TextMessage(
                    "invalid",
                    "wamid.123",
                    "1234567890",
                    "text",
                    null,
                    new TextMessage.TextBody("Hello"));

            Set<ConstraintViolation<TextMessage>> violations = validator.validate(message);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("validates message ID not blank")
        void validatesIdNotBlank() {
            TextMessage message = new TextMessage(
                    "5511999999999",
                    "",
                    "1234567890",
                    "text",
                    null,
                    new TextMessage.TextBody("Hello"));

            Set<ConstraintViolation<TextMessage>> violations = validator.validate(message);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("accepts valid message")
        void acceptsValidMessage() {
            TextMessage message = new TextMessage(
                    "5511999999999",
                    "wamid.123",
                    "1234567890",
                    "text",
                    null,
                    new TextMessage.TextBody("Hello"));

            Set<ConstraintViolation<TextMessage>> violations = validator.validate(message);
            assertTrue(violations.isEmpty());
        }
    }

    @Nested
    @DisplayName("Interface Methods")
    class InterfaceMethodsTests {

        @Test
        @DisplayName("isReply() returns false without context")
        void isReplyFalseWithoutContext() {
            TextMessage message = new TextMessage(
                    "5511999999999",
                    "wamid.123",
                    "1234567890",
                    "text",
                    null,
                    new TextMessage.TextBody("Hello"));

            assertFalse(message.isReply());
        }

        @Test
        @DisplayName("isReply() returns true with context")
        void isReplyTrueWithContext() {
            MessageContext context = new MessageContext("wamid.ORIGINAL");
            TextMessage message = new TextMessage(
                    "5511999999999",
                    "wamid.123",
                    "1234567890",
                    "text",
                    context,
                    new TextMessage.TextBody("Hello"));

            assertTrue(message.isReply());
        }

        @Test
        @DisplayName("repliedToMessageId() returns null without context")
        void repliedToMessageIdNull() {
            TextMessage message = new TextMessage(
                    "5511999999999",
                    "wamid.123",
                    "1234567890",
                    "text",
                    null,
                    new TextMessage.TextBody("Hello"));

            assertNull(message.repliedToMessageId());
        }

        @Test
        @DisplayName("repliedToMessageId() returns ID with context")
        void repliedToMessageIdReturnsId() {
            MessageContext context = new MessageContext("wamid.ORIGINAL");
            TextMessage message = new TextMessage(
                    "5511999999999",
                    "wamid.123",
                    "1234567890",
                    "text",
                    context,
                    new TextMessage.TextBody("Hello"));

            assertEquals("wamid.ORIGINAL", message.repliedToMessageId());
        }
    }

    @Nested
    @DisplayName("Real-World Scenarios")
    class RealWorldScenariosTests {

        @Test
        @DisplayName("handles customer inquiry")
        void handlesCustomerInquiry() throws JsonProcessingException {
            String json = """
                    {
                      "from": "5511987654321",
                      "id": "wamid.HxA1234567890",
                      "timestamp": "1707040000",
                      "type": "text",
                      "text": {
                        "body": "Hello, I'd like to know about your products"
                      }
                    }
                    """;

            InboundMessage message = objectMapper.readValue(json, InboundMessage.class);

            assertEquals("Hello, I'd like to know about your products", message.extractTextContent());
            assertEquals("5511987654321", message.from());
            assertEquals("text", message.type());
        }

        @Test
        @DisplayName("handles button selection in customer flow")
        void handlesButtonSelection() throws JsonProcessingException {
            String json = """
                    {
                      "from": "5511987654321",
                      "id": "wamid.BTN123",
                      "timestamp": "1707040000",
                      "type": "interactive",
                      "interactive": {
                        "type": "button_reply",
                        "button_reply": {
                          "id": "track_order",
                          "title": "Track Order"
                        }
                      }
                    }
                    """;

            InboundMessage message = objectMapper.readValue(json, InboundMessage.class);

            String content = message.extractTextContent();
            assertTrue(content.contains("Track Order"));
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("handles empty text body")
        void handlesEmptyTextBody() {
            TextMessage message = new TextMessage(
                    "5511999999999",
                    "wamid.123",
                    "1234567890",
                    "text",
                    null,
                    new TextMessage.TextBody(""));

            assertEquals("", message.extractTextContent());
        }

        @Test
        @DisplayName("handles null text body in extraction")
        void handlesNullTextBody() {
            TextMessage message = new TextMessage(
                    "5511999999999",
                    "wamid.123",
                    "1234567890",
                    "text",
                    null,
                    new TextMessage.TextBody(null));

            assertEquals("", message.extractTextContent());
        }

        @Test
        @DisplayName("handles Unicode in text messages")
        void handlesUnicode() throws JsonProcessingException {
            String json = """
                    {
                      "from": "5511999999999",
                      "id": "wamid.123",
                      "timestamp": "1234567890",
                      "type": "text",
                      "text": {
                        "body": "こんにちは 🌟 Olá"
                      }
                    }
                    """;

            InboundMessage message = objectMapper.readValue(json, InboundMessage.class);

            assertEquals("こんにちは 🌟 Olá", message.extractTextContent());
        }
    }
}
