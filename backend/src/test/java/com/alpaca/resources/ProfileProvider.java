package com.alpaca.resources;

import com.alpaca.dto.request.ProfileRequestDTO;
import com.alpaca.dto.response.ProfileResponseDTO;
import com.alpaca.entity.Profile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageImpl;

public class ProfileProvider {

    public static List<Profile> listEntities() {
        return new ArrayList<>(List.of(singleEntity(), alternativeEntity()));
    }

    public static List<ProfileRequestDTO> listRequest() {
        return new ArrayList<>(List.of(singleRequest(), alternativeRequest()));
    }

    public static List<ProfileResponseDTO> listResponse() {
        return new ArrayList<>(List.of(singleResponse(), alternativeResponse()));
    }

    public static PageImpl<ProfileResponseDTO> pageResponse() {
        return new PageImpl<ProfileResponseDTO>(listResponse());
    }

    public static PageImpl<Profile> pageEntities() {
        return new PageImpl<Profile>(listEntities());
    }

    public static Profile singleTemplate() {
        return new Profile(null, "Admin", "Last", "av admin 01", "https://foto.admin.com", null);
    }

    public static Profile singleEntity() {
        return new Profile(
                UUID.fromString("fb2ba5e5-37c8-4645-994e-6a6953188801"),
                "Admin",
                "Last",
                "av admin 01",
                "https://foto.admin.com",
                UserProvider.singleEntity());
    }

    public static Profile alternativeEntity() {
        return new Profile(
                UUID.fromString("3f95f801-160f-44a4-b8e0-81e29e9d83da"),
                "User",
                "Last",
                "av user 01",
                "https://foto.user.com",
                UserProvider.alternativeEntity());
    }

    public static ProfileRequestDTO singleRequest() {
        return new ProfileRequestDTO(
                "Admin",
                "Last",
                "av admin 01",
                "https://foto.admin.com",
                UserProvider.singleEntity().getId().toString());
    }

    public static ProfileRequestDTO alternativeRequest() {
        return new ProfileRequestDTO(
                "User",
                "Last",
                "av user 01",
                "https://foto.user.com",
                UserProvider.alternativeEntity().getId().toString());
    }

    public static ProfileResponseDTO singleResponse() {
        return new ProfileResponseDTO(
                UUID.fromString("fb2ba5e5-37c8-4645-994e-6a6953188801"),
                "Admin",
                "Last",
                "av admin 01",
                "https://foto.admin.com",
                UserProvider.singleResponse().id(),
                UserProvider.singleResponse().email());
    }

    public static ProfileResponseDTO alternativeResponse() {
        return new ProfileResponseDTO(
                UUID.fromString("3f95f801-160f-44a4-b8e0-81e29e9d83da"),
                "User",
                "Last",
                "av user 01",
                "https://foto.user.com",
                UserProvider.alternativeResponse().id(),
                UserProvider.alternativeResponse().email());
    }
}
