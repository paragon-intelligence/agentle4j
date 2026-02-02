package com.paragon.messaging.whatsapp;

import com.paragon.agents.Interactable;
import com.paragon.agents.StructuredAgentResult;
import com.paragon.messaging.whatsapp.config.WhatsAppConfig;
import com.paragon.messaging.whatsapp.domain.WhatsAppResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Integrator service that bridges WhatsApp Webhooks with AI Agents.
 */
public class WhatsAppAgentIntegrator {

  private static final Logger logger = LoggerFactory.getLogger(WhatsAppAgentIntegrator.class);

  private final Interactable.Structured<WhatsAppResponse> agent;
  private final MessagingProvider messagingProvider;
  private final MessageStore messageStore;
  private final WhatsAppConfig config;

  private final ConcurrentMap<String, Long> floodProtection = new ConcurrentHashMap<>();

  public WhatsAppAgentIntegrator(
          Interactable.Structured<WhatsAppResponse> agent,
          MessagingProvider messagingProvider,
          MessageStore messageStore,
          WhatsAppConfig config) {
    this.agent = Objects.requireNonNull(agent, "Agent cannot be null");
    this.messagingProvider = Objects.requireNonNull(messagingProvider, "MessagingProvider cannot be null");
    this.messageStore = Objects.requireNonNull(messageStore, "MessageStore cannot be null");
    this.config = Objects.requireNonNull(config, "Config cannot be null");
  }

  public void processWebhook(@NonNull WebhookEvent event) {
    if (event instanceof WebhookEvent.IncomingMessageEvent messageEvent) {
      handleIncomingMessage(messageEvent);
    } else if (event instanceof WebhookEvent.MessageStatusEvent statusEvent) {
      handleStatusUpdate(statusEvent);
    }
  }

  private void handleIncomingMessage(WebhookEvent.IncomingMessageEvent event) {
    String userId = event.senderId();
    String messageId = event.messageId();

    if (messageStore.hasProcessed(userId, messageId)) {
      logger.info("Message {} already processed, skipping.", messageId);
      return;
    }

    if (isFlooding(userId)) {
      logger.warn("User {} is flooding, ignoring message.", userId);
      return;
    }

    messageStore.markProcessed(userId, messageId);

    // Convert Webhook Event to AI Message (UserMessage) and store
    com.paragon.responses.spec.Message userMessage = convertToAgentMessage(event);
    if (userMessage == null) {
      logger.warn("Unsupported message type received or failed conversion.");
      return;
    }
    
    // Store incoming message (User Role)
    messageStore.store(userId, userMessage);

    Thread.startVirtualThread(() -> {
      try {
        // Load history
        List<com.paragon.responses.spec.Message> history = messageStore.retrieve(userId);
        
        // TODO: Pass history to agent if Interactable supported it directly, 
        // but Interactable currently takes 'input' string or 'Message'.
        // Assuming we pass the *last* message as interaction trigger and Agent manages context 
        // via its internal memory or we pass context via different method.
        // For 'Interactable', we usually pass the text or the Message object.
        
        StructuredAgentResult<WhatsAppResponse> result;
        
        // If the agent supports implicit context loading, we just pass the new input.
        if (userMessage instanceof com.paragon.responses.spec.UserMessage um) {
            // Simplified: Passing text content. 
            // In a real scenario, use `agent.interact(userMessage)` if supported.
            String textInput = extractText(event);
            if (textInput != null) {
               result = agent.interactStructured(textInput);
            } else {
               // Fallback or skip
               return;
            }
        } else {
             return;
        }

        WhatsAppResponse response = result.parsed();

        // Send and Store Response (Assistant Role)
        sendResponse(userId, response);

      } catch (Exception e) {
        logger.error("Error processing message for user {}", userId, e);
      }
    });
  }

  private com.paragon.responses.spec.Message convertToAgentMessage(WebhookEvent.IncomingMessageEvent event) {
    // Basic implementation for Text
    if (event.content() instanceof WebhookEvent.TextContent text) {
        return com.paragon.responses.spec.Message.user(text.body());
    }
    // TODO: Handle Image/Audio
    return null;
  }

  private void handleStatusUpdate(WebhookEvent.MessageStatusEvent event) {
    logger.debug("Message {} status: {}", event.messageId(), event.status());
  }

  private boolean isFlooding(String userId) {
    long now = System.currentTimeMillis();
    long lastTime = floodProtection.getOrDefault(userId, 0L);
    if (now - lastTime < 500) {
      return true;
    }
    floodProtection.put(userId, now);
    return false;
  }

  private String extractText(WebhookEvent.IncomingMessageEvent event) {
      if (event.content() instanceof WebhookEvent.TextContent text) {
          return text.body();
      }
      return null;
  }

  private void sendResponse(String userId, WhatsAppResponse response) throws MessagingException {
    Recipient recipient = new Recipient(userId, Recipient.RecipientType.PHONE_NUMBER);
    Message message = convertToWhatsAppMessage(response);

    messagingProvider.sendMessage(recipient, message);

    // Convert WhatsAppResponse back to AssistantMessage for storage
    com.paragon.responses.spec.Message assistantMsg = convertToAssistantMessage(response);
    messageStore.store(userId, assistantMsg);
  }

  private Message convertToWhatsAppMessage(WhatsAppResponse response) {
      return switch (response) {
        case WhatsAppResponse.TextResponse t -> new TextMessage(t.message(), false);
        case WhatsAppResponse.ImageResponse i -> new MediaMessage.Image(
                new MediaMessage.MediaSource.Url(i.imageUrl()),
                java.util.Optional.ofNullable(i.caption())
        );
        case WhatsAppResponse.LinkResponse l -> new InteractiveMessage.CtaUrlMessage(
                 l.text(), l.buttonText(), l.url()
        );
        // Fallback for Menu
        case WhatsAppResponse.MenuResponse m -> new TextMessage(m.title() + "\n" + m.description(), false);
      };
  }

  private com.paragon.responses.spec.Message convertToAssistantMessage(WhatsAppResponse response) {
      // Simplified: Just store text representation
      String text = switch (response) {
          case WhatsAppResponse.TextResponse t -> t.message();
          case WhatsAppResponse.ImageResponse i -> "[Image: " + i.imageUrl() + "]";
          case WhatsAppResponse.LinkResponse l -> "[Link: " + l.url() + "]";
          case WhatsAppResponse.MenuResponse m -> "[Menu: " + m.title() + "]";
      };
      return com.paragon.responses.spec.Message.assistant(text);
  }
}
