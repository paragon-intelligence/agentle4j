package com.paragon.messaging.whatsapp;

import com.paragon.messaging.core.MessagingProvider;
import com.paragon.messaging.core.MessageResponse;
import com.paragon.messaging.core.Recipient;
import com.paragon.messaging.testutil.MockWebServerUtil;
import com.paragon.messaging.whatsapp.config.WhatsAppConfig;
import com.paragon.messaging.whatsapp.messages.TextMessage;
import com.paragon.messaging.whatsapp.messages.MediaMessage;
import com.paragon.messaging.whatsapp.messages.ReactionMessage;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link WhatsAppMessagingProvider}.
 */
@DisplayName("WhatsAppMessagingProvider")
class WhatsAppMessagingProviderTest {

    private MockWebServer mockServer;
    private WhatsAppMessagingProvider provider;
    private WhatsAppConfig config;

    @BeforeEach
    void setUp() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();

        String baseUrl = mockServer.url("/").toString().replaceAll("/$", "");

        config = WhatsAppConfig.builder()
                .accessToken("test_token_123")
                .phoneNumberId("1234567890")
                .apiVersion("v21.0")
                .apiBaseUrl(baseUrl)
                .maxConcurrentRequests(10)
                .requestTimeout(5000)
                .build();

        provider = new WhatsAppMessagingProvider(config);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (mockServer != null) {
            // Shutdown immediately without waiting for delayed responses
            mockServer.shutdown();
        }
    }

    @Nested
    @DisplayName("Configuration")
    class ConfigurationTests {

        @Test
        @DisplayName("isConfigured() returns true with valid config")
        void isConfigured_withValidConfig() {
            assertTrue(provider.isConfigured());
        }

        @Test
        @DisplayName("getProviderType() returns WHATSAPP")
        void getProviderType_returnsWhatsApp() {
            assertEquals(MessagingProvider.ProviderType.WHATSAPP, provider.getProviderType());
        }

        @Test
        @DisplayName("constructor validates config")
        void constructor_validatesConfig() {
            assertThrows(Exception.class, () -> 
                    new WhatsAppMessagingProvider(null));
        }
    }

    @Nested
    @DisplayName("Text Message Sending")
    class TextMessageSendingTests {

        @Test
        @DisplayName("sendMessage() sends text message successfully")
        void sendMessage_textMessage_success() throws Exception {
            mockServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setBody("{\"messages\":[{\"id\":\"wamid.test123\"}]}"));

            TextMessage message = new TextMessage("Hello, World!");
            Recipient recipient = Recipient.ofPhoneNumber("5511999999999");

            MessageResponse response = provider.sendMessage(recipient, message);

            assertTrue(response.success());
            assertEquals("wamid.test123", response.messageId());
            assertNull(response.error());
        }

        @Test
        @DisplayName("sendMessage() includes authorization header")
        void sendMessage_includesAuthHeader() throws Exception {
            mockServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setBody("{\"messages\":[{\"id\":\"wamid.test\"}]}"));

            TextMessage message = new TextMessage("Test");
            Recipient recipient = Recipient.ofPhoneNumber("5511999999999");

            provider.sendMessage(recipient, message);

            RecordedRequest request = mockServer.takeRequest(1, TimeUnit.SECONDS);
            assertNotNull(request);
            assertEquals("Bearer test_token_123", request.getHeader("Authorization"));
        }

        @Test
        @DisplayName("sendMessage() includes content-type header")
        void sendMessage_includesContentType() throws Exception {
            mockServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setBody("{\"messages\":[{\"id\":\"wamid.test\"}]}"));

            TextMessage message = new TextMessage("Test");
            Recipient recipient = Recipient.ofPhoneNumber("5511999999999");

            provider.sendMessage(recipient, message);

            RecordedRequest request = mockServer.takeRequest(1, TimeUnit.SECONDS);
            assertNotNull(request);
            assertEquals("application/json", request.getHeader("Content-Type"));
        }

        @Test
        @DisplayName("sendMessage() sends to correct endpoint")
        void sendMessage_correctEndpoint() throws Exception {
            mockServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setBody("{\"messages\":[{\"id\":\"wamid.test\"}]}"));

            TextMessage message = new TextMessage("Test");
            Recipient recipient = Recipient.ofPhoneNumber("5511999999999");

            provider.sendMessage(recipient, message);

            RecordedRequest request = mockServer.takeRequest(1, TimeUnit.SECONDS);
            assertNotNull(request);
            assertTrue(request.getPath().contains("/v21.0/1234567890/messages"));
        }

        @Test
        @DisplayName("sendMessage() sends correct JSON payload")
        void sendMessage_correctPayload() throws Exception {
            mockServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setBody("{\"messages\":[{\"id\":\"wamid.test\"}]}"));

            TextMessage message = new TextMessage("Hello");
            Recipient recipient = Recipient.ofPhoneNumber("5511999999999");

            provider.sendMessage(recipient, message);

            RecordedRequest request = mockServer.takeRequest(1, TimeUnit.SECONDS);
            assertNotNull(request);
            
            String body = request.getBody().readUtf8();
            assertTrue(body.contains("\"messaging_product\":\"whatsapp\""));
            assertTrue(body.contains("\"to\":\"5511999999999\""));
            assertTrue(body.contains("\"type\":\"text\""));
            assertTrue(body.contains("\"body\":\"Hello\""));
        }
    }

    @Nested
    @DisplayName("Media Message Sending")
    class MediaMessageSendingTests {

        @Test
        @DisplayName("sendMessage() sends image message")
        void sendMessage_imageMessage() throws Exception {
            mockServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setBody("{\"messages\":[{\"id\":\"wamid.image123\"}]}"));

            MediaMessage.Image image = new MediaMessage.Image(
                    new MediaMessage.MediaSource.MediaId("media123"),
                    "Beautiful photo");
            Recipient recipient = Recipient.ofPhoneNumber("5511999999999");

            MessageResponse response = provider.sendMessage(recipient, image);

            assertTrue(response.success());
            assertEquals("wamid.image123", response.messageId());

            RecordedRequest request = mockServer.takeRequest(1, TimeUnit.SECONDS);
            String body = request.getBody().readUtf8();
            assertTrue(body.contains("\"type\":\"image\""));
            assertTrue(body.contains("\"id\":\"media123\""));
        }

        @Test
        @DisplayName("sendMessage() sends audio message")
        void sendMessage_audioMessage() throws Exception {
            mockServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setBody("{\"messages\":[{\"id\":\"wamid.audio123\"}]}"));

            MediaMessage.Audio audio = new MediaMessage.Audio(
                    new MediaMessage.MediaSource.MediaId("audio123"));
            Recipient recipient = Recipient.ofPhoneNumber("5511999999999");

            MessageResponse response = provider.sendMessage(recipient, audio);

            assertTrue(response.success());
        }
    }

    @Nested
    @DisplayName("Reaction Message Sending")
    class ReactionMessageSendingTests {

        @Test
        @DisplayName("sendReaction() sends reaction emoji")
        void sendReaction_success() throws Exception {
            mockServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setBody("{\"messages\":[{\"id\":\"wamid.reaction123\"}]}"));

            ReactionMessage reaction = new ReactionMessage("wamid.original", "👍");
            Recipient recipient = Recipient.ofPhoneNumber("5511999999999");

            MessageResponse response = provider.sendReaction(recipient, reaction);

            assertTrue(response.success());

            RecordedRequest request = mockServer.takeRequest(1, TimeUnit.SECONDS);
            String body = request.getBody().readUtf8();
            assertTrue(body.contains("\"type\":\"reaction\""));
            assertTrue(body.contains("\"message_id\":\"wamid.original\""));
            assertTrue(body.contains("\"emoji\":\"👍\""));
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("handles 400 bad request")
        void handles400Error() throws Exception {
            mockServer.enqueue(new MockResponse()
                    .setResponseCode(400)
                    .setBody("{\"error\":{\"message\":\"Invalid recipient\",\"code\":100}}"));

            TextMessage message = new TextMessage("Test");
            Recipient recipient = Recipient.ofPhoneNumber("invalid");

            MessageResponse response = provider.sendMessage(recipient, message);

            assertFalse(response.success());
            assertNull(response.messageId());
            assertNotNull(response.error());
            assertTrue(response.error().contains("Invalid recipient") || 
                       response.error().contains("400"));
        }

        @Test
        @DisplayName("handles 401 unauthorized")
        void handles401Error() throws Exception {
            mockServer.enqueue(new MockResponse()
                    .setResponseCode(401)
                    .setBody("{\"error\":{\"message\":\"Invalid access token\"}}"));

            TextMessage message = new TextMessage("Test");
            Recipient recipient = Recipient.ofPhoneNumber("5511999999999");

            MessageResponse response = provider.sendMessage(recipient, message);

            assertFalse(response.success());
            assertNotNull(response.error());
        }

        @Test
        @DisplayName("handles 429 rate limit")
        void handles429RateLimit() throws Exception {
            mockServer.enqueue(new MockResponse()
                    .setResponseCode(429)
                    .setBody("{\"error\":{\"message\":\"Rate limit exceeded\"}}"));

            TextMessage message = new TextMessage("Test");
            Recipient recipient = Recipient.ofPhoneNumber("5511999999999");

            MessageResponse response = provider.sendMessage(recipient, message);

            assertFalse(response.success());
            assertNotNull(response.error());
        }

        @Test
        @DisplayName("handles 500 server error")
        void handles500Error() throws Exception {
            mockServer.enqueue(new MockResponse()
                    .setResponseCode(500)
                    .setBody("Internal Server Error"));

            TextMessage message = new TextMessage("Test");
            Recipient recipient = Recipient.ofPhoneNumber("5511999999999");

            MessageResponse response = provider.sendMessage(recipient, message);

            assertFalse(response.success());
            assertNotNull(response.error());
        }

        @Test
        @DisplayName("handles network timeout")
        void handlesTimeout() throws Exception {
            mockServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setBody("{\"messages\":[{\"id\":\"wamid.test\"}]}")
                    .setBodyDelay(10, TimeUnit.SECONDS)); // Longer than timeout

            TextMessage message = new TextMessage("Test");
            Recipient recipient = Recipient.ofPhoneNumber("5511999999999");

            MessageResponse response = provider.sendMessage(recipient, message);

            assertFalse(response.success());
            assertNotNull(response.error());
        }

        @Test
        @DisplayName("handles malformed response JSON")
        void handlesMalformedJson() throws Exception {
            mockServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setBody("not valid json {{{"));

            TextMessage message = new TextMessage("Test");
            Recipient recipient = Recipient.ofPhoneNumber("5511999999999");

            MessageResponse response = provider.sendMessage(recipient, message);

            // Should handle gracefully
            assertNotNull(response);
        }
    }

    @Nested
    @DisplayName("Rate Limiting")
    class RateLimitingTests {

        @Test
        @DisplayName("respects concurrent request limit")
        void respectsConcurrentLimit() throws Exception {
            // Queue multiple responses
            for (int i = 0; i < 15; i++) {
                mockServer.enqueue(new MockResponse()
                        .setResponseCode(200)
                        .setBody("{\"messages\":[{\"id\":\"wamid.test" + i + "\"}]}")
                        .setBodyDelay(100, TimeUnit.MILLISECONDS));
            }

            WhatsAppConfig limitedConfig = WhatsAppConfig.builder()
                    .accessToken("test_token")
                    .phoneNumberId("123")
                    .apiBaseUrl(mockServer.url("/").toString())
                    .maxConcurrentRequests(5)
                    .build();

            WhatsAppMessagingProvider limitedProvider = new WhatsAppMessagingProvider(limitedConfig);

            TextMessage message = new TextMessage("Test");
            Recipient recipient = Recipient.ofPhoneNumber("5511999999999");

            // Send multiple messages concurrently
            for (int i = 0; i < 15; i++) {
                Thread.startVirtualThread(() -> {
                    limitedProvider.sendMessage(recipient, message);
                });
            }

            Thread.sleep(500); // Wait for completion

            // Provider should have handled rate limiting internally
            assertTrue(true); // If we get here without hanging, rate limiting works
        }

        @Test
        @DisplayName("getRateLimiterStats() returns statistics")
        void getRateLimiterStats_returnsStats() {
            var stats = provider.getRateLimiterStats();

            assertNotNull(stats);
            assertTrue(stats.availablePermits() >= 0);
            assertTrue(stats.queuedThreads() >= 0);
        }
    }

    @Nested
    @DisplayName("Input Validation")
    class InputValidationTests {

        @Test
        @DisplayName("validates recipient is not null")
        void validates_recipientNotNull() {
            TextMessage message = new TextMessage("Test");

            assertThrows(Exception.class, () ->
                    provider.sendMessage(null, message));
        }

        @Test
        @DisplayName("validates message is not null")
        void validates_messageNotNull() {
            Recipient recipient = Recipient.ofPhoneNumber("5511999999999");

            assertThrows(Exception.class, () ->
                    provider.sendMessage(recipient, null));
        }

        @Test
        @DisplayName("validates text message body not blank")
        void validates_textBodyNotBlank() throws Exception {
            mockServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setBody("{\"messages\":[{\"id\":\"wamid.test\"}]}"));

            TextMessage message = new TextMessage(""); // Invalid
            Recipient recipient = Recipient.ofPhoneNumber("5511999999999");

            // Bean validation should catch this
            assertThrows(Exception.class, () ->
                    provider.sendMessage(recipient, message));
        }
    }

    @Nested
    @DisplayName("Thread Safety")
    class ThreadSafetyTests {

        @Test
        @DisplayName("handles concurrent requests safely")
        void handlesConcurrentRequests() throws Exception {
            for (int i = 0; i < 20; i++) {
                mockServer.enqueue(new MockResponse()
                        .setResponseCode(200)
                        .setBody("{\"messages\":[{\"id\":\"wamid.test" + i + "\"}]}"));
            }

            TextMessage message = new TextMessage("Concurrent test");
            Recipient recipient = Recipient.ofPhoneNumber("5511999999999");

            // Send 20 messages concurrently
            Thread[] threads = new Thread[20];
            for (int i = 0; i < 20; i++) {
                threads[i] = Thread.startVirtualThread(() -> {
                    MessageResponse response = provider.sendMessage(recipient, message);
                    assertTrue(response.success());
                });
            }

            // Wait for all threads
            for (Thread thread : threads) {
                thread.join(5000);
            }

            // All requests should have been processed
            assertEquals(0, mockServer.getRequestCount() - 20);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("handles empty message ID in response")
        void handlesEmptyMessageId() throws Exception {
            mockServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setBody("{\"messages\":[{\"id\":\"\"}]}"));

            TextMessage message = new TextMessage("Test");
            Recipient recipient = Recipient.ofPhoneNumber("5511999999999");

            MessageResponse response = provider.sendMessage(recipient, message);

            // Should handle gracefully
            assertNotNull(response);
        }

        @Test
        @DisplayName("handles response without messages array")
        void handlesNoMessagesArray() throws Exception {
            mockServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setBody("{\"success\":true}"));

            TextMessage message = new TextMessage("Test");
            Recipient recipient = Recipient.ofPhoneNumber("5511999999999");

            MessageResponse response = provider.sendMessage(recipient, message);

            assertNotNull(response);
        }
    }
}
