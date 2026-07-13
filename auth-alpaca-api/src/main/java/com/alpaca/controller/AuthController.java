package com.alpaca.controller;

import com.alpaca.dto.request.AuthLoginRequestDTO;
import com.alpaca.dto.request.AuthRequestDTO;
import com.alpaca.dto.response.AuthResponseDTO;
import com.alpaca.dto.response.RateLimitResult;
import com.alpaca.exception.RateLimitExceededException;
import com.alpaca.model.AuthCode;
import com.alpaca.model.UserPrincipal;
import com.alpaca.security.ratelimit.IPRateLimit;
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
 * REST controller for authentication operations at {@code /api/auth}.
 *
 * <p>Public endpoints: {@code POST /login}, {@code POST /register}, {@code POST /exchange} (OAuth2
 * auth code flow), and {@code GET /} (health). Authenticated endpoints ({@code @IsAuthenticated}):
 * {@code POST /logout}, {@code GET /me}.
 *
 * @see IAuthService
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;
    private final AuthenticationManager manager;
    private final IPRateLimit rateLimit;

    /**
     * Authenticates a user with email and password.
     *
     * <p>On success, issues an access token and a refresh token. The returned refresh token should
     * be provided on subsequent {@code POST /api/auth/rotate} calls via the {@code X-Refresh-Token}
     * header.
     *
     * @param requestDTO the credentials containing email and password; must not be {@code null}
     * @param clientId the OAuth2 client identifier, provided via {@code X-Client-Id} header
     * @param userAgent the user agent string, provided via {@code User-Agent} header
     * @param request the HTTP servlet request (used for client IP extraction)
     * @return {@link ResponseEntity} containing the {@link AuthResponseDTO} with status {@link
     *     HttpStatus#OK}
     * @throws org.springframework.security.authentication.BadCredentialsException if the email or
     *     password is invalid
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(
            @Valid @RequestBody AuthRequestDTO requestDTO,
            @RequestHeader("X-Client-Id") String clientId,
            @RequestHeader("User-Agent") String userAgent,
            HttpServletRequest request) {

        String clientIp = Utils.extractClientIP(request);
        RateLimitResult result = rateLimit.check(clientIp);
        if (!result.allowed()) {
            throw new RateLimitExceededException(result.retryAfterSeconds());
        }

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
                                clientIp)),
                HttpStatus.OK);
    }

    /**
     * Registers a new user with the provided credentials.
     *
     * <p>Creates a user account and immediately issues an access token and refresh token upon
     * successful registration.
     *
     * @param requestDTO the registration details containing email and password; must not be {@code
     *     null}
     * @param clientId the OAuth2 client identifier, provided via {@code X-Client-Id} header
     * @param userAgent the user agent string, provided via {@code User-Agent} header
     * @param request the HTTP servlet request (used for client IP extraction)
     * @return {@link ResponseEntity} containing the {@link AuthResponseDTO} with status {@link
     *     HttpStatus#OK}
     * @throws com.alpaca.exception.BadRequestException if the email is already registered or the
     *     input is invalid
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(
            @Valid @RequestBody AuthRequestDTO requestDTO,
            @RequestHeader("X-Client-Id") String clientId,
            @RequestHeader("User-Agent") String userAgent,
            HttpServletRequest request) {

        String clientIp = Utils.extractClientIP(request);
        RateLimitResult result = rateLimit.check(clientIp);
        if (!result.allowed()) {
            throw new RateLimitExceededException(result.retryAfterSeconds());
        }

        return ResponseEntity.ok(
                authService.register(
                        new AuthLoginRequestDTO(
                                requestDTO.getEmail(),
                                requestDTO.getPassword(),
                                clientId,
                                userAgent,
                                clientIp)));
    }

    /**
     * Logs out the current user by revoking the provided refresh token.
     *
     * <p>The identified session is invalidated server-side. Subsequent requests using the revoked
     * refresh token will be rejected.
     *
     * @param refreshToken the refresh token to revoke, provided via {@code X-Refresh-Token} header
     * @param clientId the client identifier, provided via {@code X-Client-Id} header
     * @param userAgent the user agent string, provided via {@code User-Agent} header
     * @param user the currently authenticated user; may be {@code null}
     * @param request the HTTP servlet request (used for client IP extraction)
     * @return {@link ResponseEntity} with a confirmation message and status {@link HttpStatus#OK},
     *     or {@link HttpStatus#UNAUTHORIZED} if not authenticated
     */
    @PostMapping("/logout")
    @IsAuthenticated
    public ResponseEntity<String> logout(
            @RequestHeader("X-Refresh-Token") String refreshToken,
            @RequestHeader("X-Client-Id") String clientId,
            @RequestHeader("User-Agent") String userAgent,
            @AuthenticationPrincipal UserPrincipal user,
            HttpServletRequest request) {
                
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        authService.logout(refreshToken, clientId, userAgent, Utils.extractClientIP(request));
        return ResponseEntity.ok("{\"message\":\"Logout successful\"}");
    }

    /**
     * Exchanges an authorization code for authentication tokens (OAuth 2.0 Authorization Code flow
     * with PKCE).
     *
     * <p>The request body must contain {@code code}, {@code code_verifier}, {@code redirect_uri},
     * and {@code client_id}. PKCE verification is performed using the provided {@code
     * code_verifier}.
     *
     * @param request the HTTP servlet request (used for client IP extraction)
     * @param userAgent the user agent string, provided via {@code User-Agent} header
     * @param body a map containing the authorization grant parameters ({@code code}, {@code
     *     code_verifier}, {@code redirect_uri}, {@code client_id})
     * @return {@link ResponseEntity} containing the {@link AuthResponseDTO} with status {@link
     *     HttpStatus#OK}
     */
    @PostMapping("/exchange")
    public ResponseEntity<AuthResponseDTO> exchangeToken(
            HttpServletRequest request,
            @RequestHeader("User-Agent") String userAgent,
            @RequestBody Map<String, String> body) {

        String clientIp = Utils.extractClientIP(request);
        RateLimitResult result = rateLimit.check(clientIp);
        if (!result.allowed()) {
            throw new RateLimitExceededException(result.retryAfterSeconds());
        }

        String code = body.get("code");
        String codeVerifier = body.get("code_verifier");
        String redirectUri = body.get("redirect_uri");
        String clientId = body.get("client_id");
        return new ResponseEntity<>(
                authService.login(
                        new AuthCode(
                                code, codeVerifier, redirectUri, clientId, userAgent, clientIp)),
                HttpStatus.OK);
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
