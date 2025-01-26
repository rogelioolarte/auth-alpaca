package com.example.dto.response;

import java.util.Set;
import java.util.UUID;

public record RoleResponseDTO(
        UUID id,
        String roleName,
        String roleDescription,
        Set<PermissionResponseDTO> permissions
) {
}
