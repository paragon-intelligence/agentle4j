package com.paragon.messaging.whatsapp.payload;

import com.paragon.messaging.core.ContactMessageInterface;
import com.paragon.messaging.core.OutboundMessage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Contact message (vCard) for outbound delivery.
 */
public record ContactMessage(

        @NotNull(message = "Contacts cannot be null")
        @Size(min = 1, message = "Must have at least one contact")
        @Valid
        List<Contact> contacts

) implements ContactMessageInterface {

  public ContactMessage {
    contacts = List.copyOf(contacts);
  }

  public ContactMessage(Contact... contacts) {
    this(List.of(contacts));
  }

  @Override
  public OutboundMessageType type() {
    return OutboundMessageType.CONTACT;
  }

  /**
   * Represents an individual contact.
   */
  public record Contact(
          @NotBlank(message = "Name cannot be blank") String name,
          @NotBlank(message = "Phone cannot be blank") String phone
  ) {
  }
}