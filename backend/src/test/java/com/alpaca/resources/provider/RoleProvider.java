package com.alpaca.resources.provider;

import com.alpaca.dto.request.RoleRequestDTO;
import com.alpaca.dto.response.RoleResponseDTO;
import com.alpaca.entity.Role;
import org.springframework.data.domain.PageImpl;

import java.util.*;

public class RoleProvider {

    public static List<Role> listEntities() {
        return new ArrayList<>(List.of(singleEntity(), alternativeEntity()));
    }

    public static List<RoleResponseDTO> listResponse() {
        return new ArrayList<>(List.of(singleResponse(), alternativeResponse()));
    }

    public static PageImpl<RoleResponseDTO> pageResponse() {
        return new PageImpl<>(listResponse());
    }

    public static Role singleTemplate() {
        return Role.builder()
                .name("ADVISOR")
                .description("It's an advisor")
                .rolePermissions(new HashSet<>())
                .userRoles(new HashSet<>())
                .build();
    }

    public static Role alternativeTemplate() {
        return Role.builder()
                .name("TEST_USER")
                .description("It's an test user")
                .rolePermissions(new HashSet<>())
                .userRoles(new HashSet<>())
                .build();
    }

    public static Role singleEntity() {
        Role firstEntity =
                new Role(
                        UUID.fromString("e87ce3ba-fe71-4cf1-b302-94446a3684ca"),
                        "ADVISOR",
                        "It's an advisor",
                        new HashSet<>(),
                        new HashSet<>());
        firstEntity.setRolePermissions(new HashSet<>(Set.of(PermissionProvider.singleEntity())));
        return firstEntity;
    }

    public static Role alternativeEntity() {
        Role secondEntity =
                new Role(
                        UUID.fromString("33d6bb03-ae1c-4b0d-8f31-08095452bc40"),
                        "TEST_USER",
                        "It's an test user",
                        new HashSet<>(),
                        new HashSet<>());
        secondEntity.setRolePermissions(
                new HashSet<>(Set.of(PermissionProvider.alternativeEntity())));
        return secondEntity;
    }

    public static RoleRequestDTO singleRequest() {
        return new RoleRequestDTO(
                "ADVISOR",
                "It's an advisor",
                new HashSet<>(Set.of(PermissionProvider.singleEntity().getId())));
    }

    public static RoleResponseDTO singleResponse() {
        return new RoleResponseDTO(
                UUID.fromString("e87ce3ba-fe71-4cf1-b302-94446a3684ca"),
                "ADVISOR",
                "It's an advisor",
                new ArrayList<>(List.of(PermissionProvider.singleResponse())));
    }

    public static RoleResponseDTO alternativeResponse() {
        return new RoleResponseDTO(
                UUID.fromString("33d6bb03-ae1c-4b0d-8f31-08095452bc40"),
                "TEST_USER",
                "It's an test user",
                new ArrayList<>(List.of(PermissionProvider.alternativeResponse())));
    }
}
