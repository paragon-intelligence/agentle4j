package com.paragon.messaging.whatsapp.payload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class WhatsappMetaWebhookPayload {

  @NotNull public final String object;

  @NotEmpty @Valid public final List<Entry> entry;

  @JsonCreator
  public WhatsappMetaWebhookPayload(
      @JsonProperty("object") String object, @JsonProperty("entry") List<Entry> entry) {
    this.object = object;
    this.entry = entry;
  }
}
