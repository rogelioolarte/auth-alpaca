package com.alpaca.controller;

import com.alpaca.dto.request.AuthLoginRequestDTO;
import com.alpaca.dto.request.AuthRequestDTO;
import com.alpaca.dto.response.AuthResponseDTO;
import com.alpaca.exception.UnauthorizedException;
import com.alpaca.model.UserPrincipal;
import com.alpaca.security.manager.TokenExchangeManager;
import com.alpaca.service.IAuthService;
import com.alpaca.utils.IsAuthenticated;
import com.alpaca.utils.Utils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
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
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;
    private final AuthenticationManager manager;
    private final TokenExchangeManager exchangeManager;

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
            @RequestHeader("User-Agent") String userAgent,
            HttpServletRequest request) {
        Authentication authentication =
                manager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                requestDTO.getEmail(), requestDTO.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return new ResponseEntity<>(
                authService.login(
                        ((UserPrincipal) authentication.getPrincipal()),
                        new AuthLoginRequestDTO(
                                requestDTO.getEmail(),
                                requestDTO.getPassword(),
                                clientId,
                                userAgent,
                                Utils.extractClientIP(request))),
                HttpStatus.OK);
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
            @RequestHeader("User-Agent") String userAgent,
            HttpServletRequest request) {
        return ResponseEntity.ok(
                authService.register(
                        new AuthLoginRequestDTO(
                                requestDTO.getEmail(),
                                requestDTO.getPassword(),
                                clientId,
                                userAgent,
                                Utils.extractClientIP(request))));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @RequestHeader("X-Refresh-Token") String refreshToken,
            @RequestHeader("X-Client-Id") String clientId,
            @RequestHeader("User-Agent") String userAgent,
            HttpServletRequest request) {
        authService.logout(refreshToken, clientId, userAgent, Utils.extractClientIP(request));
        return ResponseEntity.ok("{\"message\":\"Logout successful\"}");
    }

    @PostMapping("/exchange")
    public ResponseEntity<AuthResponseDTO> exchangeToken(@RequestBody Map<String, String> request) {
        String code = request.get("code");

        if (code == null || code.isEmpty()) {
            throw new UnauthorizedException("Exchange code is required");
        }

        return exchangeManager
                .consumeCode(code)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new UnauthorizedException("Code invalid or expired"));
    }

    /**
     * Retrieves the current authenticated user.
     *
     * @param user the authenticated user principal; may be {@code null} if not authenticated
     * @return {@link ResponseEntity} containing the {@link UserPrincipal} with status {@link
     *     HttpStatus#OK} or {@link HttpStatus#UNAUTHORIZED} if the user is not authenticated
     */
    @IsAuthenticated
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
