package com.paragon.messaging.security;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link WebhookSignatureValidator}. */
@DisplayName("WebhookSignatureValidator")
class WebhookSignatureValidatorTest {

  private static final String APP_SECRET = "test_secret_key_12345";
  private static final String SAMPLE_PAYLOAD =
      "{\"object\":\"whatsapp_business_account\",\"entry\":[]}";

  @Nested
  @DisplayName("Construction")
  class ConstructionTests {

    @Test
    @DisplayName("create(String) creates validator with app secret")
    void create_withAppSecret_createsValidator() {
      WebhookSignatureValidator validator = WebhookSignatureValidator.create(APP_SECRET);

      assertNotNull(validator);
      assertTrue(validator.isEnabled());
    }

    @Test
    @DisplayName("create(String) throws for null secret")
    void create_withNull_throws() {
      assertThrows(
          NullPointerException.class, () -> WebhookSignatureValidator.create((String) null));
    }

    @Test
    @DisplayName("create(String) throws for blank secret")
    void create_withBlankSecret_throws() {
      assertThrows(IllegalArgumentException.class, () -> WebhookSignatureValidator.create("   "));
    }

    @Test
    @DisplayName("create(SecurityConfig) creates from config")
    void create_withConfig_createsValidator() {
      SecurityConfig config =
          SecurityConfig.builder()
              .webhookVerifyToken("test-verify-token-12345")
              .appSecret(APP_SECRET)
              .validateSignatures(true)
              .build();

      WebhookSignatureValidator validator = WebhookSignatureValidator.create(config);

      assertNotNull(validator);
      assertTrue(validator.isEnabled());
    }

    @Test
    @DisplayName("create(SecurityConfig) creates disabled validator when validation disabled")
    void create_withDisabledConfig_createsDisabledValidator() {
      SecurityConfig config =
          SecurityConfig.builder()
              .webhookVerifyToken("test-verify-token-12345")
              .validateSignatures(false)
              .build();

      WebhookSignatureValidator validator = WebhookSignatureValidator.create(config);

      assertFalse(validator.isEnabled());
    }

    @Test
    @DisplayName("disabled() creates disabled validator")
    void disabled_createsDisabledValidator() {
      WebhookSignatureValidator validator = WebhookSignatureValidator.disabled();

      assertNotNull(validator);
      assertFalse(validator.isEnabled());
    }
  }

  @Nested
  @DisplayName("Signature Validation")
  class SignatureValidationTests {

    @Test
    @DisplayName("isValid() returns true for correct signature")
    void isValid_withCorrectSignature_returnsTrue() {
      WebhookSignatureValidator validator = WebhookSignatureValidator.create(APP_SECRET);

      String expectedSignature = validator.computeSignature(SAMPLE_PAYLOAD);
      String signatureHeader = "sha256=" + expectedSignature;

      assertTrue(validator.isValid(signatureHeader, SAMPLE_PAYLOAD));
    }

    @Test
    @DisplayName("isValid() returns true for correct signature without prefix")
    void isValid_withoutPrefix_returnsTrue() {
      WebhookSignatureValidator validator = WebhookSignatureValidator.create(APP_SECRET);

      String expectedSignature = validator.computeSignature(SAMPLE_PAYLOAD);

      assertTrue(validator.isValid(expectedSignature, SAMPLE_PAYLOAD));
    }

    @Test
    @DisplayName("isValid() returns false for incorrect signature")
    void isValid_withIncorrectSignature_returnsFalse() {
      WebhookSignatureValidator validator = WebhookSignatureValidator.create(APP_SECRET);

      String invalidSignature = "sha256=invalid_signature_12345";

      assertFalse(validator.isValid(invalidSignature, SAMPLE_PAYLOAD));
    }

    @Test
    @DisplayName("isValid() returns false for modified payload")
    void isValid_withModifiedPayload_returnsFalse() {
      WebhookSignatureValidator validator = WebhookSignatureValidator.create(APP_SECRET);

      String validSignature = "sha256=" + validator.computeSignature(SAMPLE_PAYLOAD);
      String modifiedPayload = SAMPLE_PAYLOAD + "TAMPERED";

      assertFalse(validator.isValid(validSignature, modifiedPayload));
    }

    @Test
    @DisplayName("isValid() returns false for null signature")
    void isValid_withNullSignature_returnsFalse() {
      WebhookSignatureValidator validator = WebhookSignatureValidator.create(APP_SECRET);

      assertFalse(validator.isValid(null, SAMPLE_PAYLOAD));
    }

    @Test
    @DisplayName("isValid() returns false for empty signature")
    void isValid_withEmptySignature_returnsFalse() {
      WebhookSignatureValidator validator = WebhookSignatureValidator.create(APP_SECRET);

      assertFalse(validator.isValid("", SAMPLE_PAYLOAD));
      assertFalse(validator.isValid("   ", SAMPLE_PAYLOAD));
    }

    @Test
    @DisplayName("isValid() throws for null payload")
    void isValid_withNullPayload_throws() {
      WebhookSignatureValidator validator = WebhookSignatureValidator.create(APP_SECRET);

      assertThrows(
          NullPointerException.class, () -> validator.isValid("sha256=abc123", (String) null));
    }

    @Test
    @DisplayName("isValid() with empty payload validates correctly")
    void isValid_withEmptyPayload_validates() {
      WebhookSignatureValidator validator = WebhookSignatureValidator.create(APP_SECRET);

      String emptyPayload = "";
      String signature = "sha256=" + validator.computeSignature(emptyPayload);

      assertTrue(validator.isValid(signature, emptyPayload));
    }

    @Test
    @DisplayName("isValid() with byte array validates correctly")
    void isValid_withByteArray_validates() {
      WebhookSignatureValidator validator = WebhookSignatureValidator.create(APP_SECRET);

      byte[] payloadBytes = SAMPLE_PAYLOAD.getBytes(StandardCharsets.UTF_8);
      String signature = "sha256=" + validator.computeSignature(payloadBytes);

      assertTrue(validator.isValid(signature, payloadBytes));
    }
  }

  @Nested
  @DisplayName("Disabled Validator Behavior")
  class DisabledValidatorTests {

    @Test
    @DisplayName("disabled validator always returns true")
    void disabledValidator_alwaysReturnsTrue() {
      WebhookSignatureValidator validator = WebhookSignatureValidator.disabled();

      assertTrue(validator.isValid("invalid_signature", SAMPLE_PAYLOAD));
      assertTrue(validator.isValid(null, SAMPLE_PAYLOAD));
      assertTrue(validator.isValid("", SAMPLE_PAYLOAD));
    }

    @Test
    @DisplayName("validator from config with validation disabled always returns true")
    void validatorFromDisabledConfig_alwaysReturnsTrue() {
      SecurityConfig config =
          SecurityConfig.builder()
              .webhookVerifyToken("test-verify-token-12345")
              .validateSignatures(false)
              .build();
      WebhookSignatureValidator validator = WebhookSignatureValidator.create(config);

      assertTrue(validator.isValid("invalid_signature", SAMPLE_PAYLOAD));
    }
  }

  @Nested
  @DisplayName("Signature Computation")
  class SignatureComputationTests {

    @Test
    @DisplayName("computeSignature() produces consistent results")
    void computeSignature_isConsistent() {
      WebhookSignatureValidator validator = WebhookSignatureValidator.create(APP_SECRET);

      String sig1 = validator.computeSignature(SAMPLE_PAYLOAD);
      String sig2 = validator.computeSignature(SAMPLE_PAYLOAD);

      assertEquals(sig1, sig2);
    }

    @Test
    @DisplayName("computeSignature() produces different results for different payloads")
    void computeSignature_differentForDifferentPayloads() {
      WebhookSignatureValidator validator = WebhookSignatureValidator.create(APP_SECRET);

      String sig1 = validator.computeSignature("payload1");
      String sig2 = validator.computeSignature("payload2");

      assertNotEquals(sig1, sig2);
    }

    @Test
    @DisplayName("computeSignature() produces different results for different secrets")
    void computeSignature_differentForDifferentSecrets() {
      WebhookSignatureValidator validator1 = WebhookSignatureValidator.create("secret1");
      WebhookSignatureValidator validator2 = WebhookSignatureValidator.create("secret2");

      String sig1 = validator1.computeSignature(SAMPLE_PAYLOAD);
      String sig2 = validator2.computeSignature(SAMPLE_PAYLOAD);

      assertNotEquals(sig1, sig2);
    }

    @Test
    @DisplayName("computeSignature() returns hex-encoded string")
    void computeSignature_returnsHexEncoded() {
      WebhookSignatureValidator validator = WebhookSignatureValidator.create(APP_SECRET);

      String signature = validator.computeSignature(SAMPLE_PAYLOAD);

      // Hex string should only contain 0-9 and a-f
      assertTrue(signature.matches("[0-9a-f]+"), "Signature should be hex-encoded");
    }

    @Test
    @DisplayName("computeSignature() with byte array produces same result as string")
    void computeSignature_byteArrayMatchesString() {
      WebhookSignatureValidator validator = WebhookSignatureValidator.create(APP_SECRET);

      String sigFromString = validator.computeSignature(SAMPLE_PAYLOAD);
      String sigFromBytes =
          validator.computeSignature(SAMPLE_PAYLOAD.getBytes(StandardCharsets.UTF_8));

      assertEquals(sigFromString, sigFromBytes);
    }
  }

  @Nested
  @DisplayName("Case Sensitivity")
  class CaseSensitivityTests {

    @Test
    @DisplayName("isValid() is case-insensitive for hex signature")
    void isValid_caseInsensitive() {
      WebhookSignatureValidator validator = WebhookSignatureValidator.create(APP_SECRET);

      String signature = validator.computeSignature(SAMPLE_PAYLOAD);
      String upperCaseSig = "sha256=" + signature.toUpperCase();

      assertTrue(validator.isValid(upperCaseSig, SAMPLE_PAYLOAD));
    }

    @Test
    @DisplayName("isValid() accepts mixed case prefix")
    void isValid_mixedCasePrefix() {
      WebhookSignatureValidator validator = WebhookSignatureValidator.create(APP_SECRET);

      String signature = validator.computeSignature(SAMPLE_PAYLOAD);
      String mixedCaseSig = "SHA256=" + signature;

      assertTrue(validator.isValid(mixedCaseSig, SAMPLE_PAYLOAD));
    }
  }

  @Nested
  @DisplayName("Real WhatsApp Examples")
  class RealWhatsAppExamplesTests {

    @Test
    @DisplayName("validates real WhatsApp webhook example")
    void validates_realWhatsAppWebhook() {
      String realSecret = "your_app_secret";
      String realPayload =
          """
          {
            "object": "whatsapp_business_account",
            "entry": [{
              "id": "WHATSAPP_BUSINESS_ACCOUNT_ID",
              "changes": [{
                "value": {
                  "messaging_product": "whatsapp",
                  "metadata": {
                    "display_phone_number": "PHONE_NUMBER",
                    "phone_number_id": "PHONE_NUMBER_ID"
                  },
                  "messages": [{
                    "from": "SENDER_PHONE_NUMBER",
                    "id": "wamid.ID",
                    "timestamp": "1234567890",
                    "text": {
                      "body": "Hello World"
                    },
                    "type": "text"
                  }]
                },
                "field": "messages"
              }]
            }]
          }
          """;

      WebhookSignatureValidator validator = WebhookSignatureValidator.create(realSecret);
      String signature = "sha256=" + validator.computeSignature(realPayload);

      assertTrue(validator.isValid(signature, realPayload));
    }
  }

  @Nested
  @DisplayName("State")
  class StateTests {

    @Test
    @DisplayName("isEnabled() returns true for enabled validator")
    void isEnabled_returnsTrue() {
      WebhookSignatureValidator validator = WebhookSignatureValidator.create(APP_SECRET);

      assertTrue(validator.isEnabled());
    }

    @Test
    @DisplayName("isEnabled() returns false for disabled validator")
    void isEnabled_returnsFalse() {
      WebhookSignatureValidator validator = WebhookSignatureValidator.disabled();

      assertFalse(validator.isEnabled());
    }
  }
}
