package com.paragon.messaging.whatsapp;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link E164PhoneNumber} annotation and utilities.
 */
@DisplayName("E164PhoneNumber")
class E164PhoneNumberTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Nested
    @DisplayName("Validation")
    class ValidationTests {

        static class PhoneHolder {
            @E164PhoneNumber
            String phone;

            PhoneHolder(String phone) {
                this.phone = phone;
            }
        }

        @Test
        @DisplayName("accepts valid E.164 numbers with +")
        void acceptsValidWithPlus() {
            PhoneHolder holder = new PhoneHolder("+5511999999999");
            Set<ConstraintViolation<PhoneHolder>> violations = validator.validate(holder);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("accepts valid E.164 numbers without +")
        void acceptsValidWithoutPlus() {
            PhoneHolder holder = new PhoneHolder("5511999999999");
            Set<ConstraintViolation<PhoneHolder>> violations = validator.validate(holder);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("accepts US number")
        void acceptsUSNumber() {
            PhoneHolder holder = new PhoneHolder("+14155552671");
            Set<ConstraintViolation<PhoneHolder>> violations = validator.validate(holder);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("accepts minimum length number")
        void acceptsMinLength() {
            PhoneHolder holder = new PhoneHolder("+12");  // Minimum 2 digits
            Set<ConstraintViolation<PhoneHolder>> violations = validator.validate(holder);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("accepts maximum length number")
        void acceptsMaxLength() {
            PhoneHolder holder = new PhoneHolder("+123456789012345");  // 15 digits
            Set<ConstraintViolation<PhoneHolder>> violations = validator.validate(holder);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("rejects number starting with 0")
        void rejectsStartingWithZero() {
            PhoneHolder holder = new PhoneHolder("+0123456789");
            Set<ConstraintViolation<PhoneHolder>> violations = validator.validate(holder);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> 
                v.getMessage().contains("cannot start with 0")));
        }

        @Test
        @DisplayName("rejects number too short")
        void rejectsTooShort() {
            PhoneHolder holder = new PhoneHolder("+1");  // Only 1 digit
            Set<ConstraintViolation<PhoneHolder>> violations = validator.validate(holder);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("rejects number too long")
        void rejectsTooLong() {
            PhoneHolder holder = new PhoneHolder("+1234567890123456");  // 16 digits
            Set<ConstraintViolation<PhoneHolder>> violations = validator.validate(holder);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> 
                v.getMessage().contains("between 2 and 15 digits")));
        }

        @Test
        @DisplayName("rejects number with spaces")
        void rejectsWithSpaces() {
            PhoneHolder holder = new PhoneHolder("+55 11 99999 9999");
            Set<ConstraintViolation<PhoneHolder>> violations = validator.validate(holder);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("rejects number with parentheses")
        void rejectsWithParentheses() {
            PhoneHolder holder = new PhoneHolder("+55(11)999999999");
            Set<ConstraintViolation<PhoneHolder>> violations = validator.validate(holder);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("rejects number with hyphens")
        void rejectsWithHyphens() {
            PhoneHolder holder = new PhoneHolder("+55-11-99999-9999");
            Set<ConstraintViolation<PhoneHolder>> violations = validator.validate(holder);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("rejects number with letters")
        void rejectsWithLetters() {
            PhoneHolder holder = new PhoneHolder("+55ABC123456");
            Set<ConstraintViolation<PhoneHolder>> violations = validator.validate(holder);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> 
                v.getMessage().contains("only digits")));
        }

        @Test
        @DisplayName("rejects blank number")
        void rejectsBlank() {
            PhoneHolder holder = new PhoneHolder("");
            Set<ConstraintViolation<PhoneHolder>> violations = validator.validate(holder);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("allows null (use @NotNull separately)")
        void allowsNull() {
            PhoneHolder holder = new PhoneHolder(null);
            Set<ConstraintViolation<PhoneHolder>> violations = validator.validate(holder);
            assertTrue(violations.isEmpty());
        }
    }

    @Nested
    @DisplayName("Utils.isValid()")
    class IsValidTests {

        @Test
        @DisplayName("returns true for valid numbers with +")
        void validWithPlus() {
            assertTrue(E164PhoneNumber.Utils.isValid("+5511999999999"));
            assertTrue(E164PhoneNumber.Utils.isValid("+14155552671"));
        }

        @Test
        @DisplayName("returns true for valid numbers without +")
        void validWithoutPlus() {
            assertTrue(E164PhoneNumber.Utils.isValid("5511999999999"));
            assertTrue(E164PhoneNumber.Utils.isValid("14155552671"));
        }

        @Test
        @DisplayName("returns false for invalid formats")
        void invalidFormats() {
            assertFalse(E164PhoneNumber.Utils.isValid("+0123456789"));
            assertFalse(E164PhoneNumber.Utils.isValid("+55 11 99999 9999"));
            assertFalse(E164PhoneNumber.Utils.isValid(null));
            assertFalse(E164PhoneNumber.Utils.isValid(""));
            assertFalse(E164PhoneNumber.Utils.isValid("   "));
        }
    }

    @Nested
    @DisplayName("Utils.normalize()")
    class NormalizeTests {

        @Test
        @DisplayName("normalizes number with spaces")
        void normalizesWithSpaces() {
            String result = E164PhoneNumber.Utils.normalize("+55 11 99999 9999");
            assertEquals("+5511999999999", result);
        }

        @Test
        @DisplayName("normalizes number with parentheses")
        void normalizesWithParentheses() {
            String result = E164PhoneNumber.Utils.normalize("+55(11)999999999");
            assertEquals("+5511999999999", result);
        }

        @Test
        @DisplayName("normalizes number with hyphens")
        void normalizesWithHyphens() {
            String result = E164PhoneNumber.Utils.normalize("+55-11-99999-9999");
            assertEquals("+5511999999999", result);
        }

        @Test
        @DisplayName("adds + if missing")
        void addsPlus() {
            String result = E164PhoneNumber.Utils.normalize("5511999999999");
            assertEquals("+5511999999999", result);
        }

        @Test
        @DisplayName("normalizes US number")
        void normalizesUSNumber() {
            String result = E164PhoneNumber.Utils.normalize("1 (415) 555-2671");
            assertEquals("+14155552671", result);
        }

        @Test
        @DisplayName("returns null for invalid number starting with 0")
        void returnsNullForZero() {
            String result = E164PhoneNumber.Utils.normalize("+0123456789");
            assertNull(result);
        }

        @Test
        @DisplayName("returns null for too short")
        void returnsNullForTooShort() {
            String result = E164PhoneNumber.Utils.normalize("+1");
            assertNull(result);
        }

        @Test
        @DisplayName("returns null for null input")
        void returnsNullForNull() {
            String result = E164PhoneNumber.Utils.normalize(null);
            assertNull(result);
        }

        @Test
        @DisplayName("returns null for blank input")
        void returnsNullForBlank() {
            String result = E164PhoneNumber.Utils.normalize("");
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("Utils.extractCountryCode()")
    class ExtractCountryCodeTests {

        @Test
        @DisplayName("extracts US country code (1)")
        void extractsUS() {
            String code = E164PhoneNumber.Utils.extractCountryCode("+14155552671");
            assertEquals("1", code);
        }

        @Test
        @DisplayName("extracts Brazil country code (55)")
        void extractsBrazil() {
            String code = E164PhoneNumber.Utils.extractCountryCode("+5511999999999");
            assertEquals("55", code);
        }

        @Test
        @DisplayName("extracts 2-digit country code as default")
        void extractsTwoDigits() {
            String code = E164PhoneNumber.Utils.extractCountryCode("+441234567890");
            assertEquals("44", code);
        }

        @Test
        @DisplayName("extracts from number without +")
        void extractsWithoutPlus() {
            String code = E164PhoneNumber.Utils.extractCountryCode("5511999999999");
            assertEquals("55", code);
        }

        @Test
        @DisplayName("returns null for invalid number")
        void returnsNullForInvalid() {
            String code = E164PhoneNumber.Utils.extractCountryCode("+0123456789");
            assertNull(code);
        }

        @Test
        @DisplayName("returns null for null input")
        void returnsNullForNull() {
            String code = E164PhoneNumber.Utils.extractCountryCode(null);
            assertNull(code);
        }
    }

    @Nested
    @DisplayName("Real-World Numbers")
    class RealWorldNumbersTests {

        @Test
        @DisplayName("validates Brazilian mobile number")
        void brazilianMobile() {
            assertTrue(E164PhoneNumber.Utils.isValid("+5511987654321"));
        }

        @Test
        @DisplayName("validates US mobile number")
        void usMobile() {
            assertTrue(E164PhoneNumber.Utils.isValid("+14155552671"));
        }

        @Test
        @DisplayName("validates UK mobile number")
        void ukMobile() {
            assertTrue(E164PhoneNumber.Utils.isValid("+447911123456"));
        }

        @Test
        @DisplayName("validates German mobile number")
        void germanMobile() {
            assertTrue(E164PhoneNumber.Utils.isValid("+491701234567"));
        }

        @Test
        @DisplayName("validates Indian mobile number")
        void indianMobile() {
            assertTrue(E164PhoneNumber.Utils.isValid("+919876543210"));
        }

        @Test
        @DisplayName("normalizes formatted Brazilian number")
        void normalizesBrazilian() {
            String result = E164PhoneNumber.Utils.normalize("(11) 98765-4321");
            assertEquals("+11987654321", result);  // Note: adds default +, but validates if correct
        }

        @Test
        @DisplayName("extracts country codes from various countries")
        void extractsVariousCodes() {
            assertEquals("1", E164PhoneNumber.Utils.extractCountryCode("+14155552671")); // US
            assertEquals("55", E164PhoneNumber.Utils.extractCountryCode("+5511987654321")); // BR
            assertEquals("44", E164PhoneNumber.Utils.extractCountryCode("+447911123456")); // UK
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("handles whitespace-only number")
        void handlesWhitespace() {
            assertFalse(E164PhoneNumber.Utils.isValid("   "));
        }

        @Test
        @DisplayName("handles number with only +")
        void handlesOnlyPlus() {
            assertFalse(E164PhoneNumber.Utils.isValid("+"));
        }

        @Test
        @DisplayName("handles multiple + signs")
        void handlesMultiplePlus() {
            String result = E164PhoneNumber.Utils.normalize("++5511999999999");
            // Should still work after normalization
            assertNotNull(result);
        }

        @Test
        @DisplayName("handles exactly 15 digits at boundary")
        void handlesMaxDigits() {
            assertTrue(E164PhoneNumber.Utils.isValid("+123456789012345"));
        }

        @Test
        @DisplayName("handles exactly 2 digits at boundary")
        void handlesMinDigits() {
            assertTrue(E164PhoneNumber.Utils.isValid("+12"));
        }
    }
}
