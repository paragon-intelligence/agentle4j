package com.paragon;

import com.paragon.responses.spec.Message;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jspecify.annotations.NonNull;

public record Messages(@NonNull List<Message> messages) {

  public static Messages of(Message... message) {
    List<Message> messageList = new ArrayList<>(Arrays.asList(message));
    return new Messages(messageList);
  }

  public static Messages of() {
    return new Messages(new ArrayList<>());
  }

  public Messages add(Message message) {
    messages.add(message);
    return this;
  }
}
