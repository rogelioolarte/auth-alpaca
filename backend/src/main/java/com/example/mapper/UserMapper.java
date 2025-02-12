package com.example.mapper;

import com.example.dto.request.UserRequestDTO;
import com.example.dto.response.UserResponseDTO;
import com.example.entity.User;

public interface UserMapper extends GenericMapper<User, UserResponseDTO, UserRequestDTO> {

    UserResponseDTO toResponseDTO(User entity);

    User toEntity(UserRequestDTO requestDTO);

}
