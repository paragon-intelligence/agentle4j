package com.paragon.messaging.whatsapp.payload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Inbound order message from WhatsApp webhook.
 *
 * <p>Order messages are received when a user places an order through WhatsApp Commerce features.
 */
public final class OrderMessage extends AbstractInboundMessage {

  @Nullable public final String catalogId;

  @Nullable public final String text;

  @Nullable public final List<ProductItem> productItems;

  @JsonCreator
  public OrderMessage(
      @JsonProperty("from") String from,
      @JsonProperty("id") String id,
      @JsonProperty("timestamp") String timestamp,
      @JsonProperty("type") String type,
      @JsonProperty("context") MessageContext context,
      @JsonProperty("order") OrderContent order) {
    super(from, id, timestamp, type, context);
    if (order != null) {
      this.catalogId = order.catalogId();
      this.text = order.text();
      this.productItems =
          order.productItems() != null ? List.copyOf(order.productItems()) : List.of();
    } else {
      this.catalogId = null;
      this.text = null;
      this.productItems = List.of();
    }
  }

  public record OrderContent(
      @JsonProperty("catalog_id") @Nullable String catalogId,
      @JsonProperty("text") @Nullable String text,
      @JsonProperty("product_items") @Nullable List<ProductItem> productItems) {}

  public record ProductItem(
      @JsonProperty("product_retailer_id") String productRetailerId,
      @JsonProperty("quantity") int quantity,
      @JsonProperty("item_price") @Nullable String itemPrice,
      @JsonProperty("currency") @Nullable String currency) {}
}
