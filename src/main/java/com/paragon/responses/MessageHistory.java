package com.paragon.responses;

import com.paragon.responses.spec.Message;
import java.util.List;
import org.jspecify.annotations.NonNull;

public record MessageHistory(@NonNull List<Message> messages) {

  public static MessageHistory of() {
    return new MessageHistory(List.of());
  }

  public static MessageHistory of(@NonNull List<Message> messages) {
    return new MessageHistory(messages);
  }
}
