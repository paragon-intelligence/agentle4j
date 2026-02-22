package com.paragon.messaging.core;

import java.util.Optional;
import org.jspecify.annotations.NonNull;

/**
 * Sealed sub-interface for location messages.
 *
 * <p>This interface is part of the {@link OutboundMessage} sealed hierarchy and is implemented by
 * concrete location message classes.
 *
 * @since 2.1
 */
public non-sealed interface LocationMessageInterface extends OutboundMessage {

  /**
   * @return latitude coordinate
   */
  double latitude();

  /**
   * @return longitude coordinate
   */
  double longitude();

  /**
   * @return optional location name
   */
  Optional<String> name();

  /**
   * @return optional location address
   */
  Optional<String> address();

  /**
   * @return formatted coordinates string
   */
  default @NonNull String toCoordinatesString() {
    return latitude() + "," + longitude();
  }

  @Override
  default OutboundMessageType type() {
    return OutboundMessageType.LOCATION;
  }
}
