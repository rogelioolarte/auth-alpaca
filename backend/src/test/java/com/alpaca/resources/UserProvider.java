package com.alpaca.resources;

import com.alpaca.dto.request.UserRequestDTO;
import com.alpaca.dto.response.UserResponseDTO;
import com.alpaca.entity.User;
import com.alpaca.entity.intermediate.UserRole;

import java.util.*;

public class UserProvider {

    public static User firstEntity = new User(
            UUID.fromString("1632eb79-63a4-4213-b905-0ad176f0004a"),
            "admin@admin.com","123456789",
            true, true, true,
            true, true, true,
            null, null, null);
    public static User secondEntity = new User(
            UUID.fromString("982a1001-b033-48f6-b2e6-6b327f0a61eb"),
            "user@user.com", "123456789",
            true, true, true,
            true, true, true,
            null, null, null);
    public static UserRole firstUserRole = new UserRole(
            firstEntity, RoleProvider.singleEntity());
    public static UserRole secondUserRole = new UserRole(
            secondEntity, RoleProvider.alternativeEntity());

    public static UserRequestDTO firstPReqDTO = new UserRequestDTO(
            "admin@admin.com","123456789",
            new HashSet<>(Set.of(RoleProvider.singleEntity().getId())));
    public static UserRequestDTO secondPReqDTO = new UserRequestDTO(
            "user@user.com", "123456789",
            new HashSet<>(Set.of(RoleProvider.alternativeEntity().getId())));

    public static UserResponseDTO firstPResDTO = new UserResponseDTO(
            UUID.fromString("1632eb79-63a4-4213-b905-0ad176f0004a"), "admin@admin.com",
            new ArrayList<>(List.of(RoleProvider.singleResponse())), null, null);
    public static UserResponseDTO secondPResDTO = new UserResponseDTO(
            UUID.fromString("982a1001-b033-48f6-b2e6-6b327f0a61eb"), "user@user.com",
            new ArrayList<>(List.of(RoleProvider.alternativeResponse())), null, null);

    public static List<User> listEntities() {
        firstEntity.setUserRoles(new HashSet<>(Set.of(firstUserRole)));
        secondEntity.setUserRoles(new HashSet<>(Set.of(secondUserRole)));
        return  new ArrayList<>(List.of(firstEntity, secondEntity));
    }

    public static List<UserRequestDTO> listRequest() {
        return List.of(firstPReqDTO, secondPReqDTO);
    }

    public static List<UserResponseDTO> listResponse() {
        return List.of(firstPResDTO, secondPResDTO);
    }

    public static User singleEntity() {
        firstEntity.setUserRoles(new HashSet<>(Set.of(firstUserRole)));
        return firstEntity;
    }

    public static User alternativeEntity() {
        secondEntity.setUserRoles(new HashSet<>(Set.of(secondUserRole)));
        return secondEntity;
    }

    public static UserRequestDTO singleRequest() {
        return firstPReqDTO;
    }

    public static UserRequestDTO alternativeRequest() {
        return secondPReqDTO;
    }

    public static UserResponseDTO singleResponse() {
        return firstPResDTO;
    }

    public static UserResponseDTO alternativeResponse() {
        return secondPResDTO;
    }
}
