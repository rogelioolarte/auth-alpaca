package com.alpaca.resources;

import com.alpaca.dto.request.UserRequestDTO;
import com.alpaca.dto.response.UserResponseDTO;
import com.alpaca.entity.Profile;
import com.alpaca.entity.User;
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

    public static User singleTemplate() {
        return new User(
                null,
                "admin@admin.com",
                "123456789",
                true,
                true,
                true,
                true,
                false,
                false,
                new HashSet<>(),
                null,
                null);
    }

    public static User alternativeTemplate() {
        return new User(
            null,
            "user@user.com",
            "1234567890",
            true,
            true,
            true,
            true,
            false,
            false,
            new HashSet<>(),
            null,
            null);
    }

    public static User singleEntity() {
        return new User(
                UUID.fromString("1632eb79-63a4-4213-b905-0ad176f0004a"),
                "admin@admin.com",
                "123456789",
                true,
                true,
                true,
                true,
                false,
                false,
                new HashSet<>(),
                null,
                null);
    }

    public static User alternativeEntity() {
        return new User(
                UUID.fromString("982a1001-b033-48f6-b2e6-6b327f0a61eb"),
                "user@user.com",
                "1234567890",
                true,
                true,
                true,
                true,
                false,
                false,
                new HashSet<>(),
                null,
                null);
    }

    public static User completeEntity() {
        User secondEntity =
                new User(
                        UUID.fromString("982a1001-b033-48f6-b2e6-6b327f0a61eb"),
                        "user@user.com",
                        "123456789",
                        true,
                        true,
                        true,
                        true,
                        false,
                        false,
                        new HashSet<>(),
                        null,
                        null);
        secondEntity.setUserRoles(
                new HashSet<>(
                        Set.of(RoleProvider.alternativeEntity(), RoleProvider.singleEntity())));
        return secondEntity;
    }

    public static User notAllowEntity() {
        User secondEntity =
                new User(
                        UUID.fromString("982a1001-b033-48f6-b2e6-6b327f0a61eb"),
                        "user@user.com",
                        "123456789",
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        new HashSet<>(),
                        null,
                        null);
        secondEntity.setUserRoles(new HashSet<>(Set.of(RoleProvider.alternativeEntity())));
        return secondEntity;
    }

    public static UserRequestDTO singleRequest() {
        return new UserRequestDTO(
                "admin@admin.com",
                "123456789",
                new HashSet<>(Set.of(RoleProvider.singleEntity().getId())));
    }

    public static UserRequestDTO alternativeRequest() {
        return new UserRequestDTO(
                "user@user.com",
                "123456789",
                new HashSet<>(Set.of(RoleProvider.alternativeEntity().getId())));
    }

    public static UserResponseDTO singleResponse() {
        return new UserResponseDTO(
                UUID.fromString("1632eb79-63a4-4213-b905-0ad176f0004a"),
                "admin@admin.com",
                new ArrayList<>(List.of(RoleProvider.singleResponse())),
                null,
                null);
    }

    public static UserResponseDTO alternativeResponse() {
        return new UserResponseDTO(
                UUID.fromString("982a1001-b033-48f6-b2e6-6b327f0a61eb"),
                "user@user.com",
                new ArrayList<>(List.of(RoleProvider.alternativeResponse())),
                null,
                null);
    }

    /** Utility to create an object of oauth2 attributes * */
    public static Map<String, Object> createAttributes(
            User user, Profile profile, boolean emailVerified) {
        return Map.of(
                "sub", user.getId().toString(),
                "email", user.getEmail(),
                "name", profile.getFirstName(),
                "given_name", (profile.getFirstName() + " " + profile.getLastName()),
                "family_name", profile.getLastName(),
                "picture", profile.getAvatarUrl(),
                "email_verified", emailVerified);
    }

    public static String getEmail() {
        return "test@example.com";
    }

    public static String getPassword() {
        return "encodedPassword";
    }

    /** Utility to create a User entity * */
    public static User createUser(
            boolean enabled,
            boolean accountNonExpired,
            boolean accountNonLocked,
            boolean credentialsNonExpired,
            boolean emailVerified,
            boolean googleConnected) {
        return new User(
                getEmail(),
                getPassword(),
                enabled,
                accountNonExpired,
                accountNonLocked,
                credentialsNonExpired,
                emailVerified,
                googleConnected,
                new HashSet<>());
    }
}
