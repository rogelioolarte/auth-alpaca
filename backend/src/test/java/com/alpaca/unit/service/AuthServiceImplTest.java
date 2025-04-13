package com.alpaca.unit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
import java.util.HashSet;
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
import org.springframework.security.oauth2.core.user.OAuth2User;

/** Unit tests for {@link AuthServiceImpl} */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

  @Mock private RoleServiceImpl roleService;
  @Mock private UserServiceImpl userService;
  @Mock private ProfileServiceImpl profileService;
  @Mock private JJwtManager jJwtManager;
  @Mock private PasswordManager passwordManager;
  @Mock private OAuth2User oAuth2User;

  @InjectMocks private AuthServiceImpl service;

  private static final String EMAIL = "test@example.com";
  private static final String PASSWORD = "encodedPassword";
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
    attributesVerified = createAttributes(secondEntity, profile, true);
    attributesNotVerified = createAttributes(secondEntity, profile, false);
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
    AuthRequestDTO request = new AuthRequestDTO(firstEntity.getEmail(), firstEntity.getPassword());
    when(userService.findByEmail(request.getEmail())).thenReturn(firstEntity);
    when(passwordManager.matches(request.getPassword(), firstEntity.getPassword()))
        .thenReturn(true);
    when(jJwtManager.createToken(any(UserPrincipal.class))).thenReturn(MOCKED_JWT);
    AuthResponseDTO response = service.login(request);
    assertNotNull(response);
    assertEquals(MOCKED_JWT, response.token());
    verify(userService).findByEmail(request.getEmail());
    verify(passwordManager).matches(request.getPassword(), firstEntity.getPassword());
    verify(jJwtManager).createToken(any(UserPrincipal.class));
  }

  // --- register ---
  @Test
  void registerCaseOne() {
    when(userService.existsByEmail(firstEntity.getEmail())).thenReturn(true);
    assertThrows(
        BadRequestException.class,
        () ->
            service.register(
                new AuthRequestDTO(firstEntity.getEmail(), firstEntity.getPassword())));
    verify(userService).existsByEmail(firstEntity.getEmail());
  }

  @Test
  void registerCaseTwo() {
    AuthRequestDTO request =
        new AuthRequestDTO(secondEntity.getEmail(), secondEntity.getPassword());
    secondEntity.setUserRoles(Set.of(new UserRole(secondEntity, role)));
    when(userService.existsByEmail(request.getEmail())).thenReturn(false);
    when(roleService.getUserRoles()).thenReturn(Set.of(role));
    when(userService.register(any(User.class))).thenReturn(secondEntity);
    when(userService.findByEmail(request.getEmail())).thenReturn(secondEntity);
    when(passwordManager.matches(request.getPassword(), secondEntity.getPassword()))
        .thenReturn(true);
    when(jJwtManager.createToken(any(UserPrincipal.class))).thenReturn(MOCKED_JWT);
    AuthResponseDTO response = service.register(request);
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
        () -> service.validateUserDetails(new UserPrincipal(new User(), null), null));
  }

  @Test
  void validateUserDetailsCaseThree() {
    assertThrows(
        BadRequestException.class,
        () -> service.validateUserDetails(new UserPrincipal(new User(), null), " "));
  }

  @Test
  void validateUserDetailsCaseFour() {
    when(passwordManager.matches(RAW_PASSWORD, secondEntity.getPassword())).thenReturn(false);
    assertThrows(
        BadRequestException.class,
        () -> service.validateUserDetails(new UserPrincipal(secondEntity, null), RAW_PASSWORD));
  }

  @Test
  void validateUserDetailsCaseFive() {
    when(passwordManager.matches(thirdEntity.getPassword(), thirdEntity.getPassword()))
        .thenReturn(true);
    assertThrows(
        UnauthorizedException.class,
        () ->
            service.validateUserDetails(
                new UserPrincipal(thirdEntity, null), thirdEntity.getPassword()));
  }

  @Test
  void validateUserDetailsCaseSix() {
    User user = createUser(false, true, true, true, true, true);
    when(passwordManager.matches(user.getPassword(), user.getPassword())).thenReturn(true);
    assertThrows(
        UnauthorizedException.class,
        () -> service.validateUserDetails(new UserPrincipal(user, null), user.getPassword()));
  }

  @Test
  void validateUserDetailsCaseSeven() {
    User user = createUser(true, false, true, true, true, true);
    when(passwordManager.matches(user.getPassword(), user.getPassword())).thenReturn(true);
    assertThrows(
        UnauthorizedException.class,
        () -> service.validateUserDetails(new UserPrincipal(user, null), user.getPassword()));
  }

  @Test
  void validateUserDetailsCaseEight() {
    User user = createUser(true, true, false, true, true, true);
    when(passwordManager.matches(user.getPassword(), user.getPassword())).thenReturn(true);
    assertThrows(
        UnauthorizedException.class,
        () -> service.validateUserDetails(new UserPrincipal(user, null), user.getPassword()));
  }

  @Test
  void validateUserDetailsCaseNine() {
    User user = createUser(true, true, true, false, false, false);
    when(passwordManager.matches(user.getPassword(), user.getPassword())).thenReturn(true);
    assertThrows(
        UnauthorizedException.class,
        () -> service.validateUserDetails(new UserPrincipal(user, null), user.getPassword()));
  }

  @Test
  void validateUserDetailsCaseTen() {
    when(passwordManager.matches(userDetails.getPassword(), user.getPassword())).thenReturn(true);
    UserDetails result = service.validateUserDetails(userDetails, userDetails.getPassword());
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

  // --- loadUser ---
  @Test
  void loadUser() {}

  // --- processOAuth2User ---
  @Test
  void processOAuth2User() {}

  // --- registerProfile ---
  @Test
  void registerProfileCaseOne() {
    assertThrows(BadRequestException.class, () -> service.registerProfile(user, null));
  }

  @Test
  void registerProfileCaseTwo() {
    assertThrows(BadRequestException.class, () -> service.registerProfile(null, userInfoGoogle));
  }

  @Test
  void registerProfileCaseThree() {
    User user = new User();
    user.setId(null);
    assertThrows(BadRequestException.class, () -> service.registerProfile(user, userInfoGoogle));
  }

  @Test
  void registerProfileCaseFour() {
    when(profileService.save(any(Profile.class))).thenReturn(profile);
    User newUser = service.registerProfile(user, userInfoGoogle);
    assertNotNull(newUser);
    assertNotNull(newUser.getProfile());
    assertEquals(profile, newUser.getProfile());
    verify(profileService).save(any(Profile.class));
  }

  // --- checkExistingUser ---
  @Test
  void checkExistingUserCaseOne() {
    assertThrows(
        UnauthorizedException.class, () -> service.checkExistingUser(thirdEntity, userInfoGoogle));
  }

  @Test
  void checkExistingUserCaseTwo() {
    User user = UserProvider.alternativeEntity();
    user.setGoogleConnected(false);
    user.setEmailVerified(true);
    when(userService.register(any(User.class))).thenReturn(user);
    User newUser = service.checkExistingUser(user, userInfoGoogle);
    assertEquals(user.getId(), newUser.getId());
    assertTrue(newUser.isGoogleConnected());
    verify(userService).register(any(User.class));
  }

  @Test
  void checkExistingUserCaseThree() {
    User user = UserProvider.alternativeEntity();
    user.setGoogleConnected(true);
    user.setEmailVerified(false);
    when(userService.register(any(User.class))).thenReturn(user);
    User newUser = service.checkExistingUser(user, userInfoGoogle);
    assertEquals(user.getId(), newUser.getId());
    assertEquals(userInfoGoogle.getEmailVerified(), newUser.isEmailVerified());
    verify(userService).register(any(User.class));
  }

  @Test
  void checkExistingUserCaseFour() {
    User user = UserProvider.alternativeEntity();
    user.setGoogleConnected(true);
    user.setEmailVerified(false);
    User newUser = service.checkExistingUser(user, userInfoGoogleNotVerified);
    assertEquals(user.getId(), newUser.getId());
    assertEquals(userInfoGoogleNotVerified.getEmailVerified(), newUser.isEmailVerified());
  }

  @Test
  void checkExistingUserCaseFive() {
    User user = UserProvider.alternativeEntity();
    user.setGoogleConnected(true);
    user.setEmailVerified(true);
    User newUser = service.checkExistingUser(user, userInfoGoogle);
    assertEquals(user.getId(), newUser.getId());
    assertEquals(userInfoGoogle.getEmailVerified(), newUser.isEmailVerified());
  }

  /** Utility to create an object of oauth2 attributes * */
  private Map<String, Object> createAttributes(User user, Profile profile, boolean emailVerified) {
    return Map.of(
        "sub", user.getId().toString(),
        "email", user.getEmail(),
        "name", profile.getFirstName(),
        "given_name", (profile.getFirstName() + " " + profile.getLastName()),
        "family_name", profile.getLastName(),
        "picture", profile.getAvatarUrl(),
        "email_verified", emailVerified);
  }

  /** Utility to create a User entity * */
  private User createUser(
      boolean enabled,
      boolean accountNonExpired,
      boolean accountNonLocked,
      boolean credentialsNonExpired,
      boolean emailVerified,
      boolean googleConnected) {
    return new User(
        EMAIL,
        PASSWORD,
        enabled,
        accountNonExpired,
        accountNonLocked,
        credentialsNonExpired,
        emailVerified,
        googleConnected,
        new HashSet<>());
  }
}
