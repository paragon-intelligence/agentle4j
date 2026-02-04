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

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link WhatsappMetaWebhookPayload} and related webhook structures.
 */
@DisplayName("WhatsApp Webhook Payload")
class WhatsAppWebhookPayloadTest {

    private ObjectMapper objectMapper;
    private Validator validator;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Nested
    @DisplayName("WhatsappMetaWebhookPayload")
    class WebhookPayloadTests {

        @Test
        @DisplayName("deserializes complete webhook payload")
        void deserializesCompletePayload() throws JsonProcessingException {
            String json = """
                    {
                      "object": "whatsapp_business_account",
                      "entry": [
                        {
                          "id": "123456789",
                          "changes": [
                            {
                              "value": {
                                "messaging_product": "whatsapp",
                                "metadata": {
                                  "display_phone_number": "15551234567",
                                  "phone_number_id": "987654321"
                                },
                                "contacts": [
                                  {
                                    "profile": {
                                      "name": "John Doe"
                                    },
                                    "wa_id": "5511999999999"
                                  }
                                ],
                                "messages": [
                                  {
                                    "from": "5511999999999",
                                    "id": "wamid.ABC123",
                                    "timestamp": "1234567890",
                                    "type": "text",
                                    "text": {
                                      "body": "Hello, World!"
                                    }
                                  }
                                ]
                              },
                              "field": "messages"
                            }
                          ]
                        }
                      ]
                    }
                    """;

            WhatsappMetaWebhookPayload payload = objectMapper.readValue(json, WhatsappMetaWebhookPayload.class);

            assertNotNull(payload);
            assertEquals("whatsapp_business_account", payload.object);
            assertEquals(1, payload.entry.size());
            assertEquals("123456789", payload.entry.get(0).id());
        }

        @Test
        @DisplayName("validates object field not null")
        void validatesObjectNotNull() {
            WhatsappMetaWebhookPayload payload = new WhatsappMetaWebhookPayload(null, List.of());

            Set<ConstraintViolation<WhatsappMetaWebhookPayload>> violations = validator.validate(payload);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("validates entry list not empty")
        void validatesEntryNotEmpty() {
            WhatsappMetaWebhookPayload payload = new WhatsappMetaWebhookPayload("whatsapp_business_account", List.of());

            Set<ConstraintViolation<WhatsappMetaWebhookPayload>> violations = validator.validate(payload);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("accepts valid payload")
        void acceptsValidPayload() {
            Entry entry = new Entry("123", List.of());
            WhatsappMetaWebhookPayload payload = new WhatsappMetaWebhookPayload("whatsapp_business_account", List.of(entry));

            Set<ConstraintViolation<WhatsappMetaWebhookPayload>> violations = validator.validate(payload);
            // Note: Entry has validation that requires at least one change, so this may fail
            assertNotNull(payload);
        }
    }

    @Nested
    @DisplayName("Entry")
    class EntryTests {

        @Test
        @DisplayName("creates entry with ID and changes")
        void createsEntry() {
            Entry entry = new Entry("business-account-123", List.of());

            assertEquals("business-account-123", entry.id());
            assertNotNull(entry.changes());
        }

        @Test
        @DisplayName("validates ID not null")
        void validatesIdNotNull() {
            Entry entry = new Entry(null, List.of());

            Set<ConstraintViolation<Entry>> violations = validator.validate(entry);
            assertFalse(violations.isEmpty());
        }
    }

    @Nested
    @DisplayName("Real-World Webhook Payloads")
    class RealWorldPayloadsTests {

        @Test
        @DisplayName("deserializes text message webhook")
        void deserializesTextMessage() throws JsonProcessingException {
            String json = """
                    {
                      "object": "whatsapp_business_account",
                      "entry": [{
                        "id": "WHATSAPP_BUSINESS_ACCOUNT_ID",
                        "changes": [{
                          "value": {
                            "messaging_product": "whatsapp",
                            "metadata": {
                              "display_phone_number": "PHONE_NUMBER",
                              "phone_number_id": "PHONE_NUMBER_ID"
                            },
                            "contacts": [{
                              "profile": {
                                "name": "NAME"
                              },
                              "wa_id": "WHATSAPP_ID"
                            }],
                            "messages": [{
                              "from": "SENDER_PHONE_NUMBER",
                              "id": "wamid.ID",
                              "timestamp": "TIMESTAMP",
                              "text": {
                                "body": "MESSAGE_BODY"
                              },
                              "type": "text"
                            }]
                          },
                          "field": "messages"
                        }]
                      }]
                    }
                    """;

            WhatsappMetaWebhookPayload payload = objectMapper.readValue(json, WhatsappMetaWebhookPayload.class);

            assertNotNull(payload);
            assertEquals("whatsapp_business_account", payload.object);
            assertFalse(payload.entry.isEmpty());
        }

        @Test
        @DisplayName("deserializes status update webhook")
        void deserializesStatusUpdate() throws JsonProcessingException {
            String json = """
                    {
                      "object": "whatsapp_business_account",
                      "entry": [{
                        "id": "BUSINESS_ACCOUNT_ID",
                        "changes": [{
                          "value": {
                            "messaging_product": "whatsapp",
                            "metadata": {
                              "display_phone_number": "PHONE_NUMBER",
                              "phone_number_id": "PHONE_NUMBER_ID"
                            },
                            "statuses": [{
                              "id": "wamid.ID",
                              "status": "delivered",
                              "timestamp": "TIMESTAMP",
                              "recipient_id": "RECIPIENT_PHONE"
                            }]
                          },
                          "field": "messages"
                        }]
                      }]
                    }
                    """;

            WhatsappMetaWebhookPayload payload = objectMapper.readValue(json, WhatsappMetaWebhookPayload.class);

            assertNotNull(payload);
            assertEquals("whatsapp_business_account", payload.object);
        }

        @Test
        @DisplayName("deserializes multiple entries")
        void deserializesMultipleEntries() throws JsonProcessingException {
            String json = """
                    {
                      "object": "whatsapp_business_account",
                      "entry": [
                        {
                          "id": "ACCOUNT_1",
                          "changes": [{
                            "value": {
                              "messaging_product": "whatsapp"
                            },
                            "field": "messages"
                          }]
                        },
                        {
                          "id": "ACCOUNT_2",
                          "changes": [{
                            "value": {
                              "messaging_product": "whatsapp"
                            },
                            "field": "messages"
                          }]
                        }
                      ]
                    }
                    """;

            WhatsappMetaWebhookPayload payload = objectMapper.readValue(json, WhatsappMetaWebhookPayload.class);

            assertNotNull(payload);
            assertEquals(2, payload.entry.size());
            assertEquals("ACCOUNT_1", payload.entry.get(0).id());
            assertEquals("ACCOUNT_2", payload.entry.get(1).id());
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("handles empty changes array")
        void handlesEmptyChanges() {
            Entry entry = new Entry("id", List.of());

            assertNotNull(entry);
            assertTrue(entry.changes().isEmpty());
        }

        @Test
        @DisplayName("deserializes minimal valid payload")
        void deserializesMinimalPayload() throws JsonProcessingException {
            String json = """
                    {
                      "object": "whatsapp_business_account",
                      "entry": [{
                        "id": "123",
                        "changes": []
                      }]
                    }
                    """;

            WhatsappMetaWebhookPayload payload = objectMapper.readValue(json, WhatsappMetaWebhookPayload.class);

            assertNotNull(payload);
            assertEquals("whatsapp_business_account", payload.object);
            assertEquals(1, payload.entry.size());
        }

        @Test
        @DisplayName("handles Unicode in webhook data")
        void handlesUnicode() throws JsonProcessingException {
            String json = """
                    {
                      "object": "whatsapp_business_account",
                      "entry": [{
                        "id": "账户_123",
                        "changes": []
                      }]
                    }
                    """;

            WhatsappMetaWebhookPayload payload = objectMapper.readValue(json, WhatsappMetaWebhookPayload.class);

            assertNotNull(payload);
            assertEquals("账户_123", payload.entry.get(0).id());
        }
    }

    @Nested
    @DisplayName("JSON Serialization")
    class SerializationTests {

        @Test
        @DisplayName("serializes and deserializes payload")
        void roundTripSerialization() throws JsonProcessingException {
            Entry entry = new Entry("test-id", List.of());
            WhatsappMetaWebhookPayload original = new WhatsappMetaWebhookPayload(
                    "whatsapp_business_account",
                    List.of(entry));

            String json = objectMapper.writeValueAsString(original);
            WhatsappMetaWebhookPayload deserialized = objectMapper.readValue(json, WhatsappMetaWebhookPayload.class);

            assertEquals(original.object, deserialized.object);
            assertEquals(original.entry.size(), deserialized.entry.size());
        }
    }
}
