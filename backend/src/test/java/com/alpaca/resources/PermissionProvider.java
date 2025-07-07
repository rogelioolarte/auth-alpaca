package com.alpaca.resources;

import com.alpaca.dto.request.PermissionRequestDTO;
import com.alpaca.dto.response.PermissionResponseDTO;
import com.alpaca.entity.Permission;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class PermissionProvider {

    public static List<Permission> listEntities() {
        return new ArrayList<>(
                List.of(
                        new Permission(singleEntity().getPermissionName()),
                        new Permission(alternativeEntity().getPermissionName())));
    }

    public static List<PermissionRequestDTO> listRequest() {
        return new ArrayList<>(List.of(singleRequest(), alternativeRequest()));
    }

    public static List<PermissionResponseDTO> listResponse() {
        return new ArrayList<>(List.of(singleResponse(), alternativeResponse()));
    }

    public static Permission templateSingleEntity() {
        return new Permission(null, "DELETE", new HashSet<>());
    }

    public static Permission templateAlternativeEntity() {
        return new Permission(null, "CREATE", new HashSet<>());
    }

    public static Permission singleEntity() {
        return new Permission(
                UUID.fromString("b1f383ce-4c1e-4d0e-bb43-a9674377c4a2"), "DELETE", new HashSet<>());
    }

    public static Permission alternativeEntity() {
        return new Permission(
                UUID.fromString("c06f3206-c469-4216-bbc7-77fed3a8a133"), "CREATE", new HashSet<>());
    }

    public static PermissionRequestDTO singleRequest() {
        return new PermissionRequestDTO("CREATE");
    }

    public static PermissionRequestDTO alternativeRequest() {
        return new PermissionRequestDTO("READ");
    }

    public static PermissionResponseDTO singleResponse() {
        return new PermissionResponseDTO(
                UUID.fromString("b1f383ce-4c1e-4d0e-bb43-a9674377c4a2"), "DELETE");
    }

    public static PermissionResponseDTO alternativeResponse() {
        return new PermissionResponseDTO(
                UUID.fromString("c06f3206-c469-4216-bbc7-77fed3a8a133"), "CREATE");
    }
}
