package com.alpaca.dto.request.groups;

/**
 * Bean Validation group activated during resource <em>creation</em>.
 *
 * <p>Apply this marker to constraints that must be enforced when a new entity is being created but
 * may be relaxed during updates (e.g. a required password on user registration).
 *
 * @see jakarta.validation.groups.Default
 */
public interface OnCreate {}
