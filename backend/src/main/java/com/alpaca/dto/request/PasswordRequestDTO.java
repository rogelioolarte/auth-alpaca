package com.alpaca.dto.request;

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
public class PasswordRequestDTO {

    @Size(min = 8, max = 200, message = "CurrentPassword must be at least 8 to 200 characters.")
    String currentPassword;

    @Size(min = 8, max = 200, message = "NewPassword must be at least 8 to 200 characters.")
    @NotBlank(message = "NewPassword is required.")
    String newPassword;

    @Size(min = 8, max = 200, message = "ReNewPassword must be at least 8 to 200 characters.")
    @NotBlank(message = "ReNewPassword is required.")
    String reNewPassword;
}
