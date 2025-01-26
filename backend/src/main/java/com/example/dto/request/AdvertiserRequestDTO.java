package com.example.dto.request;

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
public class AdvertiserRequestDTO {

    @Size(min = 5, max = 250, message = "The Title must be at least 5 to 250 characters.")
    private String title;

    @Size(min = 5, max = 250, message = "The Description must be at least 5 to 250 characters.")
    private String description;

    @Size(min = 5, max = 250, message = "The BannerUrl must be at least 5 to 250 characters.")
    private String bannerUrl;

    @Size(min = 5, max = 250, message = "The Avatar Url must be at least 5 to 250 characters.")
    private String avatarUrl;

    @Size(min = 5, max = 250, message = "The Public Location must be at least 5 to 250 characters.")
    private String publicLocation;

    @Size(min = 5, max = 250, message = "The Public Url Location must be at least 5 to 250 characters.")
    private String publicUrlLocation;

    private boolean indexed;

    @UUID(message = "UUID format is required.")
    private String userId;
}
