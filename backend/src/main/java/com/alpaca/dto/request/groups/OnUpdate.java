package com.alpaca.dto.request.groups;

/**
 * Bean Validation group activated during resource <em>update</em>.
 *
 * <p>Apply this marker to constraints that apply when an existing entity is being modified, where
 * some fields may be optional or have different rules compared to creation.
 *
 * @see jakarta.validation.groups.Default
 */
public interface OnUpdate {}
