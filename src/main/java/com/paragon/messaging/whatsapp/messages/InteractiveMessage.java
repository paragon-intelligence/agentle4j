package com.paragon.messaging.whatsapp.messages;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;
import java.util.Optional;

/**
 * Sealed interface for interactive messages (buttons, lists, CTA URL).
 *
 * @author Agentle Team
 * @since 2.0
 */
public sealed interface InteractiveMessage extends OutboundMessage permits
        InteractiveMessage.ButtonMessage,
        InteractiveMessage.ListMessage,
        InteractiveMessage.CtaUrlMessage {

  /**
   * Retorna o corpo da mensagem interativa.
   */
  String body();

  /**
   * Retorna o cabeçalho opcional.
   */
  Optional<String> header();

  /**
   * Retorna o rodapé opcional.
   */
  Optional<String> footer();

  /**
   * Mensagem com botões de resposta rápida (máximo 3 botões).
   *
   * @param body    texto do corpo (obrigatório, 1-1024 caracteres)
   * @param buttons lista de botões (1-3 botões)
   * @param header  texto do cabeçalho (opcional, máx 60 caracteres)
   * @param footer  texto do rodapé (opcional, máx 60 caracteres)
   */
  record ButtonMessage(

          @NotBlank(message = "Button message body cannot be blank")
          @Size(min = 1, max = 1024, message = "Body must be between 1 and 1024 characters")
          String body,

          @NotNull(message = "Buttons list cannot be null")
          @Size(min = 1, max = 3, message = "Must have between 1 and 3 buttons")
          @Valid
          List<ReplyButton> buttons,

          Optional<@Size(max = 60, message = "Header cannot exceed 60 characters") String> header,
          Optional<@Size(max = 60, message = "Footer cannot exceed 60 characters") String> footer

  ) implements InteractiveMessage {

    public static final int MAX_BUTTONS = 3;
    public static final int MAX_HEADER_LENGTH = 60;
    public static final int MAX_BODY_LENGTH = 1024;
    public static final int MAX_FOOTER_LENGTH = 60;

    /**
     * Compact constructor para garantir imutabilidade da lista.
     */
    public ButtonMessage {
      buttons = List.copyOf(buttons);
    }

    public ButtonMessage(String body, List<ReplyButton> buttons) {
      this(body, buttons, Optional.empty(), Optional.empty());
    }

    public ButtonMessage(String body, List<ReplyButton> buttons, String header, String footer) {
      this(body, buttons, Optional.ofNullable(header), Optional.ofNullable(footer));
    }

    public static Builder builder() {
      return new Builder();
    }

    @Override
    public OutboundMessageType type() {
      return OutboundMessageType.INTERACTIVE_BUTTON;
    }

    public static class Builder {
      private final List<ReplyButton> buttons = new java.util.ArrayList<>();
      private String body;
      private String header;
      private String footer;

      public Builder body(String body) {
        this.body = body;
        return this;
      }

      public Builder addButton(String id, String title) {
        this.buttons.add(new ReplyButton(id, title));
        return this;
      }

      public Builder addButton(ReplyButton button) {
        this.buttons.add(button);
        return this;
      }

      public Builder header(String header) {
        this.header = header;
        return this;
      }

      public Builder footer(String footer) {
        this.footer = footer;
        return this;
      }

      public ButtonMessage build() {
        return new ButtonMessage(body, buttons, header, footer);
      }
    }
  }

  /**
   * Mensagem com lista de opções (máximo 10 itens total).
   *
   * @param body       texto do corpo
   * @param buttonText texto do botão que abre a lista (máx 20 caracteres)
   * @param sections   seções da lista (cada seção pode ter múltiplos itens)
   * @param header     texto do cabeçalho (opcional)
   * @param footer     texto do rodapé (opcional)
   */
  record ListMessage(

          @NotBlank(message = "List message body cannot be blank")
          @Size(min = 1, max = 1024, message = "Body must be between 1 and 1024 characters")
          String body,

          @NotBlank(message = "Button text cannot be blank")
          @Size(min = 1, max = 20, message = "Button text must be between 1 and 20 characters")
          String buttonText,

          @NotNull(message = "Sections list cannot be null")
          @Size(min = 1, message = "Must have at least one section")
          @Valid
          List<ListSection> sections,

          Optional<@Size(max = 60, message = "Header cannot exceed 60 characters") String> header,
          Optional<@Size(max = 60, message = "Footer cannot exceed 60 characters") String> footer

  ) implements InteractiveMessage {

    public static final int MAX_BUTTON_TEXT_LENGTH = 20;
    public static final int MAX_TOTAL_ROWS = 10;

    /**
     * Compact constructor com validação adicional do total de linhas.
     */
    public ListMessage {
      sections = List.copyOf(sections);

      int totalRows = sections.stream()
              .mapToInt(s -> s.rows().size())
              .sum();

      if (totalRows > MAX_TOTAL_ROWS) {
        throw new IllegalArgumentException(
                "Total rows across all sections cannot exceed " + MAX_TOTAL_ROWS +
                        " (found: " + totalRows + ")"
        );
      }
    }

    public ListMessage(String body, String buttonText, List<ListSection> sections) {
      this(body, buttonText, sections, Optional.empty(), Optional.empty());
    }

    public static Builder builder() {
      return new Builder();
    }

    @Override
    public OutboundMessageType type() {
      return OutboundMessageType.INTERACTIVE_LIST;
    }

    public static class Builder {
      private final List<ListSection> sections = new java.util.ArrayList<>();
      private String body;
      private String buttonText;
      private String header;
      private String footer;

      public Builder body(String body) {
        this.body = body;
        return this;
      }

      public Builder buttonText(String buttonText) {
        this.buttonText = buttonText;
        return this;
      }

      public Builder addSection(ListSection section) {
        this.sections.add(section);
        return this;
      }

      public Builder addSection(String title, List<ListRow> rows) {
        this.sections.add(new ListSection(title, rows));
        return this;
      }

      public Builder header(String header) {
        this.header = header;
        return this;
      }

      public Builder footer(String footer) {
        this.footer = footer;
        return this;
      }

      public ListMessage build() {
        return new ListMessage(body, buttonText, sections,
                Optional.ofNullable(header), Optional.ofNullable(footer));
      }
    }
  }

  /**
   * Mensagem com botão de Call-to-Action (URL).
   *
   * @param body        texto do corpo
   * @param displayText texto exibido no botão
   * @param url         URL para onde o botão direciona
   */
  record CtaUrlMessage(

          @NotBlank(message = "Body cannot be blank")
          @Size(min = 1, max = 1024, message = "Body must be between 1 and 1024 characters")
          String body,

          @NotBlank(message = "Display text cannot be blank")
          @Size(min = 1, max = 20, message = "Display text must be between 1 and 20 characters")
          String displayText,

          @NotBlank(message = "URL cannot be blank")
          @Pattern(regexp = "^https?://.*", message = "URL must start with http:// or https://")
          String url

  ) implements InteractiveMessage {

    @Override
    public Optional<String> header() {
      return Optional.empty();
    }

    @Override
    public Optional<String> footer() {
      return Optional.empty();
    }

    @Override
    public OutboundMessageType type() {
      return OutboundMessageType.INTERACTIVE_CTA_URL;
    }
  }

  /**
   * Representa um botão de resposta rápida.
   *
   * @param id    identificador único do botão
   * @param title texto exibido no botão (máx. 20 caracteres)
   */
  record ReplyButton(

          @NotBlank(message = "Button ID cannot be blank")
          @Size(min = 1, max = 256, message = "Button ID must be between 1 and 256 characters")
          String id,

          @NotBlank(message = "Button title cannot be blank")
          @Size(min = 1, max = 20, message = "Button title must be between 1 and 20 characters")
          String title

  ) {
    public static final int MAX_TITLE_LENGTH = 20;
  }

  /**
   * Representa uma seção de lista.
   *
   * @param title título da seção (opcional, máx 24 caracteres)
   * @param rows  itens da seção (mínimo 1)
   */
  record ListSection(

          Optional<@Size(max = 24, message = "Section title cannot exceed 24 characters") String> title,

          @NotNull(message = "Rows cannot be null")
          @Size(min = 1, message = "Section must have at least one row")
          @Valid
          List<ListRow> rows

  ) {

    public static final int MAX_TITLE_LENGTH = 24;

    public ListSection {
      rows = List.copyOf(rows);
    }

    public ListSection(String title, List<ListRow> rows) {
      this(Optional.ofNullable(title), rows);
    }

    public ListSection(List<ListRow> rows) {
      this(Optional.empty(), rows);
    }
  }

  /**
   * Representa um item de lista.
   *
   * @param id          identificador único
   * @param title       título do item (máx. 24 caracteres)
   * @param description descrição do item (opcional, máx. 72 caracteres)
   */
  record ListRow(

          @NotBlank(message = "Row ID cannot be blank")
          @Size(min = 1, max = 200, message = "Row ID must be between 1 and 200 characters")
          String id,

          @NotBlank(message = "Row title cannot be blank")
          @Size(min = 1, max = 24, message = "Row title must be between 1 and 24 characters")
          String title,

          Optional<@Size(max = 72, message = "Row description cannot exceed 72 characters") String> description

  ) {

    public static final int MAX_TITLE_LENGTH = 24;
    public static final int MAX_DESCRIPTION_LENGTH = 72;

    public ListRow(String id, String title) {
      this(id, title, Optional.empty());
    }

    public ListRow(String id, String title, String description) {
      this(id, title, Optional.ofNullable(description));
    }
  }
}
