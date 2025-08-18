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

/**
 * REST controller for authentication operations.
 *
 * <p>Provides endpoints for user login, registration, and retrieving the current authenticated
 * user. Utilizes {@link IAuthService} for authentication logic.
 *
 * @see IAuthService
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;

    /**
     * Authenticates a user based on provided credentials.
     *
     * @param requestDTO the authentication request containing email and password; must not be
     *     {@code null}
     * @return {@link ResponseEntity} containing the {@link AuthResponseDTO} with status {@link
     *     HttpStatus#OK}
     * @throws IllegalArgumentException if the provided credentials are invalid
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody AuthRequestDTO requestDTO) {
        return new ResponseEntity<>(
                authService.login(requestDTO.getEmail(), requestDTO.getPassword()), HttpStatus.OK);
    }

    /**
     * Registers a new user with the provided credentials.
     *
     * @param requestDTO the registration request containing email and password; must not be {@code
     *     null}
     * @return {@link ResponseEntity} containing the {@link AuthResponseDTO} with status {@link
     *     HttpStatus#OK}
     * @throws IllegalArgumentException if the provided credentials are invalid
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody AuthRequestDTO requestDTO) {
        return new ResponseEntity<>(
                authService.register(requestDTO.getEmail(), requestDTO.getPassword()),
                HttpStatus.OK);
    }

    /**
     * Retrieves the current authenticated user.
     *
     * @param user the authenticated user principal; may be {@code null} if not authenticated
     * @return {@link ResponseEntity} containing the {@link UserPrincipal} with status {@link
     *     HttpStatus#OK} or {@link HttpStatus#UNAUTHORIZED} if the user is not authenticated
     */
    @GetMapping("/me")
    public ResponseEntity<UserPrincipal> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal user) {
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        return ResponseEntity.ok(user);
    }

    /**
     * Health check endpoint to verify if the API is online.
     *
     * @return {@link ResponseEntity} containing a simple string message with status {@link
     *     HttpStatus#OK}
     */
    @GetMapping("/")
    public ResponseEntity<String> health() {
        return new ResponseEntity<>("API Online", HttpStatus.OK);
    }
}
