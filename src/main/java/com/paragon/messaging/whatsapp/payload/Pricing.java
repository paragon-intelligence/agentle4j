package com.paragon.messaging.whatsapp.payload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @param category marketing, utility, authentication, service
 */
public record Pricing(boolean billable, String pricingModel, String category) {

  @JsonCreator
  public Pricing(
      @JsonProperty("billable") boolean billable,
      @JsonProperty("pricing_model") String pricingModel,
      @JsonProperty("category") String category) {
    this.billable = billable;
    this.pricingModel = pricingModel;
    this.category = category;
  }
}
