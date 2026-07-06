package com.alpaca.dto.response;

import java.util.UUID;

/**
 * Permission resource returned by the API. A permission represents a single fine-grained action the
 * system can authorize.
 */
public record PermissionResponseDTO(UUID id, String name) {}
