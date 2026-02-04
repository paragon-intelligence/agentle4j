package com.paragon.messaging.whatsapp.messages;

import com.paragon.messaging.core.OutboundMessage.OutboundMessageType;
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
 * Tests for {@link TextMessage}.
 */
@DisplayName("TextMessage")
class TextMessageTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Nested
    @DisplayName("Construction")
    class ConstructionTests {

        @Test
        @DisplayName("creates message with body only")
        void createWithBodyOnly() {
            TextMessage message = new TextMessage("Hello");

            assertEquals("Hello", message.body());
            assertFalse(message.previewUrl());
            assertNull(message.replyToMessageId());
        }

        @Test
        @DisplayName("creates message with preview URL")
        void createWithPreviewUrl() {
            TextMessage message = new TextMessage("Check https://example.com", true);

            assertEquals("Check https://example.com", message.body());
            assertTrue(message.previewUrl());
        }

        @Test
        @DisplayName("creates message with reply context")
        void createWithReplyContext() {
            TextMessage message = new TextMessage("Thanks!", false, "wamid.xyz123");

            assertEquals("Thanks!", message.body());
            assertEquals("wamid.xyz123", message.replyToMessageId());
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("builds message with all fields")
        void buildsWithAllFields() {
            TextMessage message = TextMessage.builder()
                    .body("Hello, World!")
                    .previewUrl(true)
                    .replyTo("wamid.123")
                    .build();

            assertEquals("Hello, World!", message.body());
            assertTrue(message.previewUrl());
            assertEquals("wamid.123", message.replyToMessageId());
        }

        @Test
        @DisplayName("enablePreviewUrl() sets preview to true")
        void enablePreviewUrl() {
            TextMessage message = TextMessage.builder()
                    .body("Test")
                    .enablePreviewUrl()
                    .build();

            assertTrue(message.previewUrl());
        }

        @Test
        @DisplayName("disablePreviewUrl() sets preview to false")
        void disablePreviewUrl() {
            TextMessage message = TextMessage.builder()
                    .body("Test")
                    .disablePreviewUrl()
                    .build();

            assertFalse(message.previewUrl());
        }
    }

    @Nested
    @DisplayName("Validation")
    class ValidationTests {

        @Test
        @DisplayName("validates body is not blank")
        void validatesBodyNotBlank() {
            TextMessage message = new TextMessage("");

            Set<ConstraintViolation<TextMessage>> violations = validator.validate(message);

            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getMessage().contains("blank")));
        }

        @Test
        @DisplayName("validates body length <= 4096")
        void validatesBodyLength() {
            String tooLong = "A".repeat(4097);
            TextMessage message = new TextMessage(tooLong);

            Set<ConstraintViolation<TextMessage>> violations = validator.validate(message);

            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getMessage().contains("4096")));
        }

        @Test
        @DisplayName("accepts valid body length")
        void acceptsValidLength() {
            String valid = "A".repeat(4096); // Exactly at limit
            TextMessage message = new TextMessage(valid);

            Set<ConstraintViolation<TextMessage>> violations = validator.validate(message);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("accepts null reply context")
        void acceptsNullReplyContext() {
            TextMessage message = new TextMessage("Test", false, null);

            Set<ConstraintViolation<TextMessage>> violations = validator.validate(message);

            assertTrue(violations.isEmpty());
        }
    }

    @Nested
    @DisplayName("Interface Methods")
    class InterfaceMethodsTests {

        @Test
        @DisplayName("type() returns TEXT")
        void typeReturnsText() {
            TextMessage message = new TextMessage("Test");

            assertEquals(OutboundMessageType.TEXT, message.type());
        }

        @Test
        @DisplayName("replyToMessageId() returns reply context")
        void replyToMessageIdReturns() {
            TextMessage message = new TextMessage("Test", false, "wamid.xyz");

            assertEquals("wamid.xyz", message.replyToMessageId());
        }

        @Test
        @DisplayName("withReplyTo() creates new message with reply")
        void withReplyToCreatesNew() {
            TextMessage original = new TextMessage("Test");
            TextMessage withReply = (TextMessage) original.withReplyTo("wamid.new");

            assertNotSame(original, withReply);
            assertNull(original.replyToMessageId());
            assertEquals("wamid.new", withReply.replyToMessageId());
            assertEquals("Test", withReply.body()); // Preserves other fields
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("handles Unicode characters")
        void handlesUnicode() {
            TextMessage message = new TextMessage("Hello 👋 مرحبا 你好");

            Set<ConstraintViolation<TextMessage>> violations = validator.validate(message);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("handles special characters")
        void handlesSpecialChars() {
            TextMessage message = new TextMessage("Quote: \"test\"\nNewline\tTab");

            Set<ConstraintViolation<TextMessage>> violations = validator.validate(message);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("handles single character")
        void handlesSingleChar() {
            TextMessage message = new TextMessage("A");

            Set<ConstraintViolation<TextMessage>> violations = validator.validate(message);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("handles whitespace-only body as invalid")
        void handlesWhitespaceOnly() {
            TextMessage message = new TextMessage("   ");

            Set<ConstraintViolation<TextMessage>> violations = validator.validate(message);

            // @NotBlank should catch whitespace-only
            assertFalse(violations.isEmpty());
        }
    }

    @Nested
    @DisplayName("Constants")
    class ConstantsTests {

        @Test
        @DisplayName("MAX_BODY_LENGTH is 4096")
        void maxBodyLengthIs4096() {
            assertEquals(4096, TextMessage.MAX_BODY_LENGTH);
        }
    }
}
