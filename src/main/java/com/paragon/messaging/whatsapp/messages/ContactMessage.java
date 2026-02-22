package com.paragon.messaging.whatsapp.messages;

import com.paragon.messaging.core.ContactMessageInterface;
import java.util.ArrayList;
import java.util.List;

/** Outbound contact message with builder API. */
public record ContactMessage(String formattedName, List<PhoneEntry> phones, List<EmailEntry> emails)
    implements ContactMessageInterface {

  public ContactMessage {
    phones = List.copyOf(phones);
    emails = List.copyOf(emails);
  }

  @Override
  public OutboundMessageType type() {
    return OutboundMessageType.CONTACT;
  }

  public record PhoneEntry(String phone, String type) {}

  public record EmailEntry(String email, String type) {}

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String formattedName;
    private final List<PhoneEntry> phones = new ArrayList<>();
    private final List<EmailEntry> emails = new ArrayList<>();

    public Builder formattedName(String name) {
      this.formattedName = name;
      return this;
    }

    public Builder addPhone(String phone, String type) {
      this.phones.add(new PhoneEntry(phone, type));
      return this;
    }

    public Builder addEmail(String email, String type) {
      this.emails.add(new EmailEntry(email, type));
      return this;
    }

    public ContactMessage build() {
      return new ContactMessage(formattedName, phones, emails);
    }
  }
}
