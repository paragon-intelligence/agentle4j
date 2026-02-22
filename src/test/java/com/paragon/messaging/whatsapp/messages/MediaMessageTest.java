package com.paragon.messaging.whatsapp.messages;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.messaging.core.OutboundMessage.OutboundMessageType;
import com.paragon.messaging.whatsapp.messages.MediaMessage.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link MediaMessage} and its implementations. */
@DisplayName("MediaMessage")
class MediaMessageTest {

  private Validator validator;

  @BeforeEach
  void setUp() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Nested
  @DisplayName("MediaSource")
  class MediaSourceTests {

    @Nested
    @DisplayName("URL Source")
    class UrlSourceTests {

      @Test
      @DisplayName("creates URL source with valid HTTPS URL")
      void createsHttpsUrl() {
        MediaSource.Url url = new MediaSource.Url("https://example.com/image.jpg");

        assertEquals("https://example.com/image.jpg", url.url());

        Set<ConstraintViolation<MediaSource.Url>> violations = validator.validate(url);
        assertTrue(violations.isEmpty());
      }

      @Test
      @DisplayName("creates URL source with valid HTTP URL")
      void createsHttpUrl() {
        MediaSource.Url url = new MediaSource.Url("http://example.com/image.jpg");

        Set<ConstraintViolation<MediaSource.Url>> violations = validator.validate(url);
        assertTrue(violations.isEmpty());
      }

      @Test
      @DisplayName("rejects invalid URL format")
      void rejectsInvalidUrl() {
        MediaSource.Url url = new MediaSource.Url("not-a-url");

        Set<ConstraintViolation<MediaSource.Url>> violations = validator.validate(url);
        assertFalse(violations.isEmpty());
      }

      @Test
      @DisplayName("rejects blank URL")
      void rejectsBlankUrl() {
        MediaSource.Url url = new MediaSource.Url("");

        Set<ConstraintViolation<MediaSource.Url>> violations = validator.validate(url);
        assertFalse(violations.isEmpty());
      }
    }

    @Nested
    @DisplayName("MediaID Source")
    class MediaIdSourceTests {

      @Test
      @DisplayName("creates media ID source")
      void createsMediaId() {
        MediaSource.MediaId mediaId = new MediaSource.MediaId("media123");

        assertEquals("media123", mediaId.id());

        Set<ConstraintViolation<MediaSource.MediaId>> violations = validator.validate(mediaId);
        assertTrue(violations.isEmpty());
      }

      @Test
      @DisplayName("rejects blank media ID")
      void rejectsBlankId() {
        MediaSource.MediaId mediaId = new MediaSource.MediaId("");

        Set<ConstraintViolation<MediaSource.MediaId>> violations = validator.validate(mediaId);
        assertFalse(violations.isEmpty());
      }
    }
  }

  @Nested
  @DisplayName("Image Message")
  class ImageMessageTests {

    @Test
    @DisplayName("creates image with media ID")
    void createsWithMediaId() {
      Image image = new Image(new MediaSource.MediaId("img123"));

      assertNotNull(image.source());
      assertTrue(image.caption().isEmpty());
      assertEquals(OutboundMessageType.IMAGE, image.type());
    }

    @Test
    @DisplayName("creates image with caption")
    void createsWithCaption() {
      Image image = new Image(new MediaSource.MediaId("img123"), "Beautiful sunset");

      assertTrue(image.caption().isPresent());
      assertEquals("Beautiful sunset", image.caption().get());
    }

    @Test
    @DisplayName("creates image with URL source")
    void createsWithUrl() {
      Image image = new Image(new MediaSource.Url("https://example.com/image.jpg"));

      assertNotNull(image.source());
    }

    @Test
    @DisplayName("validates caption length <= 1024")
    void validatesCaptionLength() {
      String longCaption = "A".repeat(1025);
      Image image = new Image(new MediaSource.MediaId("img123"), longCaption);

      Set<ConstraintViolation<Image>> violations = validator.validate(image);
      assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("accepts caption at max length")
    void acceptsMaxCaption() {
      String maxCaption = "A".repeat(1024);
      Image image = new Image(new MediaSource.MediaId("img123"), maxCaption);

      Set<ConstraintViolation<Image>> violations = validator.validate(image);
      assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("rejects null source")
    void rejectsNullSource() {
      Image image = new Image(null);

      Set<ConstraintViolation<Image>> violations = validator.validate(image);
      assertFalse(violations.isEmpty());
    }
  }

  @Nested
  @DisplayName("Video Message")
  class VideoMessageTests {

    @Test
    @DisplayName("creates video with media ID")
    void createsWithMediaId() {
      Video video = new Video(new MediaSource.MediaId("vid123"));

      assertNotNull(video.source());
      assertTrue(video.caption().isEmpty());
      assertEquals(OutboundMessageType.VIDEO, video.type());
    }

    @Test
    @DisplayName("creates video with caption")
    void createsWithCaption() {
      Video video = new Video(new MediaSource.MediaId("vid123"), "Funny video");

      assertTrue(video.caption().isPresent());
      assertEquals("Funny video", video.caption().get());
    }

    @Test
    @DisplayName("validates caption length <= 1024")
    void validatesCaptionLength() {
      String longCaption = "A".repeat(1025);
      Video video = new Video(new MediaSource.MediaId("vid123"), longCaption);

      Set<ConstraintViolation<Video>> violations = validator.validate(video);
      assertFalse(violations.isEmpty());
    }
  }

  @Nested
  @DisplayName("Audio Message")
  class AudioMessageTests {

    @Test
    @DisplayName("creates audio with media ID")
    void createsWithMediaId() {
      Audio audio = new Audio(new MediaSource.MediaId("aud123"));

      assertNotNull(audio.source());
      assertTrue(audio.caption().isEmpty());
      assertEquals(OutboundMessageType.AUDIO, audio.type());
    }

    @Test
    @DisplayName("caption() always returns empty")
    void captionAlwaysEmpty() {
      Audio audio = new Audio(new MediaSource.MediaId("aud123"));

      Optional<String> caption = audio.caption();
      assertNotNull(caption);
      assertFalse(caption.isPresent());
    }

    @Test
    @DisplayName("creates audio with URL source")
    void createsWithUrl() {
      Audio audio = new Audio(new MediaSource.Url("https://example.com/audio.mp3"));

      assertNotNull(audio.source());
    }

    @Test
    @DisplayName("validates source not null")
    void validatesSourceNotNull() {
      Audio audio = new Audio(null);

      Set<ConstraintViolation<Audio>> violations = validator.validate(audio);
      assertFalse(violations.isEmpty());
    }
  }

  @Nested
  @DisplayName("Document Message")
  class DocumentMessageTests {

    @Test
    @DisplayName("creates document with media ID only")
    void createsWithMediaIdOnly() {
      Document document = new Document(new MediaSource.MediaId("doc123"), null);

      assertNotNull(document.source());
      assertTrue(document.filename().isEmpty());
      assertTrue(document.caption().isEmpty());
      assertEquals(OutboundMessageType.DOCUMENT, document.type());
    }

    @Test
    @DisplayName("creates document with filename")
    void createsWithFilename() {
      Document document = new Document(new MediaSource.MediaId("doc123"), "report.pdf");

      assertTrue(document.filename().isPresent());
      assertEquals("report.pdf", document.filename().get());
    }

    @Test
    @DisplayName("creates document with filename and caption")
    void createsWithFilenameAndCaption() {
      Document document =
          new Document(new MediaSource.MediaId("doc123"), "report.pdf", "Q4 Financial Report");

      assertTrue(document.filename().isPresent());
      assertTrue(document.caption().isPresent());
      assertEquals("report.pdf", document.filename().get());
      assertEquals("Q4 Financial Report", document.caption().get());
    }

    @Test
    @DisplayName("validates filename length <= 1000")
    void validatesFilenameLength() {
      String longFilename = "A".repeat(1001);
      Document document = new Document(new MediaSource.MediaId("doc123"), longFilename);

      Set<ConstraintViolation<Document>> violations = validator.validate(document);
      assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("validates caption length <= 1024")
    void validatesCaptionLength() {
      String longCaption = "A".repeat(1025);
      Document document = new Document(new MediaSource.MediaId("doc123"), "file.pdf", longCaption);

      Set<ConstraintViolation<Document>> violations = validator.validate(document);
      assertFalse(violations.isEmpty());
    }
  }

  @Nested
  @DisplayName("Sticker Message")
  class StickerMessageTests {

    @Test
    @DisplayName("creates sticker with media ID")
    void createsWithMediaId() {
      Sticker sticker = new Sticker(new MediaSource.MediaId("stk123"));

      assertNotNull(sticker.source());
      assertTrue(sticker.caption().isEmpty());
      assertEquals(OutboundMessageType.STICKER, sticker.type());
    }

    @Test
    @DisplayName("caption() always returns empty")
    void captionAlwaysEmpty() {
      Sticker sticker = new Sticker(new MediaSource.MediaId("stk123"));

      Optional<String> caption = sticker.caption();
      assertNotNull(caption);
      assertFalse(caption.isPresent());
    }

    @Test
    @DisplayName("validates source not null")
    void validatesSourceNotNull() {
      Sticker sticker = new Sticker(null);

      Set<ConstraintViolation<Sticker>> violations = validator.validate(sticker);
      assertFalse(violations.isEmpty());
    }
  }

  @Nested
  @DisplayName("Sealed Interface")
  class SealedInterfaceTests {

    @Test
    @DisplayName("all message types implement MediaMessage")
    void allTypesImplementMediaMessage() {
      MediaSource source = new MediaSource.MediaId("test");

      MediaMessage image = new Image(source);
      MediaMessage video = new Video(source);
      MediaMessage audio = new Audio(source);
      MediaMessage document = new Document(source, null);
      MediaMessage sticker = new Sticker(source);

      assertInstanceOf(MediaMessage.class, image);
      assertInstanceOf(MediaMessage.class, video);
      assertInstanceOf(MediaMessage.class, audio);
      assertInstanceOf(MediaMessage.class, document);
      assertInstanceOf(MediaMessage.class, sticker);
    }

    @Test
    @DisplayName("all message types have source")
    void allTypesHaveSource() {
      MediaSource source = new MediaSource.MediaId("test");

      assertEquals(source, new Image(source).source());
      assertEquals(source, new Video(source).source());
      assertEquals(source, new Audio(source).source());
      assertEquals(source, new Document(source, null).source());
      assertEquals(source, new Sticker(source).source());
    }
  }

  @Nested
  @DisplayName("Edge Cases")
  class EdgeCasesTests {

    @Test
    @DisplayName("handles empty optional caption")
    void handlesEmptyCaption() {
      Image image = new Image(new MediaSource.MediaId("img123"));

      Optional<String> caption = image.caption();
      assertNotNull(caption);
      assertTrue(caption.isEmpty());
    }

    @Test
    @DisplayName("handles null caption string")
    void handlesNullCaptionString() {
      Image image = new Image(new MediaSource.MediaId("img123"), (String) null);

      Optional<String> caption = image.caption();
      assertNotNull(caption);
      assertTrue(caption.isEmpty());
    }

    @Test
    @DisplayName("handles Unicode in caption")
    void handlesUnicodeCaption() {
      Image image = new Image(new MediaSource.MediaId("img123"), "Beautiful 😍 جميل 美丽");

      Set<ConstraintViolation<Image>> violations = validator.validate(image);
      assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("handles special characters in filename")
    void handlesSpecialFilename() {
      Document document =
          new Document(new MediaSource.MediaId("doc123"), "report-2024_final (1).pdf");

      Set<ConstraintViolation<Document>> violations = validator.validate(document);
      assertTrue(violations.isEmpty());
    }
  }
}
