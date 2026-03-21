package com.paragon.responses.json;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class PolymorphicTypeMetadata {

  private final String propertyName;
  private final List<Branch> branches;

  private PolymorphicTypeMetadata(String propertyName, List<Branch> branches) {
    this.propertyName = propertyName;
    this.branches = List.copyOf(branches);
  }

  static Optional<PolymorphicTypeMetadata> resolve(Class<?> type) {
    JsonTypeInfo typeInfo = type.getAnnotation(JsonTypeInfo.class);
    if (typeInfo == null || typeInfo.use() != JsonTypeInfo.Id.NAME) {
      return Optional.empty();
    }

    if (typeInfo.include() != JsonTypeInfo.As.PROPERTY
        && typeInfo.include() != JsonTypeInfo.As.EXISTING_PROPERTY) {
      throw new IllegalArgumentException(
          "Unsupported JsonTypeInfo inclusion mode for structured output: "
              + type.getName()
              + " uses "
              + typeInfo.include());
    }

    JsonSubTypes subTypes = type.getAnnotation(JsonSubTypes.class);
    if (subTypes == null || subTypes.value().length == 0) {
      throw new IllegalArgumentException(
          "Polymorphic structured output requires @JsonSubTypes on " + type.getName());
    }

    String propertyName = typeInfo.property();
    if (propertyName == null || propertyName.isBlank()) {
      propertyName = typeInfo.use().getDefaultPropertyName();
    }
    if (propertyName == null || propertyName.isBlank()) {
      throw new IllegalArgumentException(
          "Polymorphic structured output requires a discriminator property on " + type.getName());
    }

    List<Branch> branches = new ArrayList<>(subTypes.value().length);
    for (JsonSubTypes.Type subType : subTypes.value()) {
      Class<?> subtypeClass = subType.value();
      String typeId = subType.name();
      if (typeId == null || typeId.isBlank()) {
        JsonTypeName jsonTypeName = subtypeClass.getAnnotation(JsonTypeName.class);
        typeId = jsonTypeName != null ? jsonTypeName.value() : null;
      }
      if (typeId == null || typeId.isBlank()) {
        throw new IllegalArgumentException(
            "Polymorphic structured output requires a subtype id for "
                + subtypeClass.getName()
                + " in "
                + type.getName());
      }
      branches.add(new Branch(typeId, subtypeClass));
    }

    return Optional.of(new PolymorphicTypeMetadata(propertyName, branches));
  }

  String propertyName() {
    return propertyName;
  }

  List<Branch> branches() {
    return branches;
  }

  record Branch(String typeId, Class<?> subtypeClass) {}
}
