package com.paragon.messaging.testutil;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

/**
 * Utility for configuring MockWebServer for WhatsApp API testing.
 */
public class MockWebServerUtil {

    public static void enqueueWhatsAppSuccess(MockWebServer server, String messageId) {
        String json = """
                {
                  "messaging_product": "whatsapp",
                  "contacts": [{
                    "input": "1234567890",
                    "wa_id": "1234567890"
                  }],
                  "messages": [{
                    "id": "%s"
                  }]
                }
                """.formatted(messageId);

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(json)
                .addHeader("Content-Type", "application/json"));
    }

    public static void enqueueWhatsAppError(MockWebServer server, int errorCode, String errorMessage) {
        String json = """
                {
                  "error": {
                    "message": "%s",
                    "type": "OAuthException",
                    "code": %d,
                    "fbtrace_id": "test-trace-id"
                  }
                }
                """.formatted(errorMessage, errorCode);

        server.enqueue(new MockResponse()
                .setResponseCode(errorCode)
                .setBody(json)
                .addHeader("Content-Type", "application/json"));
    }

    public static void enqueueWhatsAppRateLimitError(MockWebServer server) {
        enqueueWhatsAppError(server, 429, "Rate limit exceeded");
    }

    public static void enqueueWhatsAppMediaUploadSuccess(MockWebServer server, String mediaId) {
        String json = """
                {
                  "id": "%s"
                }
                """.formatted(mediaId);

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(json)
                .addHeader("Content-Type", "application/json"));
    }

    private MockWebServerUtil() {
        // Utility class
    }
}
