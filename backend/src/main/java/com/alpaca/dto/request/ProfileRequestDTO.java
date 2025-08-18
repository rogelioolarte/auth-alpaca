package com.alpaca.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.UUID;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ProfileRequestDTO {

    @Size(min = 2, max = 250, message = "First Name must be at least 5 to 250 characters.")
    @NotBlank(message = "First Name is required.")
    private String firstName;

    @Size(min = 2, max = 250, message = "Last Name must be at least 5 to 250 characters.")
    @NotBlank(message = "Last Name is required.")
    private String lastName;

    @Size(min = 5, max = 250, message = "Address must be at least 5 to 250 characters.")
    @NotBlank(message = "Address is required.")
    private String address;

    @Pattern(regexp = "^(http://|https://).*", message = "URL must be valid.")
    @Size(min = 7, max = 250, message = "URL must be at least 7 to 250 characters.")
    @NotBlank(message = "Avatar URL is required.")
    private String avatarUrl;

    @UUID(message = "UUID format is required.")
    @NotBlank(message = "User is required.")
    private String userId;
}
