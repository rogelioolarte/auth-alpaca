package com.alpaca.unit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.alpaca.dto.request.AuthLoginRequestDTO;
import com.alpaca.dto.response.AuthResponseDTO;
import com.alpaca.entity.RefreshToken;
import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.exception.UnauthorizedException;
import com.alpaca.model.UserPrincipal;
import com.alpaca.resources.SessionProvider;
import com.alpaca.resources.UserProvider;
import com.alpaca.security.manager.JJwtManager;
import com.alpaca.security.manager.PasswordManager;
import com.alpaca.service.IRefreshTokenService;
import com.alpaca.service.IRoleService;
import com.alpaca.service.ISessionService;
import com.alpaca.service.IUserService;
import com.alpaca.service.impl.AuthServiceImpl;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/** Unit tests for {@link AuthServiceImpl} */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private IRoleService roleService;
    @Mock private IUserService userService;
    @Mock private ISessionService sessionService;
    @Mock private IRefreshTokenService refreshTokenService;
    @Mock private PasswordManager passwordManager;
    @Mock private JJwtManager manager;

    @InjectMocks private AuthServiceImpl service;

    private static final String RAW_PASSWORD = "rawPassword";
    private User existingUser;

    @BeforeEach
    void setup() {
        existingUser = UserProvider.alternativeEntity();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        clearInvocations(
                roleService,
                userService,
                sessionService,
                refreshTokenService,
                passwordManager,
                manager);
    }

    // ----- setSecurityContextBefore -----
    @Test
    void setSecurityContextBefore_whenNull_throwsUnauthorized() {
        assertThrows(UnauthorizedException.class, () -> service.setSecurityContextBefore(null));
    }

    @Test
    void setSecurityContextBefore_whenAuthProvided_setsSecurityContext() {
        UserPrincipal principal = new UserPrincipal(existingUser);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null);
        service.setSecurityContextBefore(auth);
        assertSame(auth, SecurityContextHolder.getContext().getAuthentication());
    }

    // ----- login -----
    @Test
    void login_success_returnsAuthResponse() {
        UserPrincipal userPrincipal = new UserPrincipal(existingUser);
        AuthLoginRequestDTO requestDTO =
                new AuthLoginRequestDTO(
                        existingUser.getEmail(), "pw", "clientId", "userAgent", "127.0.0.1");

        when(sessionService.createSession(any(), anyString(), anyString(), anyString()))
                .thenReturn(SessionProvider.singleEntity());
        when(refreshTokenService.generateJWTTokens(any(UserPrincipal.class), any()))
                .thenReturn(new AuthResponseDTO("access-token", "refresh-token"));

        AuthResponseDTO response = service.login(userPrincipal, requestDTO);

        assertNotNull(response);
        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        verify(sessionService)
                .createSession(
                        eq(userPrincipal.getId()),
                        eq("userAgent"),
                        eq("clientId"),
                        eq("127.0.0.1"));
        verify(refreshTokenService).generateJWTTokens(eq(userPrincipal), any());
    }

    // ----- register -----
    @Test
    void register_whenEmailAlreadyRegistered_throwsBadRequest() {
        AuthLoginRequestDTO dto =
                new AuthLoginRequestDTO(existingUser.getEmail(), RAW_PASSWORD, "c", "ua", "ip");
        when(userService.existsByEmail(existingUser.getEmail())).thenReturn(true);

        assertThrows(BadRequestException.class, () -> service.register(dto));
        verify(userService).existsByEmail(existingUser.getEmail());
    }

    @Test
    void register_success_encodesPassword_registersAndReturnsTokens() {
        AuthLoginRequestDTO dto =
                new AuthLoginRequestDTO("new@example.com", RAW_PASSWORD, "c", "ua", "ip");

        // Prepare returned user from register()
        User registered = new User(dto.email(), "encoded-password", new HashSet<>());
        registered.setId(existingUser.getId());

        when(userService.existsByEmail(dto.email())).thenReturn(false);
        when(passwordManager.encodePassword(RAW_PASSWORD)).thenReturn("encoded-password");
        when(roleService.getUserRoles()).thenReturn(java.util.Set.of()); // roles empty allowed
        // Capture the user passed to register
        when(userService.register(any(User.class))).thenReturn(registered);
        when(sessionService.createSession(any(), anyString(), anyString(), anyString()))
                .thenReturn(SessionProvider.singleEntity());
        when(refreshTokenService.generateJWTTokens(any(UserPrincipal.class), any()))
                .thenReturn(new AuthResponseDTO("access", "refresh"));

        AuthResponseDTO response = service.register(dto);

        assertNotNull(response);
        assertEquals("access", response.accessToken());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(passwordManager).encodePassword(RAW_PASSWORD);
        verify(userService).register(userCaptor.capture());
        User passed = userCaptor.getValue();
        assertEquals(dto.email(), passed.getEmail());
        assertEquals("encoded-password", passed.getPassword());
        verify(refreshTokenService).generateJWTTokens(any(UserPrincipal.class), any());
    }

    // ----- logout -----
    @Test
    void logout_whenRefreshTokenBlank_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> service.logout("   ", "client", "ua", "ip"));
    }

    @Test
    void logout_whenRefreshTokenNotFound_throwsNotFound() {
        String token = "rt";
        when(manager.createRefreshTokenHash(token)).thenReturn("hash");
        when(refreshTokenService.findByTokenHashSecure("hash")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.logout(token, "cid", "ua", "ip"));
        verify(refreshTokenService).findByTokenHashSecure("hash");
    }

    @Test
    void logout_whenRefreshTokenAlreadyRevoked_throwsBadRequest() {
        String token = "rt";
        when(manager.createRefreshTokenHash(token)).thenReturn("hash");
        RefreshToken rt = new RefreshToken();
        rt.setRevoked(true);
        when(refreshTokenService.findByTokenHashSecure("hash")).thenReturn(Optional.of(rt));

        assertThrows(BadRequestException.class, () -> service.logout(token, "cid", "ua", "ip"));
    }

    @Test
    void logout_success_revokesFamilyAndClearsSecurityContext() {
        String token = "rt";
        when(manager.createRefreshTokenHash(token)).thenReturn("hash");
        RefreshToken rt = new RefreshToken();
        rt.setRevoked(false);
        UUID familyId = UUID.randomUUID();
        rt.setFamilyId(familyId);
        when(refreshTokenService.findByTokenHashSecure("hash")).thenReturn(Optional.of(rt));

        service.logout(token, "cid", "ua", "ip");

        verify(refreshTokenService)
                .revokeRefreshTokensAndSessionByFamilyId(
                        eq(familyId), any(Instant.class), eq("logout-session"));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    // ----- loadUserByUsername -----
    @Test
    void loadUserByUsername_returnsUserDetails() {
        when(userService.findByEmail(existingUser.getEmail())).thenReturn(existingUser);
        UserDetails userDetails = service.loadUserByUsername(existingUser.getEmail());
        assertNotNull(userDetails);
        assertEquals(existingUser.getEmail(), userDetails.getUsername());
    }

    // ----- validateUserDetails -----
    @Test
    void validateUserDetails_whenUserDetailsNull_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> service.validateUserDetails("x", null));
    }

    @Test
    void validateUserDetails_whenRawPasswordBlank_throwsBadRequest() {
        UserPrincipal up = new UserPrincipal(new User());
        assertThrows(BadRequestException.class, () -> service.validateUserDetails(" ", up));
    }

    @Test
    void validateUserDetails_whenPasswordMismatch_throwsBadRequest() {
        User u = UserProvider.alternativeEntity();
        UserPrincipal up = new UserPrincipal(u, null);
        when(passwordManager.matches(anyString(), anyString())).thenReturn(false);
        assertThrows(
                BadRequestException.class, () -> service.validateUserDetails(RAW_PASSWORD, up));
    }

    @Test
    void validateUserDetails_whenAccountNotActive_throwsUnauthorized() {
        // Create a user principal with disabled account
        User u = UserProvider.createUser(true, true, true, true, true, true);
        // simulate account disabled -> isEnabled() false by setting flags appropriately
        // Note: adapt createUser signature to your provider; here we simulate by modifying the
        // returned user
        // For safety, construct a user manually if provider doesn't allow flags change.
        u.setEnabled(false);
        UserPrincipal up = new UserPrincipal(u, null);
        when(passwordManager.matches(anyString(), anyString())).thenReturn(true);

        assertThrows(
                UnauthorizedException.class,
                () -> service.validateUserDetails(u.getPassword(), up));
    }

    @Test
    void validateUserDetails_success_returnsUserDetails() {
        User user = UserProvider.alternativeEntity();
        UserPrincipal up = new UserPrincipal(user, null);
        when(passwordManager.matches(user.getPassword(), user.getPassword())).thenReturn(true);

        UserDetails result = service.validateUserDetails(user.getPassword(), up);

        assertNotNull(result);
        assertEquals(up, result);
    }
}
