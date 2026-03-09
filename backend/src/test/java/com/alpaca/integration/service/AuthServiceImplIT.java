package com.alpaca.integration.service;

import static org.junit.jupiter.api.Assertions.*;

import com.alpaca.dto.request.AuthLoginRequestDTO;
import com.alpaca.dto.response.AuthResponseDTO;
import com.alpaca.entity.RefreshToken;
import com.alpaca.entity.Role;
import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.exception.UnauthorizedException;
import com.alpaca.model.UserPrincipal;
import com.alpaca.resources.RefreshTokenProvider;
import com.alpaca.resources.UserProvider;
import com.alpaca.security.manager.JJwtManager;
import com.alpaca.security.manager.PasswordManager;
import com.alpaca.service.IRefreshTokenService;
import com.alpaca.service.impl.AuthServiceImpl;
import com.alpaca.service.impl.RoleServiceImpl;
import com.alpaca.service.impl.SessionServiceImpl;
import com.alpaca.service.impl.UserServiceImpl;
import java.time.Instant;
import java.util.HashSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for {@link AuthServiceImpl} */
@SpringBootTest
@Transactional
class AuthServiceImplIT {

    @Autowired private AuthServiceImpl service;

    @Autowired private UserServiceImpl userService;

    @Autowired private RoleServiceImpl roleService;

    @Autowired private SessionServiceImpl sessionService;

    @Autowired private IRefreshTokenService refreshTokenService;

    @Autowired private PasswordManager passwordManager;

    @Autowired private JJwtManager jwtManager;

    private User userTemplate;

    private AuthLoginRequestDTO request;

    @BeforeEach
    void setup() {

        userTemplate = UserProvider.singleTemplate();

        String rawPassword = userTemplate.getPassword();

        request =
                new AuthLoginRequestDTO(
                        userTemplate.getEmail(),
                        rawPassword,
                        "JUnit-Agent",
                        "test-client",
                        "127.0.0.1");
    }

    // ------------------------------------------------
    // setSecurityContextBefore
    // ------------------------------------------------

    @Test
    void setSecurityContextBeforeShouldThrowWhenAuthenticationNull() {

        assertThrows(UnauthorizedException.class, () -> service.setSecurityContextBefore(null));
    }

    @Test
    void setSecurityContextBeforeShouldSetAuthentication() {

        User user = userService.register(userTemplate);

        UserPrincipal principal = new UserPrincipal(user);

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null);

        service.setSecurityContextBefore(auth);

        assertEquals(auth, SecurityContextHolder.getContext().getAuthentication());
    }

    // ------------------------------------------------
    // register
    // ------------------------------------------------

    @Test
    @Transactional
    void registerShouldCreateUserAndReturnTokens() {
        roleService.save(new Role("USER", "", new HashSet<>()));
        AuthResponseDTO response = service.register(request);

        assertNotNull(response);
        assertNotNull(response.accessToken());
        assertNotNull(response.refreshToken());

        assertTrue(
                jwtManager.isValidAccessToken(
                        jwtManager.validateAccessToken(response.accessToken())));
    }

    @Test
    @Transactional
    void registerShouldFailWhenEmailAlreadyExists() {
        roleService.save(new Role("USER", "", new HashSet<>()));
        service.register(request);

        assertThrows(BadRequestException.class, () -> service.register(request));
    }

    // ------------------------------------------------
    // login
    // ------------------------------------------------

    @Test
    @Transactional
    void loginShouldGenerateTokens() {

        User user = userService.register(userTemplate);

        UserPrincipal principal = new UserPrincipal(user);

        AuthResponseDTO response = service.login(principal, request);

        assertNotNull(response);
        assertNotNull(response.accessToken());
        assertNotNull(response.refreshToken());
    }

    // ------------------------------------------------
    // logout
    // ------------------------------------------------

    @Test
    void logoutShouldFailWhenTokenBlank() {

        assertThrows(
                BadRequestException.class,
                () -> service.logout("", "client", "agent", "127.0.0.1"));
    }

    @Test
    void logoutShouldFailWhenTokenNotFound() {

        String fakeToken = "fake-token";

        assertThrows(
                NotFoundException.class,
                () -> service.logout(fakeToken, "client", "agent", "127.0.0.1"));
    }

    @Test
    @Transactional
    void logoutShouldFailWhenTokenAlreadyRevoked() {
        User user = userService.save(userTemplate);
        String rawToken = "raw-token";
        String jwtRT = jwtManager.createRefreshTokenHash(rawToken);
        RefreshToken token = RefreshTokenProvider.singleTemplate();
        token.setTokenHash(jwtRT);
        token.setExpiresAt(Instant.now().plusSeconds(50));
        token.setUser(user);
        token.setRevoked(true);

        refreshTokenService.save(token);

        assertThrows(
                BadRequestException.class,
                () ->
                        service.logout(
                                rawToken,
                                token.getClientId(),
                                token.getUserAgent(),
                                token.getIpAddress()));
    }

    @Test
    @Transactional
    void logoutShouldRevokeTokenAndClearContext() {
        roleService.save(new Role("USER", "", new HashSet<>()));
        AuthResponseDTO register = service.register(request);
        UsernamePasswordAuthenticationToken userToken =
                jwtManager.manageAuthentication(register.accessToken());

        AuthResponseDTO response = service.login((UserPrincipal) userToken.getPrincipal(), request);

        service.logout(
                response.refreshToken(),
                request.clientId(),
                request.userAgent(),
                request.clientIp());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    // ------------------------------------------------
    // loadUserByUsername
    // ------------------------------------------------

    @Test
    @Transactional
    void loadUserByUsernameShouldReturnPrincipal() {

        User user = userService.register(userTemplate);

        UserPrincipal principal = (UserPrincipal) service.loadUserByUsername(user.getEmail());

        assertNotNull(principal);
        assertEquals(user.getId(), principal.getId());
    }

    // ------------------------------------------------
    // validateUserDetails
    // ------------------------------------------------

    @Test
    void validateUserDetailsShouldFailWhenUserNull() {

        assertThrows(BadRequestException.class, () -> service.validateUserDetails(null, null));
    }

    @Test
    void validateUserDetailsShouldFailWhenPasswordInvalid() {

        User user = UserProvider.singleTemplate();

        user.setPassword(passwordManager.encodePassword(user.getPassword()));

        UserPrincipal principal = new UserPrincipal(user);

        assertThrows(
                BadRequestException.class,
                () -> service.validateUserDetails("wrong-password", principal));
    }

    @Test
    void validateUserDetailsShouldFailWhenAccountDisabled() {

        User user = UserProvider.createUser(false, true, true, true, true, true);

        String raw = user.getPassword();

        user.setPassword(passwordManager.encodePassword(raw));

        UserPrincipal principal = new UserPrincipal(user);

        assertThrows(
                UnauthorizedException.class, () -> service.validateUserDetails(raw, principal));
    }

    @Test
    void validateUserDetailsShouldReturnUserWhenValid() {

        User user = UserProvider.singleTemplate();

        String raw = user.getPassword();

        user.setPassword(passwordManager.encodePassword(raw));

        UserPrincipal principal = new UserPrincipal(user);

        UserDetails result = service.validateUserDetails(raw, principal);

        assertEquals(principal, result);
    }
}
