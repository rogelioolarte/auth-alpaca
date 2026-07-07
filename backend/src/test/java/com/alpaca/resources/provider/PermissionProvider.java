package com.alpaca.resources.provider;

import com.alpaca.dto.request.PermissionRequestDTO;
import com.alpaca.dto.response.PermissionResponseDTO;
import com.alpaca.entity.Permission;
import org.springframework.data.domain.PageImpl;

import java.util.*;

public class PermissionProvider {

    public static List<Permission> listEntities() {
        return new ArrayList<>(
                List.of(
                        new Permission(singleEntity().getName()),
                        new Permission(alternativeEntity().getName())));
    }

    /**
     * public static List<PermissionRequestDTO> listRequest() { return new
     * ArrayList<>(List.of(singleRequest(), alternativeRequest())); }
     */
    public static List<PermissionResponseDTO> listResponse() {
        return new ArrayList<>(List.of(singleResponse(), alternativeResponse()));
    }

    public static PageImpl<PermissionResponseDTO> pageResponse() {
        return new PageImpl<>(listResponse());
    }

    public static PageImpl<Permission> pageEntities() {
        return new PageImpl<>(listEntities());
    }

    /**
     * public static Permission templateSingleEntity() { return new Permission(null, "TEST_CREATE",
     * new HashSet<>()); }
     *
     * <p>public static Permission templateAlternativeEntity() { return new Permission(null,
     * "TEST_READ", new HashSet<>()); }
     */
    public static Permission singleEntity() {
        return new Permission(
                UUID.fromString("b1f383ce-4c1e-4d0e-bb43-a9674377c4a2"),
                "TEST_CREATE",
                new HashSet<>());
    }

    public static Permission alternativeEntity() {
        return new Permission(
                UUID.fromString("c06f3206-c469-4216-bbc7-77fed3a8a133"),
                "TEST_READ",
                new HashSet<>());
    }

    public static Permission singleTemplate() {
        return Permission.builder()
                .name("TEST_CREATE")
                .rolePermissions(Collections.emptySet())
                .build();
    }

    public static Permission alternativeTemplate() {
        return Permission.builder()
                .name("TEST_READ")
                .rolePermissions(Collections.emptySet())
                .build();
    }

    /**
     * public static List<Permission> listTemplates() { return new
     * ArrayList<>(List.of(singleTemplate(), alternativeTemplate())); }
     */
    public static PermissionRequestDTO singleRequest() {
        return new PermissionRequestDTO("TEST_CREATE");
    }

    /**
     * public static PermissionRequestDTO alternativeRequest() { return new
     * PermissionRequestDTO("TEST_READ"); }
     */
    public static PermissionResponseDTO singleResponse() {
        return new PermissionResponseDTO(
                UUID.fromString("b1f383ce-4c1e-4d0e-bb43-a9674377c4a2"), "TEST_CREATE");
    }

    public static PermissionResponseDTO alternativeResponse() {
        return new PermissionResponseDTO(
                UUID.fromString("c06f3206-c469-4216-bbc7-77fed3a8a133"), "TEST_READ");
    }
}
