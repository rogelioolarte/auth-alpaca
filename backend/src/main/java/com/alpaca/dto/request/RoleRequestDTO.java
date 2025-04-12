package com.alpaca.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RoleRequestDTO {

  @Size(min = 4, max = 25, message = "Role Name must be between 4 and 25 characters.")
  @NotBlank(message = "Role Name is required.")
  private String roleName;

  @Size(min = 5, max = 250, message = "Role Description must be between 5 and 250 characters.")
  @NotBlank(message = "Role Description is required.")
  private String roleDescription;

  @Size(min = 1, message = "At least 1 Permission is required.")
  @NotEmpty(message = "Permissions are required.")
  private Set<UUID> permissions;
}
