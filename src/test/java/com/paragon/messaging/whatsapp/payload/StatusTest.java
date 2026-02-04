package com.paragon.messaging.whatsapp.payload;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Status} (message status updates).
 */
@DisplayName("Status (Message Status)")
class StatusTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("Status Deserialization")
    class DeserializationTests {

        @Test
        @DisplayName("deserializes sent status")
        void deserializesSentStatus() throws JsonProcessingException {
            String json = """
                    {
                      "id": "wamid.ABC123",
                      "status": "sent",
                      "timestamp": "1707040000",
                      "recipient_id": "5511999999999"
                    }
                    """;

            Status status = objectMapper.readValue(json, Status.class);

            assertNotNull(status);
            assertEquals("wamid.ABC123", status.id());
            assertEquals("sent", status.status());
            assertEquals("1707040000", status.timestamp());
            assertEquals("5511999999999", status.recipientId());
        }

        @Test
        @DisplayName("deserializes delivered status")
        void deserializesDeliveredStatus() throws JsonProcessingException {
            String json = """
                    {
                      "id": "wamid.DEL123",
                      "status": "delivered",
                      "timestamp": "1707040100",
                      "recipient_id": "5511999999999"
                    }
                    """;

            Status status = objectMapper.readValue(json, Status.class);

            assertEquals("delivered", status.status());
        }

        @Test
        @DisplayName("deserializes read status")
        void deserializesReadStatus() throws JsonProcessingException {
            String json = """
                    {
                      "id": "wamid.READ123",
                      "status": "read",
                      "timestamp": "1707040200",
                      "recipient_id": "5511999999999"
                    }
                    """;

            Status status = objectMapper.readValue(json, Status.class);

            assertEquals("read", status.status());
        }

        @Test
        @DisplayName("deserializes failed status with errors")
        void deserializesFailedStatus() throws JsonProcessingException {
            String json = """
                    {
                      "id": "wamid.FAIL123",
                      "status": "failed",
                      "timestamp": "1707040300",
                      "recipient_id": "5511999999999",
                      "errors": [{
                        "code": 131047,
                        "title": "Re-engagement message",
                        "message": "Re-engagement message not sent"
                      }]
                    }
                    """;

            Status status = objectMapper.readValue(json, Status.class);

            assertEquals("failed", status.status());
            assertNotNull(status.errors());
            assertEquals(1, status.errors().size());
            assertEquals(131047, status.errors().get(0).code());
        }

        @Test
        @DisplayName("deserializes status with pricing")
        void deserializesWithPricing() throws JsonProcessingException {
            String json = """
                    {
                      "id": "wamid.PRICE123",
                      "status": "sent",
                      "timestamp": "1707040000",
                      "recipient_id": "5511999999999",
                      "pricing": {
                        "billable": true,
                        "pricing_model": "CBP",
                        "category": "business_initiated"
                      }
                    }
                    """;

            Status status = objectMapper.readValue(json, Status.class);

            assertNotNull(status.pricing());
        }

        @Test
        @DisplayName("deserializes status with conversation")
        void deserializesWithConversation() throws JsonProcessingException {
            String json = """
                    {
                      "id": "wamid.CONV123",
                      "status": "sent",
                      "timestamp": "1707040000",
                      "recipient_id": "5511999999999",
                      "conversation": {
                        "id": "conv_id_123",
                        "origin": {
                          "type": "business_initiated"
                        }
                      }
                    }
                    """;

            Status status = objectMapper.readValue(json, Status.class);

            assertNotNull(status.conversation());
        }
    }

    @Nested
    @DisplayName("Status Types")
    class StatusTypesTests {

        @Test
        @DisplayName("handles all standard status types")
        void handlesAllStatusTypes() throws JsonProcessingException {
            String[] statuses = {"sent", "delivered", "read", "failed"};

            for (String statusType : statuses) {
                String json = String.format("""
                        {
                          "id": "wamid.123",
                          "status": "%s",
                          "timestamp": "1707040000",
                          "recipient_id": "5511999999999"
                        }
                        """, statusType);

                Status status = objectMapper.readValue(json, Status.class);
                assertEquals(statusType, status.status());
            }
        }
    }

    @Nested
    @DisplayName("Real-World Scenarios")
    class RealWorldScenariosTests {

        @Test
        @DisplayName("tracks message delivery progression")
        void tracksDeliveryProgression() throws JsonProcessingException {
            String messageId = "wamid.TRACK123";

            // Sent
            String sentJson = String.format("""
                    {
                      "id": "%s",
                      "status": "sent",
                      "timestamp": "1707040000",
                      "recipient_id": "5511999999999"
                    }
                    """, messageId);
            Status sent = objectMapper.readValue(sentJson, Status.class);
            assertEquals("sent", sent.status());

            // Delivered
            String deliveredJson = String.format("""
                    {
                      "id": "%s",
                      "status": "delivered",
                      "timestamp": "1707040100",
                      "recipient_id": "5511999999999"
                    }
                    """, messageId);
            Status delivered = objectMapper.readValue(deliveredJson, Status.class);
            assertEquals("delivered", delivered.status());

            // Read
            String readJson = String.format("""
                    {
                      "id": "%s",
                      "status": "read",
                      "timestamp": "1707040200",
                      "recipient_id": "5511999999999"
                    }
                    """, messageId);
            Status read = objectMapper.readValue(readJson, Status.class);
            assertEquals("read", read.status());

            // All statuses share the same message ID
            assertEquals(sent.id(), delivered.id());
            assertEquals(delivered.id(), read.id());
        }

        @Test
        @DisplayName("handles business-initiated conversation")
        void handlesBusinessInitiatedConversation() throws JsonProcessingException {
            String json = """
                    {
                      "id": "wamid.BIZ123",
                      "status": "sent",
                      "timestamp": "1707040000",
                      "recipient_id": "5511999999999",
                      "conversation": {
                        "id": "conversation_id_abc",
                        "expiration_timestamp": "1707126400",
                        "origin": {
                          "type": "business_initiated"
                        }
                      },
                      "pricing": {
                        "billable": true,
                        "pricing_model": "CBP",
                        "category": "business_initiated"
                      }
                    }
                    """;

            Status status = objectMapper.readValue(json, Status.class);

            assertNotNull(status.conversation());
            assertNotNull(status.pricing());
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("handles status with null optional fields")
        void handlesNullOptionalFields() throws JsonProcessingException {
            String json = """
                    {
                      "id": "wamid.MIN123",
                      "status": "sent",
                      "timestamp": "1707040000",
                      "recipient_id": "5511999999999"
                    }
                    """;

            Status status = objectMapper.readValue(json, Status.class);

            assertNotNull(status);
            assertNull(status.pricing());
            assertNull(status.conversation());
            assertNull(status.errors());
        }

        @Test
        @DisplayName("handles multiple errors")
        void handlesMultipleErrors() throws JsonProcessingException {
            String json = """
                    {
                      "id": "wamid.ERR123",
                      "status": "failed",
                      "timestamp": "1707040000",
                      "recipient_id": "5511999999999",
                      "errors": [
                        {
                          "code": 100,
                          "title": "Error 1",
                          "message": "First error"
                        },
                        {
                          "code": 200,
                          "title": "Error 2",
                          "message": "Second error"
                        }
                      ]
                    }
                    """;

            Status status = objectMapper.readValue(json, Status.class);

            assertNotNull(status.errors());
            assertEquals(2, status.errors().size());
        }

        @Test
        @DisplayName("handles empty errors list")
        void handlesEmptyErrorsList() throws JsonProcessingException {
            String json = """
                    {
                      "id": "wamid.EMPTY123",
                      "status": "sent",
                      "timestamp": "1707040000",
                      "recipient_id": "5511999999999",
                      "errors": []
                    }
                    """;

            Status status = objectMapper.readValue(json, Status.class);

            assertNotNull(status.errors());
            assertTrue(status.errors().isEmpty());
        }
    }

    @Nested
    @DisplayName("JSON Field Mapping")
    class JsonFieldMappingTests {

        @Test
        @DisplayName("maps recipient_id to recipientId")
        void mapsRecipientId() throws JsonProcessingException {
            String json = """
                    {
                      "id": "wamid.123",
                      "status": "sent",
                      "timestamp": "1707040000",
                      "recipient_id": "5511987654321"
                    }
                    """;

            Status status = objectMapper.readValue(json, Status.class);

            assertEquals("5511987654321", status.recipientId());
        }

        @Test
        @DisplayName("round-trip serialization preserves snake_case")
        void roundTripPreservesSnakeCase() throws JsonProcessingException {
            Status original = new Status(
                    "wamid.123",
                    "sent",
                    "1707040000",
                    "5511999999999",
                    null,
                    null,
                    null);

            String json = objectMapper.writeValueAsString(original);
            Status deserialized = objectMapper.readValue(json, Status.class);

            assertEquals(original.id(), deserialized.id());
            assertEquals(original.recipientId(), deserialized.recipientId());
        }
    }
}
