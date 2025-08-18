package com.alpaca.mapper.impl;

import com.alpaca.dto.request.UserRequestDTO;
import com.alpaca.dto.response.UserResponseDTO;
import com.alpaca.entity.User;
import com.alpaca.mapper.AdvertiserMapper;
import com.alpaca.mapper.ProfileMapper;
import com.alpaca.mapper.RoleMapper;
import com.alpaca.mapper.UserMapper;
import com.alpaca.service.IRoleService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementation of the {@link UserMapper} interface responsible for mapping between {@link User}
 * entities and their corresponding request and response DTOs.
 *
 * <p>This mapper delegates nested object transformations to other mappers:
 *
 * <ul>
 *   <li>{@link RoleMapper} for mapping user roles.
 *   <li>{@link ProfileMapper} for mapping the user profile.
 *   <li>{@link AdvertiserMapper} for mapping advertiser-related data.
 * </ul>
 *
 * <p>Additionally, it uses the {@link IRoleService} to retrieve role entities based on IDs provided
 * in the request DTO.
 *
 * <p>Key responsibilities:
 *
 * <ul>
 *   <li>Convert {@link User} entities into {@link UserResponseDTO} objects.
 *   <li>Convert {@link UserRequestDTO} objects into {@link User} entities.
 *   <li>Transform collections of {@link User} entities into lists of {@link UserResponseDTO}.
 *   <li>Handle null and empty inputs gracefully by returning safe defaults.
 * </ul>
 *
 * <p>This class is annotated with {@link Component} to enable Spring's dependency injection and
 * uses Lombok's {@link RequiredArgsConstructor} for constructor-based injection (if required in
 * future).
 *
 * @see User
 * @see UserRequestDTO
 * @see UserResponseDTO
 * @see RoleMapper
 * @see ProfileMapper
 * @see AdvertiserMapper
 */
@Component
@RequiredArgsConstructor
public class UserMapperImpl implements UserMapper {

    private final RoleMapper roleMapper;
    private final ProfileMapper profileMapper;
    private final AdvertiserMapper advertiserMapper;
    private final IRoleService roleService;

    /**
     * Converts a {@link User} entity into a {@link UserResponseDTO}.
     *
     * @param entity the user entity to convert, may be {@code null}.
     * @return the corresponding response DTO, or {@code null} if the input is {@code null}.
     */
    @Override
    public UserResponseDTO toResponseDTO(User entity) {
        if (entity == null) return null;
        return new UserResponseDTO(
                entity.getId(),
                entity.getEmail(),
                roleMapper.toListResponseDTO(entity.getRoles()),
                profileMapper.toResponseDTO(entity.getProfile()),
                advertiserMapper.toResponseDTO(entity.getAdvertiser()));
    }

    /**
     * Converts a {@link UserRequestDTO} into a {@link User} entity.
     *
     * <p>Roles are resolved using the {@link IRoleService} to fetch the corresponding entities.
     *
     * @param requestDTO the request DTO containing user input data, may be {@code null}.
     * @return the corresponding {@link User} entity, or {@code null} if the input is {@code null}.
     */
    @Override
    public User toEntity(UserRequestDTO requestDTO) {
        if (requestDTO == null) return null;
        return new User(
                requestDTO.getEmail(),
                requestDTO.getPassword(),
                roleService.findAllByIdsToSet(requestDTO.getRoles()));
    }

    /**
     * Converts a collection of {@link User} entities into a list of {@link UserResponseDTO}.
     *
     * @param entities the collection of user entities, may be {@code null} or empty.
     * @return a list of response DTOs, or an empty list if the input is {@code null} or empty.
     */
    @Override
    public List<UserResponseDTO> toListResponseDTO(Collection<User> entities) {
        if (entities == null || entities.isEmpty()) return Collections.emptyList();
        List<UserResponseDTO> userResponseDTOS = new ArrayList<>(entities.size());
        for (User user : entities) {
            userResponseDTOS.add(toResponseDTO(user));
        }
        return userResponseDTOS;
    }
}
