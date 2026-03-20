package com.paragon.json;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.deser.std.StdScalarDeserializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdScalarSerializer;
import java.time.Duration;
import java.time.Instant;

/**
 * Minimal Java time module for the temporal types persisted by this project.
 *
 * <p>The published Jackson 3 jsr310 module currently lags behind databind 3.1.x, so we register
 * lightweight ISO-8601 serializers/deserializers locally for the types used in this codebase.
 */
public final class ParagonJavaTimeModule extends SimpleModule {

  public ParagonJavaTimeModule() {
    super("ParagonJavaTimeModule");

    addSerializer(Instant.class, new InstantSerializer());
    addDeserializer(Instant.class, new InstantDeserializer());

    addSerializer(Duration.class, new DurationSerializer());
    addDeserializer(Duration.class, new DurationDeserializer());
  }

  private static final class InstantSerializer extends StdScalarSerializer<Instant> {
    private InstantSerializer() {
      super(Instant.class);
    }

    @Override
    public void serialize(Instant value, JsonGenerator gen, SerializationContext provider)
        throws tools.jackson.core.JacksonException {
      gen.writeString(value.toString());
    }
  }

  private static final class InstantDeserializer extends StdScalarDeserializer<Instant> {
    private InstantDeserializer() {
      super(Instant.class);
    }

    @Override
    public Instant deserialize(JsonParser p, DeserializationContext ctxt)
        throws tools.jackson.core.JacksonException {
      return Instant.parse(p.getText().trim());
    }
  }

  private static final class DurationSerializer extends StdScalarSerializer<Duration> {
    private DurationSerializer() {
      super(Duration.class);
    }

    @Override
    public void serialize(Duration value, JsonGenerator gen, SerializationContext provider)
        throws tools.jackson.core.JacksonException {
      gen.writeString(value.toString());
    }
  }

  private static final class DurationDeserializer extends StdScalarDeserializer<Duration> {
    private DurationDeserializer() {
      super(Duration.class);
    }

    @Override
    public Duration deserialize(JsonParser p, DeserializationContext ctxt)
        throws tools.jackson.core.JacksonException {
      return Duration.parse(p.getText().trim());
    }
  }
}
