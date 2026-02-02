package com.paragon.messaging.whatsapp.payload;

import com.paragon.messaging.whatsapp.Message;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Mensagem de contato (vCard).
 */
public record ContactMessage(

        @NotNull(message = "Contacts cannot be null")
        @Size(min = 1, message = "Must have at least one contact")
        @Valid
        List<Contact> contacts

) implements Message {

  public ContactMessage {
    contacts = List.copyOf(contacts);
  }

  public ContactMessage(Contact... contacts) {
    this(List.of(contacts));
  }

  @Override
  public MessageType getType() {
    return MessageType.CONTACT;
  }

  /**
   * Representa um contato individual.
   */
  public record Contact(
          @NotBlank(message = "Name cannot be blank") String name,
          @NotBlank(message = "Phone cannot be blank") String phone
  ) {
  }
}