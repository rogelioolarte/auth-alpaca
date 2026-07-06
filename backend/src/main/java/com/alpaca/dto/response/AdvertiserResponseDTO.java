package com.alpaca.dto.response;

import java.util.UUID;

/**
 * Advertiser profile returned by the API, including branding assets, location info, and
 * administrative flags ({@code indexed}, {@code paid}, {@code verified}).
 *
 * <p>Includes the owning user's email for convenience.
 */
public record AdvertiserResponseDTO(
        UUID id,
        String title,
        String description,
        String bannerUrl,
        String avatarUrl,
        String publicLocation,
        String publicUrlLocation,
        boolean indexed,
        boolean paid,
        boolean verified,
        UUID userId,
        String email) {}
