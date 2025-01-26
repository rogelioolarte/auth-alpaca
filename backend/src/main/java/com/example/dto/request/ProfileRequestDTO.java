package com.example.dto.request;

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

    @Size(min = 5, max = 250, message = "The First Name must be at least 5 to 250 characters.")
    private String firstName;

    @Size(min = 5, max = 250, message = "The Last Name must be at least 5 to 250 characters.")
    private String lastName;

    @Size(min = 5, max = 250, message = "The Address must be at least 5 to 250 characters.")
    private String address;

    @Pattern(regexp = "^(http://|https://).*", message = "The URL must be valid.")
    @Size(min = 7, max = 250, message = "The URL must be at least 7 to 250 characters.")
    private String avatarUrl;

    @UUID(message = "UUID format is required.")
    private String userId;
}
