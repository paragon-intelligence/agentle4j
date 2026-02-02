package com.paragon.messaging.whatsapp;

import com.example.messaging.api.exception.MessagingException;
import com.example.messaging.model.common.Recipient;
import com.paragon.agents.Interactable;
import com.paragon.agents.StructuredAgentResult;
import com.paragon.messaging.whatsapp.config.WhatsAppConfig;
import com.paragon.messaging.whatsapp.domain.WhatsAppResponse;
import okhttp3.OkHttpClient;
import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Demo runner for verifying WhatsApp Integration Refactoring.
 */
public class DemoRunner {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Starting WhatsApp Integration Demo...");

        // 1. Setup Config & Mocks
        WhatsAppConfig config = WhatsAppConfig.builder()
                .phoneNumberId("123456789")
                .accessToken("mock_access_token")
                .verifyToken("mock_verify_token")
                .build();

        MessageStore messageStore = InMemoryMessageStore.create();
        
        MessagingProvider mockProvider = new MessagingProvider() {
            @Override
            public String getProviderType() { return "MOCK"; }
            @Override
            public boolean isConfigured() { return true; }
            @Override
            public MessageResponse sendMessage(Recipient recipient, com.paragon.messaging.whatsapp.Message message) throws MessagingException {
                System.out.println("\n[PROVIDER] Sending Message to " + recipient.identifier());
                System.out.println("[PROVIDER] Content: " + message);
                return new MessageResponse("msg_id_" + System.currentTimeMillis(), MessageResponse.MessageStatus.SENT, Instant.now());
            }
        };

        MockAgent mockAgent = new MockAgent();

        // 2. Initialize Integrator
        WhatsAppAgentIntegrator integrator = new WhatsAppAgentIntegrator(
                mockAgent,
                mockProvider,
                messageStore,
                config
        );

        // 3. Simulate Incoming Message (Webhook)
        System.out.println("\n[SIMULATION] Incoming Message: 'Hello Agent'");
        WebhookEvent.IncomingMessageEvent event = new WebhookEvent.IncomingMessageEvent(
                "msg_123",
                "5511999999999",
                "User",
                WebhookEvent.IncomingMessageType.TEXT,
                new WebhookEvent.TextContent("Hello Agent"),
                Instant.now(),
                null
        );

        integrator.processWebhook(event);

        // Wait for async execution
        Thread.sleep(1000); // Virtual threads are fast, but we need main thread to stay alive

        System.out.println("\n[DEMO] Finished.");
    }

    private static class MockAgent implements Interactable.Structured<WhatsAppResponse> {
        @Override
        public @NonNull String name() { return "MockAgent"; }

        @Override
        public @NonNull StructuredAgentResult<WhatsAppResponse> interactStructured(@NonNull String input) {
            System.out.println("[AGENT] Received input: " + input);
            WhatsAppResponse response = new WhatsAppResponse.TextResponse("Hello! I received your message: " + input);
            return new StructuredAgentResult<>(response, null, null);
        }
        
        // Boilerplate methods for interface compliance
        @Override public com.paragon.agents.AgentResult interact(String i) { return null; }
        @Override public com.paragon.agents.AgentStream interactStream(String i) { return null; }
        @Override public com.paragon.agents.AgentResult interact(com.paragon.responses.spec.Message m) { return null; }
        @Override public com.paragon.agents.AgentStream interactStream(com.paragon.responses.spec.Message m) { return null; }
        @Override public StructuredAgentResult<WhatsAppResponse> interactStructured(com.paragon.responses.spec.Message m) { return null; }
        @Override public com.paragon.agents.StructuredAgentStream<WhatsAppResponse> interactStructuredStream(String i) { return null; }
        @Override public com.paragon.agents.StructuredAgentStream<WhatsAppResponse> interactStructuredStream(com.paragon.responses.spec.Message m) { return null; }
    }
}
