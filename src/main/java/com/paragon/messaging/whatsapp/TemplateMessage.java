package com.paragon.messaging.whatsapp;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Mensagem baseada em template pré-aprovado.
 */
public record TemplateMessage(

        @NotBlank(message = "Template name cannot be blank")
        String name,

        @NotBlank(message = "Language code cannot be blank")
        @Size(min = 2, max = 10, message = "Language code must be between 2 and 10 characters")
        String languageCode,

        @NotNull(message = "Components cannot be null")
        @Valid
        List<TemplateComponent> components

) implements OutboundMessage {

  public TemplateMessage {
    components = List.copyOf(components);
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public OutboundMessageType type() {
    return OutboundMessageType.TEMPLATE;
  }

  /**
   * Componente de template (simplificado para exemplo).
   */
  public record TemplateComponent(
          @NotBlank String type,
          @NotNull List<String> parameters
  ) {
    public TemplateComponent {
      parameters = List.copyOf(parameters);
    }
  }

  public static class Builder {
    private final List<TemplateComponent> components = new java.util.ArrayList<>();
    private String name;
    private String languageCode;

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder language(String code) {
      this.languageCode = code;
      return this;
    }

    public Builder addComponent(String type, String... params) {
      components.add(new TemplateComponent(type, List.of(params)));
      return this;
    }

    public TemplateMessage build() {
      return new TemplateMessage(name, languageCode, components);
    }
  }
}
