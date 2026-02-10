package com.paragon.messaging.whatsapp;

import com.paragon.messaging.core.*;
import com.paragon.messaging.whatsapp.messages.*;

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
  public String serialize(Recipient recipient, OutboundMessage message) {
    String json = switch (message) {
      case TextMessageInterface text -> serializeTextMessage(recipient, (TextMessage) text);
      case MediaMessageInterface media -> serializeMediaMessage(recipient, (MediaMessage) media);
      case TemplateMessageInterface template -> serializeTemplateMessage(recipient, (TemplateMessage) template);
      case InteractiveMessageInterface interactive -> serializeInteractiveMessage(recipient, (InteractiveMessage) interactive);
      case LocationMessageInterface location -> serializeLocationMessage(recipient, (LocationMessage) location);
      case ContactMessageInterface contact -> serializeContactMessage(recipient, (ContactMessage) contact);
      case ReactionMessageInterface reaction -> serializeReactionMessage(recipient, (ReactionMessage) reaction);
    };
    return json.strip();
  }

  /**
   * Serializa mensagem de texto.
   */
  private String serializeTextMessage(Recipient recipient, TextMessage message) {
    String contextField = "";
    if (message.replyToMessageId() != null && !message.replyToMessageId().isBlank()) {
      contextField = String.format("""
              "context":{
                "message_id":"%s"
              },
              """, escapeJson(message.replyToMessageId()));
    }

    return String.format("""
                    {
                      "messaging_product":"whatsapp",
                      "recipient_type":"individual",
                      "to":"%s",
                      %s"type":"text",
                      "text":{
                        "body":"%s",
                        "preview_url":%b
                      }
                    }
                    """,
            escapeJson(recipient.value()),
            contextField,
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
            .map(c -> ",\n        \"caption\":\"" + escapeJson(c) + "\"")
            .orElse("");

    return String.format("""
                    {
                      "messaging_product":"whatsapp",
                      "recipient_type":"individual",
                      "to":"%s",
                      "type":"image",
                      "image":{
                        %s%s
                      }
                    }
                    """,
            escapeJson(recipient.value()),
            sourceField,
            captionField
    );
  }

  private String serializeVideo(Recipient recipient, MediaMessage.Video video) {
    String sourceField = serializeMediaSource(video.source());
    String captionField = video.caption()
            .map(c -> ",\n        \"caption\":\"" + escapeJson(c) + "\"")
            .orElse("");

    return String.format("""
                    {
                      "messaging_product":"whatsapp",
                      "to":"%s",
                      "type":"video",
                      "video":{
                        %s%s
                      }
                    }
                    """,
            escapeJson(recipient.value()),
            sourceField,
            captionField
    );
  }

  private String serializeAudio(Recipient recipient, MediaMessage.Audio audio) {
    String sourceField = serializeMediaSource(audio.source());

    return String.format("""
                    {
                      "messaging_product":"whatsapp",
                      "to":"%s",
                      "type":"audio",
                      "audio":{
                        %s
                      }
                    }
                    """,
            escapeJson(recipient.value()),
            sourceField
    );
  }

  private String serializeDocument(Recipient recipient, MediaMessage.Document document) {
    String sourceField = serializeMediaSource(document.source());
    String filenameField = document.filename()
            .map(f -> ",\n        \"filename\":\"" + escapeJson(f) + "\"")
            .orElse("");
    String captionField = document.caption()
            .map(c -> ",\n        \"caption\":\"" + escapeJson(c) + "\"")
            .orElse("");

    return String.format("""
                    {
                      "messaging_product":"whatsapp",
                      "to":"%s",
                      "type":"document",
                      "document":{
                        %s%s%s
                      }
                    }
                    """,
            escapeJson(recipient.value()),
            sourceField,
            filenameField,
            captionField
    );
  }

  private String serializeSticker(Recipient recipient, MediaMessage.Sticker sticker) {
    String sourceField = serializeMediaSource(sticker.source());

    return String.format("""
                    {
                      "messaging_product":"whatsapp",
                      "to":"%s",
                      "type":"sticker",
                      "sticker":{
                        %s
                      }
                    }
                    """,
            escapeJson(recipient.value()),
            sourceField
    );
  }

  /**
   * Serializa a fonte de mídia (URL ou ID).
   */
  private String serializeMediaSource(MediaMessage.MediaSource source) {
    return switch (source) {
      case MediaMessage.MediaSource.Url url -> "\"link\":\"" + escapeJson(url.url()) + "\"";
      case MediaMessage.MediaSource.MediaId id -> "\"id\":\"" + escapeJson(id.id()) + "\"";
    };
  }

  /**
   * Serializa mensagem de template.
   */
  private String serializeTemplateMessage(Recipient recipient, TemplateMessage template) {
    StringBuilder componentsJson = new StringBuilder();

    if (!template.components().isEmpty()) {
      componentsJson.append(",\n      \"components\":[\n");

      for (int i = 0; i < template.components().size(); i++) {
        if (i > 0) componentsJson.append(",\n");
        componentsJson.append("        ");
        componentsJson.append(serializeTemplateComponent(template.components().get(i)));
      }

      componentsJson.append("\n      ]");
    }

    return String.format("""
                    {
                      "messaging_product":"whatsapp",
                      "to":"%s",
                      "type":"template",
                      "template":{
                        "name":"%s",
                        "language":{
                          "code":"%s"
                        }%s
                      }
                    }
                    """,
            escapeJson(recipient.value()),
            escapeJson(template.name()),
            escapeJson(template.languageCode()),
            componentsJson.toString()
    );
  }

  private String serializeTemplateComponent(TemplateMessage.TemplateComponent component) {
    return String.format("""
            {
              "type":"%s",
              "parameters":%s
            }""",
            escapeJson(component.type()),
            serializeStringList(component.parameters()));
  }

  private String serializeStringList(java.util.List<String> params) {
    if (params.isEmpty()) return "[]";

    StringBuilder json = new StringBuilder("[");
    for (int i = 0; i < params.size(); i++) {
      if (i > 0) json.append(", ");
      json.append("{\"type\":\"text\", \"text\":\"")
              .append(escapeJson(params.get(i)))
              .append("\"}");
    }
    json.append("]");
    return json.toString();
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
    String headerField = msg.header()
            .map(h -> String.format("""
                    "header":{
                      "type":"text",
                      "text":"%s"
                    },
                    """, escapeJson(h)))
            .orElse("");

    String footerField = msg.footer()
            .map(f -> String.format("""
                    ,
                    "footer":{
                      "text":"%s"
                    }""", escapeJson(f)))
            .orElse("");

    StringBuilder buttonsJson = new StringBuilder("[");
    for (int i = 0; i < msg.buttons().size(); i++) {
      if (i > 0) buttonsJson.append(",\n          ");
      var btn = msg.buttons().get(i);
      buttonsJson.append(String.format("""
              {
                "type":"reply",
                "reply":{
                  "id":"%s",
                  "title":"%s"
                }
              }""",
              escapeJson(btn.id()),
              escapeJson(btn.title())));
    }
    buttonsJson.append("\n        ]");

    return String.format("""
                    {
                      "messaging_product":"whatsapp",
                      "to":"%s",
                      "type":"interactive",
                      "interactive":{
                        "type":"button",
                        %s"body":{
                          "text":"%s"
                        }%s,
                        "action":{
                          "buttons":%s
                        }
                      }
                    }
                    """,
            escapeJson(recipient.value()),
            headerField,
            escapeJson(msg.body()),
            footerField,
            buttonsJson.toString()
    );
  }

  private String serializeListMessage(Recipient recipient, InteractiveMessage.ListMessage msg) {
    String headerField = msg.header()
            .map(h -> String.format("""
                    "header":{
                      "type":"text",
                      "text":"%s"
                    },
                    """, escapeJson(h)))
            .orElse("");

    String footerField = msg.footer()
            .map(f -> String.format("""
                    ,
                    "footer":{
                      "text":"%s"
                    }""", escapeJson(f)))
            .orElse("");

    StringBuilder sectionsJson = new StringBuilder("[\n");
    for (int i = 0; i < msg.sections().size(); i++) {
      if (i > 0) sectionsJson.append(",\n");
      var section = msg.sections().get(i);
      
      sectionsJson.append("          {\n");
      
      section.title().ifPresent(title -> 
              sectionsJson.append(String.format("            \"title\":\"%s\",\n", escapeJson(title)))
      );
      
      sectionsJson.append("            \"rows\":[\n");
      for (int j = 0; j < section.rows().size(); j++) {
        if (j > 0) sectionsJson.append(",\n");
        var row = section.rows().get(j);
        
        sectionsJson.append(String.format("""
                              {
                                "id":"%s",
                                "title":"%s\"""",
                escapeJson(row.id()),
                escapeJson(row.title())));
        
        row.description().ifPresent(desc ->
                sectionsJson.append(String.format(",\n                \"description\":\"%s\"", escapeJson(desc)))
        );
        
        sectionsJson.append("\n              }");
      }
      sectionsJson.append("\n            ]\n");
      sectionsJson.append("          }");
    }
    sectionsJson.append("\n        ]");

    return String.format("""
                    {
                      "messaging_product":"whatsapp",
                      "to":"%s",
                      "type":"interactive",
                      "interactive":{
                        "type":"list",
                        %s"body":{
                          "text":"%s"
                        }%s,
                        "action":{
                          "button":"%s",
                          "sections":%s
                        }
                      }
                    }
                    """,
            escapeJson(recipient.value()),
            headerField,
            escapeJson(msg.body()),
            footerField,
            escapeJson(msg.buttonText()),
            sectionsJson.toString()
    );
  }

  private String serializeCtaUrlMessage(Recipient recipient, InteractiveMessage.CtaUrlMessage msg) {
    String footerField = msg.footer()
            .map(f -> ",\n                \"footer\":{\"text\":\"" + escapeJson(f) + "\"}")
            .orElse("");

    return String.format("""
                    {
                      "messaging_product":"whatsapp",
                      "to":"%s",
                      "type":"interactive",
                      "interactive":{
                        "type":"cta_url",
                        "body":{"text":"%s"}%s,
                        "action":{
                          "name":"cta_url",
                          "parameters":{
                            "display_text":"%s",
                            "url":"%s"
                          }
                        }
                      }
                    }
                    """,
            escapeJson(recipient.value()),
            escapeJson(msg.body()),
            footerField,
            escapeJson(msg.displayText()),
            escapeJson(msg.url())
    );
  }

  /**
   * Serializa mensagem de localização.
   */
  private String serializeLocationMessage(Recipient recipient, LocationMessage location) {
    String nameField = location.name()
            .map(n -> ",\n        \"name\":\"" + escapeJson(n) + "\"")
            .orElse("");
    String addressField = location.address()
            .map(a -> ",\n        \"address\":\"" + escapeJson(a) + "\"")
            .orElse("");

    return String.format("""
                    {
                      "messaging_product":"whatsapp",
                      "to":"%s",
                      "type":"location",
                      "location":{
                        "latitude":%s,
                        "longitude":%s%s%s
                      }
                    }
                    """,
            escapeJson(recipient.value()),
            Double.toString(location.latitude()),
            Double.toString(location.longitude()),
            nameField,
            addressField
    );
  }

  /**
   * Serializa mensagem de contato.
   */
  private String serializeContactMessage(Recipient recipient, ContactMessage contact) {
    StringBuilder phonesJson = new StringBuilder("[");
    for (int i = 0; i < contact.phones().size(); i++) {
      if (i > 0) phonesJson.append(",");
      var p = contact.phones().get(i);
      phonesJson.append("{\"phone\":\"").append(escapeJson(p.phone()))
              .append("\",\"type\":\"").append(escapeJson(p.type())).append("\"}");
    }
    phonesJson.append("]");

    StringBuilder emailsJson = new StringBuilder("[");
    for (int i = 0; i < contact.emails().size(); i++) {
      if (i > 0) emailsJson.append(",");
      var e = contact.emails().get(i);
      emailsJson.append("{\"email\":\"").append(escapeJson(e.email()))
              .append("\",\"type\":\"").append(escapeJson(e.type())).append("\"}");
    }
    emailsJson.append("]");

    return String.format("""
                    {
                      "messaging_product":"whatsapp",
                      "to":"%s",
                      "type":"contacts",
                      "contacts":[{
                        "name":{"formatted_name":"%s"},
                        "phone":%s,
                        "email":%s
                      }]
                    }
                    """,
            escapeJson(recipient.value()),
            escapeJson(contact.formattedName()),
            phonesJson.toString(),
            emailsJson.toString()
    );
  }

  /**
   * Serializa mensagem de reação.
   */
  private String serializeReactionMessage(Recipient recipient, ReactionMessage reaction) {
    String emojiField = reaction.emoji()
            .map(e -> "\"emoji\":\"" + escapeJson(e) + "\"")
            .orElse("\"emoji\":\"\"");

    return String.format("""
                    {
                      "messaging_product":"whatsapp",
                      "to":"%s",
                      "type":"reaction",
                      "reaction":{
                        "message_id":"%s",
                        %s
                      }
                    }
                    """,
            escapeJson(recipient.value()),
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
