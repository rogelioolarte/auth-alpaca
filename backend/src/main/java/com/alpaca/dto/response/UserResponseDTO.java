package com.alpaca.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * Full user representation returned by the API, including nested roles, personal profile, and
 * advertiser profile (if any).
 */
public record UserResponseDTO(
        UUID id,
        String email,
        List<RoleResponseDTO> roles,
        ProfileResponseDTO profile,
        AdvertiserResponseDTO advertiser) {}
