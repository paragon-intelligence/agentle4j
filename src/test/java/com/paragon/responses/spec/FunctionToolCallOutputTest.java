package com.paragon.responses.spec;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for FunctionToolCallOutput.
 *
 * <p>Tests cover:
 * - Factory methods (success, error, inProgress)
 * - Image and file outputs
 * - CallId generation
 */
@DisplayName("FunctionToolCallOutput Tests")
class FunctionToolCallOutputTest {

  // ═══════════════════════════════════════════════════════════════════════════
  // SUCCESS FACTORY
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Success Factory")
  class SuccessFactory {

    @Test
    @DisplayName("success with message generates callId")
    void successWithMessage() {
      FunctionToolCallOutput output = FunctionToolCallOutput.success("Done!");

      assertNotNull(output.callId());
      assertTrue(output.callId().startsWith("call_"));
      assertTrue(output.output() instanceof Text);
      assertEquals("Done!", output.toString());
      assertEquals(FunctionToolCallOutputStatus.COMPLETED, output.status());
    }

    @Test
    @DisplayName("success with callId and message")
    void successWithCallIdAndMessage() {
      FunctionToolCallOutput output = FunctionToolCallOutput.success("call_123", "Success!");

      assertEquals("call_123", output.callId());
      assertTrue(output.output() instanceof Text);
      assertEquals(FunctionToolCallOutputStatus.COMPLETED, output.status());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ERROR FACTORY
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Error Factory")
  class ErrorFactory {

    @Test
    @DisplayName("error with message generates callId")
    void errorWithMessage() {
      FunctionToolCallOutput output = FunctionToolCallOutput.error("Something went wrong");

      assertNotNull(output.callId());
      assertTrue(output.output() instanceof Text);
      assertTrue(output.toString().contains("Error:"));
      assertEquals(FunctionToolCallOutputStatus.INCOMPLETE, output.status());
    }

    @Test
    @DisplayName("error with callId and message")
    void errorWithCallIdAndMessage() {
      FunctionToolCallOutput output = FunctionToolCallOutput.error("call_456", "Failed");

      assertEquals("call_456", output.callId());
      assertEquals(FunctionToolCallOutputStatus.INCOMPLETE, output.status());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // IN PROGRESS FACTORY
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("InProgress Factory")
  class InProgressFactory {

    @Test
    @DisplayName("inProgress with message generates callId")
    void inProgressWithMessage() {
      FunctionToolCallOutput output = FunctionToolCallOutput.inProgress("Working...");

      assertNotNull(output.callId());
      assertEquals(FunctionToolCallOutputStatus.IN_PROGRESS, output.status());
    }

    @Test
    @DisplayName("inProgress with callId and message")
    void inProgressWithCallIdAndMessage() {
      FunctionToolCallOutput output = FunctionToolCallOutput.inProgress("call_789", "Processing");

      assertEquals("call_789", output.callId());
      assertEquals(FunctionToolCallOutputStatus.IN_PROGRESS, output.status());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // IMAGE OUTPUT
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Image Output")
  class ImageOutput {

    @Test
    @DisplayName("withImage generates callId")
    void withImageGeneratesCallId() {
      Image image = Image.fromUrl("https://example.com/image.png");
      FunctionToolCallOutput output = FunctionToolCallOutput.withImage(image);

      assertNotNull(output.callId());
      assertTrue(output.output() instanceof Image);
      assertEquals(FunctionToolCallOutputStatus.COMPLETED, output.status());
    }

    @Test
    @DisplayName("withImage with callId")
    void withImageWithCallId() {
      Image image = Image.fromUrl("https://example.com/img.jpg");
      FunctionToolCallOutput output = FunctionToolCallOutput.withImage("call_img", image);

      assertEquals("call_img", output.callId());
      assertTrue(output.output() instanceof Image);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // FILE OUTPUT
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("File Output")
  class FileOutput {

    @Test
    @DisplayName("withFile generates callId")
    void withFileGeneratesCallId() {
      File file = new File("base64data", "file_123", null, "report.pdf");
      FunctionToolCallOutput output = FunctionToolCallOutput.withFile(file);

      assertNotNull(output.callId());
      assertTrue(output.output() instanceof File);
      assertEquals(FunctionToolCallOutputStatus.COMPLETED, output.status());
    }

    @Test
    @DisplayName("withFile with callId")
    void withFileWithCallId() {
      File file = new File("base64data", "file_456", null, "data.csv");
      FunctionToolCallOutput output = FunctionToolCallOutput.withFile("call_file", file);

      assertEquals("call_file", output.callId());
      assertTrue(output.output() instanceof File);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ACCESSORS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Accessors")
  class Accessors {

    @Test
    @DisplayName("id is initially null")
    void idIsNull() {
      FunctionToolCallOutput output = FunctionToolCallOutput.success("Test");

      assertNull(output.id());
    }

    @Test
    @DisplayName("toString returns output toString")
    void toStringWorks() {
      FunctionToolCallOutput output = FunctionToolCallOutput.success("Test message");

      assertEquals("Test message", output.toString());
    }
  }
}
