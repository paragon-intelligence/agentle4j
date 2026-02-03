package com.paragon.messaging.processor;

import com.paragon.agents.AgentContext;
import com.paragon.agents.AgentResult;
import com.paragon.agents.Interactable;
import com.paragon.agents.StructuredAgentResult;
import com.paragon.messaging.conversion.DefaultMessageConverter;
import com.paragon.messaging.conversion.MessageConverter;
import com.paragon.messaging.core.MessageProcessor;
import com.paragon.messaging.core.MessagingProvider;
import com.paragon.messaging.core.OutboundMessage;
import com.paragon.messaging.store.history.ConversationHistoryStore;
import com.paragon.messaging.store.history.InMemoryConversationHistoryStore;
import com.paragon.messaging.whatsapp.config.TTSConfig;
import com.paragon.messaging.whatsapp.messages.TextMessage;
import com.paragon.messaging.whatsapp.messages.MediaMessage;
import com.paragon.messaging.whatsapp.payload.InboundMessage;
import com.paragon.messaging.whatsapp.response.WhatsAppResponse;
import com.paragon.messaging.whatsapp.WhatsAppMediaUploader;
import com.paragon.responses.spec.Message;
import com.paragon.responses.spec.ResponseInputItem;
import com.paragon.responses.spec.UserMessage;
import com.paragon.tts.TTSProvider;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * AI-powered message processor using the Interactable interface.
 *
 * <p>Processes incoming WhatsApp messages through an AI agent and sends responses
 * back to the user. Supports both simple text responses and structured outputs
 * via {@link Interactable.Structured}.</p>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Conversation history for multi-turn context</li>
 *   <li>Message batching (combines multiple messages into one user turn)</li>
 *   <li>Optional TTS (text-to-speech) responses</li>
 *   <li>Structured output support for interactive responses</li>
 *   <li>Reply context preservation</li>
 * </ul>
 *
 * <h2>Simple Usage</h2>
 * <pre>{@code
 * // Create a simple text-response processor
 * AIAgentProcessor processor = AIAgentProcessor.forAgent(myAgent)
 *     .messagingProvider(whatsappProvider)
 *     .build();
 *
 * // Use with MessageBatchingService
 * MessageBatchingService service = MessageBatchingService.builder()
 *     .processor(processor)
 *     .build();
 * }</pre>
 *
 * <h2>Structured Output Usage</h2>
 * <pre>{@code
 * // Create agent with structured output
 * Interactable.Structured<MenuResponse> structuredAgent = Agent.builder()
 *     .name("MenuAssistant")
 *     .instructions("Help users navigate our menu")
 *     .structured(MenuResponse.class)
 *     .responder(responder)
 *     .build();
 *
 * // Create processor for structured responses
 * AIAgentProcessor<MenuResponse> processor = AIAgentProcessor
 *     .forStructuredAgent(structuredAgent)
 *     .messagingProvider(whatsappProvider)
 *     .historyStore(RedisConversationHistoryStore.create(redis))
 *     .maxHistoryMessages(20)
 *     .build();
 * }</pre>
 *
 * <h2>With TTS</h2>
 * <pre>{@code
 * AIAgentProcessor processor = AIAgentProcessor.forAgent(myAgent)
 *     .messagingProvider(whatsappProvider)
 *     .ttsConfig(TTSConfig.builder()
 *         .provider(elevenLabsProvider)
 *         .speechChance(0.3)
 *         .build())
 *     .build();
 * }</pre>
 *
 * @param <T> the structured output type (Void for simple text responses)
 * @author Agentle Team
 * @see Interactable
 * @see Interactable.Structured
 * @see MessageProcessor
 * @since 2.1
 */
public final class AIAgentProcessor<T> implements MessageProcessor {

  private final Interactable agent;
  private final Interactable.@Nullable Structured<T> structuredAgent;
  private final MessagingProvider messagingProvider;
  private final @Nullable WhatsAppMediaUploader mediaUploader;
  private final MessageConverter messageConverter;
  private final @Nullable ConversationHistoryStore historyStore;
  private final @Nullable TTSProvider ttsProvider;
  private final TTSConfig ttsConfig;
  private final int maxHistoryMessages;
  private final Duration maxHistoryAge;
  private final Random random;

  private AIAgentProcessor(Builder<T> builder) {
    this.agent = Objects.requireNonNull(builder.agent, "agent cannot be null");
    this.structuredAgent = builder.structuredAgent;
    this.messagingProvider = Objects.requireNonNull(builder.messagingProvider, "messagingProvider cannot be null");
    this.mediaUploader = builder.mediaUploader;
    this.messageConverter = builder.messageConverter != null
            ? builder.messageConverter
            : DefaultMessageConverter.create();
    this.historyStore = builder.historyStore;
    this.ttsProvider = builder.ttsConfig != null ? builder.ttsConfig.provider() : null;
    this.ttsConfig = builder.ttsConfig != null ? builder.ttsConfig : TTSConfig.disabled();
    this.maxHistoryMessages = builder.maxHistoryMessages;
    this.maxHistoryAge = builder.maxHistoryAge;
    this.random = new Random();
  }

  /**
   * Creates a builder for a simple (non-structured) agent processor.
   *
   * @param agent the agent to process messages
   * @return a new builder
   */
  public static Builder<Void> forAgent(@NonNull Interactable agent) {
    return new Builder<>(agent, null);
  }

  /**
   * Creates a builder for a structured output agent processor.
   *
   * @param agent the structured agent to process messages
   * @param <T>   the structured output type
   * @return a new builder
   */
  public static <T> Builder<T> forStructuredAgent(Interactable.@NonNull Structured<T> agent) {
    return new Builder<>(agent, agent);
  }

  @Override
  public void process(
          @NonNull String userId,
          @NonNull List<? extends InboundMessage> messages,
          @NonNull ProcessingContext context
  ) throws Exception {
    Objects.requireNonNull(userId, "userId cannot be null");
    Objects.requireNonNull(messages, "messages cannot be null");

    if (messages.isEmpty()) {
      return;
    }

    // Convert inbound messages to a single UserMessage
    UserMessage userMessage = messageConverter.toUserMessage(messages);

    // Build agent context with conversation history
    AgentContext agentContext = buildAgentContext(userId, userMessage);

    // Get the last message ID for reply context
    String lastMessageId = messages.getLast().id();

    // Process through agent and send response
    if (structuredAgent != null) {
      processStructured(userId, agentContext, lastMessageId);
    } else {
      processSimple(userId, agentContext, lastMessageId);
    }

    // Store messages in history
    if (historyStore != null) {
      historyStore.addMessage(userId, userMessage);
    }
  }

  private void processSimple(String userId, AgentContext context, String replyToMessageId) throws Exception {
    AgentResult result = agent.interact(context);

    if (result.isError()) {
      throw new AIProcessingException("Agent processing failed", result.error());
    }

    String response = result.output();

    if (response == null || response.isBlank()) {
      return;
    }

    // Store assistant response in history
    if (historyStore != null) {
      historyStore.addMessage(userId, Message.assistant(response));
    }

    // Send response (text or audio)
    sendResponse(userId, response, replyToMessageId);
  }

  @SuppressWarnings("unchecked")
  private void processStructured(String userId, AgentContext context, String replyToMessageId) throws Exception {
    StructuredAgentResult<T> result = structuredAgent.interactStructured(context);

    if (result.isError()) {
      throw new AIProcessingException("Structured agent processing failed", result.error());
    }

    T output = result.output();

    if (output == null) {
      return;
    }

    // Store the raw output in history
    if (historyStore != null) {
      historyStore.addMessage(userId, Message.assistant(result.rawOutput()));
    }

    // Handle WhatsAppResponse implementations
    if (output instanceof WhatsAppResponse whatsAppResponse) {
      sendStructuredResponse(userId, whatsAppResponse, replyToMessageId);
    } else {
      // Fallback: convert to string and send as text
      sendResponse(userId, output.toString(), replyToMessageId);
    }
  }

  private void sendResponse(String userId, String response, @Nullable String replyToMessageId) throws Exception {
    MessagingProvider.Recipient recipient = MessagingProvider.Recipient.ofPhoneNumber(userId);

    // Decide text vs audio based on TTS config
    boolean shouldSendAudio = ttsConfig.isEnabled()
            && ttsProvider != null
            && random.nextDouble() < ttsConfig.speechChance();

    if (shouldSendAudio) {
      sendAudioResponse(recipient, response);
    } else {
      sendTextResponse(recipient, response, replyToMessageId);
    }
  }

  private void sendTextResponse(
          MessagingProvider.Recipient recipient,
          String response,
          @Nullable String replyToMessageId
  ) {
    OutboundMessage message;
    if (replyToMessageId != null && !replyToMessageId.isBlank()) {
      message = TextMessage.builder()
              .body(response)
              .replyTo(replyToMessageId)
              .build();
    } else {
      message = new TextMessage(response);
    }
    messagingProvider.sendMessage(recipient, message);
  }

  private void sendAudioResponse(MessagingProvider.Recipient recipient, String response) throws Exception {
    // Generate TTS audio
    com.paragon.tts.TTSConfig ttsProviderConfig = com.paragon.tts.TTSConfig.builder()
            .voiceId(ttsConfig.defaultVoiceId())
            .languageCode(ttsConfig.languageCode())
            .build();

    byte[] audioBytes = ttsProvider.synthesize(response, ttsProviderConfig);

    // Upload audio if uploader is configured
    if (mediaUploader != null) {
      try {
        // Upload audio to WhatsApp Media API
        WhatsAppMediaUploader.MediaUploadResponse uploadResponse =
                mediaUploader.uploadAudio(audioBytes, "audio/ogg; codecs=opus");

        // Send audio message using media ID
        MediaMessage.Audio audioMessage = new MediaMessage.Audio(
                new MediaMessage.MediaSource.MediaId(uploadResponse.mediaId())
        );
        messagingProvider.sendMessage(recipient, audioMessage);
        return;
      } catch (Exception e) {
        // Log error and fall back to text
        System.err.println("Failed to upload audio, falling back to text: " + e.getMessage());
      }
    }

    // Fallback to text if uploader not configured or upload failed
    sendTextResponse(recipient, response, null);
  }

  private void sendStructuredResponse(
          String userId,
          WhatsAppResponse response,
          @Nullable String defaultReplyToMessageId
  ) {
    MessagingProvider.Recipient recipient = MessagingProvider.Recipient.ofPhoneNumber(userId);

    // Get reply context from response or use default
    String replyToMessageId = response.getReplyToMessageId();
    if (replyToMessageId == null) {
      replyToMessageId = defaultReplyToMessageId;
    }

    // Send reaction if present
    String reactToMessageId = response.getReactToMessageId();
    String reactionEmoji = response.getReactionEmoji();
    if (reactToMessageId != null && reactionEmoji != null) {
      ReactionMessage reaction = new ReactionMessage(reactToMessageId, reactionEmoji);
      messagingProvider.sendReaction(recipient, reaction);
    }

    // Convert response to outbound messages and send
    List<OutboundMessage> outboundMessages = response.toMessages();
    for (OutboundMessage message : outboundMessages) {
      // Add reply context if supported and available
      OutboundMessage messageToSend = message;
      if (replyToMessageId != null && !replyToMessageId.isBlank()) {
        messageToSend = message.withReplyTo(replyToMessageId);
      }
      messagingProvider.sendMessage(recipient, messageToSend);
    }
  }

  private AgentContext buildAgentContext(String userId, UserMessage currentMessage) {
    if (historyStore == null) {
      // No history - just use current message
      return AgentContext.create().addMessage(currentMessage);
    }

    // Get conversation history
    List<ResponseInputItem> history = historyStore.getHistory(
            userId,
            maxHistoryMessages,
            maxHistoryAge
    );

    // Create context with history and add current message
    AgentContext context = AgentContext.withHistory(history);
    context.addMessage(currentMessage);
    return context;
  }

  /**
   * Builder for AIAgentProcessor.
   *
   * @param <T> the structured output type
   */
  public static final class Builder<T> {
    private final Interactable agent;
    private final @Nullable Interactable.Structured<T> structuredAgent;
    private MessagingProvider messagingProvider;
    private WhatsAppMediaUploader mediaUploader;
    private MessageConverter messageConverter;
    private ConversationHistoryStore historyStore;
    private TTSConfig ttsConfig;
    private int maxHistoryMessages = 20;
    private Duration maxHistoryAge = Duration.ofHours(24);

    private Builder(Interactable agent, @Nullable Interactable.Structured<T> structuredAgent) {
      this.agent = agent;
      this.structuredAgent = structuredAgent;
    }

    /**
     * Sets the messaging provider for sending responses.
     *
     * <p>This is required.</p>
     *
     * @param provider the messaging provider
     * @return this builder
     */
    public Builder<T> messagingProvider(@NonNull MessagingProvider provider) {
      this.messagingProvider = provider;
      return this;
    }

    /**
     * Sets a custom message converter.
     *
     * <p>If not set, uses {@link DefaultMessageConverter}.</p>
     *
     * @param converter the message converter
     * @return this builder
     */
    public Builder<T> messageConverter(@NonNull MessageConverter converter) {
      this.messageConverter = converter;
      return this;
    }

    /**
     * Sets the conversation history store.
     *
     * <p>If not set, no history is maintained between calls.
     * For multi-turn conversations, use an in-memory or Redis store.</p>
     *
     * @param store the history store
     * @return this builder
     */
    public Builder<T> historyStore(@NonNull ConversationHistoryStore store) {
      this.historyStore = store;
      return this;
    }

    /**
     * Enables in-memory conversation history with default settings.
     *
     * @return this builder
     */
    public Builder<T> withInMemoryHistory() {
      this.historyStore = InMemoryConversationHistoryStore.create();
      return this;
    }

    /**
     * Enables in-memory conversation history with custom per-user limit.
     *
     * @param maxPerUser maximum messages to store per user
     * @return this builder
     */
    public Builder<T> withInMemoryHistory(int maxPerUser) {
      this.historyStore = InMemoryConversationHistoryStore.create(maxPerUser);
      return this;
    }

    /**
     * Sets the TTS configuration for optional audio responses.
     *
     * @param config TTS configuration
     * @return this builder
     */
    public Builder<T> ttsConfig(@NonNull TTSConfig config) {
      this.ttsConfig = config;
      return this;
    }

    /**
     * Sets the media uploader for sending audio messages.
     *
     * <p>If not set, TTS audio will fall back to text messages.</p>
     *
     * @param uploader the WhatsApp media uploader
     * @return this builder
     */
    public Builder<T> mediaUploader(@NonNull WhatsAppMediaUploader uploader) {
      this.mediaUploader = uploader;
      return this;
    }

    /**
     * Sets the maximum number of history messages to include in context.
     *
     * <p>Default is 20.</p>
     *
     * @param max maximum history messages
     * @return this builder
     */
    public Builder<T> maxHistoryMessages(int max) {
      this.maxHistoryMessages = max;
      return this;
    }

    /**
     * Sets the maximum age of history messages to include.
     *
     * <p>Default is 24 hours.</p>
     *
     * @param maxAge maximum history age
     * @return this builder
     */
    public Builder<T> maxHistoryAge(@NonNull Duration maxAge) {
      this.maxHistoryAge = maxAge;
      return this;
    }

    /**
     * Builds the AIAgentProcessor.
     *
     * @return the configured processor
     * @throws IllegalStateException if required fields are not set
     */
    public AIAgentProcessor<T> build() {
      if (messagingProvider == null) {
        throw new IllegalStateException("messagingProvider must be set");
      }
      return new AIAgentProcessor<>(this);
    }
  }

  /**
   * Exception thrown when AI processing fails.
   */
  public static class AIProcessingException extends RuntimeException {
    public AIProcessingException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
