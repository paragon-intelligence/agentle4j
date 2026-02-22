package com.paragon.messaging.whatsapp.messages;

import com.paragon.messaging.core.TemplateMessageInterface;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;

/** Mensagem baseada em template pré-aprovado. */
public record TemplateMessage(
    @NotBlank(message = "Template name cannot be blank")
        @Pattern(
            regexp = "[a-z0-9_]+",
            message = "Template name must contain only lowercase letters, numbers, and underscores")
        String name,
    @NotBlank(message = "Language code cannot be blank")
        @Pattern(
            regexp = "[a-z]{2}(_[A-Z]{2})?",
            message =
                "Language code must be in format 'xx' or 'xx_XX' (e.g., 'en', 'pt', 'pt_BR',"
                    + " 'en_US')")
        String languageCode,
    @NotNull(message = "Components cannot be null") @Valid List<TemplateComponent> components)
    implements TemplateMessageInterface {

  public TemplateMessage {
    components = List.copyOf(components);
  }

  /** Convenience constructor for template without components. */
  public TemplateMessage(String name, String languageCode) {
    this(name, languageCode, List.of());
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public OutboundMessageType type() {
    return OutboundMessageType.TEMPLATE;
  }

  /** Componente de template (simplificado para exemplo). */
  public record TemplateComponent(@NotBlank String type, @NotNull List<String> parameters) {
    public TemplateComponent {
      parameters = List.copyOf(parameters);
    }
  }

  public static class Builder {
    private final List<TemplateComponent> components = new java.util.ArrayList<>();
    private final List<String> bodyParams = new java.util.ArrayList<>();
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

    public Builder addBodyParameter(String param) {
      bodyParams.add(param);
      return this;
    }

    public TemplateMessage build() {
      if (!bodyParams.isEmpty()) {
        components.add(new TemplateComponent("body", bodyParams));
      }
      return new TemplateMessage(name, languageCode, components);
    }
  }
}
