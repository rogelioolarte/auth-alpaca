package com.alpaca.dto.response;

import java.util.UUID;

/**
 * User profile returned by the API.
 *
 * <p>Includes the user's email alongside profile fields for convenience, avoiding an extra lookup
 * by the consumer.
 */
public record ProfileResponseDTO(
        UUID id,
        String firstName,
        String lastName,
        String address,
        String avatarUrl,
        UUID userId,
        String email) {}
