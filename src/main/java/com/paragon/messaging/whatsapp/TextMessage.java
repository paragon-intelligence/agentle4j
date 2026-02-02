package com.paragon.messaging.whatsapp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Representa uma mensagem de texto simples.
 *
 * <p>Utiliza Bean Validation (Hibernate Validator) para validação declarativa
 * dos campos, garantindo que a mensagem atenda aos requisitos da API antes
 * do envio.</p>
 *
 * @param body       conteúdo da mensagem (1-4096 caracteres)
 * @param previewUrl se true, gera preview de URLs no texto
 * @author Your Name
 * @since 2.0
 */
public record TextMessage(

        @NotBlank(message = "Message body cannot be blank")
        @Size(min = 1, max = 4096, message = "Message body must be between 1 and 4096 characters")
        String body,

        boolean previewUrl

) implements Message {

  /**
   * Tamanho máximo permitido para o corpo da mensagem.
   */
  public static final int MAX_BODY_LENGTH = 4096;

  /**
   * Constructor conveniente sem preview de URL.
   *
   * @param body conteúdo da mensagem
   */
  public TextMessage(String body) {
    this(body, false);
  }

  /**
   * Cria um builder para construir TextMessage.
   *
   * @return novo builder
   */
  public static Builder builder() {
    return new Builder();
  }

  @Override
  public MessageType getType() {
    return MessageType.TEXT;
  }

  /**
   * Builder para TextMessage com validação fluente.
   */
  public static class Builder {
    private String body;
    private boolean previewUrl = false;

    private Builder() {
    }

    public Builder body(String body) {
      this.body = body;
      return this;
    }

    public Builder previewUrl(boolean previewUrl) {
      this.previewUrl = previewUrl;
      return this;
    }

    public Builder enablePreviewUrl() {
      this.previewUrl = true;
      return this;
    }

    public Builder disablePreviewUrl() {
      this.previewUrl = false;
      return this;
    }

    /**
     * Constrói a mensagem.
     *
     * <p>Nota: A validação será executada quando o objeto for passado
     * para o MessagingProvider através da anotação @Valid.</p>
     *
     * @return TextMessage construída
     */
    public TextMessage build() {
      return new TextMessage(body, previewUrl);
    }
  }
}
