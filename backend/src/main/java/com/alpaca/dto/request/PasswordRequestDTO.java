package com.alpaca.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Carries the three fields required to change a user's password: the current password for
 * verification, the new password, and a confirmation field.
 *
 * <p>The confirmation ({@code reNewPassword}) is expected to match {@code newPassword}; that
 * equality check is performed at the service layer rather than via Bean Validation.
 */
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
