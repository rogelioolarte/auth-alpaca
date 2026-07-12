package com.alpaca.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * Full user representation returned by the API, including nested roles, personal profile, and
 * advertiser profile (if any).
 *
 * @param id the user's unique identifier
 * @param email the user's email address
 * @param roles the roles assigned to this user
 * @param profile the user's personal profile, or {@code null} if not set
 * @param advertiser the user's advertiser profile, or {@code null} if not an advertiser
 */
public record UserResponseDTO(
        UUID id,
        String email,
        List<RoleResponseDTO> roles,
        ProfileResponseDTO profile,
        AdvertiserResponseDTO advertiser) {}
