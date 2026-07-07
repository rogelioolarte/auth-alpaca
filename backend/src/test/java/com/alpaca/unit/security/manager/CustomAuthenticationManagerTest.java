package com.alpaca.unit.security.manager;

import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.UnauthorizedException;
import com.alpaca.security.manager.CustomAuthenticationManager;
import com.alpaca.security.manager.PasswordManager;
import com.alpaca.service.IAuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomAuthenticationManagerTest {

    @Mock private IAuthService userDetailsService;

    @Mock private PasswordManager passwordManager;

    @Mock private UserDetails userDetails;

    @Mock private Authentication authentication;

    @InjectMocks private CustomAuthenticationManager authManager;

    private final String password = "securePassword123";
    private final String encodedPassword = "encoded_hash";

    @Test
    @DisplayName("authenticate: Should return token when credentials and account status are valid")
    void authenticate_ShouldReturnToken_WhenValid() {
        String username = "rogelio.olarte";
        when(authentication.getName()).thenReturn(username);
        when(authentication.getCredentials()).thenReturn(password);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(userDetails.getPassword()).thenReturn(encodedPassword);
        when(passwordManager.matches(password, encodedPassword)).thenReturn(true);
        when(userDetails.isEnabled()).thenReturn(true);
        when(userDetails.isAccountNonLocked()).thenReturn(true);
        when(userDetails.isAccountNonExpired()).thenReturn(true);
        when(userDetails.isCredentialsNonExpired()).thenReturn(true);
        doReturn(Collections.emptyList()).when(userDetails).getAuthorities();

        Authentication result = authManager.authenticate(authentication);

        assertNotNull(result);
        assertEquals(userDetails, result.getPrincipal());
        assertEquals(password, result.getCredentials());
        verify(userDetailsService).loadUserByUsername(username);
    }

    @Test
    @DisplayName("supports: Should return true only for UsernamePasswordAuthenticationToken")
    void supports_ShouldReturnTrueForCompatibleClass() {
        assertTrue(authManager.supports(UsernamePasswordAuthenticationToken.class));
        assertFalse(authManager.supports(String.class));
    }

    @Test
    @DisplayName("validateUserDetails: Should throw BadRequestException when UserDetails is null")
    void validateUserDetails_ShouldThrowException_WhenUserNotFound() {
        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () -> authManager.validateUserDetails(password, null));
        assertEquals("Invalid Username or Password", exception.getReason());
    }

    @Test
    @DisplayName(
            "validateUserDetails: Should throw BadRequestException when password does not match")
    void validateUserDetails_ShouldThrowException_WhenPasswordMismatch() {
        when(userDetails.getPassword()).thenReturn(encodedPassword);
        when(passwordManager.matches(password, encodedPassword)).thenReturn(false);

        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () -> authManager.validateUserDetails(password, userDetails));
        assertEquals("Invalid Password", exception.getReason());
    }

    @Test
    @DisplayName("validateUserDetails: Should throw BadRequestException when password is blank")
    void validateUserDetails_ShouldThrowException_WhenPasswordIsBlank() {
        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () -> authManager.validateUserDetails("", userDetails));
        assertEquals("Invalid Password", exception.getReason());
    }

    @Test
    @DisplayName("validateUserDetails: Should throw UnauthorizedException when account is disabled")
    void validateUserDetails_ShouldThrowException_WhenAccountDisabled() {
        when(userDetails.getPassword()).thenReturn(encodedPassword);
        when(passwordManager.matches(password, encodedPassword)).thenReturn(true);
        when(userDetails.isEnabled()).thenReturn(false);

        UnauthorizedException exception =
                assertThrows(
                        UnauthorizedException.class,
                        () -> authManager.validateUserDetails(password, userDetails));
        assertEquals("The account has been deactivated or blocked", exception.getReason());
    }

    @Test
    @DisplayName("validateUserDetails: Should throw UnauthorizedException when account is locked")
    void validateUserDetails_ShouldThrowException_WhenAccountLocked() {
        when(userDetails.getPassword()).thenReturn(encodedPassword);
        when(passwordManager.matches(password, encodedPassword)).thenReturn(true);
        when(userDetails.isEnabled()).thenReturn(true);
        when(userDetails.isAccountNonLocked()).thenReturn(false);

        assertThrows(
                UnauthorizedException.class,
                () -> authManager.validateUserDetails(password, userDetails));
    }

    @Test
    @DisplayName("validateUserDetails: Should throw UnauthorizedException when account is expired")
    void validateUserDetails_ShouldThrowException_WhenAccountExpired() {
        when(userDetails.getPassword()).thenReturn(encodedPassword);
        when(passwordManager.matches(password, encodedPassword)).thenReturn(true);
        when(userDetails.isEnabled()).thenReturn(true);
        when(userDetails.isAccountNonLocked()).thenReturn(true);
        when(userDetails.isAccountNonExpired()).thenReturn(false);

        assertThrows(
                UnauthorizedException.class,
                () -> authManager.validateUserDetails(password, userDetails));
    }

    @Test
    @DisplayName("validateUserDetails: Should throw UnauthorizedException when credentials expired")
    void validateUserDetails_ShouldThrowException_WhenCredentialsExpired() {
        when(userDetails.getPassword()).thenReturn(encodedPassword);
        when(passwordManager.matches(password, encodedPassword)).thenReturn(true);
        when(userDetails.isEnabled()).thenReturn(true);
        when(userDetails.isAccountNonLocked()).thenReturn(true);
        when(userDetails.isAccountNonExpired()).thenReturn(true);
        when(userDetails.isCredentialsNonExpired()).thenReturn(false);

        assertThrows(
                UnauthorizedException.class,
                () -> authManager.validateUserDetails(password, userDetails));
    }
}
