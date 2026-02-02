package com.paragon.messaging.whatsapp;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Processa lote de mensagens de um usuário.
 *
 * <p>Implementações tipicamente:</p>
 * <ol>
 *   <li>Combinam mensagens em input único</li>
 *   <li>Chamam AI agent (Agentle)</li>
 *   <li>Enviam resposta via plataforma de mensageria</li>
 *   <li>Opcionalmente convertem para áudio (TTS)</li>
 * </ol>
 *
 * <p><b>Exemplo Simples:</b></p>
 * <pre>{@code
 * MessageProcessor processor = (userId, messages) -> {
 *     // 1. Combinar mensagens
 *     String input = messages.stream()
 *         .map(Message::content)
 *         .collect(Collectors.joining("\n"));
 *
 *     // 2. Processar com AI agent
 *     AgentResult result = agent.interact(input);
 *     String response = result.output();
 *
 *     // 3. Enviar resposta
 *     whatsappProvider.sendText(
 *         Recipient.ofPhoneNumber(userId),
 *         new TextMessage(response)
 *     );
 * };
 * }</pre>
 *
 * <p><b>Exemplo com TTS:</b></p>
 * <pre>{@code
 * MessageProcessor processor = (userId, messages) -> {
 *     String input = combineMessages(messages);
 *     String response = agent.interact(input).output();
 *
 *     // Decidir texto vs áudio
 *     if (random.nextDouble() < speechPlayChance) {
 *         byte[] audio = ttsProvider.synthesize(response, ttsConfig);
 *         sendAudio(userId, audio);
 *     } else {
 *         sendText(userId, response);
 *     }
 * };
 * }</pre>
 *
 * <p><b>Thread Safety:</b></p>
 * <p>Processors são chamados de virtual threads e podem ser invocados
 * concorrentemente para diferentes usuários. Implementações devem ser
 * thread-safe se acessarem estado mutável compartilhado.</p>
 *
 * <p><b>Error Handling:</b></p>
 * <p>Exceções lançadas são capturadas por {@link MessageBatchingService}
 * e tratadas conforme {@link ErrorHandlingStrategy}.</p>
 *
 * @author Agentle Team
 * @see MessageBatchingService
 * @since 1.0
 */
@FunctionalInterface
public interface MessageProcessor {

  /**
   * Processor no-op que não faz nada.
   *
   * @return processor vazio
   */
  static MessageProcessor noOp() {
    return (userId, messages) -> {
    };
  }

  /**
   * Processor de logging que apenas loga mensagens.
   *
   * @param logger consumidor de mensagens de log
   * @return processor de logging
   */
  static MessageProcessor logging(Consumer<String> logger) {
    return (userId, messages) -> {
      String log = String.format(
              "Processando %d mensagens para usuário %s: %s",
              messages.size(),
              userId,
              messages.stream()
                      .map(Message::content)
                      .collect(Collectors.joining(", "))
      );
      logger.accept(log);
    };
  }

  /**
   * Processa lote de mensagens de um usuário.
   *
   * @param userId   ID do usuário
   * @param messages lote de mensagens (garantidamente não-vazio e ordenado por timestamp)
   * @throws Exception se processamento falhar (será tratado por error strategy)
   */
  void process(String userId, List<Message> messages) throws Exception;
}