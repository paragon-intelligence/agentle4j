package com.paragon.messaging.whatsapp;

/**
 * Processa mensagens com IA e opcionalmente converte para áudio.
 */
public class AIAgentProcessor implements MessageProcessor {

  private final Agent agent;
  private final MessagingProvider messagingProvider;
  private final Optional<TTSProvider> ttsProvider;
  private final double speechPlayChance;
  private final Random random = new Random();

  @Override
  public void process(String userId, List<Message> messages) throws Exception {

    // 1. Concatenar mensagens
    String userInput = messages.stream()
            .map(Message::content)
            .collect(Collectors.joining("\n"));

    // 2. Processar com agente
    AgentResult result = agent.interact(userInput);

    if (result.isError()) {
      throw new RuntimeException("Agent error", result.error());
    }

    String response = result.output();

    // 3. Decidir se envia como texto ou áudio
    boolean shouldSendAudio = ttsProvider.isPresent()
            && random.nextDouble() < speechPlayChance;

    Recipient recipient = Recipient.ofPhoneNumber(userId);

    if (shouldSendAudio) {
      // Enviar como áudio
      byte[] audio = ttsProvider.get().synthesize(
              response,
              TTSConfig.defaults()
      );

      // Upload áudio e enviar
      MediaMessage.Audio audioMsg = uploadAndCreateAudioMessage(audio);
      messagingProvider.sendMedia(recipient, audioMsg);

    } else {
      // Enviar como texto
      TextMessage textMsg = new TextMessage(response);
      messagingProvider.sendText(recipient, textMsg);
    }
  }
}