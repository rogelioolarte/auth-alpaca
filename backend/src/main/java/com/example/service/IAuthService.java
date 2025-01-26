package com.example.service;

import com.example.dto.request.AuthRequestDTO;
import com.example.dto.response.AuthResponseDTO;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface IAuthService extends UserDetailsService {
    AuthResponseDTO login(AuthRequestDTO requestDTO);
    AuthResponseDTO register(AuthRequestDTO requestDTO);
}
