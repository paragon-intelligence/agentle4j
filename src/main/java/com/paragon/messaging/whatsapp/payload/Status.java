package com.paragon.messaging.whatsapp.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record Status(
    @JsonProperty("id") String id,
    @JsonProperty("status") String status,
    @JsonProperty("timestamp") String timestamp,
    @JsonProperty("recipient_id") String recipientId,
    @JsonProperty("pricing") Pricing pricing,
    @JsonProperty("conversation") Conversation conversation,
    @JsonProperty("errors") List<ErrorData> errors) {}
