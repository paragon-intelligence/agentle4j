package com.paragon.messaging.whatsapp.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.paragon.messaging.batching.Message;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChangeValue(@NotNull String messagingProduct, Metadata metadata, @Valid List<Contact> contacts,
                          @Valid List<Message> messages, @Valid List<Status> statuses) {

}
