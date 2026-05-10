package com.alpaca.resources;

import com.alpaca.dto.request.UserRequestDTO;
import com.alpaca.dto.response.UserResponseDTO;
import com.alpaca.entity.Profile;
import com.alpaca.entity.User;
import java.util.*;
import org.springframework.data.domain.PageImpl;

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

    public static PageImpl<UserResponseDTO> pageResponse() {
        return new PageImpl<>(listResponse());
    }

    public static PageImpl<User> pageEntities() {
        return new PageImpl<>(listEntities());
    }

    public static User singleTemplate() {
        return User.builder()
                .email("admin@admin.com")
                .password("123456789")
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialNonExpired(true)
                .emailVerified(false)
                .googleConnected(false)
                .userRoles(new HashSet<>())
                .refreshTokens(new HashSet<>())
                .sessions(new HashSet<>())
                .build();
    }

    public static User alternativeTemplate() {
        return User.builder()
                .email("user@user.com")
                .password("1234567890")
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialNonExpired(true)
                .emailVerified(false)
                .googleConnected(false)
                .userRoles(new HashSet<>())
                .refreshTokens(new HashSet<>())
                .sessions(new HashSet<>())
                .build();
    }

    public static User singleEntity() {
        return User.builder()
                .id(UUID.fromString("019e0f51-038c-7f79-96b5-be2e0b329111"))
                .email("admin@admin.com")
                .password("123456789")
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialNonExpired(true)
                .emailVerified(false)
                .googleConnected(false)
                .userRoles(new HashSet<>())
                .refreshTokens(new HashSet<>())
                .sessions(new HashSet<>())
                .build();
    }

    public static User alternativeEntity() {
        return User.builder()
                .id(UUID.fromString("019e0f52-a9da-7560-a196-359bbcf6571c"))
                .email("user@user.com")
                .password("1234567890")
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialNonExpired(true)
                .emailVerified(false)
                .googleConnected(false)
                .userRoles(new HashSet<>())
                .refreshTokens(new HashSet<>())
                .sessions(new HashSet<>())
                .build();
    }

    public static User notAllowEntity() {
        User secondEntity =
                User.builder()
                        .id(UUID.fromString("019b2092-e007-7671-a9fe-b2713081ea08"))
                        .email("user@user.com")
                        .password("123456789")
                        .enabled(false)
                        .accountNonExpired(false)
                        .accountNonLocked(false)
                        .credentialNonExpired(false)
                        .emailVerified(false)
                        .googleConnected(false)
                        .userRoles(new HashSet<>())
                        .refreshTokens(new HashSet<>())
                        .sessions(new HashSet<>())
                        .build();
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
                UUID.fromString("019e0f51-038c-7f79-96b5-be2e0b329111"),
                "admin@admin.com",
                new ArrayList<>(List.of(RoleProvider.singleResponse())),
                null,
                null);
    }

    public static UserResponseDTO alternativeResponse() {
        return new UserResponseDTO(
                UUID.fromString("019e0f52-a9da-7560-a196-359bbcf6571c"),
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

    /** Utility to create a User entity * */
    public static User createUser(
            boolean enabled,
            boolean accountNonExpired,
            boolean accountNonLocked,
            boolean credentialsNonExpired,
            boolean emailVerified,
            boolean googleConnected) {
        return User.builder()
                .email("test@example.com")
                .password("encodedPassword")
                .enabled(enabled)
                .accountNonExpired(accountNonExpired)
                .accountNonLocked(accountNonLocked)
                .credentialNonExpired(credentialsNonExpired)
                .emailVerified(emailVerified)
                .googleConnected(googleConnected)
                .userRoles(new HashSet<>())
                .refreshTokens(new HashSet<>())
                .sessions(new HashSet<>())
                .build();
    }
}
