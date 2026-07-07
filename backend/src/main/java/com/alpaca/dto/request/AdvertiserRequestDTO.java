package com.alpaca.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.UUID;

/**
 * Payload for creating or updating an advertiser profile: branding assets (title, description,
 * banner and avatar URLs), public-facing location info, and the indexing/publishing state.
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AdvertiserRequestDTO {

    @Size(min = 5, max = 250, message = "Title must be at least 5 to 250 characters.")
    @NotBlank(message = "Title is required.")
    private String title;

    @Size(min = 5, max = 250, message = "Description must be at least 5 to 250 characters.")
    @NotBlank(message = "Description is required.")
    private String description;

    @Size(min = 5, max = 250, message = "Banner URL must be at least 5 to 250 characters.")
    @NotBlank(message = "Banner URL is required.")
    private String bannerUrl;

    @Size(min = 5, max = 250, message = "Avatar URL must be at least 5 to 250 characters.")
    @NotBlank(message = "Avatar URL is required.")
    private String avatarUrl;

    @Size(min = 5, max = 250, message = "Public Location must be at least 5 to 250 characters.")
    @NotBlank(message = "Public Location is required.")
    private String publicLocation;

    @Size(min = 5, max = 250, message = "Public URL Location must be at least 5 to 250 characters.")
    @NotBlank(message = "Public URL Location is required.")
    private String publicUrlLocation;

    @NotNull(message = "Indexed State is required.")
    private boolean indexed;

    @UUID(
            message = "UUID format is required.",
            allowNil = false,
            version = {7})
    @NotBlank(message = "User is required.")
    private String userId;
}
