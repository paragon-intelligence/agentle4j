package com.paragon.messaging.core;

import com.paragon.messaging.whatsapp.messages.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link OutboundMessage} sealed interface and implementations.
 */
@DisplayName("OutboundMessage")
class OutboundMessageTest {

    @Nested
    @DisplayName("Sealed Interface")
    class SealedInterfaceTests {

        @Test
        @DisplayName("TextMessage implements OutboundMessage")
        void textMessageImplements() {
            OutboundMessage message = new TextMessage("Hello");

            assertInstanceOf(TextMessageInterface.class, message);
            assertInstanceOf(OutboundMessage.class, message);
        }

        @Test
        @DisplayName("MediaMessage implements OutboundMessage")
        void mediaMessageImplements() {
            OutboundMessage message = new MediaMessage.Image(
                    new MediaMessage.MediaSource.MediaId("media123"));

            assertInstanceOf(MediaMessageInterface.class, message);
            assertInstanceOf(OutboundMessage.class, message);
        }

        @Test
        @DisplayName("InteractiveMessage implements OutboundMessage")
        void interactiveMessageImplements() {
            OutboundMessage message = InteractiveMessage.ButtonMessage.builder()
                    .body("Choose:")
                    .addButton("yes", "Yes")
                    .build();

            assertInstanceOf(InteractiveMessageInterface.class, message);
            assertInstanceOf(OutboundMessage.class, message);
        }

        @Test
        @DisplayName("LocationMessage implements OutboundMessage")
        void locationMessageImplements() {
            OutboundMessage message = new LocationMessage(-23.550520, -46.633308);

            assertInstanceOf(LocationMessageInterface.class, message);
            assertInstanceOf(OutboundMessage.class, message);
        }

        @Test
        @DisplayName("ReactionMessage implements OutboundMessage")
        void reactionMessageImplements() {
            OutboundMessage message = new ReactionMessage("wamid.123", "👍");

            assertInstanceOf(ReactionMessageInterface.class, message);
            assertInstanceOf(OutboundMessage.class, message);
        }

        @Test
        @DisplayName("TemplateMessage implements OutboundMessage")
        void templateMessageImplements() {
            OutboundMessage message = new TemplateMessage("hello_world", "en_US");

            assertInstanceOf(TemplateMessageInterface.class, message);
            assertInstanceOf(OutboundMessage.class, message);
        }
    }

    @Nested
    @DisplayName("Type Method")
    class TypeMethodTests {

        @Test
        @DisplayName("TextMessage returns TEXT type")
        void textMessageReturnsTextType() {
            OutboundMessage message = new TextMessage("Hello");

            assertEquals(OutboundMessage.OutboundMessageType.TEXT, message.type());
        }

        @Test
        @DisplayName("Image returns IMAGE type")
        void imageReturnsImageType() {
            OutboundMessage message = new MediaMessage.Image(
                    new MediaMessage.MediaSource.MediaId("media123"));

            assertEquals(OutboundMessage.OutboundMessageType.IMAGE, message.type());
        }

        @Test
        @DisplayName("Video returns VIDEO type")
        void videoReturnsVideoType() {
            OutboundMessage message = new MediaMessage.Video(
                    new MediaMessage.MediaSource.MediaId("media123"));

            assertEquals(OutboundMessage.OutboundMessageType.VIDEO, message.type());
        }

        @Test
        @DisplayName("Audio returns AUDIO type")
        void audioReturnsAudioType() {
            OutboundMessage message = new MediaMessage.Audio(
                    new MediaMessage.MediaSource.MediaId("media123"));

            assertEquals(OutboundMessage.OutboundMessageType.AUDIO, message.type());
        }

        @Test
        @DisplayName("ButtonMessage returns INTERACTIVE_BUTTON type")
        void buttonMessageReturnsButtonType() {
            OutboundMessage message = InteractiveMessage.ButtonMessage.builder()
                    .body("Choose:")
                    .addButton("yes", "Yes")
                    .build();

            assertEquals(OutboundMessage.OutboundMessageType.INTERACTIVE_BUTTON, message.type());
        }

        @Test
        @DisplayName("ListMessage returns INTERACTIVE_LIST type")
        void listMessageReturnsListType() {
            OutboundMessage message = InteractiveMessage.ListMessage.builder()
                    .body("Select:")
                    .buttonText("View Options")
                    .addSection("Options")
                    .addRow("opt1", "Option 1")
                    .build();

            assertEquals(OutboundMessage.OutboundMessageType.INTERACTIVE_LIST, message.type());
        }
    }

    @Nested
    @DisplayName("Extract Text Content")
    class ExtractTextContentTests {

        @Test
        @DisplayName("extracts text from TextMessage")
        void extractsTextFromTextMessage() {
            OutboundMessage message = new TextMessage("Hello, World!");

            assertEquals("Hello, World!", message.extractTextContent());
        }

        @Test
        @DisplayName("extracts caption from Image with caption")
        void extractsCaptionFromImage() {
            OutboundMessage message = new MediaMessage.Image(
                    new MediaMessage.MediaSource.MediaId("media123"),
                    "Check this out!");

            String content = message.extractTextContent();
            assertTrue(content.contains("Image"));
            assertTrue(content.contains("Check this out!"));
        }

        @Test
        @DisplayName("extracts placeholder from Image without caption")
        void extractsPlaceholderFromImage() {
            OutboundMessage message = new MediaMessage.Image(
                    new MediaMessage.MediaSource.MediaId("media123"));

            assertEquals("[Image]", message.extractTextContent());
        }

        @Test
        @DisplayName("extracts placeholder from Audio")
        void extractsPlaceholderFromAudio() {
            OutboundMessage message = new MediaMessage.Audio(
                    new MediaMessage.MediaSource.MediaId("media123"));

            assertEquals("[Audio message]", message.extractTextContent());
        }

        @Test
        @DisplayName("extracts body from ButtonMessage")
        void extractsBodyFromButtons() {
            OutboundMessage message = InteractiveMessage.ButtonMessage.builder()
                    .body("Choose an option:")
                    .addButton("yes", "Yes")
                    .build();

            assertEquals("Choose an option:", message.extractTextContent());
        }

        @Test
        @DisplayName("extracts location name")
        void extractsLocationName() {
            OutboundMessage message = LocationMessage.builder()
                    .latitude(-23.550520)
                    .longitude(-46.633308)
                    .name("São Paulo")
                    .build();

            assertEquals("São Paulo", message.extractTextContent());
        }

        @Test
        @DisplayName("extracts location coordinates when no name")
        void extractsLocationCoordinates() {
            OutboundMessage message = new LocationMessage(-23.550520, -46.633308);

            String content = message.extractTextContent();
            assertTrue(content.contains("Location at"));
            assertTrue(content.contains("-23.550520"));
            assertTrue(content.contains("-46.633308"));
        }

        @Test
        @DisplayName("extracts emoji from Reaction")
        void extractsEmojiFromReaction() {
            OutboundMessage message = new ReactionMessage("wamid.123", "👍");

            assertEquals("👍", message.extractTextContent());
        }

        @Test
        @DisplayName("extracts template name")
        void extractsTemplateName() {
            OutboundMessage message = new TemplateMessage("hello_world", "en_US");

            String content = message.extractTextContent();
            assertTrue(content.contains("Template"));
            assertTrue(content.contains("hello_world"));
        }
    }

    @Nested
    @DisplayName("Has Text Content")
    class HasTextContentTests {

        @Test
        @DisplayName("returns true for TextMessage")
        void returnsTrueForTextMessage() {
            OutboundMessage message = new TextMessage("Hello");

            assertTrue(message.hasTextContent());
        }

        @Test
        @DisplayName("returns false for empty TextMessage")
        void returnsFalseForEmptyText() {
            OutboundMessage message = new TextMessage("");

            assertFalse(message.hasTextContent());
        }

        @Test
        @DisplayName("returns false for blank TextMessage")
        void returnsFalseForBlankText() {
            OutboundMessage message = new TextMessage("   ");

            assertFalse(message.hasTextContent());
        }

        @Test
        @DisplayName("returns true for Image with caption")
        void returnsTrueForImageWithCaption() {
            OutboundMessage message = new MediaMessage.Image(
                    new MediaMessage.MediaSource.MediaId("media123"),
                    "Caption");

            assertTrue(message.hasTextContent());
        }

        @Test
        @DisplayName("returns true for Image without caption")
        void returnsTrueForImageWithoutCaption() {
            OutboundMessage message = new MediaMessage.Image(
                    new MediaMessage.MediaSource.MediaId("media123"));

            // [Image] is not blank
            assertTrue(message.hasTextContent());
        }

        @Test
        @DisplayName("returns true for Reaction with emoji")
        void returnsTrueForReaction() {
            OutboundMessage message = new ReactionMessage("wamid.123", "👍");

            assertTrue(message.hasTextContent());
        }
    }

    @Nested
    @DisplayName("Reply Context")
    class ReplyContextTests {

        @Test
        @DisplayName("replyToMessageId() returns null by default")
        void replyToMessageIdReturnsNull() {
            OutboundMessage message = new TextMessage("Hello");

            assertNull(message.replyToMessageId());
        }

        @Test
        @DisplayName("withReplyTo() returns same instance by default")
        void withReplyToReturnsSameInstance() {
            OutboundMessage message = new TextMessage("Hello");

            OutboundMessage replyMessage = message.withReplyTo("wamid.123");

            assertSame(message, replyMessage);
        }

        @Test
        @DisplayName("TextMessage supports reply context")
        void textMessageSupportsReply() {
            TextMessage message = new TextMessage("Hello");

            TextMessage replyMessage = message.withReplyTo("wamid.original");

            assertEquals("wamid.original", replyMessage.replyToMessageId());
            assertNotSame(message, replyMessage);
        }

        @Test
        @DisplayName("MediaMessage supports reply context")
        void mediaMessageSupportsReply() {
            MediaMessage.Image message = new MediaMessage.Image(
                    new MediaMessage.MediaSource.MediaId("media123"));

            MediaMessage.Image replyMessage = message.withReplyTo("wamid.original");

            assertEquals("wamid.original", replyMessage.replyToMessageId());
        }
    }

    @Nested
    @DisplayName("OutboundMessageType Enum")
    class OutboundMessageTypeTests {

        @Test
        @DisplayName("has TEXT type")
        void hasTextType() {
            assertEquals(OutboundMessage.OutboundMessageType.TEXT,
                    OutboundMessage.OutboundMessageType.valueOf("TEXT"));
        }

        @Test
        @DisplayName("has all media types")
        void hasAllMediaTypes() {
            assertNotNull(OutboundMessage.OutboundMessageType.IMAGE);
            assertNotNull(OutboundMessage.OutboundMessageType.VIDEO);
            assertNotNull(OutboundMessage.OutboundMessageType.AUDIO);
            assertNotNull(OutboundMessage.OutboundMessageType.DOCUMENT);
            assertNotNull(OutboundMessage.OutboundMessageType.STICKER);
        }

        @Test
        @displayName("has all interactive types")
        void hasAllInteractiveTypes() {
            assertNotNull(OutboundMessage.OutboundMessageType.INTERACTIVE_BUTTON);
            assertNotNull(OutboundMessage.OutboundMessageType.INTERACTIVE_LIST);
            assertNotNull(OutboundMessage.OutboundMessageType.INTERACTIVE_CTA_URL);
        }

        @Test
        @DisplayName("has other types")
        void hasOtherTypes() {
            assertNotNull(OutboundMessage.OutboundMessageType.TEMPLATE);
            assertNotNull(OutboundMessage.OutboundMessageType.LOCATION);
            assertNotNull(OutboundMessage.OutboundMessageType.CONTACT);
            assertNotNull(OutboundMessage.OutboundMessageType.REACTION);
        }
    }

    @Nested
    @DisplayName("Real-World Scenarios")
    class RealWorldScenariosTests {

        @Test
        @DisplayName("creates customer greeting")
        void createsCustomerGreeting() {
            OutboundMessage message = new TextMessage("Welcome to our service! How can we help you today?");

            assertTrue(message.hasTextContent());
            assertEquals(OutboundMessage.OutboundMessageType.TEXT, message.type());
        }

        @Test
        @DisplayName("creates product image with caption")
        void createsProductImage() {
            OutboundMessage message = new MediaMessage.Image(
                    new MediaMessage.MediaSource.Url("https://example.com/product.jpg"),
                    "Our latest product - now available!");

            assertTrue(message.hasTextContent());
            assertTrue(message.extractTextContent().contains("latest product"));
        }

        @Test
        @DisplayName("creates customer support buttons")
        void createsCustomerSupportButtons() {
            OutboundMessage message = InteractiveMessage.ButtonMessage.builder()
                    .body("How can we assist you?")
                    .addButton("sales", "Talk to Sales")
                    .addButton("support", "Technical Support")
                    .addButton("billing", "Billing Question")
                    .build();

            assertEquals("How can we assist you?", message.extractTextContent());
            assertEquals(OutboundMessage.OutboundMessageType.INTERACTIVE_BUTTON, message.type());
        }

        @Test
        @DisplayName("creates menu list")
        void createsMenuList() {
            OutboundMessage message = InteractiveMessage.ListMessage.builder()
                    .body("Here's our menu:")
                    .buttonText("View Menu")
                    .addSection("Main Dishes")
                    .addRow("pizza", "Pizza Margherita")
                    .addRow("pasta", "Pasta Carbonara")
                    .build();

            assertTrue(message.extractTextContent().contains("menu"));
        }

        @Test
        @DisplayName("creates location share")
        void createsLocationShare() {
            OutboundMessage message = LocationMessage.builder()
                    .latitude(-23.550520)
                    .longitude(-46.633308)
                    .name("Our Office")
                    .address("São Paulo, Brazil")
                    .build();

            assertEquals("Our Office", message.extractTextContent());
        }

        @Test
        @DisplayName("creates reaction to customer message")
        void createsReaction() {
            OutboundMessage message = new ReactionMessage("wamid.customer123", "👍");

            assertEquals("👍", message.extractTextContent());
        }

        @Test
        @DisplayName("creates reply to customer question")
        void createsReply() {
            TextMessage message = new TextMessage("Yes, we're open today until 6 PM")
                    .withReplyTo("wamid.customer.question");

            assertEquals("wamid.customer.question", message.replyToMessageId());
            assertTrue(message.hasTextContent());
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("handles empty text body")
        void handlesEmptyTextBody() {
            OutboundMessage message = new TextMessage("");

            assertEquals("", message.extractTextContent());
            assertFalse(message.hasTextContent());
        }

        @Test
        @DisplayName("handles Unicode in text")
        void handlesUnicodeInText() {
            OutboundMessage message = new TextMessage("こんにちは 🌟 Olá");

            assertEquals("こんにちは 🌟 Olá", message.extractTextContent());
        }

        @Test
        @DisplayName("handles very long text")
        void handlesVeryLongText() {
            String longText = "x".repeat(4096);
            OutboundMessage message = new TextMessage(longText);

            assertEquals(longText, message.extractTextContent());
        }

        @Test
        @DisplayName("handles Reaction removal")
        void handlesReactionRemoval() {
            OutboundMessage message = ReactionMessage.remove("wamid.123");

            assertEquals("", message.extractTextContent());
        }

        @Test
        @DisplayName("handles complex emoji in Reaction")
        void handlesComplexEmoji() {
            OutboundMessage message = new ReactionMessage("wamid.123", "👨‍👩‍👧‍👦");

            assertEquals("👨‍👩‍👧‍👦", message.extractTextContent());
        }
    }
}
