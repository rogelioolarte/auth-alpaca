package com.example.qualifier;

import org.mapstruct.Qualifier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom qualifier annotation used to indicate that a method is responsible
 * for retrieving a single entity from the database.
 * <p>
 * This annotation is typically used in conjunction with MapStruct to
 * differentiate mapping methods that return a single entity.
 * </p>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>
 * {@code
 * @FindEntity
 * public Entity findById(Long id) {
 *     return entityRepository.findById(id)
 *         .orElseThrow(() -> new NotFoundException("Entity not found"));
 * }
 * }
 * </pre>
 *
 * @see org.mapstruct.Qualifier
 */
@Qualifier
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface FindEntity {
}
