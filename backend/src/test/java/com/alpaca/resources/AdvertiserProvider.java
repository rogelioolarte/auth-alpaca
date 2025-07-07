package com.alpaca.resources;

import com.alpaca.dto.request.AdvertiserRequestDTO;
import com.alpaca.dto.response.AdvertiserResponseDTO;
import com.alpaca.entity.Advertiser;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AdvertiserProvider {

    public static List<Advertiser> listEntities() {
        return new ArrayList<>(List.of(singleEntity(), alternativeEntity()));
    }

    public static List<AdvertiserRequestDTO> listRequest() {
        return new ArrayList<>(List.of(singleRequest(), alternativeRequest()));
    }

    public static List<AdvertiserResponseDTO> listResponse() {
        return new ArrayList<>(List.of(singleResponse(), alternativeResponse()));
    }

    public static Advertiser singleEntity() {
        Advertiser firstEntity =
                new Advertiser(
                        UUID.fromString("ca6e66dc-900e-42f5-8f4b-c81385d799c3"),
                        "Advertiser_1",
                        "Advertiser version 1",
                        "https://banner.com/advertiser1",
                        "https://avatar.com/advertiser1",
                        "av. root 123",
                        "https://location.com/advertiser1",
                        true,
                        true,
                        true,
                        null);
        firstEntity.setUser(UserProvider.singleEntity());
        return firstEntity;
    }

    public static Advertiser alternativeEntity() {
        Advertiser secondEntity =
                new Advertiser(
                        UUID.fromString("91a16ad9-2d6c-4482-9a5c-02f8948afa8d"),
                        "Advertiser_2",
                        "Advertiser version 2",
                        "https://banner.com/advertiser2",
                        "https://avatar.com/advertiser2",
                        "av. root 321",
                        "https://location.com/advertiser2",
                        true,
                        true,
                        true,
                        null);
        secondEntity.setUser(UserProvider.alternativeEntity());
        return secondEntity;
    }

    public static AdvertiserRequestDTO singleRequest() {
        return new AdvertiserRequestDTO(
                "Advertiser_1",
                "Advertiser version 1",
                "https://banner.com/advertiser1",
                "https://avatar.com/advertiser1",
                "av. root 123",
                "https://location.com/advertiser1",
                true,
                UserProvider.singleEntity().getId().toString());
    }

    public static AdvertiserRequestDTO alternativeRequest() {
        return new AdvertiserRequestDTO(
                "Advertiser_2",
                "Advertiser version 2",
                "https://banner.com/advertiser2",
                "https://avatar.com/advertiser2",
                "av. root 321",
                "https://location.com/advertiser2",
                true,
                UserProvider.alternativeEntity().getId().toString());
    }

    public static AdvertiserResponseDTO singleResponse() {
        return new AdvertiserResponseDTO(
                UUID.fromString("ca6e66dc-900e-42f5-8f4b-c81385d799c3"),
                "Advertiser_1",
                "Advertiser version 1",
                "https://banner.com/advertiser1",
                "https://avatar.com/advertiser1",
                "av. root 123",
                "https://location.com/advertiser1",
                true,
                true,
                true,
                UserProvider.singleResponse().id(),
                UserProvider.singleResponse().email());
    }

    public static AdvertiserResponseDTO alternativeResponse() {
        return new AdvertiserResponseDTO(
                UUID.fromString("91a16ad9-2d6c-4482-9a5c-02f8948afa8d"),
                "Advertiser_2",
                "Advertiser version 2",
                "https://banner.com/advertiser2",
                "https://avatar.com/advertiser2",
                "av. root 321",
                "https://location.com/advertiser2",
                true,
                true,
                true,
                UserProvider.alternativeResponse().id(),
                UserProvider.alternativeResponse().email());
    }
}
