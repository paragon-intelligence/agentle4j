package com.paragon.messaging.whatsapp.payload;


import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TextMessage.class, name = "text"),
        @JsonSubTypes.Type(value = ImageMessage.class, name = "image"),
        @JsonSubTypes.Type(value = VideoMessage.class, name = "video"),
        @JsonSubTypes.Type(value = AudioMessage.class, name = "audio"),
        @JsonSubTypes.Type(value = DocumentMessage.class, name = "document"),
        @JsonSubTypes.Type(value = StickerMessage.class, name = "sticker"),
        @JsonSubTypes.Type(value = InteractiveMessage.class, name = "interactive"),
        @JsonSubTypes.Type(value = LocationMessage.class, name = "location"),
        @JsonSubTypes.Type(value = ReactionMessage.class, name = "reaction"),
        @JsonSubTypes.Type(value = SystemMessage.class, name = "system"),
        @JsonSubTypes.Type(value = OrderMessage.class, name = "order")
})
public sealed interface Message permits
        AbstractMessage, TextMessage, ImageMessage, VideoMessage, AudioMessage,
        DocumentMessage, StickerMessage, InteractiveMessage, LocationMessage,
        ReactionMessage, SystemMessage, OrderMessage {
}
