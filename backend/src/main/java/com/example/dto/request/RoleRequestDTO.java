package com.example.dto.request;

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
public class RoleRequestDTO {

    @Size(min = 4, max = 25, message = "The Role Name must be between 4 and 25 characters.")
    private String roleName;

    @Size(min = 5, max = 250, message = "The Role Description must be between 5 and 250 characters.")
    private String roleDescription;

    @Size(min = 1, message = "At least 1 permit is required.")
    private Set<UUID> permissions;
}
