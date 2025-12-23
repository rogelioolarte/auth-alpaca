package com.alpaca.controller;

import com.alpaca.dto.request.AuthLoginRequestDTO;
import com.alpaca.dto.request.AuthRequestDTO;
import com.alpaca.dto.response.AuthResponseDTO;
import com.alpaca.model.UserPrincipal;
import com.alpaca.service.IAuthService;
import com.alpaca.service.IRefreshTokenService;
import com.alpaca.utils.Utils;
import jakarta.servlet.http.HttpServletRequest;
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
    private final IRefreshTokenService refreshTokenService;

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
    public ResponseEntity<AuthResponseDTO> login(
            @Valid @RequestBody AuthRequestDTO requestDTO,
            @RequestHeader("X-Client-Id") String clientId,
            @RequestHeader(value = "User-Agent") String userAgent,
            HttpServletRequest request) {
        return new ResponseEntity<>(
                authService.login(
                        new AuthLoginRequestDTO(requestDTO.getEmail(),
                                requestDTO.getPassword(),
                                clientId,
                                userAgent,
                                Utils.extractClientIP(request))
                ), HttpStatus.OK);
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
    public ResponseEntity<AuthResponseDTO> register(
            @Valid @RequestBody AuthRequestDTO requestDTO,
            @RequestHeader("X-Client-Id") String clientId,
            @RequestHeader(value = "User-Agent") String userAgent,
            HttpServletRequest request) {
        return ResponseEntity.ok(
                authService.register(
                        new AuthLoginRequestDTO(requestDTO.getEmail(),
                                requestDTO.getPassword(),
                                clientId,
                                userAgent,
                                Utils.extractClientIP(request))));
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
    @GetMapping
    public ResponseEntity<String> health() {
        return new ResponseEntity<>("API Online", HttpStatus.OK);
    }
}
