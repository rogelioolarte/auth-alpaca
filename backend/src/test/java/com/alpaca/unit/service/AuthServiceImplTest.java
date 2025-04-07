package com.alpaca.unit.service;

import com.alpaca.dto.request.AuthRequestDTO;
import com.alpaca.dto.response.AuthResponseDTO;
import com.alpaca.entity.Profile;
import com.alpaca.entity.Role;
import com.alpaca.entity.User;
import com.alpaca.entity.intermediate.UserRole;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

  @Mock private RoleServiceImpl roleService;
  @Mock private UserServiceImpl userService;
  @Mock private ProfileServiceImpl profileService;
  @Mock private JJwtManager jJwtManager;
  @Mock private PasswordManager passwordManager;
  @Mock private OAuth2User oAuth2User;

  @InjectMocks private AuthServiceImpl service;

  @Test
  void setSecurityContextBefore() {
    assertThrows(UnauthorizedException.class, () -> service.setSecurityContextBefore(null));

    User user = UserProvider.singleEntity();
    UserPrincipal userDetails = new UserPrincipal(user, null);
    Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null);
    Object object = service.setSecurityContextBefore(authentication);
    assertNotNull(object);
    assertEquals(userDetails, object);
    assertEquals(authentication, SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void login() {
    User user = UserProvider.completeEntity();
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
    User userSecond = UserProvider.singleEntity();
    AuthRequestDTO requestSecond =
        new AuthRequestDTO(userSecond.getEmail(), userSecond.getPassword());
    when(userService.existsByEmail(userSecond.getEmail())).thenReturn(true);
    assertThrows(BadRequestException.class, () -> service.register(requestSecond));
    verify(userService).existsByEmail(userSecond.getEmail());

    User user = UserProvider.alternativeEntity();
    Role role = RoleProvider.alternativeEntity();
    AuthRequestDTO request = new AuthRequestDTO(user.getEmail(), user.getPassword());
    String mockedResponse = "mocked-jwt-token";
    when(userService.existsByEmail(user.getEmail())).thenReturn(false);
    when(roleService.getUserRoles()).thenReturn(new HashSet<>(Set.of(role)));
    user.setUserRoles(Set.of(new UserRole(user, role)));
    when(userService.register(any(User.class))).thenReturn(user);
    when(userService.findByEmail(request.getEmail())).thenReturn(user);
    when(passwordManager.matches(request.getPassword(), user.getPassword())).thenReturn(true);
    when(jJwtManager.createToken(any(UserPrincipal.class))).thenReturn(mockedResponse);
    AuthResponseDTO response = service.register(request);
    assertNotNull(response);
    assertEquals(mockedResponse, response.token());
    verify(userService).existsByEmail(user.getEmail());
    verify(roleService).getUserRoles();
    verify(userService).findByEmail(request.getEmail());
    verify(passwordManager).matches(request.getPassword(), user.getPassword());
    verify(jJwtManager).createToken(any(UserPrincipal.class));
  }

  @Test
  void loadUserByUsername() {
    User user = UserProvider.alternativeEntity();
    Role role = RoleProvider.alternativeEntity();
    user.setUserRoles(Set.of(new UserRole(user, role)));
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
    User userSecond =
        new User(
            "test@example.com",
            "encodedPassword",
            false,
            false,
            false,
            false,
            false,
            false,
            new HashSet<>());
    UserPrincipal userDetailsSecond = new UserPrincipal(userSecond, null);

    assertAll(
        () ->
            assertThrows(BadRequestException.class, () -> service.validateUserDetails(null, null)),
        () ->
            assertThrows(
                BadRequestException.class,
                () -> service.validateUserDetails(new UserPrincipal(new User(), null), null)),
        () ->
            assertThrows(
                BadRequestException.class,
                () -> service.validateUserDetails(new UserPrincipal(new User(), null), "   ")));

    when(passwordManager.matches("rawPassword", userSecond.getPassword())).thenReturn(false);
    assertThrows(
        BadRequestException.class,
        () -> service.validateUserDetails(userDetailsSecond, "rawPassword"));
    verify(passwordManager).matches("rawPassword", userSecond.getPassword());

    User userThird = UserProvider.notAllowEntity();
    UserPrincipal userDetailsThird = new UserPrincipal(userThird, null);
    when(passwordManager.matches(userDetailsThird.getPassword(), userThird.getPassword()))
        .thenReturn(true);

    assertThrows(
        UnauthorizedException.class,
        () -> service.validateUserDetails(userDetailsThird, userThird.getPassword()));
    verify(passwordManager).matches(userDetailsThird.getPassword(), userThird.getPassword());

    assertThrows(
        UnauthorizedException.class,
        () -> {
          User userDisabled =
              new User(
                  "test@example.com",
                  "encodedPassword",
                  false,
                  true,
                  true,
                  true,
                  true,
                  true,
                  new HashSet<>());
          UserPrincipal userDetailsDisabled = new UserPrincipal(userDisabled, null);
          when(passwordManager.matches(
                  userDetailsDisabled.getPassword(), userDisabled.getPassword()))
              .thenReturn(true);
          service.validateUserDetails(userDetailsDisabled, userDisabled.getPassword());
        });

    assertThrows(
        UnauthorizedException.class,
        () -> {
          User userExpired =
              new User(
                  "test@example.com",
                  "encodedPassword",
                  true,
                  false,
                  true,
                  true,
                  true,
                  true,
                  new HashSet<>());
          UserPrincipal userDetailsExpired = new UserPrincipal(userExpired, null);
          when(passwordManager.matches(userDetailsExpired.getPassword(), userExpired.getPassword()))
              .thenReturn(true);
          service.validateUserDetails(userDetailsExpired, userExpired.getPassword());
        });

    assertThrows(
        UnauthorizedException.class,
        () -> {
          User userLocked =
              new User(
                  "test@example.com",
                  "encodedPassword",
                  true,
                  true,
                  false,
                  true,
                  true,
                  true,
                  new HashSet<>());
          UserPrincipal userDetailsLocked = new UserPrincipal(userLocked, null);
          when(passwordManager.matches(userDetailsLocked.getPassword(), userLocked.getPassword()))
              .thenReturn(true);
          service.validateUserDetails(userDetailsLocked, userLocked.getPassword());
        });

    assertThrows(
        UnauthorizedException.class,
        () -> {
          User userCredentialsExpired =
              new User(
                  "test@example.com",
                  "encodedPassword",
                  true,
                  true,
                  true,
                  false,
                  false,
                  false,
                  new HashSet<>());
          UserPrincipal userDetailsCredentialsExpired =
              new UserPrincipal(userCredentialsExpired, null);
          when(passwordManager.matches(
                  userDetailsCredentialsExpired.getPassword(),
                  userCredentialsExpired.getPassword()))
              .thenReturn(true);
          service.validateUserDetails(
              userDetailsCredentialsExpired, userCredentialsExpired.getPassword());
        });

    User user = UserProvider.alternativeEntity();
    UserPrincipal userDetails = new UserPrincipal(user, null);
    when(passwordManager.matches(userDetails.getPassword(), user.getPassword())).thenReturn(true);
    UserDetails newUserDetails =
        service.validateUserDetails(userDetails, userDetails.getPassword());
    assertNotNull(newUserDetails);
    assertEquals(userDetails, newUserDetails);
    verify(passwordManager).matches(userDetails.getPassword(), user.getPassword());
  }

  @Test
  void authenticate() {
    User user = UserProvider.alternativeEntity();
    UserDetails userDetails = new UserPrincipal(user, null);
    when(userService.findByEmail(user.getEmail())).thenReturn(user);
    when(passwordManager.matches(userDetails.getPassword(), user.getPassword())).thenReturn(true);
    Authentication authentication = service.authenticate(user.getEmail(), user.getPassword());
    assertNotNull(authentication);
    assertEquals(userDetails, authentication.getPrincipal());
    verify(userService).findByEmail(user.getEmail());
    verify(passwordManager).matches(userDetails.getPassword(), user.getPassword());
  }

  @Test
  void loadUser() {}

  @Test
  void processOAuth2User() {}

  @Test
  void registerProfile() {
    User user = UserProvider.alternativeEntity();
    Profile profile = ProfileProvider.alternativeEntity();
    Map<String, Object> attributesThird =
        Map.of(
            "sub", user.getId(),
            "email", user.getEmail(),
            "name", profile.getFirstName(),
            "given_name", profile.getFirstName() + " " + profile.getLastName(),
            "family_name", profile.getLastName(),
            "picture", profile.getAvatarUrl(),
            "email_verified", true);

    OAuth2UserInfo userInfoThird =
        OAuth2UserInfoFactory.getOAuth2UserInfo("google", attributesThird);

    assertThrows(BadRequestException.class, () -> service.registerProfile(user, null));

    assertThrows(BadRequestException.class, () -> service.registerProfile(null, userInfoThird));

    User userSecond = new User();
    userSecond.setId(null);
    assertThrows(
        BadRequestException.class, () -> service.registerProfile(userSecond, userInfoThird));

    when(profileService.save(any(Profile.class))).thenReturn(profile);
    User newUser = service.registerProfile(user, userInfoThird);
    assertNotNull(newUser);
    assertNotNull(newUser.getProfile());
    assertEquals(profile, newUser.getProfile());
    verify(profileService).save(any(Profile.class));
  }

  @Test
  void checkExistingUser() {
    User userThird = UserProvider.notAllowEntity();
    Profile profileThird = ProfileProvider.alternativeEntity();
    Map<String, Object> attributesSecond =
        Map.of(
            "sub", userThird.getId(),
            "email", userThird.getEmail(),
            "name", profileThird.getFirstName(),
            "given_name", profileThird.getFirstName() + " " + profileThird.getLastName(),
            "family_name", profileThird.getLastName(),
            "picture", profileThird.getAvatarUrl(),
            "email_verified", false);
    Map<String, Object> attributesThird =
        Map.of(
            "sub", userThird.getId(),
            "email", userThird.getEmail(),
            "name", profileThird.getFirstName(),
            "given_name", profileThird.getFirstName() + " " + profileThird.getLastName(),
            "family_name", profileThird.getLastName(),
            "picture", profileThird.getAvatarUrl(),
            "email_verified", true);
    OAuth2UserInfo userInfoThird =
        OAuth2UserInfoFactory.getOAuth2UserInfo("google", attributesThird);
    OAuth2UserInfo userInfoSecond =
        OAuth2UserInfoFactory.getOAuth2UserInfo("google", attributesSecond);
    assertThrows(
        UnauthorizedException.class, () -> service.checkExistingUser(userThird, userInfoThird));

    User userSecond = UserProvider.alternativeEntity();
    userSecond.setGoogleConnected(false);
    userSecond.setEmailVerified(true);
    when(userService.register(any(User.class))).thenReturn(userSecond);
    User newUserSecond = service.checkExistingUser(userSecond, userInfoThird);
    assertEquals(userSecond.getId(), newUserSecond.getId());
    assertTrue(newUserSecond.isGoogleConnected());
    verify(userService).register(any(User.class));

    User userFourth = UserProvider.alternativeEntity();
    userFourth.setGoogleConnected(true);
    userFourth.setEmailVerified(false);
    when(userService.register(any(User.class))).thenReturn(userFourth);
    User newUserFourth = service.checkExistingUser(userFourth, userInfoThird);
    assertEquals(userFourth.getId(), newUserFourth.getId());
    assertEquals(userInfoThird.getEmailVerified(), newUserFourth.isEmailVerified());
    verify(userService, times(2)).register(any(User.class));

    User userFifth = UserProvider.alternativeEntity();
    userFifth.setGoogleConnected(true);
    userFifth.setEmailVerified(false);
    User newUserFifth = service.checkExistingUser(userFifth, userInfoSecond);
    assertEquals(userFifth.getId(), newUserFifth.getId());
    assertEquals(userInfoSecond.getEmailVerified(), newUserFifth.isEmailVerified());

    User user = UserProvider.alternativeEntity();
    user.setGoogleConnected(true);
    user.setEmailVerified(true);
    User newUser = service.checkExistingUser(user, userInfoThird);
    assertEquals(user.getId(), newUser.getId());
    assertEquals(userInfoThird.getEmailVerified(), newUser.isEmailVerified());
  }
}
