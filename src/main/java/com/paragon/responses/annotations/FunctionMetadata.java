package com.paragon.responses.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to define function tool metadata. Apply this to FunctionTool subclasses to specify
 * name and description.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface FunctionMetadata {
  /** The name of the function tool. */
  String name();

  /** The description of the function tool. */
  String description() default "";
}
