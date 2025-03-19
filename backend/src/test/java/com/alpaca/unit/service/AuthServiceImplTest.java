package com.alpaca.unit.service;

import com.alpaca.dto.request.AuthRequestDTO;
import com.alpaca.dto.response.AuthResponseDTO;
import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.UnauthorizedException;
import com.alpaca.model.UserPrincipal;
import com.alpaca.resources.UserProvider;
import com.alpaca.security.manager.JJwtManager;
import com.alpaca.security.manager.PasswordManager;
import com.alpaca.service.impl.AuthServiceImpl;
import com.alpaca.service.impl.ProfileServiceImpl;
import com.alpaca.service.impl.RoleServiceImpl;
import com.alpaca.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private RoleServiceImpl roleService;
    @Mock
    private UserServiceImpl userService;
    @Mock
    private ProfileServiceImpl profileService;
    @Mock
    private JJwtManager jJwtManager;
    @Mock
    private PasswordManager passwordManager;

    @InjectMocks
    private AuthServiceImpl service;

    public static final OAuth2UserRequest SAMPLE_USER_REQUEST =
            new OAuth2UserRequest(
                    ClientRegistration.withRegistrationId("app-service")
                            .clientId("982a1001-b033-48f6-b2e6-6b327f0a61eb")
                            .clientSecret("123456789")
                            .authorizationUri("https://auth.example.com/oauth/authorize")
                            .tokenUri("https://auth.example.com/oauth/token")
                            .userInfoUri("https://auth.example.com/userinfo")
                            .userNameAttributeName("email")
                            .clientName("User Service")
                            .build(),
                    new OAuth2AccessToken(
                            OAuth2AccessToken.TokenType.BEARER,
                            "mocked-access-token",
                            Instant.now(),
                            Instant.now().plusSeconds(3600)
                    ),
                    Map.of(
                            "sub", "982a1001-b033-48f6-b2e6-6b327f0a61eb",
                            "email", "user@user.com",
                            "enabled", true
                    )
            );


    @Test
    void setSecurityContextBefore() {
        assertThrows(UnauthorizedException.class, () -> service.setSecurityContextBefore(null));

        User user = UserProvider.singleEntity();
        UserPrincipal userDetails = new UserPrincipal(user,null);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails,null);
        Object object = service.setSecurityContextBefore(authentication);
        assertNotNull(object);
        assertEquals(userDetails, object);
        assertEquals(authentication, SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void login() {
        User user = UserProvider.alternativeEntity();
        AuthRequestDTO request = new AuthRequestDTO(user.getEmail(), user.getPassword());
        String mockedResponse = "mocked-jwt-token";
        when(userService.findByEmail(request.getEmail())).thenReturn(user);
        when(passwordManager.matches(request.getPassword(), user.getPassword())).thenReturn(true);
        when(jJwtManager.createToken(any(UserPrincipal.class))).thenReturn(mockedResponse);
        AuthResponseDTO response = service.login(request);
        assertNotNull(response);
        assertEquals(mockedResponse, response.token());
        verify(userService).findByEmail(request.getEmail());
        verify(passwordManager).matches(request.getPassword(), user.getPassword());
        verify(jJwtManager).createToken(any(UserPrincipal.class));
    }

    @Test
    void register() {
        User user = UserProvider.alternativeEntity();
        AuthRequestDTO request = new AuthRequestDTO(user.getEmail(), user.getPassword());
        String mockedResponse = "mocked-jwt-token";
        when(userService.register(user)).thenReturn(user);
        when(userService.findByEmail(request.getEmail())).thenReturn(user);
        when(passwordManager.matches(request.getPassword(), user.getPassword())).thenReturn(true);
        when(jJwtManager.createToken(any(UserPrincipal.class))).thenReturn(mockedResponse);
        AuthResponseDTO response = service.login(request);
        assertNotNull(response);
        assertEquals(mockedResponse, response.token());
        verify(userService).findByEmail(request.getEmail());
        verify(passwordManager).matches(request.getPassword(), user.getPassword());
        verify(jJwtManager).createToken(any(UserPrincipal.class));
    }

    @Test
    void loadUserByUsername() {
        User user = UserProvider.alternativeEntity();
        when(userService.findByEmail(user.getEmail())).thenReturn(user);
        UserDetails userDetails = service.loadUserByUsername(user.getEmail());
        assertNotNull(userDetails);
        assertEquals(user.getEmail(), userDetails.getUsername());
        assertEquals(user.getPassword(), userDetails.getPassword());
        assertEquals(user.getAuthorities(), userDetails.getAuthorities());
        verify(userService).findByEmail(user.getEmail());
    }

    @Test
    void validateUserDetails() {
        User userSecond = new User("test@example.com", "encodedPassword",
                false, false, false, false,
                false, false, null);
        UserPrincipal userDetailsSecond = new UserPrincipal(userSecond,null);

        assertThrows(BadRequestException.class, () -> service
                .validateUserDetails(null, null));

        assertThrows(BadRequestException.class, () -> service
                .validateUserDetails(userDetailsSecond, null));

        assertThrows(BadRequestException.class, () -> service
                .validateUserDetails(userDetailsSecond, "   "));

        when(passwordManager.matches("rawPassword", userSecond.getPassword()))
                .thenReturn(false);
        assertThrows(BadRequestException.class, () -> service
                .validateUserDetails(userDetailsSecond, "rawPassword"));
        verify(passwordManager).matches("rawPassword", userSecond.getPassword());


        User user = UserProvider.singleEntity();
        UserPrincipal userDetails = new UserPrincipal(user,null);
        when(passwordManager.matches(userDetails.getPassword(),
                user.getPassword())).thenReturn(true);
        UserDetails newUserDetails = service.validateUserDetails(userDetails, userDetails.getPassword());
        assertNotNull(newUserDetails);
        assertEquals(userDetails, newUserDetails);
        verify(passwordManager).matches(userDetails.getPassword(), user.getPassword());
    }

    @Test
    void authenticate() {
        User user = UserProvider.alternativeEntity();
        UserDetails userDetails = service.loadUserByUsername(user.getEmail());
        when(userService.findByEmail(user.getEmail())).thenReturn(user);
        when(passwordManager.matches(userDetails.getPassword(), user.getPassword())).thenReturn(true);
        Authentication authentication = service.authenticate(user.getEmail(), user.getPassword());
        assertNotNull(authentication);
        assertEquals(userDetails, authentication.getPrincipal());
    }

    @Test
    void loadUser() {
        OAuth2User oAuth2User = service.loadUser(SAMPLE_USER_REQUEST);
    }
}