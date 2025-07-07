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

@Component
@RequiredArgsConstructor
public class UserMapperImpl implements UserMapper {

    private final RoleMapper roleMapper;
    private final ProfileMapper profileMapper;
    private final AdvertiserMapper advertiserMapper;
    private final IRoleService roleService;

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

    @Override
    public User toEntity(UserRequestDTO requestDTO) {
        if (requestDTO == null) return null;
        return new User(
                requestDTO.getEmail(),
                requestDTO.getPassword(),
                roleService.findAllByIdsToSet(requestDTO.getRoles()));
    }

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
