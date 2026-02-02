package com.paragon.messaging.whatsapp.payload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record Status(String id, String status, String timestamp, String recipientId, Pricing pricing,
                     Conversation conversation, List<ErrorData> errors) {

  @JsonCreator
  public Status(
          @JsonProperty("id") String id,
          @JsonProperty("status") String status,
          @JsonProperty("timestamp") String timestamp,
          @JsonProperty("recipient_id") String recipientId,
          @JsonProperty("pricing") Pricing pricing,
          @JsonProperty("conversation") Conversation conversation,
          @JsonProperty("errors") List<ErrorData> errors
  ) {
    this.id = id;
    this.status = status;
    this.timestamp = timestamp;
    this.recipientId = recipientId;
    this.pricing = pricing;
    this.conversation = conversation;
    this.errors = errors;
  }
}
