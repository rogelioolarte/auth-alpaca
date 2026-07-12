package com.alpaca.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.hibernate.annotations.IdGeneratorType;

/**
 * Annotation to mark an entity field as using the UUID Version 7 generation strategy.
 *
 * <p>This annotation serves as a specialized shortcut for the combination of
 * {@code @GeneratedValue} and {@code @GenericGenerator} when utilizing the custom {@link
 * UUIDv7Generator}. It leverages the {@code @IdGeneratorType} meta-annotation introduced in
 * Hibernate 6+ to define the generation logic concisely.
 *
 * <p>Applying this annotation to an ID field ensures that IDs are generated based on the
 * time-ordered UUID v7 specification, which significantly improves database index performance
 * (insertions are sequential rather than random).
 */
@IdGeneratorType(UUIDv7Generator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface GeneratorUUIDv7 {}
