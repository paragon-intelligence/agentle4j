package com.paragon.messaging.whatsapp;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.messaging.core.Recipient;
import com.paragon.messaging.whatsapp.messages.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link WhatsAppMessageSerializer}. */
@DisplayName("WhatsAppMessageSerializer")
class WhatsAppMessageSerializerTest {

  private WhatsAppMessageSerializer serializer;
  private Recipient recipient;

  @BeforeEach
  void setUp() {
    serializer = new WhatsAppMessageSerializer();
    recipient = Recipient.ofPhoneNumber("5511999999999");
  }

  @Nested
  @DisplayName("Text Message Serialization")
  class TextMessageSerializationTests {

    @Test
    @DisplayName("serializes simple text message")
    void serializeSimpleTextMessage() {
      TextMessage message = new TextMessage("Hello, World!");

      String json = serializer.serialize(recipient, message);

      assertNotNull(json);
      assertTrue(json.contains("\"messaging_product\":\"whatsapp\""));
      assertTrue(json.contains("\"to\":\"5511999999999\""));
      assertTrue(json.contains("\"type\":\"text\""));
      assertTrue(json.contains("\"text\""));
      assertTrue(json.contains("\"body\":\"Hello, World!\""));
    }

    @Test
    @DisplayName("serializes text message with URL preview")
    void serializeTextMessageWithPreview() {
      TextMessage message = new TextMessage("Check out https://example.com", true);

      String json = serializer.serialize(recipient, message);

      assertTrue(json.contains("\"preview_url\":true"));
    }

    @Test
    @DisplayName("serializes text message with reply context")
    void serializeTextMessageWithReply() {
      TextMessage message = TextMessage.builder().body("Thanks!").replyTo("wamid.xyz123").build();

      String json = serializer.serialize(recipient, message);

      assertTrue(json.contains("\"context\""));
      assertTrue(json.contains("\"message_id\":\"wamid.xyz123\""));
    }

    @Test
    @DisplayName("escapes special characters in text")
    void escapesSpecialCharacters() {
      TextMessage message = new TextMessage("Quote: \"Hello\"\nNewline\tTab\\Backslash");

      String json = serializer.serialize(recipient, message);

      assertTrue(json.contains("\\\""));
      assertTrue(json.contains("\\n"));
      assertTrue(json.contains("\\t"));
      assertTrue(json.contains("\\\\"));
    }
  }

  @Nested
  @DisplayName("Media Message Serialization")
  class MediaMessageSerializationTests {

    @Test
    @DisplayName("serializes image with media ID")
    void serializeImageWithMediaId() {
      MediaMessage.Image image =
          new MediaMessage.Image(new MediaMessage.MediaSource.MediaId("media123"));

      String json = serializer.serialize(recipient, image);

      assertTrue(json.contains("\"type\":\"image\""));
      assertTrue(json.contains("\"image\""));
      assertTrue(json.contains("\"id\":\"media123\""));
      assertFalse(json.contains("\"link\""));
    }

    @Test
    @DisplayName("serializes image with URL")
    void serializeImageWithUrl() {
      MediaMessage.Image image =
          new MediaMessage.Image(new MediaMessage.MediaSource.Url("https://example.com/image.jpg"));

      String json = serializer.serialize(recipient, image);

      assertTrue(json.contains("\"type\":\"image\""));
      assertTrue(json.contains("\"link\":\"https://example.com/image.jpg\""));
      assertFalse(json.contains("\"id\""));
    }

    @Test
    @DisplayName("serializes image with caption")
    void serializeImageWithCaption() {
      MediaMessage.Image image =
          new MediaMessage.Image(
              new MediaMessage.MediaSource.MediaId("media123"), "Beautiful sunset");

      String json = serializer.serialize(recipient, image);

      assertTrue(json.contains("\"caption\":\"Beautiful sunset\""));
    }

    @Test
    @DisplayName("serializes video message")
    void serializeVideo() {
      MediaMessage.Video video =
          new MediaMessage.Video(new MediaMessage.MediaSource.MediaId("video123"), "Funny video");

      String json = serializer.serialize(recipient, video);

      assertTrue(json.contains("\"type\":\"video\""));
      assertTrue(json.contains("\"video\""));
      assertTrue(json.contains("\"id\":\"video123\""));
      assertTrue(json.contains("\"caption\":\"Funny video\""));
    }

    @Test
    @DisplayName("serializes audio message")
    void serializeAudio() {
      MediaMessage.Audio audio =
          new MediaMessage.Audio(new MediaMessage.MediaSource.MediaId("audio123"));

      String json = serializer.serialize(recipient, audio);

      assertTrue(json.contains("\"type\":\"audio\""));
      assertTrue(json.contains("\"audio\""));
      assertTrue(json.contains("\"id\":\"audio123\""));
      assertFalse(json.contains("\"caption\""));
    }

    @Test
    @DisplayName("serializes document message")
    void serializeDocument() {
      MediaMessage.Document document =
          new MediaMessage.Document(
              new MediaMessage.MediaSource.MediaId("doc123"), "report.pdf", "Q4 Report");

      String json = serializer.serialize(recipient, document);

      assertTrue(json.contains("\"type\":\"document\""));
      assertTrue(json.contains("\"document\""));
      assertTrue(json.contains("\"id\":\"doc123\""));
      assertTrue(json.contains("\"filename\":\"report.pdf\""));
      assertTrue(json.contains("\"caption\":\"Q4 Report\""));
    }

    @Test
    @DisplayName("serializes sticker message")
    void serializeSticker() {
      MediaMessage.Sticker sticker =
          new MediaMessage.Sticker(new MediaMessage.MediaSource.MediaId("sticker123"));

      String json = serializer.serialize(recipient, sticker);

      assertTrue(json.contains("\"type\":\"sticker\""));
      assertTrue(json.contains("\"sticker\""));
      assertTrue(json.contains("\"id\":\"sticker123\""));
    }
  }

  @Nested
  @DisplayName("Interactive Message Serialization")
  class InteractiveMessageSerializationTests {

    @Test
    @DisplayName("serializes button message")
    void serializeButtonMessage() {
      InteractiveMessage.ButtonMessage buttonMsg =
          InteractiveMessage.ButtonMessage.builder()
              .bodyText("Choose an option:")
              .addButton("option1", "Option 1")
              .addButton("option2", "Option 2")
              .build();

      String json = serializer.serialize(recipient, buttonMsg);

      assertTrue(json.contains("\"type\":\"interactive\""));
      assertTrue(json.contains("\"interactive\""));
      assertTrue(json.contains("\"button\""));
      assertTrue(json.contains("\"body\""));
      assertTrue(json.contains("\"text\":\"Choose an option:\""));
      assertTrue(json.contains("\"action\""));
      assertTrue(json.contains("\"buttons\""));
      assertTrue(json.contains("\"id\":\"option1\""));
      assertTrue(json.contains("\"title\":\"Option 1\""));
    }

    @Test
    @DisplayName("serializes list message")
    void serializeListMessage() {
      InteractiveMessage.ListMessage listMsg =
          InteractiveMessage.ListMessage.builder()
              .bodyText("Select from menu:")
              .buttonText("View Menu")
              .addSection("Main Dishes")
              .addRow("dish1", "Pasta", "Delicious pasta")
              .addRow("dish2", "Pizza", "Wood-fired pizza")
              .build();

      String json = serializer.serialize(recipient, listMsg);

      assertTrue(json.contains("\"type\":\"interactive\""));
      assertTrue(json.contains("\"list\""));
      assertTrue(json.contains("\"body\""));
      assertTrue(json.contains("\"button\":\"View Menu\""));
      assertTrue(json.contains("\"sections\""));
      assertTrue(json.contains("\"title\":\"Main Dishes\""));
      assertTrue(json.contains("\"id\":\"dish1\""));
      assertTrue(json.contains("\"title\":\"Pasta\""));
      assertTrue(json.contains("\"description\":\"Delicious pasta\""));
    }

    @Test
    @DisplayName("serializes CTA URL message")
    void serializeCtaUrlMessage() {
      InteractiveMessage.CtaUrlMessage ctaMsg =
          InteractiveMessage.CtaUrlMessage.builder()
              .bodyText("Visit our website:")
              .footerText("Click to learn more")
              .displayText("Visit Now")
              .url("https://example.com")
              .build();

      String json = serializer.serialize(recipient, ctaMsg);

      assertTrue(json.contains("\"type\":\"interactive\""));
      assertTrue(json.contains("\"cta_url\""));
      assertTrue(json.contains("\"body\""));
      assertTrue(json.contains("\"footer\""));
      assertTrue(json.contains("\"display_text\":\"Visit Now\""));
      assertTrue(json.contains("\"url\":\"https://example.com\""));
    }
  }

  @Nested
  @DisplayName("Location Message Serialization")
  class LocationMessageSerializationTests {

    @Test
    @DisplayName("serializes location message")
    void serializeLocationMessage() {
      LocationMessage location =
          new LocationMessage(-23.5505, -46.6333, "São Paulo", "City center");

      String json = serializer.serialize(recipient, location);

      assertTrue(json.contains("\"type\":\"location\""));
      assertTrue(json.contains("\"location\""));
      assertTrue(json.contains("\"latitude\":-23.5505"));
      assertTrue(json.contains("\"longitude\":-46.6333"));
      assertTrue(json.contains("\"name\":\"São Paulo\""));
      assertTrue(json.contains("\"address\":\"City center\""));
    }

    @Test
    @DisplayName("serializes location without name and address")
    void serializeLocationMinimal() {
      LocationMessage location = new LocationMessage(0.0, 0.0, (String) null, (String) null);

      String json = serializer.serialize(recipient, location);

      assertTrue(json.contains("\"latitude\":0.0"));
      assertTrue(json.contains("\"longitude\":0.0"));
      assertFalse(json.contains("\"name\""));
      assertFalse(json.contains("\"address\""));
    }
  }

  @Nested
  @DisplayName("Reaction Message Serialization")
  class ReactionMessageSerializationTests {

    @Test
    @DisplayName("serializes reaction message")
    void serializeReaction() {
      ReactionMessage reaction = new ReactionMessage("wamid.xyz123", "👍");

      String json = serializer.serialize(recipient, reaction);

      assertTrue(json.contains("\"type\":\"reaction\""));
      assertTrue(json.contains("\"reaction\""));
      assertTrue(json.contains("\"message_id\":\"wamid.xyz123\""));
      assertTrue(json.contains("\"emoji\":\"👍\""));
    }

    @Test
    @DisplayName("serializes reaction removal")
    void serializeReactionRemoval() {
      ReactionMessage reaction = new ReactionMessage("wamid.xyz123", "");

      String json = serializer.serialize(recipient, reaction);

      assertTrue(json.contains("\"emoji\":\"\""));
    }
  }

  @Nested
  @DisplayName("Template Message Serialization")
  class TemplateMessageSerializationTests {

    @Test
    @DisplayName("serializes simple template message")
    void serializeSimpleTemplate() {
      TemplateMessage template =
          TemplateMessage.builder().name("hello_world").language("en_US").build();

      String json = serializer.serialize(recipient, template);

      assertTrue(json.contains("\"type\":\"template\""));
      assertTrue(json.contains("\"template\""));
      assertTrue(json.contains("\"name\":\"hello_world\""));
      assertTrue(json.contains("\"language\""));
      assertTrue(json.contains("\"code\":\"en_US\""));
    }

    @Test
    @DisplayName("serializes template with parameters")
    void serializeTemplateWithParameters() {
      TemplateMessage template =
          TemplateMessage.builder()
              .name("order_confirmation")
              .language("pt_BR")
              .addBodyParameter("12345")
              .addBodyParameter("R$ 99.00")
              .build();

      String json = serializer.serialize(recipient, template);

      assertTrue(json.contains("\"name\":\"order_confirmation\""));
      assertTrue(json.contains("\"components\""));
      assertTrue(json.contains("\"type\":\"body\""));
      assertTrue(json.contains("\"parameters\""));
      assertTrue(json.contains("\"12345\""));
      assertTrue(json.contains("\"R$ 99.00\""));
    }
  }

  @Nested
  @DisplayName("Contact Message Serialization")
  class ContactMessageSerializationTests {

    @Test
    @DisplayName("serializes contact message")
    void serializeContact() {
      ContactMessage contact =
          ContactMessage.builder()
              .formattedName("John Doe")
              .addPhone("+5511999999999", "CELL")
              .addEmail("john@example.com", "WORK")
              .build();

      String json = serializer.serialize(recipient, contact);

      assertTrue(json.contains("\"type\":\"contacts\""));
      assertTrue(json.contains("\"contacts\""));
      assertTrue(json.contains("\"formatted_name\":\"John Doe\""));
      assertTrue(json.contains("\"phone\":\"+5511999999999\""));
      assertTrue(json.contains("\"type\":\"CELL\""));
      assertTrue(json.contains("\"email\":\"john@example.com\""));
    }
  }

  @Nested
  @DisplayName("Edge Cases")
  class EdgeCasesTests {

    @Test
    @DisplayName("handles empty text message body")
    void handlesEmptyBody() {
      TextMessage message = new TextMessage("");

      // Should still serialize but might fail validation later
      String json = serializer.serialize(recipient, message);
      assertNotNull(json);
    }

    @Test
    @DisplayName("handles very long text")
    void handlesLongText() {
      String longText = "A".repeat(4096);
      TextMessage message = new TextMessage(longText);

      String json = serializer.serialize(recipient, message);
      assertTrue(json.contains(longText));
    }

    @Test
    @DisplayName("handles special Unicode characters")
    void handlesUnicode() {
      TextMessage message = new TextMessage("Hello 👋 مرحبا 你好 こんにちは");

      String json = serializer.serialize(recipient, message);
      assertTrue(json.contains("Hello"));
      assertTrue(json.contains("👋"));
    }

    @Test
    @DisplayName("handles null optional fields gracefully")
    void handlesNullOptionals() {
      TextMessage message = new TextMessage("Test", false, null);

      String json = serializer.serialize(recipient, message);
      assertNotNull(json);
      assertFalse(json.contains("\"context\""));
    }
  }

  @Nested
  @DisplayName("JSON Structure Validation")
  class JsonStructureValidationTests {

    @Test
    @DisplayName("includes required WhatsApp fields")
    void includesRequiredFields() {
      TextMessage message = new TextMessage("Test");

      String json = serializer.serialize(recipient, message);

      // All WhatsApp messages must have these fields
      assertTrue(json.contains("\"messaging_product\":\"whatsapp\""));
      assertTrue(json.contains("\"to\""));
      assertTrue(json.contains("\"type\""));
    }

    @Test
    @DisplayName("produces valid JSON structure")
    void producesValidJson() {
      TextMessage message = new TextMessage("Test");

      String json = serializer.serialize(recipient, message);

      // Basic JSON validation
      assertTrue(json.startsWith("{"));
      assertTrue(json.endsWith("}"));

      // Count braces
      long openBraces = json.chars().filter(ch -> ch == '{').count();
      long closeBraces = json.chars().filter(ch -> ch == '}').count();
      assertEquals(openBraces, closeBraces);
    }
  }
}
