package com.alpaca.controller;

import com.alpaca.dto.request.AuthRequestDTO;
import com.alpaca.dto.response.AuthResponseDTO;
import com.alpaca.model.UserPrincipal;
import com.alpaca.service.IAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("auth")
@RequiredArgsConstructor
public class AuthController {

  private final IAuthService authService;

  @PostMapping("/login")
  public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody AuthRequestDTO requestDTO) {
    return new ResponseEntity<>(
        authService.login(requestDTO.getEmail(), requestDTO.getPassword()), HttpStatus.OK);
  }

  @PostMapping("/register")
  public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody AuthRequestDTO requestDTO) {
    return new ResponseEntity<>(
        authService.register(requestDTO.getEmail(), requestDTO.getPassword()), HttpStatus.OK);
  }

  @GetMapping("/me")
  public ResponseEntity<UserPrincipal> getCurrentUser(@AuthenticationPrincipal UserPrincipal user) {
    return ResponseEntity.ok(user);
  }

  @GetMapping("/")
  public ResponseEntity<String> register() {
    return new ResponseEntity<>("API Online", HttpStatus.OK);
  }
}
