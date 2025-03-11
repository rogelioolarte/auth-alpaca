package com.alpaca.resources;

import com.alpaca.dto.request.UserRequestDTO;
import com.alpaca.dto.response.UserResponseDTO;
import com.alpaca.entity.User;
import com.alpaca.entity.intermediate.UserRole;

import java.util.*;

public class UserProvider {

    public static List<User> listEntities() {
        return new ArrayList<>(List.of(singleEntity(), alternativeEntity()));
    }

    public static List<UserRequestDTO> listRequest() {
        return new ArrayList<>(List.of(singleRequest(), alternativeRequest()));
    }

    public static List<UserResponseDTO> listResponse() {
        return new ArrayList<>(List.of(singleResponse(), alternativeResponse()));
    }

    public static User singleEntity() {
        User firstEntity = new User(
                UUID.fromString("1632eb79-63a4-4213-b905-0ad176f0004a"),
                "admin@admin.com","123456789",
                true, true, true,
                true, true, true,
                null, null, null);
        UserRole firstUserRole = new UserRole(
                firstEntity, RoleProvider.singleEntity());
        firstEntity.setUserRoles(new HashSet<>(Set.of(firstUserRole)));
        return firstEntity;
    }

    public static User alternativeEntity() {
        User secondEntity = new User(
                UUID.fromString("982a1001-b033-48f6-b2e6-6b327f0a61eb"),
                "user@user.com", "123456789",
                true, true, true,
                true, true, true,
                null, null, null);
        UserRole secondUserRole = new UserRole(
                secondEntity, RoleProvider.alternativeEntity());
        secondEntity.setUserRoles(new HashSet<>(Set.of(secondUserRole)));
        return secondEntity;
    }

    public static UserRequestDTO singleRequest() {
        return new UserRequestDTO(
                "admin@admin.com","123456789",
                new HashSet<>(Set.of(RoleProvider.singleEntity().getId())));
    }

    public static UserRequestDTO alternativeRequest() {
        return new UserRequestDTO(
                "user@user.com", "123456789",
                new HashSet<>(Set.of(RoleProvider.alternativeEntity().getId())));
    }

    public static UserResponseDTO singleResponse() {
        return new UserResponseDTO(
                UUID.fromString("1632eb79-63a4-4213-b905-0ad176f0004a"),
                "admin@admin.com",
                new ArrayList<>(List.of(RoleProvider.singleResponse())),
                null, null);
    }

    public static UserResponseDTO alternativeResponse() {
        return new UserResponseDTO(
                UUID.fromString("982a1001-b033-48f6-b2e6-6b327f0a61eb"),
                "user@user.com",
                new ArrayList<>(List.of(RoleProvider.alternativeResponse())),
                null, null);
    }
}
