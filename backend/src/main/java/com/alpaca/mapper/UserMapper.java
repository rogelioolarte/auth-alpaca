package com.alpaca.mapper;

import com.alpaca.dto.request.UserRequestDTO;
import com.alpaca.dto.response.UserResponseDTO;
import com.alpaca.entity.User;

public interface UserMapper extends GenericMapper<User, UserResponseDTO, UserRequestDTO> {

    UserResponseDTO toResponseDTO(User entity);

    User toEntity(UserRequestDTO requestDTO);

}
