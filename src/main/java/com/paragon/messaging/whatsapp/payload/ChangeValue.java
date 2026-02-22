package com.paragon.messaging.whatsapp.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChangeValue(
    @JsonProperty("messaging_product") @NotNull String messagingProduct,
    @JsonProperty("metadata") Metadata metadata,
    @JsonProperty("contacts") @Valid List<Contact> contacts,
    @JsonProperty("messages") @Valid List<InboundMessage> messages,
    @JsonProperty("statuses") @Valid List<Status> statuses) {}
