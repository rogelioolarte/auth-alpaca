package com.alpaca.resources;

import com.alpaca.dto.request.RoleRequestDTO;
import com.alpaca.dto.response.PermissionResponseDTO;
import com.alpaca.dto.response.RoleResponseDTO;
import com.alpaca.entity.Permission;
import com.alpaca.entity.Role;
import org.springframework.data.domain.PageImpl;

import java.util.*;

public class RoleProvider {

    public static List<Role> listEntities() {
        return new ArrayList<>(List.of(singleEntity(), alternativeEntity()));
    }

    public static List<RoleRequestDTO> listRequest() {
        return new ArrayList<>(List.of(singleRequest(), alternativeRequest()));
    }

    public static List<RoleResponseDTO> listResponse() {
        return new ArrayList<>(List.of(singleResponse(), alternativeResponse()));
    }

    public static PageImpl<RoleResponseDTO> pageResponse() {
      return new PageImpl<RoleResponseDTO>(listResponse());
    }

    public static PageImpl<Role> pageEntities() {
      return new PageImpl<Role>(listEntities());
    }

    public static Role singleTemplate() {
      return new Role(null, "ADMIN", "It's an admin", null, null);
    }

    public static Role singleEntity() {
        Role firstEntity =
                new Role(
                        UUID.fromString("e87ce3ba-fe71-4cf1-b302-94446a3684ca"),
                        "ADMIN",
                        "It's an admin",
                        null,
                        null);
        firstEntity.setRolePermissions(new HashSet<>(Set.of(PermissionProvider.singleEntity())));
        return firstEntity;
    }

    public static Role alternativeEntity() {
        Role secondEntity =
                new Role(
                        UUID.fromString("33d6bb03-ae1c-4b0d-8f31-08095452bc40"),
                        "USER",
                        "It's an user",
                        null,
                        null);
        secondEntity.setRolePermissions(
                new HashSet<>(Set.of(PermissionProvider.alternativeEntity())));
        return secondEntity;
    }

    public static RoleRequestDTO singleRequest() {
        return new RoleRequestDTO(
                "ADMIN",
                "It's an admin",
                new HashSet<>(Set.of(PermissionProvider.singleEntity().getId())));
    }

    public static RoleRequestDTO alternativeRequest() {
        return new RoleRequestDTO(
                "USER",
                "It's an user",
                new HashSet<>(Set.of(PermissionProvider.alternativeEntity().getId())));
    }

    public static RoleResponseDTO singleResponse() {
        return new RoleResponseDTO(
                UUID.fromString("e87ce3ba-fe71-4cf1-b302-94446a3684ca"),
                "ADMIN",
                "It's an admin",
                new ArrayList<>(List.of(PermissionProvider.singleResponse())));
    }

    public static RoleResponseDTO alternativeResponse() {
        return new RoleResponseDTO(
                UUID.fromString("33d6bb03-ae1c-4b0d-8f31-08095452bc40"),
                "USER",
                "It's an user",
                new ArrayList<>(List.of(PermissionProvider.alternativeResponse())));
    }
}
