# Package `com.paragon.messaging.core`

---

## :material-code-braces: Classs

| Name | Description |
|------|-------------|
| [`MessagingException`](messagingexception.md) | Exceção base para erros de mensageria |

## :material-approximately-equal: Interfaces

| Name | Description |
|------|-------------|
| [`ContactMessageInterface`](contactmessageinterface.md) | Sealed sub-interface for contact messages |
| [`InteractiveMessageInterface`](interactivemessageinterface.md) | Sealed sub-interface for interactive messages (buttons, lists, CTA URLs) |
| [`LocationMessageInterface`](locationmessageinterface.md) | Sealed sub-interface for location messages |
| [`MediaMessageInterface`](mediamessageinterface.md) | Sealed sub-interface for media messages (images, videos, audio, documents, stickers) |
| [`MessageProcessor`](messageprocessor.md) | Processes batched messages from a user |
| [`MessagingProvider`](messagingprovider.md) | Interface for messaging providers (WhatsApp, Facebook Messenger, etc |
| [`ReactionMessageInterface`](reactionmessageinterface.md) | Sealed sub-interface for reaction messages (emoji reactions) |
| [`TemplateMessageInterface`](templatemessageinterface.md) | Sealed sub-interface for template messages |
| [`TextMessageInterface`](textmessageinterface.md) | Sealed sub-interface for text messages |

## :material-database: Records

| Name | Description |
|------|-------------|
| [`MessageResponse`](messageresponse.md) | Resposta do envio de uma mensagem |
| [`Recipient`](recipient.md) | Representa um destinatário de mensagem com validação automática |

## :material-format-list-bulleted-type: Enums

| Name | Description |
|------|-------------|
| [`OutboundMessageType`](outboundmessagetype.md) | Enum representing all outbound message types supported by WhatsApp |
