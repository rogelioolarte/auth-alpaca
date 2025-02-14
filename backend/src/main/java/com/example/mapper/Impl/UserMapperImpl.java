package com.example.mapper.impl;

import com.example.dto.request.UserRequestDTO;
import com.example.dto.response.UserResponseDTO;
import com.example.entity.User;
import com.example.mapper.AdvertiserMapper;
import com.example.mapper.ProfileMapper;
import com.example.mapper.RoleMapper;
import com.example.mapper.UserMapper;
import com.example.service.IRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UserMapperImpl implements UserMapper {

    private final RoleMapper roleMapper;
    private final ProfileMapper profileMapper;
    private final AdvertiserMapper advertiserMapper;
    private final IRoleService roleService;

    @Override
    public UserResponseDTO toResponseDTO(User entity) {
        if(entity == null) return null;
        return new UserResponseDTO(entity.getId(), entity.getEmail(),
                roleMapper.toListResponseDTO(entity.getRoles()),
                profileMapper.toResponseDTO(entity.getProfile()),
                advertiserMapper.toResponseDTO(entity.getAdvertiser()));
    }

    @Override
    public User toEntity(UserRequestDTO requestDTO) {
        if(requestDTO == null) return null;
        return new User(requestDTO.getEmail(), requestDTO.getPassword(),
                roleService.findAllByIdstoSet(requestDTO.getRoles()));
    }

    @Override
    public List<UserResponseDTO> toListResponseDTO(Collection<User> entities) {
        if(entities.isEmpty()) return Collections.emptyList();
        List<UserResponseDTO> userResponseDTOS = new ArrayList<>(entities.size());
        for(User user : entities) {
            userResponseDTOS.add(toResponseDTO(user));
        }
        return userResponseDTOS;
    }
}
