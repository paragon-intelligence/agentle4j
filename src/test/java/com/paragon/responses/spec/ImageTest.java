package com.paragon.responses.spec;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Image} record and its factory methods.
 */
class ImageTest {

  @Nested
  @DisplayName("fromUrl factory methods")
  class FromUrlTests {

    @Test
    @DisplayName("fromUrl(String) creates Image with AUTO detail and null fileId")
    void fromUrlWithDefaultDetail() {
      String url = "https://example.com/image.jpg";
      Image image = Image.fromUrl(url);

      assertEquals(ImageDetail.AUTO, image.detail());
      assertNull(image.fileId());
      assertEquals(url, image.imageUrl());
    }

    @Test
    @DisplayName("fromUrl(ImageDetail, String) creates Image with specified detail")
    void fromUrlWithCustomDetail() {
      String url = "https://example.com/image.png";

      Image lowDetail = Image.fromUrl(ImageDetail.LOW, url);
      assertEquals(ImageDetail.LOW, lowDetail.detail());
      assertNull(lowDetail.fileId());
      assertEquals(url, lowDetail.imageUrl());

      Image highDetail = Image.fromUrl(ImageDetail.HIGH, url);
      assertEquals(ImageDetail.HIGH, highDetail.detail());
      assertNull(highDetail.fileId());
      assertEquals(url, highDetail.imageUrl());
    }

    @Test
    @DisplayName("fromUrl supports base64 data URLs")
    void fromUrlWithBase64DataUrl() {
      String dataUrl = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUg...";
      Image image = Image.fromUrl(dataUrl);

      assertEquals(ImageDetail.AUTO, image.detail());
      assertEquals(dataUrl, image.imageUrl());
    }
  }

  @Nested
  @DisplayName("fromFileId factory methods")
  class FromFileIdTests {

    @Test
    @DisplayName("fromFileId(String) creates Image with AUTO detail and null imageUrl")
    void fromFileIdWithDefaultDetail() {
      String fileId = "file-abc123";
      Image image = Image.fromFileId(fileId);

      assertEquals(ImageDetail.AUTO, image.detail());
      assertEquals(fileId, image.fileId());
      assertNull(image.imageUrl());
    }

    @Test
    @DisplayName("fromFileId(ImageDetail, String) creates Image with specified detail")
    void fromFileIdWithCustomDetail() {
      String fileId = "file-xyz789";

      Image lowDetail = Image.fromFileId(ImageDetail.LOW, fileId);
      assertEquals(ImageDetail.LOW, lowDetail.detail());
      assertEquals(fileId, lowDetail.fileId());
      assertNull(lowDetail.imageUrl());

      Image highDetail = Image.fromFileId(ImageDetail.HIGH, fileId);
      assertEquals(ImageDetail.HIGH, highDetail.detail());
      assertEquals(fileId, highDetail.fileId());
      assertNull(highDetail.imageUrl());
    }
  }

  @Nested
  @DisplayName("toString method")
  class ToStringTests {

    @Test
    @DisplayName("toString returns placeholder string")
    void toStringReturnsPlaceholder() {
      Image imageFromUrl = Image.fromUrl("https://example.com/test.jpg");
      Image imageFromFileId = Image.fromFileId("file-123");

      assertEquals("</input_image>", imageFromUrl.toString());
      assertEquals("</input_image>", imageFromFileId.toString());
    }
  }

  @Nested
  @DisplayName("Record behavior")
  class RecordBehaviorTests {

    @Test
    @DisplayName("Images with same values are equal")
    void equalImages() {
      Image image1 = Image.fromUrl(ImageDetail.HIGH, "https://example.com/img.jpg");
      Image image2 = Image.fromUrl(ImageDetail.HIGH, "https://example.com/img.jpg");

      assertEquals(image1, image2);
      assertEquals(image1.hashCode(), image2.hashCode());
    }

    @Test
    @DisplayName("Images with different values are not equal")
    void differentImages() {
      Image image1 = Image.fromUrl("https://example.com/img1.jpg");
      Image image2 = Image.fromUrl("https://example.com/img2.jpg");

      assertNotEquals(image1, image2);
    }

    @Test
    @DisplayName("Images with different details are not equal")
    void differentDetailsNotEqual() {
      Image low = Image.fromUrl(ImageDetail.LOW, "https://example.com/img.jpg");
      Image high = Image.fromUrl(ImageDetail.HIGH, "https://example.com/img.jpg");

      assertNotEquals(low, high);
    }
  }

  @Nested
  @DisplayName("Interface implementation")
  class InterfaceTests {

    @Test
    @DisplayName("Image implements MessageContent")
    void implementsMessageContent() {
      Image image = Image.fromUrl("https://example.com/test.jpg");
      assertTrue(image instanceof MessageContent);
    }

    @Test
    @DisplayName("Image implements FunctionToolCallOutputKind")
    void implementsFunctionToolCallOutputKind() {
      Image image = Image.fromUrl("https://example.com/test.jpg");
      assertTrue(image instanceof FunctionToolCallOutputKind);
    }

    @Test
    @DisplayName("Image implements CustomToolCallOutputKind")
    void implementsCustomToolCallOutputKind() {
      Image image = Image.fromUrl("https://example.com/test.jpg");
      assertTrue(image instanceof CustomToolCallOutputKind);
    }
  }
}
