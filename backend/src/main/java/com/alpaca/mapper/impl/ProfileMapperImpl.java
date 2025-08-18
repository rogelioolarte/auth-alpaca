package com.alpaca.mapper.impl;

import com.alpaca.dto.request.ProfileRequestDTO;
import com.alpaca.dto.response.ProfileResponseDTO;
import com.alpaca.entity.Profile;
import com.alpaca.mapper.ProfileMapper;
import com.alpaca.service.IUserService;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link ProfileMapper} for mapping between {@link Profile} entities and their
 * corresponding request and response DTOs.
 *
 * <p>This class uses {@link IUserService} to resolve the User entity from the given user ID in the
 * request DTO when constructing a Profile entity.
 *
 * <p>Responsibilities:
 *
 * <ul>
 *   <li>Convert a Profile entity to {@link ProfileResponseDTO}, including nested user details.
 *   <li>Construct a Profile entity from {@link ProfileRequestDTO}, resolving the associated User.
 *   <li>Transform collections of Profile entities into lists of ProfileResponseDTOs.
 *   <li>Handle null or empty inputs gracefully by returning null or empty collections.
 * </ul>
 *
 * <p>This class is annotated with {@link Component} to enable Spring's dependency injection and
 * uses Lombok's {@link RequiredArgsConstructor} for constructor-based injection (if required in
 * future).
 *
 * @see Profile
 * @see ProfileRequestDTO
 * @see ProfileResponseDTO
 * @see IUserService
 */
@Component
@RequiredArgsConstructor
public class ProfileMapperImpl implements ProfileMapper {

    private final IUserService userService;

    /**
     * Converts a {@link Profile} entity into a {@link ProfileResponseDTO}.
     *
     * @param entity the profile entity to convert; may be {@code null}.
     * @return the corresponding DTO, or {@code null} if the input is {@code null}.
     */
    @Override
    public ProfileResponseDTO toResponseDTO(Profile entity) {
        if (entity == null) return null;
        return new ProfileResponseDTO(
                entity.getId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getAddress(),
                entity.getAvatarUrl(),
                entity.getUser().getId(),
                entity.getUser().getEmail());
    }

    /**
     * Converts a {@link ProfileRequestDTO} into a {@link Profile} entity.
     *
     * <p>Retrieves the associated User using {@link IUserService} based on the provided user ID
     * string in the request DTO.
     *
     * @param requestDTO the request DTO; may be {@code null}.
     * @return the constructed Profile entity, or {@code null} if the input is {@code null}.
     */
    @Override
    public Profile toEntity(ProfileRequestDTO requestDTO) {
        if (requestDTO == null) return null;
        return new Profile(
                requestDTO.getFirstName(),
                requestDTO.getLastName(),
                requestDTO.getAddress(),
                requestDTO.getAvatarUrl(),
                userService.findById(UUID.fromString(requestDTO.getUserId())));
    }

    /**
     * Converts a collection of {@link Profile} entities into a list of {@link ProfileResponseDTO}.
     *
     * @param entities the collection of profile entities; may be {@code null} or empty.
     * @return a list of response DTOs, or an empty list if the input is null or empty.
     */
    @Override
    public List<ProfileResponseDTO> toListResponseDTO(Collection<Profile> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        List<ProfileResponseDTO> profileResponseDTOS = new ArrayList<>(entities.size());
        for (Profile profile : entities) {
            profileResponseDTOS.add(toResponseDTO(profile));
        }
        return profileResponseDTOS;
    }
}
