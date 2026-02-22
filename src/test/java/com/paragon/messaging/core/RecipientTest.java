package com.paragon.messaging.core;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link Recipient} record and factory methods. */
@DisplayName("Recipient")
class RecipientTest {

  private Validator validator;

  @BeforeEach
  void setUp() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Nested
  @DisplayName("Factory Methods")
  class FactoryMethodsTests {

    @Test
    @DisplayName("ofPhoneNumber() creates phone recipient")
    void ofPhoneNumberCreatesRecipient() {
      Recipient recipient = Recipient.ofPhoneNumber("+5511999999999");

      assertEquals("+5511999999999", recipient.identifier());
      assertEquals(Recipient.RecipientType.PHONE_NUMBER, recipient.type());
      assertTrue(recipient.isPhoneNumber());
      assertFalse(recipient.isUserId());
      assertFalse(recipient.isEmail());
    }

    @Test
    @DisplayName("ofPhoneNumberNormalized() normalizes phone number")
    void ofPhoneNumberNormalizedNormalizes() {
      Recipient recipient = Recipient.ofPhoneNumberNormalized("+55 (11) 99999-9999");

      assertEquals("+5511999999999", recipient.identifier());
      assertEquals(Recipient.RecipientType.PHONE_NUMBER, recipient.type());
    }

    @Test
    @DisplayName("ofPhoneNumberNormalized() handles various formats")
    void ofPhoneNumberNormalizedHandlesFormats() {
      String[] formats = {
        "+55 11 99999-9999", "+55 (11) 99999-9999", "+55-11-99999-9999", "+5511999999999"
      };

      for (String format : formats) {
        Recipient recipient = Recipient.ofPhoneNumberNormalized(format);
        assertEquals("+5511999999999", recipient.identifier(), "Failed to normalize: " + format);
      }
    }

    @Test
    @DisplayName("ofPhoneNumberNormalized() throws on invalid format")
    void ofPhoneNumberNormalizedThrowsOnInvalid() {
      assertThrows(
          IllegalArgumentException.class, () -> Recipient.ofPhoneNumberNormalized("not-a-phone"));

      assertThrows(
          IllegalArgumentException.class, () -> Recipient.ofPhoneNumberNormalized("123456"));
    }

    @Test
    @DisplayName("ofUserId() creates user ID recipient")
    void ofUserIdCreatesRecipient() {
      Recipient recipient = Recipient.ofUserId("user-123");

      assertEquals("user-123", recipient.identifier());
      assertEquals(Recipient.RecipientType.USER_ID, recipient.type());
      assertFalse(recipient.isPhoneNumber());
      assertTrue(recipient.isUserId());
      assertFalse(recipient.isEmail());
    }

    @Test
    @DisplayName("ofEmail() creates email recipient")
    void ofEmailCreatesRecipient() {
      Recipient recipient = Recipient.ofEmail("user@example.com");

      assertEquals("user@example.com", recipient.identifier());
      assertEquals(Recipient.RecipientType.EMAIL, recipient.type());
      assertFalse(recipient.isPhoneNumber());
      assertFalse(recipient.isUserId());
      assertTrue(recipient.isEmail());
    }
  }

  @Nested
  @DisplayName("Bean Validation")
  class ValidationTests {

    @Test
    @DisplayName("validates identifier not blank")
    void validatesIdentifierNotBlank() {
      Recipient recipient = new Recipient("", Recipient.RecipientType.PHONE_NUMBER);

      Set<ConstraintViolation<Recipient>> violations = validator.validate(recipient);
      assertFalse(violations.isEmpty());
      assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("cannot be blank")));
    }

    @Test
    @DisplayName("validates type not null")
    void validatesTypeNotNull() {
      Recipient recipient = new Recipient("+5511999999999", null);

      Set<ConstraintViolation<Recipient>> violations = validator.validate(recipient);
      assertFalse(violations.isEmpty());
      assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("cannot be null")));
    }

    @Test
    @DisplayName("accepts valid recipient")
    void acceptsValidRecipient() {
      Recipient recipient = Recipient.ofPhoneNumber("+5511999999999");

      Set<ConstraintViolation<Recipient>> violations = validator.validate(recipient);
      assertTrue(violations.isEmpty());
    }
  }

  @Nested
  @DisplayName("Type Checking Methods")
  class TypeCheckingTests {

    @Test
    @DisplayName("isPhoneNumber() returns true for phone type")
    void isPhoneNumberReturnsTrue() {
      Recipient recipient = Recipient.ofPhoneNumber("+5511999999999");
      assertTrue(recipient.isPhoneNumber());
    }

    @Test
    @DisplayName("isPhoneNumber() returns false for other types")
    void isPhoneNumberReturnsFalse() {
      assertFalse(Recipient.ofUserId("user-123").isPhoneNumber());
      assertFalse(Recipient.ofEmail("user@example.com").isPhoneNumber());
    }

    @Test
    @DisplayName("isUserId() returns true for user ID type")
    void isUserIdReturnsTrue() {
      Recipient recipient = Recipient.ofUserId("user-123");
      assertTrue(recipient.isUserId());
    }

    @Test
    @DisplayName("isUserId() returns false for other types")
    void isUserIdReturnsFalse() {
      assertFalse(Recipient.ofPhoneNumber("+5511999999999").isUserId());
      assertFalse(Recipient.ofEmail("user@example.com").isUserId());
    }

    @Test
    @DisplayName("isEmail() returns true for email type")
    void isEmailReturnsTrue() {
      Recipient recipient = Recipient.ofEmail("user@example.com");
      assertTrue(recipient.isEmail());
    }

    @Test
    @DisplayName("isEmail() returns false for other types")
    void isEmailReturnsFalse() {
      assertFalse(Recipient.ofPhoneNumber("+5511999999999").isEmail());
      assertFalse(Recipient.ofUserId("user-123").isEmail());
    }
  }

  @Nested
  @DisplayName("Real-World Scenarios")
  class RealWorldScenariosTests {

    @Test
    @DisplayName("handles Brazilian phone numbers")
    void handlesBrazilianPhoneNumbers() {
      Recipient recipient = Recipient.ofPhoneNumber("+5511987654321");

      assertTrue(recipient.isPhoneNumber());
      assertEquals("+5511987654321", recipient.identifier());
    }

    @Test
    @DisplayName("handles U.S. phone numbers")
    void handlesUSPhoneNumbers() {
      Recipient recipient = Recipient.ofPhoneNumber("+15551234567");

      assertTrue(recipient.isPhoneNumber());
      assertEquals("+15551234567", recipient.identifier());
    }

    @Test
    @DisplayName("handles WhatsApp user IDs")
    void handlesWhatsAppUserIds() {
      Recipient recipient = Recipient.ofUserId("5511987654321");

      assertTrue(recipient.isUserId());
      assertEquals("5511987654321", recipient.identifier());
    }

    @Test
    @DisplayName("handles email addresses")
    void handlesEmailAddresses() {
      Recipient recipient = Recipient.ofEmail("customer.support@company.com");

      assertTrue(recipient.isEmail());
      assertEquals("customer.support@company.com", recipient.identifier());
    }

    @Test
    @DisplayName("normalizes phone with spaces")
    void normalizesPhoneWithSpaces() {
      Recipient recipient = Recipient.ofPhoneNumberNormalized("+55 11 98765 4321");

      assertEquals("+5511987654321", recipient.identifier());
    }

    @Test
    @DisplayName("normalizes phone with parentheses")
    void normalizesPhoneWithParentheses() {
      Recipient recipient = Recipient.ofPhoneNumberNormalized("+55 (11) 98765-4321");

      assertEquals("+5511987654321", recipient.identifier());
    }
  }

  @Nested
  @DisplayName("RecipientType Enum")
  class RecipientTypeTests {

    @Test
    @DisplayName("has PHONE_NUMBER type")
    void hasPhoneNumberType() {
      assertEquals(
          Recipient.RecipientType.PHONE_NUMBER, Recipient.RecipientType.valueOf("PHONE_NUMBER"));
    }

    @Test
    @DisplayName("has USER_ID type")
    void hasUserIdType() {
      assertEquals(Recipient.RecipientType.USER_ID, Recipient.RecipientType.valueOf("USER_ID"));
    }

    @Test
    @DisplayName("has EMAIL type")
    void hasEmailType() {
      assertEquals(Recipient.RecipientType.EMAIL, Recipient.RecipientType.valueOf("EMAIL"));
    }
  }

  @Nested
  @DisplayName("Edge Cases")
  class EdgeCasesTests {

    @Test
    @DisplayName("handles minimum length phone number")
    void handlesMinimumLengthPhone() {
      Recipient recipient = Recipient.ofPhoneNumber("+1234567890");

      assertEquals("+1234567890", recipient.identifier());
    }

    @Test
    @DisplayName("handles maximum length phone number")
    void handlesMaximumLengthPhone() {
      Recipient recipient = Recipient.ofPhoneNumber("+123456789012345");

      assertEquals("+123456789012345", recipient.identifier());
    }

    @Test
    @DisplayName("handles email with special characters")
    void handlesEmailSpecialChars() {
      Recipient recipient = Recipient.ofEmail("user+tag@example.co.uk");

      assertEquals("user+tag@example.co.uk", recipient.identifier());
    }

    @Test
    @DisplayName("handles user ID with special characters")
    void handlesUserIdSpecialChars() {
      Recipient recipient = Recipient.ofUserId("user-123_abc@platform");

      assertEquals("user-123_abc@platform", recipient.identifier());
    }

    @Test
    @DisplayName("ofPhoneNumberNormalized throws on empty string")
    void throwsOnEmptyString() {
      assertThrows(IllegalArgumentException.class, () -> Recipient.ofPhoneNumberNormalized(""));
    }

    @Test
    @DisplayName("ofPhoneNumberNormalized throws on malformed number")
    void throwsOnMalformedNumber() {
      assertThrows(
          IllegalArgumentException.class, () -> Recipient.ofPhoneNumberNormalized("abc-def-ghij"));
    }
  }

  @Nested
  @DisplayName("Equality and Hashing")
  class EqualityTests {

    @Test
    @DisplayName("recipients with same data are equal")
    void recipientsWithSameDataAreEqual() {
      Recipient r1 = Recipient.ofPhoneNumber("+5511999999999");
      Recipient r2 = Recipient.ofPhoneNumber("+5511999999999");

      assertEquals(r1, r2);
      assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    @DisplayName("recipients with different identifiers are not equal")
    void recipientsWithDifferentIdentifiersNotEqual() {
      Recipient r1 = Recipient.ofPhoneNumber("+5511999999999");
      Recipient r2 = Recipient.ofPhoneNumber("+5511888888888");

      assertNotEquals(r1, r2);
    }

    @Test
    @DisplayName("recipients with different types are not equal")
    void recipientsWithDifferentTypesNotEqual() {
      Recipient r1 = new Recipient("123", Recipient.RecipientType.PHONE_NUMBER);
      Recipient r2 = new Recipient("123", Recipient.RecipientType.USER_ID);

      assertNotEquals(r1, r2);
    }
  }
}
