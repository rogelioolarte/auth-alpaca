package com.alpaca.dto.response;

import java.util.UUID;

/**
 * Permission resource returned by the API. A permission represents a single fine-grained action the
 * system can authorize.
 *
 * @param id the permission's unique identifier
 * @param name the permission's unique name, e.g. {@code "user:create"}
 */
public record PermissionResponseDTO(UUID id, String name) {}
