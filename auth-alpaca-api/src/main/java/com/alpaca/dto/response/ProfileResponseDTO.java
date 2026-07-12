package com.alpaca.dto.response;

import java.util.UUID;

/**
 * User profile returned by the API.
 *
 * <p>Includes the user's email alongside profile fields for convenience, avoiding an extra lookup
 * by the consumer.
 *
 * @param id the profile's unique identifier
 * @param firstName the user's first name
 * @param lastName the user's last name
 * @param address the user's physical address
 * @param avatarUrl the URL to the user's avatar image
 * @param userId the owning user's unique identifier
 * @param email the owning user's email address
 */
public record ProfileResponseDTO(
        UUID id,
        String firstName,
        String lastName,
        String address,
        String avatarUrl,
        UUID userId,
        String email) {}
