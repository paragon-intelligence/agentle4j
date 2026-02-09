package com.paragon.messaging.processor;

import com.paragon.agents.AgentContext;
import com.paragon.agents.AgentResult;
import com.paragon.agents.Interactable;
import com.paragon.agents.StructuredAgentResult;
import com.paragon.messaging.conversion.MessageConverter;
import com.paragon.messaging.core.MessageProcessor;
import com.paragon.messaging.core.MessagingProvider;
import com.paragon.messaging.core.OutboundMessage;
import com.paragon.messaging.store.history.ConversationHistoryStore;
import com.paragon.messaging.testutil.MockMessageFactory;
import com.paragon.messaging.whatsapp.messages.TextMessage;
import com.paragon.messaging.whatsapp.payload.InboundMessage;
import com.paragon.messaging.whatsapp.response.WhatsAppResponse;
import com.paragon.responses.spec.Message;
import com.paragon.responses.spec.ResponseInputItem;
import com.paragon.responses.spec.UserMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link AIAgentProcessor}.
 */
@DisplayName("AIAgentProcessor")
class AIAgentProcessorTest {

    private Interactable mockAgent;
    private MessagingProvider mockProvider;
    private ConversationHistoryStore mockHistoryStore;
    private MessageConverter mockConverter;

    @BeforeEach
    void setUp() {
        mockAgent = mock(Interactable.class);
        mockProvider = mock(MessagingProvider.class);
        mockHistoryStore = mock(ConversationHistoryStore.class);
        mockConverter = mock(MessageConverter.class);
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("forAgent() creates simple agent builder")
        void forAgent_createsBuilder() {
            AIAgentProcessor.Builder<Void> builder = AIAgentProcessor.forAgent(mockAgent);

            assertNotNull(builder);
        }

        @Test
        @DisplayName("forStructuredAgent() creates structured builder")
        void forStructuredAgent_createsBuilder() {
            @SuppressWarnings("unchecked")
            Interactable.Structured<TestResponse> structuredAgent =
                    mock(Interactable.Structured.class);

            AIAgentProcessor.Builder<TestResponse> builder =
                    AIAgentProcessor.forStructuredAgent(structuredAgent);

            assertNotNull(builder);
        }

        @Test
        @DisplayName("build() requires messaging provider")
        void build_requiresMessagingProvider() {
            AIAgentProcessor.Builder<Void> builder = AIAgentProcessor.forAgent(mockAgent);

            assertThrows(IllegalStateException.class, () -> builder.build());
        }

        @Test
        @DisplayName("build() creates processor with required fields")
        void build_withRequiredFields_succeeds() {
            AIAgentProcessor<Void> processor = AIAgentProcessor.forAgent(mockAgent)
                    .messagingProvider(mockProvider)
                    .build();

            assertNotNull(processor);
        }

        @Test
        @DisplayName("builder with all optional fields")
        void builder_withAllOptionalFields() {
            AIAgentProcessor<Void> processor = AIAgentProcessor.forAgent(mockAgent)
                    .messagingProvider(mockProvider)
                    .messageConverter(mockConverter)
                    .historyStore(mockHistoryStore)
                    .maxHistoryMessages(10)
                    .maxHistoryAge(Duration.ofHours(12))
                    .build();

            assertNotNull(processor);
        }

        @Test
        @DisplayName("withInMemoryHistory() enables in-memory storage")
        void withInMemoryHistory_enablesStorage() {
            AIAgentProcessor<Void> processor = AIAgentProcessor.forAgent(mockAgent)
                    .messagingProvider(mockProvider)
                    .withInMemoryHistory()
                    .build();

            assertNotNull(processor);
        }

        @Test
        @DisplayName("withInMemoryHistory(maxPerUser) sets limit")
        void withInMemoryHistory_setsLimit() {
            AIAgentProcessor<Void> processor = AIAgentProcessor.forAgent(mockAgent)
                    .messagingProvider(mockProvider)
                    .withInMemoryHistory(50)
                    .build();

            assertNotNull(processor);
        }
    }

    @Nested
    @DisplayName("Simple Processing")
    class SimpleProcessingTests {

        @Test
        @DisplayName("process() sends agent response as text")
        void process_sendsAgentResponse() throws Exception {
            when(mockConverter.toUserMessage(anyList())).thenReturn(
                    UserMessage.text("Hello"));
            when(mockAgent.interact(any(AgentContext.class))).thenReturn(
                    AgentResult.success("Hello! How can I help?"));

            AIAgentProcessor<Void> processor = AIAgentProcessor.forAgent(mockAgent)
                    .messagingProvider(mockProvider)
                    .messageConverter(mockConverter)
                    .build();

            List<InboundMessage> messages = List.of(
                    MockMessageFactory.createTextMessage("user123", "msg1", "Hello"));

            processor.process("user123", messages);

            ArgumentCaptor<OutboundMessage> messageCaptor =
                    ArgumentCaptor.forClass(OutboundMessage.class);
            verify(mockProvider).sendMessage(any(), messageCaptor.capture());

            OutboundMessage sent = messageCaptor.getValue();
            assertTrue(sent instanceof TextMessage);
            assertEquals("Hello! How can I help?", ((TextMessage) sent).body());
        }

        @Test
        @DisplayName("process() throws on null userId")
        void process_nullUserId_throws() {
            AIAgentProcessor<Void> processor = AIAgentProcessor.forAgent(mockAgent)
                    .messagingProvider(mockProvider)
                    .build();

            List<InboundMessage> messages = List.of(
                    MockMessageFactory.createTextMessage("user123", "Test"));

            assertThrows(NullPointerException.class, () ->
                    processor.process(null, messages));
        }

        @Test
        @DisplayName("process() throws on null messages")
        void process_nullMessages_throws() {
            AIAgentProcessor<Void> processor = AIAgentProcessor.forAgent(mockAgent)
                    .messagingProvider(mockProvider)
                    .build();

            assertThrows(NullPointerException.class, () ->
                    processor.process("user123", null));
        }

        @Test
        @DisplayName("process() handles empty message list")
        void process_emptyMessages_doesNothing() throws Exception {
            AIAgentProcessor<Void> processor = AIAgentProcessor.forAgent(mockAgent)
                    .messagingProvider(mockProvider)
                    .build();

            processor.process("user123", List.of());

            verify(mockAgent, never()).interact(any(AgentContext.class));
            verify(mockProvider, never()).sendMessage(any(), any());
        }

        @Test
        @DisplayName("process() throws on agent error")
        void process_agentError_throws() {
            when(mockConverter.toUserMessage(anyList())).thenReturn(
                    UserMessage.text("Hello"));
            when(mockAgent.interact(any(AgentContext.class))).thenReturn(
                    AgentResult.error(new RuntimeException("Agent failed")));

            AIAgentProcessor<Void> processor = AIAgentProcessor.forAgent(mockAgent)
                    .messagingProvider(mockProvider)
                    .messageConverter(mockConverter)
                    .build();

            List<InboundMessage> messages = List.of(
                    MockMessageFactory.createTextMessage("user123", "Test"));

            assertThrows(AIAgentProcessor.AIProcessingException.class, () ->
                    processor.process("user123", messages));
        }

        @Test
        @DisplayName("process() skips empty/blank responses")
        void process_emptyResponse_skips() throws Exception {
            when(mockConverter.toUserMessage(anyList())).thenReturn(
                    UserMessage.text("Hello"));
            when(mockAgent.interact(any(AgentContext.class))).thenReturn(
                    AgentResult.success("   "));

            AIAgentProcessor<Void> processor = AIAgentProcessor.forAgent(mockAgent)
                    .messagingProvider(mockProvider)
                    .messageConverter(mockConverter)
                    .build();

            List<InboundMessage> messages = List.of(
                    MockMessageFactory.createTextMessage("user123", "Test"));

            processor.process("user123", messages);

            verify(mockProvider, never()).sendMessage(any(), any());
        }
    }

    @Nested
    @DisplayName("Structured Processing")
    class StructuredProcessingTests {

        @Test
        @DisplayName("processes structured agent output")
        void processStructured_sendsResponse() throws Exception {
            @SuppressWarnings("unchecked")
            Interactable.Structured<TestResponse> structuredAgent =
                    mock(Interactable.Structured.class);

            TestResponse testResponse = new TestResponse("Structured response");

            when(mockConverter.toUserMessage(anyList())).thenReturn(
                    UserMessage.text("Hello"));
            when(structuredAgent.interactStructured(any(AgentContext.class)))
                    .thenReturn(StructuredAgentResult.success(testResponse, "raw output"));

            AIAgentProcessor<TestResponse> processor =
                    AIAgentProcessor.forStructuredAgent(structuredAgent)
                            .messagingProvider(mockProvider)
                            .messageConverter(mockConverter)
                            .build();

            List<InboundMessage> messages = List.of(
                    MockMessageFactory.createTextMessage("user123", "Test"));

            processor.process("user123", messages);

            // Should send the structured response
            verify(mockProvider, atLeastOnce()).sendMessage(any(), any());
        }

        @Test
        @DisplayName("throws on structured agent error")
        void processStructured_agentError_throws() {
            @SuppressWarnings("unchecked")
            Interactable.Structured<TestResponse> structuredAgent =
                    mock(Interactable.Structured.class);

            when(mockConverter.toUserMessage(anyList())).thenReturn(
                    UserMessage.text("Hello"));
            when(structuredAgent.interactStructured(any(AgentContext.class)))
                    .thenReturn(StructuredAgentResult.error(
                            new RuntimeException("Structured agent failed")));

            AIAgentProcessor<TestResponse> processor =
                    AIAgentProcessor.forStructuredAgent(structuredAgent)
                            .messagingProvider(mockProvider)
                            .messageConverter(mockConverter)
                            .build();

            List<InboundMessage> messages = List.of(
                    MockMessageFactory.createTextMessage("user123", "Test"));

            assertThrows(AIAgentProcessor.AIProcessingException.class, () ->
                    processor.process("user123", messages));
        }
    }

    @Nested
    @DisplayName("Conversation History")
    class ConversationHistoryTests {

        @Test
        @DisplayName("stores user message in history")
        void storesUserMessage_inHistory() throws Exception {
            UserMessage userMsg = UserMessage.text("Hello");

            when(mockConverter.toUserMessage(anyList())).thenReturn(userMsg);
            when(mockHistoryStore.getHistory(anyString(), anyInt(), any()))
                    .thenReturn(List.of());
            when(mockAgent.interact(any(AgentContext.class))).thenReturn(
                    AgentResult.success("Hi there!"));

            AIAgentProcessor<Void> processor = AIAgentProcessor.forAgent(mockAgent)
                    .messagingProvider(mockProvider)
                    .messageConverter(mockConverter)
                    .historyStore(mockHistoryStore)
                    .build();

            List<InboundMessage> messages = List.of(
                    MockMessageFactory.createTextMessage("user123", "Hello"));

            processor.process("user123", messages);

            verify(mockHistoryStore).addMessage("user123", userMsg);
        }

        @Test
        @DisplayName("stores assistant response in history")
        void storesAssistantResponse_inHistory() throws Exception {
            when(mockConverter.toUserMessage(anyList())).thenReturn(
                    UserMessage.text("Hello"));
            when(mockHistoryStore.getHistory(anyString(), anyInt(), any()))
                    .thenReturn(List.of());
            when(mockAgent.interact(any(AgentContext.class))).thenReturn(
                    AgentResult.success("Hi there!"));

            AIAgentProcessor<Void> processor = AIAgentProcessor.forAgent(mockAgent)
                    .messagingProvider(mockProvider)
                    .messageConverter(mockConverter)
                    .historyStore(mockHistoryStore)
                    .build();

            List<InboundMessage> messages = List.of(
                    MockMessageFactory.createTextMessage("user123", "Hello"));

            processor.process("user123", messages);

            ArgumentCaptor<Message> messageCaptor =
                    ArgumentCaptor.forClass(Message.class);
            verify(mockHistoryStore, times(2)).addMessage(eq("user123"),
                    messageCaptor.capture());

            List<Message> storedMessages = messageCaptor.getAllValues();
            assertEquals(2, storedMessages.size());
            assertEquals("assistant", storedMessages.get(1).role());
        }

        @Test
        @DisplayName("retrieves history with max messages limit")
        void retrievesHistory_withMaxMessages() throws Exception {
            when(mockConverter.toUserMessage(anyList())).thenReturn(
                    UserMessage.text("Hello"));
            when(mockHistoryStore.getHistory(eq("user123"), eq(15), any()))
                    .thenReturn(List.of());
            when(mockAgent.interact(any(AgentContext.class))).thenReturn(
                    AgentResult.success("Response"));

            AIAgentProcessor<Void> processor = AIAgentProcessor.forAgent(mockAgent)
                    .messagingProvider(mockProvider)
                    .messageConverter(mockConverter)
                    .historyStore(mockHistoryStore)
                    .maxHistoryMessages(15)
                    .build();

            List<InboundMessage> messages = List.of(
                    MockMessageFactory.createTextMessage("user123", "Hello"));

            processor.process("user123", messages);

            verify(mockHistoryStore).getHistory("user123", 15, Duration.ofHours(24));
        }

        @Test
        @DisplayName("retrieves history with max age limit")
        void retrievesHistory_withMaxAge() throws Exception {
            Duration maxAge = Duration.ofHours(6);

            when(mockConverter.toUserMessage(anyList())).thenReturn(
                    UserMessage.text("Hello"));
            when(mockHistoryStore.getHistory(anyString(), anyInt(), eq(maxAge)))
                    .thenReturn(List.of());
            when(mockAgent.interact(any(AgentContext.class))).thenReturn(
                    AgentResult.success("Response"));

            AIAgentProcessor<Void> processor = AIAgentProcessor.forAgent(mockAgent)
                    .messagingProvider(mockProvider)
                    .messageConverter(mockConverter)
                    .historyStore(mockHistoryStore)
                    .maxHistoryAge(maxAge)
                    .build();

            List<InboundMessage> messages = List.of(
                    MockMessageFactory.createTextMessage("user123", "Hello"));

            processor.process("user123", messages);

            verify(mockHistoryStore).getHistory("user123", 20, maxAge);
        }

        @Test
        @DisplayName("works without history store")
        void worksWithoutHistoryStore() throws Exception {
            when(mockConverter.toUserMessage(anyList())).thenReturn(
                    UserMessage.text("Hello"));
            when(mockAgent.interact(any(AgentContext.class))).thenReturn(
                    AgentResult.success("Response"));

            AIAgentProcessor<Void> processor = AIAgentProcessor.forAgent(mockAgent)
                    .messagingProvider(mockProvider)
                    .messageConverter(mockConverter)
                    .build();

            List<InboundMessage> messages = List.of(
                    MockMessageFactory.createTextMessage("user123", "Hello"));

            assertDoesNotThrow(() -> processor.process("user123", messages));
        }
    }

    @Nested
    @DisplayName("Message Batching")
    class MessageBatchingTests {

        @Test
        @DisplayName("combines multiple messages")
        void combinesMultiple() throws Exception {
            List<InboundMessage> messages = List.of(
                    MockMessageFactory.createTextMessage("user123", "msg1", "Hello"),
                    MockMessageFactory.createTextMessage("user123", "msg2", "How are you?"),
                    MockMessageFactory.createTextMessage("user123", "msg3", "I need help")
            );

            when(mockConverter.toUserMessage(messages)).thenReturn(
                    UserMessage.text("Hello\nHow are you?\nI need help"));
            when(mockAgent.interact(any(AgentContext.class))).thenReturn(
                    AgentResult.success("I'm here to help!"));

            AIAgentProcessor<Void> processor = AIAgentProcessor.forAgent(mockAgent)
                    .messagingProvider(mockProvider)
                    .messageConverter(mockConverter)
                    .build();

            processor.process("user123", messages);

            verify(mockConverter).toUserMessage(messages);
            verify(mockAgent).interact(any(AgentContext.class));
        }

        @Test
        @DisplayName("uses last message ID for reply context")
        void usesLastMessageId_forReply() throws Exception {
            List<InboundMessage> messages = List.of(
                    MockMessageFactory.createTextMessage("user123", "msg1", "First"),
                    MockMessageFactory.createTextMessage("user123", "msg2", "Second"),
                    MockMessageFactory.createTextMessage("user123", "msg3", "Third")
            );

            when(mockConverter.toUserMessage(anyList())).thenReturn(
                    UserMessage.text("Combined"));
            when(mockAgent.interact(any(AgentContext.class))).thenReturn(
                    AgentResult.success("Response"));

            AIAgentProcessor<Void> processor = AIAgentProcessor.forAgent(mockAgent)
                    .messagingProvider(mockProvider)
                    .messageConverter(mockConverter)
                    .build();

            processor.process("user123", messages);

            ArgumentCaptor<OutboundMessage> messageCaptor =
                    ArgumentCaptor.forClass(OutboundMessage.class);
            verify(mockProvider).sendMessage(any(), messageCaptor.capture());

            OutboundMessage sent = messageCaptor.getValue();
            if (sent instanceof TextMessage textMsg) {
                assertEquals("msg3", textMsg.replyToMessageId());
            }
        }
    }

    @Nested
    @DisplayName("ProcessingContext")
    class ProcessingContextTests {

        @Test
        @DisplayName("process() accepts context parameter")
        void process_acceptsContext() throws Exception {
            when(mockConverter.toUserMessage(anyList())).thenReturn(
                    UserMessage.text("Hello"));
            when(mockAgent.interact(any(AgentContext.class))).thenReturn(
                    AgentResult.success("Response"));

            AIAgentProcessor<Void> processor = AIAgentProcessor.forAgent(mockAgent)
                    .messagingProvider(mockProvider)
                    .messageConverter(mockConverter)
                    .build();

            List<InboundMessage> messages = List.of(
                    MockMessageFactory.createTextMessage("user123", "msg1", "Hello"));

            MessageProcessor.ProcessingContext context =
                    MessageProcessor.ProcessingContext.create(
                            "batch1", "msg1", "msg1",
                            MessageProcessor.ProcessingReason.TIMEOUT);

            assertDoesNotThrow(() -> processor.process("user123", messages, context));
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("handles null agent response gracefully")
        void handlesNullResponse() throws Exception {
            when(mockConverter.toUserMessage(anyList())).thenReturn(
                    UserMessage.text("Hello"));
            when(mockAgent.interact(any(AgentContext.class))).thenReturn(
                    AgentResult.success(null));

            AIAgentProcessor<Void> processor = AIAgentProcessor.forAgent(mockAgent)
                    .messagingProvider(mockProvider)
                    .messageConverter(mockConverter)
                    .build();

            List<InboundMessage> messages = List.of(
                    MockMessageFactory.createTextMessage("user123", "Test"));

            processor.process("user123", messages);

            verify(mockProvider, never()).sendMessage(any(), any());
        }

        @Test
        @DisplayName("handles very long messages")
        void handlesLongMessages() throws Exception {
            String longText = "A".repeat(10000);

            when(mockConverter.toUserMessage(anyList())).thenReturn(
                    UserMessage.text(longText));
            when(mockAgent.interact(any(AgentContext.class))).thenReturn(
                    AgentResult.success("Processed long message"));

            AIAgentProcessor<Void> processor = AIAgentProcessor.forAgent(mockAgent)
                    .messagingProvider(mockProvider)
                    .messageConverter(mockConverter)
                    .build();

            List<InboundMessage> messages = List.of(
                    MockMessageFactory.createTextMessage("user123", "msg1", longText));

            assertDoesNotThrow(() -> processor.process("user123", messages));
        }
    }

    // Test helper class for structured output
    private static class TestResponse implements WhatsAppResponse {
        private final String message;

        TestResponse(String message) {
            this.message = message;
        }

        @Override
        public List<OutboundMessage> toMessages() {
            return List.of(new TextMessage(message));
        }

        @Override
        public String getTextContent() {
            return message;
        }

        @Override
        public String getReplyToMessageId() {
            return null;
        }

        @Override
        public String getReactToMessageId() {
            return null;
        }

        @Override
        public String getReactionEmoji() {
            return null;
        }
    }
}
