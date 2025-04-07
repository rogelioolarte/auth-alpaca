package com.alpaca.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
  @NotBlank(message = "Email is required.")
  private String email;

  @Size(min = 8, max = 200, message = "Password must be at least 8 to 200 characters.")
  @NotBlank(message = "Password is required.")
  private String password;

  @Size(min = 1, max = 3, message = "At least 1 Role is required")
  @NotEmpty(message = "Roles are required.")
  private Set<UUID> roles;
}
