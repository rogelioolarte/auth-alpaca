package com.alpaca.dto.request;

import com.alpaca.dto.request.groups.OnCreate;
import com.alpaca.dto.request.groups.OnUpdate;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload for creating or updating a user account.
 *
 * <p>Uses {@link com.alpaca.dto.request.groups.OnCreate} and {@link
 * com.alpaca.dto.request.groups.OnUpdate} validation groups to differentiate required fields:
 * {@code password} is mandatory on create but optional on update, while {@code email} and {@code
 * roles} are required in both cases.
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserRequestDTO {

    @Email(
            message = "Email must be valid.",
            groups = {OnCreate.class, OnUpdate.class})
    @NotBlank(
            message = "Email is required.",
            groups = {OnCreate.class, OnUpdate.class})
    private String email;

    @Size(
            min = 8,
            max = 200,
            message = "Password must be at least 8 to 200 characters.",
            groups = {OnCreate.class, OnUpdate.class})
    @NotBlank(message = "Password is required.", groups = OnCreate.class)
    private String password;

    @Size(
            min = 1,
            max = 3,
            message = "At least 1 Role is required",
            groups = {OnCreate.class, OnUpdate.class})
    @NotEmpty(
            message = "Roles are required.",
            groups = {OnCreate.class, OnUpdate.class})
    private Set<UUID> roles;
}
