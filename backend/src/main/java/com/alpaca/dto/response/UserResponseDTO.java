package com.alpaca.dto.response;

import java.util.List;
import java.util.UUID;

public record UserResponseDTO(
        UUID id,
        String email,
        List<RoleResponseDTO> roles,
        ProfileResponseDTO profile,
        AdvertiserResponseDTO advertiser) {}
