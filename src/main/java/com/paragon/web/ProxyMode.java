package com.paragon.web;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Proxy mode for web extraction requests.
 */
public enum ProxyMode {
  /** Basic proxy without special handling. */
  BASIC("basic"),
  
  /** Stealth proxy with anti-detection measures. */
  STEALTH("stealth"),
  
  /** Automatically choose the best proxy mode. */
  AUTO("auto");

  private final String value;

  ProxyMode(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }
}
