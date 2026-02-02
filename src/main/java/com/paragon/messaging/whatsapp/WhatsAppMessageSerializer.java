package com.paragon.messaging.whatsapp;

import com.example.messaging.model.common.Recipient;
import com.example.messaging.model.message.*;
import com.paragon.messaging.whatsapp.payload.ContactMessage;

/**
 * Serializador que converte mensagens genéricas para o formato JSON da API WhatsApp.
 *
 * <p>Esta implementação demonstra o padrão de como transformar os modelos genéricos
 * em payloads específicos do provedor.</p>
 *
 * @author Your Name
 * @since 1.0
 */
public class WhatsAppMessageSerializer {

  /**
   * Serializa uma mensagem para JSON no formato esperado pela API WhatsApp.
   *
   * @param recipient destinatário
   * @param message   mensagem a ser serializada
   * @return JSON string
   */
  public String serialize(Recipient recipient, Message message) {
    return switch (message) {
      case TextMessage text -> serializeTextMessage(recipient, text);
      case MediaMessage media -> serializeMediaMessage(recipient, media);
      case TemplateMessage template -> serializeTemplateMessage(recipient, template);
      case InteractiveMessage interactive -> serializeInteractiveMessage(recipient, interactive);
      case LocationMessage location -> serializeLocationMessage(recipient, location);
      case ContactMessage contact -> serializeContactMessage(recipient, contact);
      case ReactionMessage reaction -> serializeReactionMessage(recipient, reaction);
    };
  }

  /**
   * Serializa mensagem de texto.
   */
  private String serializeTextMessage(Recipient recipient, TextMessage message) {
    return String.format("""
                    {
                      "messaging_product": "whatsapp",
                      "recipient_type": "individual",
                      "to": "%s",
                      "type": "text",
                      "text": {
                        "body": "%s",
                        "preview_url": %b
                      }
                    }
                    """,
            escapeJson(recipient.identifier()),
            escapeJson(message.body()),
            message.previewUrl()
    );
  }

  /**
   * Serializa mensagem de mídia.
   */
  private String serializeMediaMessage(Recipient recipient, MediaMessage media) {
    return switch (media) {
      case MediaMessage.Image image -> serializeImage(recipient, image);
      case MediaMessage.Video video -> serializeVideo(recipient, video);
      case MediaMessage.Audio audio -> serializeAudio(recipient, audio);
      case MediaMessage.Document document -> serializeDocument(recipient, document);
      case MediaMessage.Sticker sticker -> serializeSticker(recipient, sticker);
    };
  }

  private String serializeImage(Recipient recipient, MediaMessage.Image image) {
    String sourceField = serializeMediaSource(image.source());
    String captionField = image.caption()
            .map(c -> ",\n        \"caption\": \"" + escapeJson(c) + "\"")
            .orElse("");

    return String.format("""
                    {
                      "messaging_product": "whatsapp",
                      "recipient_type": "individual",
                      "to": "%s",
                      "type": "image",
                      "image": {
                        %s%s
                      }
                    }
                    """,
            escapeJson(recipient.identifier()),
            sourceField,
            captionField
    );
  }

  private String serializeVideo(Recipient recipient, MediaMessage.Video video) {
    String sourceField = serializeMediaSource(video.source());
    String captionField = video.caption()
            .map(c -> ",\n        \"caption\": \"" + escapeJson(c) + "\"")
            .orElse("");

    return String.format("""
                    {
                      "messaging_product": "whatsapp",
                      "to": "%s",
                      "type": "video",
                      "video": {
                        %s%s
                      }
                    }
                    """,
            escapeJson(recipient.identifier()),
            sourceField,
            captionField
    );
  }

  private String serializeAudio(Recipient recipient, MediaMessage.Audio audio) {
    String sourceField = serializeMediaSource(audio.source());

    return String.format("""
                    {
                      "messaging_product": "whatsapp",
                      "to": "%s",
                      "type": "audio",
                      "audio": {
                        %s
                      }
                    }
                    """,
            escapeJson(recipient.identifier()),
            sourceField
    );
  }

  private String serializeDocument(Recipient recipient, MediaMessage.Document document) {
    String sourceField = serializeMediaSource(document.source());
    String filenameField = document.filename()
            .map(f -> ",\n        \"filename\": \"" + escapeJson(f) + "\"")
            .orElse("");
    String captionField = document.caption()
            .map(c -> ",\n        \"caption\": \"" + escapeJson(c) + "\"")
            .orElse("");

    return String.format("""
                    {
                      "messaging_product": "whatsapp",
                      "to": "%s",
                      "type": "document",
                      "document": {
                        %s%s%s
                      }
                    }
                    """,
            escapeJson(recipient.identifier()),
            sourceField,
            filenameField,
            captionField
    );
  }

  private String serializeSticker(Recipient recipient, MediaMessage.Sticker sticker) {
    String sourceField = serializeMediaSource(sticker.source());

    return String.format("""
                    {
                      "messaging_product": "whatsapp",
                      "to": "%s",
                      "type": "sticker",
                      "sticker": {
                        %s
                      }
                    }
                    """,
            escapeJson(recipient.identifier()),
            sourceField
    );
  }

  /**
   * Serializa a fonte de mídia (URL ou ID).
   */
  private String serializeMediaSource(MediaMessage.MediaSource source) {
    return switch (source) {
      case MediaMessage.MediaSource.Url url -> "\"link\": \"" + escapeJson(url.url()) + "\"";
      case MediaMessage.MediaSource.MediaId id -> "\"id\": \"" + escapeJson(id.id()) + "\"";
    };
  }

  /**
   * Serializa mensagem de template.
   */
  private String serializeTemplateMessage(Recipient recipient, TemplateMessage template) {
    StringBuilder componentsJson = new StringBuilder();

    if (!template.components().isEmpty()) {
      componentsJson.append(",\n      \"components\": [\n");

      for (int i = 0; i < template.components().size(); i++) {
        if (i > 0) componentsJson.append(",\n");
        componentsJson.append("        ");
        componentsJson.append(serializeTemplateComponent(template.components().get(i)));
      }

      componentsJson.append("\n      ]");
    }

    return String.format("""
                    {
                      "messaging_product": "whatsapp",
                      "to": "%s",
                      "type": "template",
                      "template": {
                        "name": "%s",
                        "language": {
                          "code": "%s",
                          "policy": "%s"
                        }%s
                      }
                    }
                    """,
            escapeJson(recipient.identifier()),
            escapeJson(template.name()),
            escapeJson(template.language().code()),
            escapeJson(template.language().policy()),
            componentsJson.toString()
    );
  }

  private String serializeTemplateComponent(TemplateMessage.TemplateComponent component) {
    return switch (component) {
      case TemplateMessage.TemplateComponent.Header h -> String.format("""
              {
                "type": "header",
                "parameters": %s
              }""", serializeTemplateParameters(h.parameters()));

      case TemplateMessage.TemplateComponent.Body b -> String.format("""
              {
                "type": "body",
                "parameters": %s
              }""", serializeTemplateParameters(b.parameters()));

      case TemplateMessage.TemplateComponent.Button btn -> String.format("""
                      {
                        "type": "button",
                        "sub_type": "%s",
                        "index": "%d",
                        "parameters": %s
                      }""",
              escapeJson(btn.subType()),
              btn.index(),
              serializeTemplateParameters(btn.parameters()));

      case TemplateMessage.TemplateComponent.Footer f -> String.format("""
              {
                "type": "footer",
                "parameters": %s
              }""", serializeTemplateParameters(f.parameters()));
    };
  }

  private String serializeTemplateParameters(java.util.List<TemplateMessage.TemplateParameter> params) {
    if (params.isEmpty()) return "[]";

    StringBuilder json = new StringBuilder("[");
    for (int i = 0; i < params.size(); i++) {
      if (i > 0) json.append(", ");
      json.append(serializeTemplateParameter(params.get(i)));
    }
    json.append("]");
    return json.toString();
  }

  private String serializeTemplateParameter(TemplateMessage.TemplateParameter param) {
    return switch (param) {
      case TemplateMessage.TemplateParameter.Text t ->
              String.format("{\"type\": \"text\", \"text\": \"%s\"}", escapeJson(t.text()));
      case TemplateMessage.TemplateParameter.Image i ->
              String.format("{\"type\": \"image\", \"image\": {\"link\": \"%s\"}}", escapeJson(i.link()));
      case TemplateMessage.TemplateParameter.Video v ->
              String.format("{\"type\": \"video\", \"video\": {\"link\": \"%s\"}}", escapeJson(v.link()));
      case TemplateMessage.TemplateParameter.Document d ->
              String.format("{\"type\": \"document\", \"document\": {\"link\": \"%s\"}}", escapeJson(d.link()));
      case TemplateMessage.TemplateParameter.Payload p ->
              String.format("{\"type\": \"payload\", \"payload\": \"%s\"}", escapeJson(p.payload()));
      case TemplateMessage.TemplateParameter.Currency c ->
              String.format("{\"type\": \"currency\", \"currency\": {\"fallback_value\": \"%s\", \"code\": \"%s\", \"amount_1000\": %d}}",
                      escapeJson(c.fallbackValue()), escapeJson(c.currencyCode()), c.amount());
      case TemplateMessage.TemplateParameter.DateTime dt ->
              String.format("{\"type\": \"date_time\", \"date_time\": {\"fallback_value\": \"%s\", \"timestamp\": %d}}",
                      escapeJson(dt.fallbackValue()), dt.timestamp());
    };
  }

  /**
   * Serializa mensagem interativa.
   */
  private String serializeInteractiveMessage(Recipient recipient, InteractiveMessage interactive) {
    return switch (interactive) {
      case InteractiveMessage.ButtonMessage btn -> serializeButtonMessage(recipient, btn);
      case InteractiveMessage.ListMessage list -> serializeListMessage(recipient, list);
      case InteractiveMessage.CtaUrlMessage cta -> serializeCtaUrlMessage(recipient, cta);
    };
  }

  private String serializeButtonMessage(Recipient recipient, InteractiveMessage.ButtonMessage msg) {
    // Implementação simplificada
    return String.format("""
                    {
                      "messaging_product": "whatsapp",
                      "to": "%s",
                      "type": "interactive",
                      "interactive": {
                        "type": "button",
                        "body": {"text": "%s"}
                      }
                    }
                    """,
            escapeJson(recipient.identifier()),
            escapeJson(msg.body())
    );
  }

  private String serializeListMessage(Recipient recipient, InteractiveMessage.ListMessage msg) {
    // Implementação simplificada
    return String.format("""
                    {
                      "messaging_product": "whatsapp",
                      "to": "%s",
                      "type": "interactive",
                      "interactive": {
                        "type": "list",
                        "body": {"text": "%s"},
                        "action": {"button": "%s"}
                      }
                    }
                    """,
            escapeJson(recipient.identifier()),
            escapeJson(msg.body()),
            escapeJson(msg.buttonText())
    );
  }

  private String serializeCtaUrlMessage(Recipient recipient, InteractiveMessage.CtaUrlMessage msg) {
    return String.format("""
                    {
                      "messaging_product": "whatsapp",
                      "to": "%s",
                      "type": "interactive",
                      "interactive": {
                        "type": "cta_url",
                        "body": {"text": "%s"},
                        "action": {
                          "name": "cta_url",
                          "parameters": {
                            "display_text": "%s",
                            "url": "%s"
                          }
                        }
                      }
                    }
                    """,
            escapeJson(recipient.identifier()),
            escapeJson(msg.body()),
            escapeJson(msg.displayText()),
            escapeJson(msg.url())
    );
  }

  /**
   * Serializa mensagem de localização.
   */
  private String serializeLocationMessage(Recipient recipient, LocationMessage location) {
    String nameField = location.name()
            .map(n -> ",\n        \"name\": \"" + escapeJson(n) + "\"")
            .orElse("");
    String addressField = location.address()
            .map(a -> ",\n        \"address\": \"" + escapeJson(a) + "\"")
            .orElse("");

    return String.format("""
                    {
                      "messaging_product": "whatsapp",
                      "to": "%s",
                      "type": "location",
                      "location": {
                        "latitude": %f,
                        "longitude": %f%s%s
                      }
                    }
                    """,
            escapeJson(recipient.identifier()),
            location.latitude(),
            location.longitude(),
            nameField,
            addressField
    );
  }

  /**
   * Serializa mensagem de contato.
   */
  private String serializeContactMessage(Recipient recipient, ContactMessage contact) {
    // Implementação simplificada - em produção seria mais complexa
    return String.format("""
                    {
                      "messaging_product": "whatsapp",
                      "to": "%s",
                      "type": "contacts",
                      "contacts": []
                    }
                    """,
            escapeJson(recipient.identifier())
    );
  }

  /**
   * Serializa mensagem de reação.
   */
  private String serializeReactionMessage(Recipient recipient, ReactionMessage reaction) {
    String emojiField = reaction.emoji()
            .map(e -> "\"emoji\": \"" + escapeJson(e) + "\"")
            .orElse("\"emoji\": \"\"");

    return String.format("""
                    {
                      "messaging_product": "whatsapp",
                      "to": "%s",
                      "type": "reaction",
                      "reaction": {
                        "message_id": "%s",
                        %s
                      }
                    }
                    """,
            escapeJson(recipient.identifier()),
            escapeJson(reaction.messageId()),
            emojiField
    );
  }

  /**
   * Escapa caracteres especiais para JSON.
   */
  private String escapeJson(String value) {
    if (value == null) return "";
    return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
  }
}
