package com.alpaca.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AuthRequestDTO {

    @Email(message = "Email must be valid.")
    @NotBlank(message = "Email is required.")
    private String email;

    @Size(min = 8, max = 200, message = "The password must be at least 8 to 200 characters.")
    @NotBlank(message = "Password is required.")
    private String password;
}
