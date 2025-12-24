package com.paragon.responses.spec;

import com.fasterxml.jackson.annotation.JsonProperty;

/** The object type of this resource - always set to response. */
public enum ResponseObject {
  @JsonProperty("response")
  RESPONSE
}
