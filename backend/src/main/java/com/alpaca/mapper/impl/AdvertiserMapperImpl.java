package com.alpaca.mapper.impl;

import com.alpaca.dto.request.AdvertiserRequestDTO;
import com.alpaca.dto.response.AdvertiserResponseDTO;
import com.alpaca.entity.Advertiser;
import com.alpaca.mapper.IAdvertiserMapper;
import com.alpaca.service.IUserService;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementation of the {@link IAdvertiserMapper} interface, responsible for converting between
 * {@link Advertiser} entities and their corresponding request and response DTOs.
 *
 * <p>This mapper relies on {@link IUserService} to resolve the associated User entity based on ID
 * provided in the request DTO during conversion to an Advertiser entity.
 *
 * <p>Primary responsibilities:
 *
 * <ul>
 *   <li>Convert a {@link Advertiser} entity into an {@link AdvertiserResponseDTO}.
 *   <li>Construct an {@link Advertiser} entity from an {@link AdvertiserRequestDTO}, initializing
 *       default flags and resolving the associated User via {@link IUserService}.
 *   <li>Transform collections of Advertiser entities into lists of {@link AdvertiserResponseDTO}.
 *   <li>Handle null or empty inputs gracefully by returning null or empty lists respectively.
 * </ul>
 *
 * <p>The class is annotated with {@link Component} for Spring integration, and leverages Lombok's
 * {@link RequiredArgsConstructor} for constructor-based dependency injection.
 *
 * @see Advertiser
 * @see AdvertiserRequestDTO
 * @see AdvertiserResponseDTO
 * @see IUserService
 */
@Component
@RequiredArgsConstructor
public class AdvertiserMapperImpl implements IAdvertiserMapper {

    private final IUserService userService;

    /**
     * Maps an {@link Advertiser} entity to an {@link AdvertiserResponseDTO}.
     *
     * @param entity the Advertiser entity; may be {@code null}
     * @return the corresponding DTO, or {@code null} if the input entity is {@code null}
     */
    @Override
    public AdvertiserResponseDTO toResponseDTO(Advertiser entity) {
        if (entity == null) return null;
        return new AdvertiserResponseDTO(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getBannerUrl(),
                entity.getAvatarUrl(),
                entity.getPublicLocation(),
                entity.getPublicUrlLocation(),
                entity.isIndexed(),
                entity.isPaid(),
                entity.isVerified(),
                entity.getUser().getId(),
                entity.getUser().getEmail());
    }

    /**
     * Converts an {@link AdvertiserRequestDTO} into an {@link Advertiser} entity. Defaults are
     * applied for indexed, paid, and verified flags. The associated User is resolved via {@link
     * IUserService}.
     *
     * @param requestDTO the request DTO; may be {@code null}
     * @return new Advertiser entity, or {@code null} if the input is {@code null}
     */
    @Override
    public Advertiser toEntity(AdvertiserRequestDTO requestDTO) {
        if (requestDTO == null) return null;
        return new Advertiser(
                requestDTO.getTitle(),
                requestDTO.getDescription(),
                requestDTO.getBannerUrl(),
                requestDTO.getAvatarUrl(),
                requestDTO.getPublicLocation(),
                requestDTO.getPublicUrlLocation(),
                true, // default indexed
                false, // default paid
                false, // default verified
                userService.findById(UUID.fromString(requestDTO.getUserId())));
    }

    /**
     * Converts a collection of {@link Advertiser} entities into a list of {@link
     * AdvertiserResponseDTO}.
     *
     * @param entities collection of Advertiser entities; may be {@code null} or empty
     * @return list of response DTOs; empty list if input is null or empty
     */
    @Override
    public List<AdvertiserResponseDTO> toListResponseDTO(Collection<Advertiser> entities) {
        if (entities == null || entities.isEmpty()) return Collections.emptyList();
        List<AdvertiserResponseDTO> responseDTOS = new ArrayList<>(entities.size());
        for (Advertiser advertiser : entities) {
            responseDTOS.add(toResponseDTO(advertiser));
        }
        return responseDTOS;
    }
}
