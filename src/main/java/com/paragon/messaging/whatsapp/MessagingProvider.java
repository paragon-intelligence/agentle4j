package com.paragon.messaging.whatsapp;

import com.example.messaging.api.exception.MessagingException;
import com.example.messaging.model.common.Recipient;
import com.example.messaging.model.message.*;
import com.example.messaging.model.response.MessageResponse;
import com.paragon.messaging.whatsapp.payload.ContactMessage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Interface principal para provedores de mensagens (WhatsApp, Facebook, Messenger, etc.).
 *
 * <p>Esta interface define o contrato para envio de mensagens através de diferentes
 * plataformas de mensageria. Com Java virtual threads, a API é síncrona e simples,
 * mas altamente escalável quando executada em virtual threads.</p>
 *
 * <h2>Uso com Virtual Threads</h2>
 * <pre>{@code
 * // Forma 1: Virtual thread per task executor
 * try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
 *     executor.submit(() -> provider.sendMessage(recipient, message));
 * }
 *
 * // Forma 2: Direct virtual thread
 * Thread.startVirtualThread(() -> {
 *     try {
 *         MessageResponse response = provider.sendMessage(recipient, message);
 *         System.out.println("Sent: " + response.messageId());
 *     } catch (MessagingException e) {
 *         e.printStackTrace();
 *     }
 * });
 *
 * // Forma 3: Structured Concurrency (Java 21+)
 * try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
 *     var task1 = scope.fork(() -> provider.sendMessage(recipient1, message));
 *     var task2 = scope.fork(() -> provider.sendMessage(recipient2, message));
 *
 *     scope.join();
 *     scope.throwIfFailed();
 *
 *     MessageResponse r1 = task1.get();
 *     MessageResponse r2 = task2.get();
 * }
 * }</pre>
 *
 * @author Your Name
 * @since 2.0
 */
public interface MessagingProvider {

  /**
   * Retorna o tipo/nome do provedor (ex: "WHATSAPP_CLOUD", "FACEBOOK_MESSENGER").
   *
   * @return identificador único do provedor
   */
  String getProviderType();

  /**
   * Verifica se o provedor está configurado e pronto para enviar mensagens.
   *
   * @return true se o provedor está pronto, false caso contrário
   */
  boolean isConfigured();

  /**
   * Envia uma mensagem através do provedor.
   *
   * <p>Este método é bloqueante mas pode ser executado de forma eficiente
   * em virtual threads sem consumir threads da plataforma.</p>
   *
   * @param recipient destinatário da mensagem (não pode ser null)
   * @param message   conteúdo da mensagem (não pode ser null, será validado)
   * @return resposta do envio contendo ID da mensagem e status
   * @throws MessagingException se houver erro no envio
   */
  MessageResponse sendMessage(
          @NotNull @Valid Recipient recipient,
          @NotNull @Valid Message message
  ) throws MessagingException;

  /**
   * Envia uma mensagem de texto simples.
   *
   * @param recipient   destinatário da mensagem
   * @param textMessage mensagem de texto
   * @return resposta do envio
   * @throws MessagingException se houver erro no envio
   */
  default MessageResponse sendText(
          @NotNull @Valid Recipient recipient,
          @NotNull @Valid TextMessage textMessage
  ) throws MessagingException {
    return sendMessage(recipient, textMessage);
  }

  /**
   * Envia uma mensagem de mídia (imagem, vídeo, áudio, documento).
   *
   * @param recipient    destinatário da mensagem
   * @param mediaMessage mensagem de mídia
   * @return resposta do envio
   * @throws MessagingException se houver erro no envio
   */
  default MessageResponse sendMedia(
          @NotNull @Valid Recipient recipient,
          @NotNull @Valid MediaMessage mediaMessage
  ) throws MessagingException {
    return sendMessage(recipient, mediaMessage);
  }

  /**
   * Envia uma mensagem de template (para mensagens fora da janela de 24h).
   *
   * @param recipient       destinatário da mensagem
   * @param templateMessage mensagem baseada em template
   * @return resposta do envio
   * @throws MessagingException se houver erro no envio
   */
  default MessageResponse sendTemplate(
          @NotNull @Valid Recipient recipient,
          @NotNull @Valid TemplateMessage templateMessage
  ) throws MessagingException {
    return sendMessage(recipient, templateMessage);
  }

  /**
   * Envia uma mensagem interativa (botões, listas, etc.).
   *
   * @param recipient          destinatário da mensagem
   * @param interactiveMessage mensagem interativa
   * @return resposta do envio
   * @throws MessagingException se houver erro no envio
   */
  default MessageResponse sendInteractive(
          @NotNull @Valid Recipient recipient,
          @NotNull @Valid InteractiveMessage interactiveMessage
  ) throws MessagingException {
    return sendMessage(recipient, interactiveMessage);
  }

  /**
   * Envia uma localização.
   *
   * @param recipient       destinatário da mensagem
   * @param locationMessage mensagem de localização
   * @return resposta do envio
   * @throws MessagingException se houver erro no envio
   */
  default MessageResponse sendLocation(
          @NotNull @Valid Recipient recipient,
          @NotNull @Valid LocationMessage locationMessage
  ) throws MessagingException {
    return sendMessage(recipient, locationMessage);
  }

  /**
   * Envia um ou mais contatos.
   *
   * @param recipient      destinatário da mensagem
   * @param contactMessage mensagem de contato
   * @return resposta do envio
   * @throws MessagingException se houver erro no envio
   */
  default MessageResponse sendContact(
          @NotNull @Valid Recipient recipient,
          @NotNull @Valid ContactMessage contactMessage
  ) throws MessagingException {
    return sendMessage(recipient, contactMessage);
  }

  /**
   * Envia uma reação a uma mensagem existente.
   *
   * @param recipient       destinatário da reação
   * @param reactionMessage reação (emoji)
   * @return resposta do envio
   * @throws MessagingException se houver erro no envio
   */
  default MessageResponse sendReaction(
          @NotNull @Valid Recipient recipient,
          @NotNull @Valid ReactionMessage reactionMessage
  ) throws MessagingException {
    return sendMessage(recipient, reactionMessage);
  }

  /**
   * Envia múltiplas mensagens em paralelo usando virtual threads.
   *
   * <p>Este método usa Structured Concurrency (Java 25 JEP 505) com o novo
   * {@code Joiner.allSuccessfulOrThrow()} que falha se QUALQUER mensagem falhar.</p>
   *
   * <p><b>Comportamento:</b></p>
   * <ul>
   *   <li>Todas as mensagens são enviadas em paralelo (uma virtual thread por mensagem)</li>
   *   <li>Se UMA mensagem falhar, TODAS as demais são canceladas automaticamente</li>
   *   <li>Retorna resultados na mesma ordem das mensagens de entrada</li>
   * </ul>
   *
   * @param recipient destinatário das mensagens
   * @param messages  lista de mensagens a enviar
   * @return lista de respostas na mesma ordem das mensagens
   * @throws MessagingException se alguma mensagem falhar
   */
  default java.util.List<MessageResponse> sendBatch(
          @NotNull @Valid Recipient recipient,
          @NotNull java.util.List<@Valid Message> messages
  ) throws MessagingException {

    try (var scope = java.util.concurrent.StructuredTaskScope.open(
            java.util.concurrent.StructuredTaskScope.Joiner.allSuccessfulOrThrow())) {

      // Fork uma subtask para cada mensagem
      var subtasks = messages.stream()
              .map(msg -> scope.fork(() -> sendMessage(recipient, msg)))
              .toList();

      // Aguardar todas (lança exceção se alguma falhar)
      scope.join();

      // Coletar resultados na ordem original
      return subtasks.stream()
              .map(java.util.concurrent.StructuredTaskScope.Subtask::get)
              .toList();

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new MessagingException("Batch send interrupted", e);
    } catch (Exception e) {
      throw new MessagingException("Batch send failed", e);
    }
  }

  /**
   * Envia para múltiplos destinatários em paralelo, retorna apenas os sucessos.
   *
   * <p>Diferente de {@link #sendBatch}, este método NÃO falha se algumas mensagens
   * falharem. Use quando quiser "melhor esforço" (ex: notificações em massa).</p>
   *
   * <p><b>Comportamento:</b></p>
   * <ul>
   *   <li>Todas as mensagens são enviadas em paralelo</li>
   *   <li>Falhas individuais NÃO cancelam as demais</li>
   *   <li>Retorna apenas os sucessos</li>
   * </ul>
   *
   * @param recipients lista de destinatários
   * @param message    mensagem a enviar para todos
   * @return lista de respostas bem-sucedidas (pode estar vazia)
   * @throws MessagingException apenas se o próprio processo de envio falhar
   */
  default java.util.List<MessageResponse> sendBroadcast(
          @NotNull java.util.List<@Valid Recipient> recipients,
          @NotNull @Valid Message message
  ) throws MessagingException {

    try (var scope = java.util.concurrent.StructuredTaskScope.open(
            java.util.concurrent.StructuredTaskScope.Joiner.<MessageResponse>awaitAll())) {

      // Fork uma subtask para cada destinatário
      var subtasks = recipients.stream()
              .map(recipient -> scope.fork(() -> sendMessage(recipient, message)))
              .toList();

      // Aguardar todas (NÃO lança exceção se alguma falhar)
      scope.join();

      // Coletar apenas os sucessos
      return subtasks.stream()
              .filter(java.util.concurrent.StructuredTaskScope.Subtask::state)
              .map(java.util.concurrent.StructuredTaskScope.Subtask::get)
              .toList();

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new MessagingException("Broadcast interrupted", e);
    }
  }
}
