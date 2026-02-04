package com.paragon.messaging.whatsapp.messages;

import com.paragon.messaging.core.OutboundMessage.OutboundMessageType;
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
 * Tests for {@link LocationMessage}.
 */
@DisplayName("LocationMessage")
class LocationMessageTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Nested
    @DisplayName("Construction")
    class ConstructionTests {

        @Test
        @DisplayName("creates location with coordinates only")
        void createsWithCoordinatesOnly() {
            LocationMessage location = new LocationMessage(-23.5505, -46.6333);

            assertEquals(-23.5505, location.latitude());
            assertEquals(-46.6333, location.longitude());
            assertTrue(location.name().isEmpty());
            assertTrue(location.address().isEmpty());
        }

        @Test
        @DisplayName("creates location with name and address")
        void createsWithNameAndAddress() {
            LocationMessage location = new LocationMessage(
                    -23.5505, -46.6333,
                    "São Paulo",
                    "City center");

            assertTrue(location.name().isPresent());
            assertTrue(location.address().isPresent());
            assertEquals("São Paulo", location.name().get());
            assertEquals("City center", location.address().get());
        }

        @Test
        @DisplayName("creates location at equator and prime meridian")
        void createsAtZeroZero() {
            LocationMessage location = new LocationMessage(0.0, 0.0);

            assertEquals(0.0, location.latitude());
            assertEquals(0.0, location.longitude());
        }

        @Test
        @DisplayName("creates location at extreme coordinates")
        void createsAtExtremes() {
            LocationMessage north = new LocationMessage(90.0, 0.0);
            LocationMessage south = new LocationMessage(-90.0, 0.0);
            LocationMessage east = new LocationMessage(0.0, 180.0);
            LocationMessage west = new LocationMessage(0.0, -180.0);

            assertEquals(90.0, north.latitude());
            assertEquals(-90.0, south.latitude());
            assertEquals(180.0, east.longitude());
            assertEquals(-180.0, west.longitude());
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("builds location with all fields")
        void buildsWithAllFields() {
            LocationMessage location = LocationMessage.builder()
                    .latitude(-23.5505)
                    .longitude(-46.6333)
                    .name("São Paulo")
                    .address("Avenida Paulista")
                    .build();

            assertEquals(-23.5505, location.latitude());
            assertEquals(-46.6333, location.longitude());
            assertEquals("São Paulo", location.name().get());
            assertEquals("Avenida Paulista", location.address().get());
        }

        @Test
        @DisplayName("builds with coordinates() method")
        void buildsWithCoordinatesMethod() {
            LocationMessage location = LocationMessage.builder()
                    .coordinates(-23.5505, -46.6333)
                    .name("São Paulo")
                    .build();

            assertEquals(-23.5505, location.latitude());
            assertEquals(-46.6333, location.longitude());
        }
    }

    @Nested
    @DisplayName("Validation")
    class ValidationTests {

        @Test
        @DisplayName("validates latitude within range -90 to 90")
        void validatesLatitudeRange() {
            LocationMessage valid = new LocationMessage(45.0, 0.0);
            Set<ConstraintViolation<LocationMessage>> violations = validator.validate(valid);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("rejects latitude > 90")
        void rejectsLatitudeTooHigh() {
            LocationMessage invalid = new LocationMessage(91.0, 0.0);

            Set<ConstraintViolation<LocationMessage>> violations = validator.validate(invalid);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getMessage().contains("90")));
        }

        @Test
        @DisplayName("rejects latitude < -90")
        void rejectsLatitudeTooLow() {
            LocationMessage invalid = new LocationMessage(-91.0, 0.0);

            Set<ConstraintViolation<LocationMessage>> violations = validator.validate(invalid);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("validates longitude within range -180 to 180")
        void validatesLongitudeRange() {
            LocationMessage valid = new LocationMessage(0.0, 120.0);
            Set<ConstraintViolation<LocationMessage>> violations = validator.validate(valid);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("rejects longitude > 180")
        void rejectsLongitudeTooHigh() {
            LocationMessage invalid = new LocationMessage(0.0, 181.0);

            Set<ConstraintViolation<LocationMessage>> violations = validator.validate(invalid);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("rejects longitude < -180")
        void rejectsLongitudeTooLow() {
            LocationMessage invalid = new LocationMessage(0.0, -181.0);

            Set<ConstraintViolation<LocationMessage>> violations = validator.validate(invalid);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("validates name length <= 256")
        void validatesNameLength() {
            String longName = "A".repeat(257);
            LocationMessage invalid = new LocationMessage(0.0, 0.0, longName, null);

            Set<ConstraintViolation<LocationMessage>> violations = validator.validate(invalid);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("validates address length <= 512")
        void validatesAddressLength() {
            String longAddress = "A".repeat(513);
            LocationMessage invalid = new LocationMessage(0.0, 0.0, null, longAddress);

            Set<ConstraintViolation<LocationMessage>> violations = validator.validate(invalid);
            assertFalse(violations.isEmpty());
        }
    }

    @Nested
    @DisplayName("Interface Methods")
    class InterfaceMethodsTests {

        @Test
        @DisplayName("type() returns LOCATION")
        void typeReturnsLocation() {
            LocationMessage location = new LocationMessage(0.0, 0.0);

            assertEquals(OutboundMessageType.LOCATION, location.type());
        }
    }

    @Nested
    @DisplayName("Utility Methods")
    class UtilityMethodsTests {

        @Test
        @DisplayName("hasValidCoordinates() returns true for valid coords")
        void hasValidCoordinatesTrue() {
            LocationMessage location = new LocationMessage(45.0, 90.0);

            assertTrue(location.hasValidCoordinates());
        }

        @Test
        @DisplayName("hasValidCoordinates() returns false for NaN latitude")
        void hasValidCoordinatesFalseNaN() {
            LocationMessage location = new LocationMessage(Double.NaN, 0.0);

            assertFalse(location.hasValidCoordinates());
        }

        @Test
        @DisplayName("hasValidCoordinates() returns false for Infinity")
        void hasValidCoordinatesFalseInfinity() {
            LocationMessage location = new LocationMessage(Double.POSITIVE_INFINITY, 0.0);

            assertFalse(location.hasValidCoordinates());
        }

        @Test
        @DisplayName("toCoordinatesString() formats correctly")
        void toCoordinatesStringFormats() {
            LocationMessage location = new LocationMessage(-23.5505, -46.6333);

            String coords = location.toCoordinatesString();

            assertTrue(coords.contains("-23.5505"));
            assertTrue(coords.contains("-46.6333"));
            assertTrue(coords.contains(","));
        }

        @Test
        @DisplayName("toGoogleMapsUrl() generates correct URL")
        void toGoogleMapsUrlGenerates() {
            LocationMessage location = new LocationMessage(-23.5505, -46.6333);

            String url = location.toGoogleMapsUrl();

            assertTrue(url.startsWith("https://www.google.com/maps?q="));
            assertTrue(url.contains("-23.5505"));
            assertTrue(url.contains("-46.6333"));
        }

        @Test
        @DisplayName("distanceTo() calculates distance between locations")
        void distanceToCalculates() {
            // São Paulo to Rio de Janeiro (approximately 360 km)
            LocationMessage saoPaulo = new LocationMessage(-23.5505, -46.6333);
            LocationMessage rio = new LocationMessage(-22.9068, -43.1729);

            double distance = saoPaulo.distanceTo(rio);

            // Should be around 360km (allowing some margin)
            assertTrue(distance > 350 && distance < 370,
                    "Distance should be approximately 360km, got: " + distance);
        }

        @Test
        @DisplayName("distanceTo() returns 0 for same location")
        void distanceToSameLocation() {
            LocationMessage loc1 = new LocationMessage(0.0, 0.0);
            LocationMessage loc2 = new LocationMessage(0.0, 0.0);

            double distance = loc1.distanceTo(loc2);

            assertEquals(0.0, distance, 0.001);
        }

        @Test
        @DisplayName("distanceTo() calculates equatorial distance")
        void distanceToEquator() {
            // Points on the equator, 1 degree apart
            LocationMessage loc1 = new LocationMessage(0.0, 0.0);
            LocationMessage loc2 = new LocationMessage(0.0, 1.0);

            double distance = loc1.distanceTo(loc2);

            // 1 degree at equator ≈ 111 km
            assertTrue(distance > 110 && distance < 112);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("handles null name and address")
        void handlesNullOptionals() {
            LocationMessage location = new LocationMessage(0.0, 0.0, null, null);

            assertTrue(location.name().isEmpty());
            assertTrue(location.address().isEmpty());

            Set<ConstraintViolation<LocationMessage>> violations = validator.validate(location);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("handles Unicode in name")
        void handlesUnicodeName() {
            LocationMessage location = new LocationMessage(
                    0.0, 0.0,
                    "北京 Beijing مسجد",
                    null);

            Set<ConstraintViolation<LocationMessage>> violations = validator.validate(location);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("handles very precise coordinates")
        void handlesPreciseCoordinates() {
            LocationMessage location = new LocationMessage(
                    -23.550520123456789,
                    -46.633308987654321);

            assertEquals(-23.550520123456789, location.latitude());
            assertEquals(-46.633308987654321, location.longitude());
        }

        @Test
        @DisplayName("handles coordinates near poles")
        void handlesNearPoles() {
            LocationMessage northPole = new LocationMessage(89.9999, 0.0);
            LocationMessage southPole = new LocationMessage(-89.9999, 0.0);

            Set<ConstraintViolation<LocationMessage>> violationsNorth =
                    validator.validate(northPole);
            Set<ConstraintViolation<LocationMessage>> violationsSouth =
                    validator.validate(southPole);

            assertTrue(violationsNorth.isEmpty());
            assertTrue(violationsSouth.isEmpty());
        }

        @Test
        @DisplayName("handles coordinates near date line")
        void handlesNearDateLine() {
            LocationMessage east = new LocationMessage(0.0, 179.9999);
            LocationMessage west = new LocationMessage(0.0, -179.9999);

            Set<ConstraintViolation<LocationMessage>> violationsEast =
                    validator.validate(east);
            Set<ConstraintViolation<LocationMessage>> violationsWest =
                    validator.validate(west);

            assertTrue(violationsEast.isEmpty());
            assertTrue(violationsWest.isEmpty());
        }
    }
}
