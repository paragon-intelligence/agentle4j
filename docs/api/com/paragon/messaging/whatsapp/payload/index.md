# Package `com.paragon.messaging.whatsapp.payload`

---

## :material-code-braces: Classs

| Name | Description |
|------|-------------|
| [`AudioMessage`](audiomessage.md) | Inbound audio/voice message from WhatsApp webhook |
| [`DocumentMessage`](documentmessage.md) | Inbound document/file message from WhatsApp webhook |
| [`ImageMessage`](imagemessage.md) | Inbound image message from WhatsApp webhook |
| [`InteractiveMessage`](interactivemessage.md) |  |
| [`ListReply`](listreply.md) | Interactive list selection reply from WhatsApp webhook |
| [`LocationMessage`](locationmessage.md) | Inbound location message from WhatsApp webhook |
| [`NfmReply`](nfmreply.md) | Interactive NFM (Natural Flow Message) reply from WhatsApp webhook |
| [`OrderMessage`](ordermessage.md) | Inbound order message from WhatsApp webhook |
| [`ReactionMessage`](reactionmessage.md) | Inbound reaction message from WhatsApp webhook |
| [`StickerMessage`](stickermessage.md) | Inbound sticker message from WhatsApp webhook |
| [`SystemMessage`](systemmessage.md) | Inbound system message from WhatsApp webhook |
| [`VideoMessage`](videomessage.md) | Inbound video message from WhatsApp webhook |

## :material-approximately-equal: Interfaces

| Name | Description |
|------|-------------|
| [`InboundMessage`](inboundmessage.md) | Sealed interface for inbound WhatsApp webhook messages |

## :material-database: Records

| Name | Description |
|------|-------------|
| [`ContactMessage`](contactmessage.md) | Contact message (vCard) for outbound delivery |
| [`Conversation`](conversation.md) | Represents a WhatsApp conversation object in status updates |
| [`ErrorData`](errordata.md) | Represents error data in WhatsApp API responses |
| [`MediaContent`](mediacontent.md) | Media content payload for images, videos, audio, documents, and stickers |
| [`MessageContext`](messagecontext.md) | Context information about a message that is being replied to or quoted |
| [`Pricing`](pricing.md) | @param category marketing, utility, authentication, service |
