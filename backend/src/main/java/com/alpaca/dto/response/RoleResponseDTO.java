package com.alpaca.dto.response;

import java.util.List;
import java.util.UUID;

public record RoleResponseDTO(
        UUID id, String name, String description, List<PermissionResponseDTO> permissions) {}
