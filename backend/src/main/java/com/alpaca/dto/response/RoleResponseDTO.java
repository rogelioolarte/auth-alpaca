package com.alpaca.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * Role resource returned by the API, including its display name, description, and the list of
 * permissions it grants.
 */
public record RoleResponseDTO(
        UUID id, String name, String description, List<PermissionResponseDTO> permissions) {}
