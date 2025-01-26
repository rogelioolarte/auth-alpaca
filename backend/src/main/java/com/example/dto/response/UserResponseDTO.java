package com.example.dto.response;

import java.util.Set;
import java.util.UUID;

public record UserResponseDTO(
        UUID id,
        String username,
        Set<RoleResponseDTO> roles,
        ProfileResponseDTO profile,
        AdvertiserResponseDTO advertiser
) {
}
