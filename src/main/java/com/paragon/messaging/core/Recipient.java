package com.paragon.messaging.core;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Representa um destinatário de mensagem com validação automática.
 *
 * @param identifier identificador do destinatário (número de telefone, user ID, email)
 * @param type       tipo de destinatário
 * @author Your Name
 * @since 2.0
 */
public record Recipient(

        @NotBlank(message = "Recipient identifier cannot be blank")
        String identifier,

        @NotNull(message = "Recipient type cannot be null")
        RecipientType type

) {

  /**
   * Cria um destinatário usando número de telefone.
   *
   * <p>O número será validado automaticamente quando usado com @Valid.</p>
   *
   * @param phoneNumber número de telefone no formato E.164 (ex: +5511999999999)
   * @return destinatário validado
   * @throws IllegalArgumentException se o número não estiver em formato E.164
   */
  public static Recipient ofPhoneNumber(
          @NotBlank String phoneNumber
  ) {
    return new Recipient(phoneNumber, RecipientType.PHONE_NUMBER);
  }

  /**
   * Cria um destinatário usando número de telefone com normalização automática.
   *
   * <p>Este método tenta normalizar o número removendo espaços, parênteses, etc.</p>
   *
   * @param phoneNumber número de telefone em qualquer formato
   * @return destinatário validado
   * @throws IllegalArgumentException se o número não puder ser normalizado
   */
  public static Recipient ofPhoneNumberNormalized(String phoneNumber) {
    // Simple normalization: remove all non-digit characters except +
    String normalized = phoneNumber.replaceAll("[^+0-9]", "");
    if (normalized.isEmpty() || !normalized.startsWith("+")) {
      throw new IllegalArgumentException(
              "Cannot normalize phone number to E.164 format: " + phoneNumber
      );
    }
    return new Recipient(normalized, RecipientType.PHONE_NUMBER);
  }

  /**
   * Cria um destinatário usando ID de usuário.
   *
   * @param userId ID do usuário na plataforma
   * @return destinatário
   */
  public static Recipient ofUserId(@NotBlank String userId) {
    return new Recipient(userId, RecipientType.USER_ID);
  }

  /**
   * Cria um destinatário usando email.
   *
   * @param email endereço de email
   * @return destinatário
   */
  public static Recipient ofEmail(@NotBlank @Email String email) {
    return new Recipient(email, RecipientType.EMAIL);
  }

  /**
   * Verifica se este destinatário é um número de telefone.
   *
   * @return true se for número de telefone
   */
  public boolean isPhoneNumber() {
    return type == RecipientType.PHONE_NUMBER;
  }

  /**
   * Verifica se este destinatário é um ID de usuário.
   *
   * @return true se for user ID
   */
  public boolean isUserId() {
    return type == RecipientType.USER_ID;
  }

  /**
   * Verifica se este destinatário é um email.
   *
   * @return true se for email
   */
  public boolean isEmail() {
    return type == RecipientType.EMAIL;
  }

  /**
   * Tipos de destinatário suportados.
   */
  public enum RecipientType {
    /**
     * Número de telefone (formato E.164).
     */
    PHONE_NUMBER,

    /**
     * ID de usuário específico da plataforma.
     */
    USER_ID,

    /**
     * Endereço de email.
     */
    EMAIL
  }
}
