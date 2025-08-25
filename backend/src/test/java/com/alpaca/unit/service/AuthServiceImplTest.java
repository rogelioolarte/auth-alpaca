package com.alpaca.unit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.alpaca.dto.response.AuthResponseDTO;
import com.alpaca.entity.Profile;
import com.alpaca.entity.Role;
import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.UnauthorizedException;
import com.alpaca.model.UserPrincipal;
import com.alpaca.resources.ProfileProvider;
import com.alpaca.resources.RoleProvider;
import com.alpaca.resources.UserProvider;
import com.alpaca.security.manager.JJwtManager;
import com.alpaca.security.manager.PasswordManager;
import com.alpaca.security.oauth2.userinfo.OAuth2UserInfo;
import com.alpaca.security.oauth2.userinfo.OAuth2UserInfoFactory;
import com.alpaca.service.impl.AuthServiceImpl;
import com.alpaca.service.impl.ProfileServiceImpl;
import com.alpaca.service.impl.RoleServiceImpl;
import com.alpaca.service.impl.UserServiceImpl;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    @Mock private RoleServiceImpl roleService;
    @Mock private UserServiceImpl userService;
    @Mock private ProfileServiceImpl profileService;
    @Mock private JJwtManager jJwtManager;
    @Mock private PasswordManager passwordManager;

    @InjectMocks private AuthServiceImpl service;

    private static final String RAW_PASSWORD = "rawPassword";
    private static final String MOCKED_JWT = "mocked-jwt-token";
    private User firstEntity;
    private User secondEntity;
    private User thirdEntity;
    private Role role;
    private Profile profile;
    private User user;
    private UserPrincipal userDetails;
    private Map<String, Object> attributesVerified;
    private Map<String, Object> attributesNotVerified;
    private OAuth2UserInfo userInfoGoogle;
    private OAuth2UserInfo userInfoGoogleNotVerified;

    @BeforeEach
    void setup() {
        firstEntity = UserProvider.singleEntity();
        secondEntity = UserProvider.alternativeEntity();
        thirdEntity = UserProvider.notAllowEntity();
        role = RoleProvider.alternativeEntity();
        profile = ProfileProvider.alternativeEntity();
        user = UserProvider.alternativeEntity();
        userDetails = new UserPrincipal(user, null);
        attributesVerified = UserProvider.createAttributes(secondEntity, profile, true);
        attributesNotVerified = UserProvider.createAttributes(secondEntity, profile, false);
        userInfoGoogle = OAuth2UserInfoFactory.getOAuth2UserInfo("google", attributesVerified);
        userInfoGoogleNotVerified =
                OAuth2UserInfoFactory.getOAuth2UserInfo("google", attributesNotVerified);
    }

    // --- setSecurityContextBefore ---
    @Test
    void setSecurityContextBeforeCaseOne() {
        assertThrows(UnauthorizedException.class, () -> service.setSecurityContextBefore(null));
    }

    @Test
    void setSecurityContextBeforeCaseTwo() {
        UserPrincipal userDetails = new UserPrincipal(firstEntity, null);
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null);
        Object result = service.setSecurityContextBefore(auth);
        assertEquals(userDetails, result);
        assertEquals(auth, SecurityContextHolder.getContext().getAuthentication());
    }

    // --- login ---
    @Test
    void loginCaseOne() {
        when(userService.findByEmail(firstEntity.getEmail())).thenReturn(firstEntity);
        when(passwordManager.matches(firstEntity.getPassword(), firstEntity.getPassword()))
                .thenReturn(true);
        when(jJwtManager.createToken(any(UserPrincipal.class))).thenReturn(MOCKED_JWT);
        AuthResponseDTO response = service.login(firstEntity.getEmail(), firstEntity.getPassword());
        assertNotNull(response);
        assertEquals(MOCKED_JWT, response.token());
        verify(userService).findByEmail(firstEntity.getEmail());
        verify(passwordManager).matches(firstEntity.getPassword(), firstEntity.getPassword());
        verify(jJwtManager).createToken(any(UserPrincipal.class));
    }

    // --- register ---
    @Test
    void registerCaseOne() {
        when(userService.existsByEmail(firstEntity.getEmail())).thenReturn(true);
        assertThrows(
                BadRequestException.class,
                () -> service.register(firstEntity.getEmail(), firstEntity.getPassword()));
        verify(userService).existsByEmail(firstEntity.getEmail());
    }

    @Test
    void registerCaseTwo() {
        secondEntity.setUserRoles(Set.of(role));
        when(userService.existsByEmail(secondEntity.getEmail())).thenReturn(false);
        when(roleService.getUserRoles()).thenReturn(Set.of(role));
        when(userService.register(any(User.class))).thenReturn(secondEntity);
        when(userService.findByEmail(secondEntity.getEmail())).thenReturn(secondEntity);
        when(passwordManager.matches(secondEntity.getPassword(), secondEntity.getPassword()))
                .thenReturn(true);
        when(jJwtManager.createToken(any(UserPrincipal.class))).thenReturn(MOCKED_JWT);
        AuthResponseDTO response =
                service.register(secondEntity.getEmail(), secondEntity.getPassword());
        assertNotNull(response);
        assertEquals(MOCKED_JWT, response.token());
    }

    // --- loadUserByUsername ---
    @Test
    void loadUserByUsernameCaseOne() {
        when(userService.findByEmail(secondEntity.getEmail())).thenReturn(secondEntity);
        UserDetails loaded = service.loadUserByUsername(secondEntity.getEmail());
        assertNotNull(loaded);
        assertEquals(secondEntity.getEmail(), loaded.getUsername());
    }

    // --- validateUserDetails ---
    @Test
    void validateUserDetailsCaseOne() {
        assertThrows(BadRequestException.class, () -> service.validateUserDetails(null, null));
    }

    @Test
    void validateUserDetailsCaseTwo() {
        assertThrows(
                BadRequestException.class,
                () -> service.validateUserDetails(null, new UserPrincipal(new User(), null)));
    }

    @Test
    void validateUserDetailsCaseThree() {
        assertThrows(
                BadRequestException.class,
                () -> service.validateUserDetails(" ", new UserPrincipal(new User(), null)));
    }

    @Test
    void validateUserDetailsCaseFour() {
        when(passwordManager.matches(RAW_PASSWORD, secondEntity.getPassword())).thenReturn(false);
        assertThrows(
                BadRequestException.class,
                () ->
                        service.validateUserDetails(
                                RAW_PASSWORD, new UserPrincipal(secondEntity, null)));
    }

    @Test
    void validateUserDetailsCaseFive() {
        when(passwordManager.matches(thirdEntity.getPassword(), thirdEntity.getPassword()))
                .thenReturn(true);
        assertThrows(
                UnauthorizedException.class,
                () ->
                        service.validateUserDetails(
                                thirdEntity.getPassword(), new UserPrincipal(thirdEntity, null)));
    }

    @Test
    void validateUserDetailsCaseSix() {
        User user = UserProvider.createUser(false, true, true, true, true, true);
        when(passwordManager.matches(user.getPassword(), user.getPassword())).thenReturn(true);
        assertThrows(
                UnauthorizedException.class,
                () ->
                        service.validateUserDetails(
                                user.getPassword(), new UserPrincipal(user, null)));
    }

    @Test
    void validateUserDetailsCaseSeven() {
        User user = UserProvider.createUser(true, false, true, true, true, true);
        when(passwordManager.matches(user.getPassword(), user.getPassword())).thenReturn(true);
        assertThrows(
                UnauthorizedException.class,
                () ->
                        service.validateUserDetails(
                                user.getPassword(), new UserPrincipal(user, null)));
    }

    @Test
    void validateUserDetailsCaseEight() {
        User user = UserProvider.createUser(true, true, false, true, true, true);
        when(passwordManager.matches(user.getPassword(), user.getPassword())).thenReturn(true);
        assertThrows(
                UnauthorizedException.class,
                () ->
                        service.validateUserDetails(
                                user.getPassword(), new UserPrincipal(user, null)));
    }

    @Test
    void validateUserDetailsCaseNine() {
        User user = UserProvider.createUser(true, true, true, false, false, false);
        when(passwordManager.matches(user.getPassword(), user.getPassword())).thenReturn(true);
        assertThrows(
                UnauthorizedException.class,
                () ->
                        service.validateUserDetails(
                                user.getPassword(), new UserPrincipal(user, null)));
    }

    @Test
    void validateUserDetailsCaseTen() {
        when(passwordManager.matches(userDetails.getPassword(), user.getPassword()))
                .thenReturn(true);
        UserDetails result = service.validateUserDetails(userDetails.getPassword(), userDetails);
        assertNotNull(result);
        assertEquals(userDetails, result);
    }

    // --- authenticate ---
    @Test
    void authenticateCaseOne() {
        when(userService.findByEmail(user.getEmail())).thenReturn(user);
        when(passwordManager.matches(user.getPassword(), user.getPassword())).thenReturn(true);
        Authentication auth = service.authenticate(user.getEmail(), user.getPassword());
        assertNotNull(auth);
        assertEquals(new UserPrincipal(user, null), auth.getPrincipal());
    }

    // --- registerOrLoginOAuth2 ---
    @Test
    void registerOrLoginOAuth2CaseOne() {
        assertThrows(
                BadRequestException.class,
                () -> service.registerOrLoginOAuth2(null, "first", "last", "image", false, null));

        assertThrows(
                BadRequestException.class,
                () -> service.registerOrLoginOAuth2(" ", "first", "last", "image", false, null));
    }

    @Test
    void registerOrLoginOAuth2CaseTwo() {
        assertThrows(
                BadRequestException.class,
                () -> service.registerOrLoginOAuth2("email", null, "last", "image", false, null));

        assertThrows(
                BadRequestException.class,
                () -> service.registerOrLoginOAuth2("email", " ", "last", "image", false, null));
    }

    @Test
    void registerOrLoginOAuth2CaseThree() {
        assertThrows(
                BadRequestException.class,
                () -> service.registerOrLoginOAuth2("email", "first", null, "image", false, null));

        assertThrows(
                BadRequestException.class,
                () -> service.registerOrLoginOAuth2("email", "first", " ", "image", false, null));
    }

    @Test
    void registerOrLoginOAuth2CaseFour() {
        assertThrows(
                BadRequestException.class,
                () -> service.registerOrLoginOAuth2("email", "first", "last", null, false, null));

        assertThrows(
                BadRequestException.class,
                () -> service.registerOrLoginOAuth2("email", "first", "last", " ", false, null));
    }

    @Test
    void registerOrLoginOAuth2CaseFive() {
        when(userService.existsByEmail(userInfoGoogleNotVerified.getEmail())).thenReturn(false);
        when(userService.register(any())).thenReturn(user);
        UserPrincipal userPrincipal =
                service.registerOrLoginOAuth2(
                        userInfoGoogleNotVerified.getEmail(),
                        userInfoGoogleNotVerified.getFirstName(),
                        userInfoGoogleNotVerified.getLastName(),
                        userInfoGoogleNotVerified.getImageUrl(),
                        userInfoGoogleNotVerified.getEmailVerified(),
                        attributesNotVerified);
        assertNotNull(userPrincipal);
        assertEquals(new UserPrincipal(user, attributesNotVerified), userPrincipal);
        verify(userService).existsByEmail(userInfoGoogleNotVerified.getEmail());
        verify(userService).register(any());
    }

    @Test
    void registerOrLoginOAuth2CaseSix() {
        when(userService.existsByEmail(userInfoGoogle.getEmail())).thenReturn(true);
        when(userService.findByEmail(userInfoGoogle.getEmail())).thenReturn(user);
        when(userService.register(any())).thenReturn(user);
        UserPrincipal userPrincipal =
                service.registerOrLoginOAuth2(
                        userInfoGoogle.getEmail(),
                        userInfoGoogle.getFirstName(),
                        userInfoGoogle.getLastName(),
                        userInfoGoogle.getImageUrl(),
                        userInfoGoogle.getEmailVerified(),
                        attributesVerified);
        assertNotNull(userPrincipal);
        assertEquals(new UserPrincipal(user, attributesVerified), userPrincipal);
        verify(userService).existsByEmail(userInfoGoogle.getEmail());
        verify(userService).findByEmail(userInfoGoogle.getEmail());
        verify(userService).register(any());
    }

    // --- registerProfile ---
    @Test
    void registerProfileCaseOne() {
        assertThrows(
                BadRequestException.class,
                () -> service.registerProfile(null, "first", "last", "image"));
    }

    @Test
    void registerProfileCaseTwo() {
        user.setId(null);
        assertThrows(
                BadRequestException.class,
                () -> service.registerProfile(user, "first", "last", "image"));
    }

    @Test
    void registerProfileCaseThree() {
        assertThrows(
                BadRequestException.class,
                () -> service.registerProfile(user, null, "last", "image"));
    }

    @Test
    void registerProfileCaseFour() {
        assertThrows(
                BadRequestException.class,
                () -> service.registerProfile(user, "first", null, "image"));
    }

    @Test
    void registerProfileCaseFive() {
        assertThrows(
                BadRequestException.class,
                () -> service.registerProfile(user, "first", "last", null));
    }

    @Test
    void registerProfileCaseSix() {
        when(profileService.save(any(Profile.class))).thenReturn(profile);
        User newUser =
                service.registerProfile(
                        user,
                        userInfoGoogle.getFirstName(),
                        userInfoGoogle.getLastName(),
                        userInfoGoogle.getImageUrl());
        assertNotNull(newUser);
        assertNotNull(newUser.getProfile());
        assertEquals(profile, newUser.getProfile());
        verify(profileService).save(any(Profile.class));
    }

    // --- checkExistingUser ---
    @Test
    void checkExistingUserCaseOne() {
        assertThrows(
                UnauthorizedException.class,
                () -> service.checkExistingUser(thirdEntity, userInfoGoogle.getEmailVerified()));
    }

    @Test
    void checkExistingUserCaseTwo() {
        user.setGoogleConnected(false);
        user.setEmailVerified(true);
        when(userService.register(any(User.class))).thenReturn(user);
        User newUser = service.checkExistingUser(user, userInfoGoogle.getEmailVerified());
        assertEquals(user, newUser);
        verify(userService).register(any(User.class));
    }

    @Test
    void checkExistingUserCaseThree() {
        user.setGoogleConnected(true);
        user.setEmailVerified(false);
        when(userService.register(any(User.class))).thenReturn(user);
        User newUser = service.checkExistingUser(user, userInfoGoogle.getEmailVerified());
        assertEquals(user, newUser);
        verify(userService).register(any(User.class));
    }

    @Test
    void checkExistingUserCaseFour() {
        user.setGoogleConnected(true);
        user.setEmailVerified(false);
        User newUser =
                service.checkExistingUser(user, userInfoGoogleNotVerified.getEmailVerified());
        assertEquals(user, newUser);
    }

    @Test
    void checkExistingUserCaseFive() {
        user.setGoogleConnected(true);
        user.setEmailVerified(true);
        User newUser = service.checkExistingUser(user, userInfoGoogle.getEmailVerified());
        assertEquals(user, newUser);
    }
}
