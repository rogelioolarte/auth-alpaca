package com.alpaca.dto.response;

import java.util.UUID;

/**
 * Advertiser profile returned by the API, including branding assets, location info, and
 * administrative flags ({@code indexed}, {@code paid}, {@code verified}).
 *
 * <p>Includes the owning user's email for convenience.
 *
 * @param id the advertiser profile's unique identifier
 * @param title the advertiser's display title
 * @param description the advertiser's description text
 * @param bannerUrl the URL to the advertiser's banner image
 * @param avatarUrl the URL to the advertiser's avatar image
 * @param publicLocation the advertiser's public location description
 * @param publicUrlLocation the advertiser's public website URL
 * @param indexed whether the profile is included in public listings
 * @param paid whether the advertiser has an active paid subscription
 * @param verified whether the advertiser has been verified
 * @param userId the owning user's unique identifier
 * @param email the owning user's email address
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
