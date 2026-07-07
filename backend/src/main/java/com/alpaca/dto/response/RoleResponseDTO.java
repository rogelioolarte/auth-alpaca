package com.alpaca.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * Role resource returned by the API, including its display name, description, and the list of
 * permissions it grants.
 *
 * @param id the role's unique identifier
 * @param name the role's display name
 * @param description a description of what the role grants
 * @param permissions the permissions assigned to this role
 */
public record RoleResponseDTO(
        UUID id, String name, String description, List<PermissionResponseDTO> permissions) {}
