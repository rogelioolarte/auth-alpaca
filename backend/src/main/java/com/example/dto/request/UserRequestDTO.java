package com.example.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserRequestDTO {

    @Email(message = "Email must be valid.")
    private String username;

    @Size(min = 8, max = 200,message = "The password must be at least 8 to 200 characters.")
    private String password;

    @Size(min = 1, max = 3, message = "At least 1 role is required")
    private Set<UUID> roles;
}
