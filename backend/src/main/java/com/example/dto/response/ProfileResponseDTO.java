package com.example.dto.response;

import java.util.UUID;

public record ProfileResponseDTO(
        UUID id,
        String firstName,
        String lastName,
        String address,
        String avatarUrl,
        UUID userId,
        String username
) {
}
