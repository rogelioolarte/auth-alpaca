package com.example.dto.response;

import java.util.UUID;

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
        String username
) {
}
