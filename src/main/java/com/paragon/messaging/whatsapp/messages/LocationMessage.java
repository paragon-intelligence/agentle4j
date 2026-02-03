package com.paragon.messaging.whatsapp.messages;

import com.paragon.messaging.core.LocationMessageInterface;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Optional;

/**
 * Mensagem de localização geográfica com validação de coordenadas.
 *
 * @param latitude  latitude da localização (-90 a 90)
 * @param longitude longitude da localização (-180 a 180)
 * @param name      nome do local (opcional, máx 1000 caracteres)
 * @param address   endereço do local (opcional, máx 1000 caracteres)
 * @author Your Name
 * @since 2.0
 */
public record LocationMessage(

        @NotNull(message = "Latitude cannot be null")
        @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
        @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
        double latitude,

        @NotNull(message = "Longitude cannot be null")
        @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
        @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
        double longitude,

        Optional<@Size(max = 256, message = "Location name cannot exceed 256 characters") String> name,
        Optional<@Size(max = 512, message = "Location address cannot exceed 512 characters") String> address

) implements LocationMessageInterface {

  public LocationMessage(double latitude, double longitude) {
    this(latitude, longitude, Optional.empty(), Optional.empty());
  }

  public LocationMessage(double latitude, double longitude, String name, String address) {
    this(latitude, longitude, Optional.ofNullable(name), Optional.ofNullable(address));
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public OutboundMessageType type() {
    return OutboundMessageType.LOCATION;
  }

  /**
   * Verifica se as coordenadas são válidas (não NaN, não Infinity).
   *
   * @return true se as coordenadas são válidas
   */
  public boolean hasValidCoordinates() {
    return !Double.isNaN(latitude) && !Double.isInfinite(latitude) &&
            !Double.isNaN(longitude) && !Double.isInfinite(longitude);
  }

  /**
   * Calcula a distância aproximada em km até outra localização.
   *
   * <p>Usa a fórmula de Haversine para cálculo de distância sobre a esfera terrestre.</p>
   *
   * @param other outra localização
   * @return distância em quilômetros
   */
  public double distanceTo(LocationMessage other) {
    final double EARTH_RADIUS_KM = 6371.0;

    double lat1Rad = Math.toRadians(latitude);
    double lat2Rad = Math.toRadians(other.latitude);
    double deltaLat = Math.toRadians(other.latitude - latitude);
    double deltaLon = Math.toRadians(other.longitude - longitude);

    double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
            Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                    Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    return EARTH_RADIUS_KM * c;
  }

  /**
   * Retorna uma representação de coordenadas no formato "lat,lng".
   *
   * @return string de coordenadas
   */
  public String toCoordinatesString() {
    return String.format("%.6f,%.6f", latitude, longitude);
  }

  /**
   * Retorna um link do Google Maps para esta localização.
   *
   * @return URL do Google Maps
   */
  public String toGoogleMapsUrl() {
    return String.format(
            "https://www.google.com/maps?q=%.6f,%.6f",
            latitude, longitude
    );
  }

  public static class Builder {
    private double latitude;
    private double longitude;
    private String name;
    private String address;

    public Builder latitude(double latitude) {
      this.latitude = latitude;
      return this;
    }

    public Builder longitude(double longitude) {
      this.longitude = longitude;
      return this;
    }

    public Builder coordinates(double latitude, double longitude) {
      this.latitude = latitude;
      this.longitude = longitude;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder address(String address) {
      this.address = address;
      return this;
    }

    public LocationMessage build() {
      return new LocationMessage(
              latitude,
              longitude,
              Optional.ofNullable(name),
              Optional.ofNullable(address)
      );
    }
  }
}
