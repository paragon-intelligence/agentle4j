package com.paragon.messaging.whatsapp.messages;

import com.paragon.messaging.core.OutboundMessage.OutboundMessageType;
import com.paragon.messaging.whatsapp.messages.TemplateMessage.TemplateComponent;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TemplateMessage}.
 */
@DisplayName("TemplateMessage")
class TemplateMessageTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Nested
    @DisplayName("Construction")
    class ConstructionTests {

        @Test
        @DisplayName("creates template with name and language")
        void createsWithNameAndLanguage() {
            TemplateMessage template = new TemplateMessage(
                    "hello_world",
                    "en_US",
                    List.of());

            assertEquals("hello_world", template.name());
            assertEquals("en_US", template.languageCode());
            assertTrue(template.components().isEmpty());
        }

        @Test
        @DisplayName("creates template with components")
        void createsWithComponents() {
            TemplateComponent bodyComponent = new TemplateComponent(
                    "body",
                    List.of("John", "5"));

            TemplateMessage template = new TemplateMessage(
                    "order_confirmation",
                    "pt_BR",
                    List.of(bodyComponent));

            assertEquals(1, template.components().size());
            assertEquals("body", template.components().get(0).type());
        }

        @Test
        @DisplayName("creates immutable component list")
        void createsImmutableList() {
            TemplateMessage template = new TemplateMessage(
                    "test",
                    "en",
                    List.of(new TemplateComponent("header", List.of("param1"))));

            assertThrows(UnsupportedOperationException.class, () ->
                    template.components().add(new TemplateComponent("body", List.of())));
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("builds template with all fields")
        void buildsWithAllFields() {
            TemplateMessage template = TemplateMessage.builder()
                    .name("welcome_message")
                    .language("en_US")
                    .addComponent("body", "Alice", "2024")
                    .addComponent("footer", "© 2024")
                    .build();

            assertEquals("welcome_message", template.name());
            assertEquals("en_US", template.languageCode());
            assertEquals(2, template.components().size());
        }

        @Test
        @DisplayName("addComponent creates component correctly")
        void addComponentCreates() {
            TemplateMessage template = TemplateMessage.builder()
                    .name("test")
                    .language("en")
                    .addComponent("body", "param1", "param2", "param3")
                    .build();

            TemplateComponent component = template.components().get(0);
            assertEquals("body", component.type());
            assertEquals(3, component.parameters().size());
            assertEquals("param1", component.parameters().get(0));
        }

        @Test
        @DisplayName("builds template without components")
        void buildsWithoutComponents() {
            TemplateMessage template = TemplateMessage.builder()
                    .name("simple_template")
                    .language("en")
                    .build();

            assertTrue(template.components().isEmpty());
        }
    }

    @Nested
    @DisplayName("Validation")
    class ValidationTests {

        @Test
        @DisplayName("validates template name is not blank")
        void validatesNameNotBlank() {
            TemplateMessage template = new TemplateMessage("", "en", List.of());

            Set<ConstraintViolation<TemplateMessage>> violations = validator.validate(template);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getMessage().contains("blank")));
        }

        @Test
        @DisplayName("validates template name format")
        void validatesNameFormat() {
            // Invalid: contains uppercase or spaces
            TemplateMessage invalid1 = new TemplateMessage("HelloWorld", "en", List.of());
            TemplateMessage invalid2 = new TemplateMessage("hello world", "en", List.of());
            TemplateMessage invalid3 = new TemplateMessage("hello-world", "en", List.of());

            assertFalse(validator.validate(invalid1).isEmpty());
            assertFalse(validator.validate(invalid2).isEmpty());
            assertFalse(validator.validate(invalid3).isEmpty());
        }

        @Test
        @DisplayName("accepts valid template names")
        void acceptsValidNames() {
            TemplateMessage valid1 = new TemplateMessage("hello_world", "en", List.of());
            TemplateMessage valid2 = new TemplateMessage("order_123", "en", List.of());
            TemplateMessage valid3 = new TemplateMessage("welcome_2024", "en", List.of());

            assertTrue(validator.validate(valid1).isEmpty());
            assertTrue(validator.validate(valid2).isEmpty());
            assertTrue(validator.validate(valid3).isEmpty());
        }

        @Test
        @DisplayName("validates language code format")
        void validatesLanguageCodeFormat() {
            // Valid formats
            TemplateMessage valid1 = new TemplateMessage("test", "en", List.of());
            TemplateMessage valid2 = new TemplateMessage("test", "pt_BR", List.of());
            TemplateMessage valid3 = new TemplateMessage("test", "en_US", List.of());

            assertTrue(validator.validate(valid1).isEmpty());
            assertTrue(validator.validate(valid2).isEmpty());
            assertTrue(validator.validate(valid3).isEmpty());
        }

        @Test
        @DisplayName("rejects invalid language codes")
        void rejectsInvalidLanguageCodes() {
            TemplateMessage invalid1 = new TemplateMessage("test", "EN", List.of()); // uppercase
            TemplateMessage invalid2 = new TemplateMessage("test", "eng", List.of()); // 3 letters
            TemplateMessage invalid3 = new TemplateMessage("test", "en-US", List.of()); // dash instead of underscore

            assertFalse(validator.validate(invalid1).isEmpty());
            assertFalse(validator.validate(invalid2).isEmpty());
            assertFalse(validator.validate(invalid3).isEmpty());
        }

        @Test
        @DisplayName("validates components not null")
        void validatesComponentsNotNull() {
            assertThrows(NullPointerException.class, () ->
                    new TemplateMessage("test", "en", null));
        }
    }

    @Nested
    @DisplayName("Template Component")
    class TemplateComponentTests {

        @Test
        @DisplayName("creates component with type and parameters")
        void createsComponent() {
            TemplateComponent component = new TemplateComponent(
                    "body",
                    List.of("param1", "param2"));

            assertEquals("body", component.type());
            assertEquals(2, component.parameters().size());
        }

        @Test
        @DisplayName("creates immutable parameter list")
        void createsImmutableParameters() {
            TemplateComponent component = new TemplateComponent(
                    "body",
                    List.of("param1"));

            assertThrows(UnsupportedOperationException.class, () ->
                    component.parameters().add("param2"));
        }

        @Test
        @DisplayName("validates component type not blank")
        void validatesTypeNotBlank() {
            TemplateComponent component = new TemplateComponent("", List.of());

            Set<ConstraintViolation<TemplateComponent>> violations = validator.validate(component);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("validates parameters not null")
        void validatesParametersNotNull() {
            assertThrows(NullPointerException.class, () ->
                    new TemplateComponent("body", null));
        }

        @Test
        @DisplayName("accepts empty parameter list")
        void acceptsEmptyParameters() {
            TemplateComponent component = new TemplateComponent("header", List.of());

            Set<ConstraintViolation<TemplateComponent>> violations = validator.validate(component);
            assertTrue(violations.isEmpty());
        }
    }

    @Nested
    @DisplayName("Interface Methods")
    class InterfaceMethodsTests {

        @Test
        @DisplayName("type() returns TEMPLATE")
        void typeReturnsTemplate() {
            TemplateMessage template = new TemplateMessage("test", "en", List.of());

            assertEquals(OutboundMessageType.TEMPLATE, template.type());
        }
    }

    @Nested
    @DisplayName("Common Template Scenarios")
    class CommonTemplateTests {

        @Test
        @DisplayName("creates hello_world template")
        void createsHelloWorld() {
            TemplateMessage template = TemplateMessage.builder()
                    .name("hello_world")
                    .language("en_US")
                    .build();

            Set<ConstraintViolation<TemplateMessage>> violations = validator.validate(template);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("creates order confirmation template")
        void createsOrderConfirmation() {
            TemplateMessage template = TemplateMessage.builder()
                    .name("order_confirmation")
                    .language("pt_BR")
                    .addComponent("body", "12345", "R$ 99.00", "2024-02-04")
                    .build();

            assertEquals("order_confirmation", template.name());
            assertEquals(1, template.components().size());
            assertEquals(3, template.components().get(0).parameters().size());
        }

        @Test
        @DisplayName("creates appointment reminder template")
        void createsAppointmentReminder() {
            TemplateMessage template = TemplateMessage.builder()
                    .name("appointment_reminder")
                    .language("en")
                    .addComponent("body", "Dr. Smith", "Feb 5, 2024", "2:00 PM")
                    .addComponent("footer")
                    .build();

            assertEquals(2, template.components().size());
        }

        @Test
        @DisplayName("creates shipping notification template")
        void createsShippingNotification() {
            TemplateMessage template = TemplateMessage.builder()
                    .name("shipping_notification")
                    .language("en_US")
                    .addComponent("header", "Your package")
                    .addComponent("body", "ABC123XYZ", "2-3 days")
                    .build();

            assertEquals(2, template.components().size());
            assertEquals("header", template.components().get(0).type());
            assertEquals("body", template.components().get(1).type());
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("handles template with many components")
        void handlesManyComponents() {
            TemplateMessage.Builder builder = TemplateMessage.builder()
                    .name("complex_template")
                    .language("en");

            for (int i = 0; i < 10; i++) {
                builder.addComponent("param" + i, "value" + i);
            }

            TemplateMessage template = builder.build();
            assertEquals(10, template.components().size());
        }

        @Test
        @DisplayName("handles component with many parameters")
        void handlesManyParameters() {
            String[] params = new String[20];
            for (int i = 0; i < 20; i++) {
                params[i] = "param" + i;
            }

            TemplateMessage template = TemplateMessage.builder()
                    .name("test")
                    .language("en")
                    .addComponent("body", params)
                    .build();

            assertEquals(20, template.components().get(0).parameters().size());
        }

        @Test
        @DisplayName("handles Unicode in parameters")
        void handlesUnicodeParameters() {
            TemplateMessage template = TemplateMessage.builder()
                    .name("multilingual_template")
                    .language("pt_BR")
                    .addComponent("body", "João", "São Paulo", "Olá 👋")
                    .build();

            Set<ConstraintViolation<TemplateMessage>> violations = validator.validate(template);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("handles all supported language codes")
        void handlesAllLanguageCodes() {
            String[] languageCodes = {"en", "pt", "es", "fr", "de", "en_US", "pt_BR", "es_MX"};

            for (String code : languageCodes) {
                TemplateMessage template = new TemplateMessage("test", code, List.of());
                Set<ConstraintViolation<TemplateMessage>> violations = validator.validate(template);
                assertTrue(violations.isEmpty(), "Language code " + code + " should be valid");
            }
        }

        @Test
        @DisplayName("handles long template names")
        void handlesLongNames() {
            String longName = "very_long_template_name_with_many_underscores_and_numbers_123_456";
            TemplateMessage template = new TemplateMessage(longName, "en", List.of());

            Set<ConstraintViolation<TemplateMessage>> violations = validator.validate(template);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("handles empty string parameters")
        void handlesEmptyStringParameters() {
            TemplateMessage template = TemplateMessage.builder()
                    .name("test")
                    .language("en")
                    .addComponent("body", "", "valid", "")
                    .build();

            // Should accept empty strings as parameters
            assertNotNull(template);
        }
    }
}
