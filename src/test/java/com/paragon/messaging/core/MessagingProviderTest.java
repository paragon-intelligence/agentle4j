package com.paragon.messaging.core;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.messaging.whatsapp.messages.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link MessagingProvider} interface contract and default methods. */
@DisplayName("MessagingProvider")
class MessagingProviderTest {

  private TestMessagingProvider provider;
  private Recipient recipient;

  @BeforeEach
  void setUp() {
    provider = new TestMessagingProvider();
    recipient = Recipient.ofPhoneNumber("+5511999999999");
  }

  @Nested
  @DisplayName("Core Contract")
  class CoreContractTests {

    @Test
    @DisplayName("getProviderType() returns provider identifier")
    void getProviderTypeReturnsIdentifier() {
      assertEquals(MessagingProvider.ProviderType.WHATSAPP, provider.getProviderType());
    }

    @Test
    @DisplayName("isConfigured() returns configuration status")
    void isConfiguredReturnsStatus() {
      assertTrue(provider.isConfigured());

      provider.setConfigured(false);
      assertFalse(provider.isConfigured());
    }

    @Test
    @DisplayName("sendMessage() sends message via provider")
    void sendMessageSendsViProvider() throws MessagingException {
      TextMessage message = new TextMessage("Hello, World!");

      MessageResponse response = provider.sendMessage(recipient, message);

      assertNotNull(response);
      assertTrue(response.success());
      assertEquals(1, provider.getSendCount());
    }
  }

  @Nested
  @DisplayName("Default Methods")
  class DefaultMethodsTests {

    @Test
    @DisplayName("sendText() delegates to sendMessage()")
    void sendTextDelegates() throws MessagingException {
      TextMessage message = new TextMessage("Test");

      provider.sendText(recipient, message);

      assertEquals(1, provider.getSendCount());
      assertEquals(message, provider.getLastMessage());
    }

    @Test
    @DisplayName("sendMedia() delegates to sendMessage()")
    void sendMediaDelegates() throws MessagingException {
      MediaMessage.Image image =
          new MediaMessage.Image(new MediaMessage.MediaSource.MediaId("media123"));

      provider.sendMedia(recipient, image);

      assertEquals(1, provider.getSendCount());
      assertEquals(image, provider.getLastMessage());
    }

    @Test
    @DisplayName("sendTemplate() delegates to sendMessage()")
    void sendTemplateDelegates() throws MessagingException {
      TemplateMessage template = new TemplateMessage("hello_world", "en_US");

      provider.sendTemplate(recipient, template);

      assertEquals(1, provider.getSendCount());
      assertEquals(template, provider.getLastMessage());
    }

    @Test
    @DisplayName("sendInteractive() delegates to sendMessage()")
    void sendInteractiveDelegates() throws MessagingException {
      InteractiveMessage.ButtonMessage buttons =
          InteractiveMessage.ButtonMessage.builder()
              .body("Choose an option:")
              .addButton("opt1", "Option 1")
              .build();

      provider.sendInteractive(recipient, buttons);

      assertEquals(1, provider.getSendCount());
      assertEquals(buttons, provider.getLastMessage());
    }

    @Test
    @DisplayName("sendLocation() delegates to sendMessage()")
    void sendLocationDelegates() throws MessagingException {
      LocationMessage location = new LocationMessage(-23.550520, -46.633308);

      provider.sendLocation(recipient, location);

      assertEquals(1, provider.getSendCount());
      assertEquals(location, provider.getLastMessage());
    }

    @Test
    @DisplayName("sendReaction() delegates to sendMessage()")
    void sendReactionDelegates() throws MessagingException {
      ReactionMessage reaction = new ReactionMessage("wamid.123", "👍");

      provider.sendReaction(recipient, reaction);

      assertEquals(1, provider.getSendCount());
      assertEquals(reaction, provider.getLastMessage());
    }
  }

  @Nested
  @DisplayName("Batch Sending")
  class BatchSendingTests {

    @Test
    @DisplayName("sendBatch() sends multiple messages")
    void sendBatchSendsMultipleMessages() throws MessagingException {
      List<OutboundMessage> messages =
          Arrays.asList(
              new TextMessage("Message 1"),
              new TextMessage("Message 2"),
              new TextMessage("Message 3"));

      List<MessageResponse> responses = provider.sendBatch(recipient, messages);

      assertEquals(3, responses.size());
      assertEquals(3, provider.getSendCount());
    }

    @Test
    @DisplayName("sendBatch() returns responses in order")
    void sendBatchReturnsInOrder() throws MessagingException {
      List<OutboundMessage> messages =
          Arrays.asList(
              new TextMessage("First"), new TextMessage("Second"), new TextMessage("Third"));

      List<MessageResponse> responses = provider.sendBatch(recipient, messages);

      assertEquals("msg-0", responses.get(0).messageId());
      assertEquals("msg-1", responses.get(1).messageId());
      assertEquals("msg-2", responses.get(2).messageId());
    }

    @Test
    @DisplayName("sendBatch() fails if any message fails")
    void sendBatchFailsIfAnyFails() {
      provider.setFailOnCount(2); // Fail on second message

      List<OutboundMessage> messages =
          Arrays.asList(
              new TextMessage("Message 1"),
              new TextMessage("Message 2"),
              new TextMessage("Message 3"));

      assertThrows(MessagingException.class, () -> provider.sendBatch(recipient, messages));
    }

    @Test
    @DisplayName("sendBatch() handles single message")
    void sendBatchHandlesSingleMessage() throws MessagingException {
      List<OutboundMessage> messages = List.of(new TextMessage("Single"));

      List<MessageResponse> responses = provider.sendBatch(recipient, messages);

      assertEquals(1, responses.size());
    }

    @Test
    @DisplayName("sendBatch() handles empty list")
    void sendBatchHandlesEmptyList() throws MessagingException {
      List<OutboundMessage> messages = List.of();

      List<MessageResponse> responses = provider.sendBatch(recipient, messages);

      assertEquals(0, responses.size());
    }
  }

  @Nested
  @DisplayName("Broadcast Sending")
  class BroadcastSendingTests {

    @Test
    @DisplayName("sendBroadcast() sends to multiple recipients")
    void sendBroadcastSendsToMultipleRecipients() throws MessagingException {
      List<Recipient> recipients =
          Arrays.asList(
              Recipient.ofPhoneNumber("+5511111111111"),
              Recipient.ofPhoneNumber("+5511222222222"),
              Recipient.ofPhoneNumber("+5511333333333"));
      TextMessage message = new TextMessage("Broadcast message");

      List<MessageResponse> responses = provider.sendBroadcast(recipients, message);

      assertEquals(3, responses.size());
      assertEquals(3, provider.getSendCount());
    }

    @Test
    @DisplayName("sendBroadcast() returns only successes when some fail")
    void sendBroadcastReturnsOnlySuccesses() throws MessagingException {
      provider.setFailOnCount(2); // Fail on second message

      List<Recipient> recipients =
          Arrays.asList(
              Recipient.ofPhoneNumber("+5511111111111"),
              Recipient.ofPhoneNumber("+5511222222222"),
              Recipient.ofPhoneNumber("+5511333333333"));
      TextMessage message = new TextMessage("Broadcast");

      List<MessageResponse> responses = provider.sendBroadcast(recipients, message);

      // Should get 2 successes (first and third)
      assertEquals(2, responses.size());
    }

    @Test
    @DisplayName("sendBroadcast() handles single recipient")
    void sendBroadcastHandlesSingleRecipient() throws MessagingException {
      List<Recipient> recipients = List.of(Recipient.ofPhoneNumber("+5511999999999"));
      TextMessage message = new TextMessage("Single");

      List<MessageResponse> responses = provider.sendBroadcast(recipients, message);

      assertEquals(1, responses.size());
    }

    @Test
    @DisplayName("sendBroadcast() handles all failures")
    void sendBroadcastHandlesAllFailures() throws MessagingException {
      provider.setFailOnCount(0); // Fail on all

      List<Recipient> recipients =
          Arrays.asList(
              Recipient.ofPhoneNumber("+5511111111111"), Recipient.ofPhoneNumber("+5511222222222"));
      TextMessage message = new TextMessage("Fail");

      List<MessageResponse> responses = provider.sendBroadcast(recipients, message);

      assertEquals(0, responses.size());
    }
  }

  @Nested
  @DisplayName("Real-World Scenarios")
  class RealWorldScenariosTests {

    @Test
    @DisplayName("sends text message to customer")
    void sendsTextToCustomer() throws MessagingException {
      TextMessage message = new TextMessage("Thank you for your order!");

      MessageResponse response = provider.sendText(recipient, message);

      assertTrue(response.success());
      assertNotNull(response.messageId());
    }

    @Test
    @DisplayName("sends buttons for customer support")
    void sendsButtonsForSupport() throws MessagingException {
      InteractiveMessage.ButtonMessage buttons =
          InteractiveMessage.ButtonMessage.builder()
              .body("How can we help you today?")
              .addButton("sales", "Sales")
              .addButton("support", "Support")
              .addButton("billing", "Billing")
              .build();

      MessageResponse response = provider.sendInteractive(recipient, buttons);

      assertTrue(response.success());
    }

    @Test
    @DisplayName("sends image with caption")
    void sendsImageWithCaption() throws MessagingException {
      MediaMessage.Image image =
          new MediaMessage.Image(
              new MediaMessage.MediaSource.Url("https://example.com/image.jpg"),
              "Check out our new product!");

      MessageResponse response = provider.sendMedia(recipient, image);

      assertTrue(response.success());
    }

    @Test
    @DisplayName("sends multiple messages in conversation")
    void sendsMultipleInConversation() throws MessagingException {
      List<OutboundMessage> messages =
          Arrays.asList(
              new TextMessage("Hello!"),
              new TextMessage("How can I help you today?"),
              InteractiveMessage.ButtonMessage.builder()
                  .body("Choose an option:")
                  .addButton("help", "I need help")
                  .build());

      List<MessageResponse> responses = provider.sendBatch(recipient, messages);

      assertEquals(3, responses.size());
      assertTrue(responses.stream().allMatch(MessageResponse::success));
    }

    @Test
    @DisplayName("broadcasts announcement to customers")
    void broadcastsAnnouncement() throws MessagingException {
      List<Recipient> recipients =
          Arrays.asList(
              Recipient.ofPhoneNumber("+5511111111111"),
              Recipient.ofPhoneNumber("+5511222222222"),
              Recipient.ofPhoneNumber("+5511333333333"),
              Recipient.ofPhoneNumber("+5511444444444"));
      TextMessage announcement = new TextMessage("New feature available! Check it out.");

      List<MessageResponse> responses = provider.sendBroadcast(recipients, announcement);

      assertEquals(4, responses.size());
    }
  }

  @Nested
  @DisplayName("Error Handling")
  class ErrorHandlingTests {

    @Test
    @DisplayName("throws MessagingException on send failure")
    void throwsMessagingException() {
      provider.setFailOnCount(0);

      assertThrows(
          MessagingException.class, () -> provider.sendText(recipient, new TextMessage("Test")));
    }

    @Test
    @DisplayName("MessagingException has message")
    void messagingExceptionHasMessage() {
      MessagingException ex = new MessagingException("Test error");

      assertEquals("Test error", ex.getMessage());
    }

    @Test
    @DisplayName("MessagingException has cause")
    void messagingExceptionHasCause() {
      Exception cause = new RuntimeException("Root cause");
      MessagingException ex = new MessagingException("Test error", cause);

      assertEquals("Test error", ex.getMessage());
      assertEquals(cause, ex.getCause());
    }
  }

  // Test implementation of MessagingProvider
  private static class TestMessagingProvider implements MessagingProvider {
    private final AtomicInteger sendCount = new AtomicInteger(0);
    private OutboundMessage lastMessage;
    private boolean configured = true;
    private int failOnCount = -1; // -1 means never fail

    @Override
    public MessagingProvider.ProviderType getProviderType() {
      return MessagingProvider.ProviderType.WHATSAPP; // Using WHATSAPP as test provider
    }

    @Override
    public boolean isConfigured() {
      return configured;
    }

    @Override
    public MessageResponse sendMessage(Recipient recipient, OutboundMessage message)
        throws MessagingException {
      int count = sendCount.getAndIncrement();

      if (failOnCount >= 0 && count >= failOnCount) {
        throw new MessagingException("Simulated send failure");
      }

      lastMessage = message;
      return new MessageResponse("msg-" + count, MessageResponse.MessageStatus.SENT, Instant.now());
    }

    public int getSendCount() {
      return sendCount.get();
    }

    public OutboundMessage getLastMessage() {
      return lastMessage;
    }

    public void setConfigured(boolean configured) {
      this.configured = configured;
    }

    public void setFailOnCount(int failOnCount) {
      this.failOnCount = failOnCount;
    }
  }
}
