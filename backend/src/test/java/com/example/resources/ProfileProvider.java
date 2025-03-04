package com.example.resources;

import com.example.dto.request.ProfileRequestDTO;
import com.example.dto.response.ProfileResponseDTO;
import com.example.entity.Profile;

import java.util.List;
import java.util.UUID;

public class ProfileProvider {

    public static Profile firstEntity = new Profile(
            UUID.fromString("fb2ba5e5-37c8-4645-994e-6a6953188801"),
            "Admin", "Last",
            "https://foto.admin.com", "av admin 01", null);
    public static Profile secondEntity = new Profile(
            UUID.fromString("3f95f801-160f-44a4-b8e0-81e29e9d83da"),
            "User", "Last", "https://foto.user.com",
            "av user 01", null);

    public static ProfileRequestDTO firstPReqDTO = new ProfileRequestDTO(
            "Admin", "Last",
            "https://foto.admin.com", "av admin 01",
            UserProvider.singleEntity().getId().toString());
    public static ProfileRequestDTO secondPReqDTO = new ProfileRequestDTO(
            "User", "Last", "https://foto.user.com",
            "av user 01", UserProvider.alternativeEntity().getId().toString());


    public static ProfileResponseDTO firstPResDTO = new ProfileResponseDTO(
            UUID.fromString("fb2ba5e5-37c8-4645-994e-6a6953188801"),
            "Admin", "Last", "https://foto.admin.com",
            "av admin 01", UserProvider.singleResponse().id(),
            UserProvider.singleResponse().email());
    public static ProfileResponseDTO secondPResDTO = new ProfileResponseDTO(
            UUID.fromString("3f95f801-160f-44a4-b8e0-81e29e9d83da"),
            "User", "Last", "https://foto.user.com",
            "av user 01", UserProvider.alternativeResponse().id(),
            UserProvider.alternativeResponse().email());

    public static List<Profile> listEntities() {
        return List.of(firstEntity, secondEntity);
    }

    public static List<ProfileRequestDTO> listRequest() {
        return List.of(firstPReqDTO, secondPReqDTO);
    }

    public static List<ProfileResponseDTO> listResponse() {
        return List.of(firstPResDTO, secondPResDTO);
    }

    public static Profile singleEntity() {
        firstEntity.setUser(UserProvider.singleEntity());
        return firstEntity;
    }

    public static Profile alternativeEntity() {
        secondEntity.setUser(UserProvider.alternativeEntity());
        return secondEntity;
    }

    public static ProfileRequestDTO singleRequest() {
        return firstPReqDTO;
    }

    public static ProfileRequestDTO alternativeRequest() {
        return secondPReqDTO;
    }

    public static ProfileResponseDTO singleResponse() {
        return firstPResDTO;
    }

    public static ProfileResponseDTO alternativeResponse() {
        return secondPResDTO;
    }
}
