package com.alpaca.integration.service;

import static org.junit.jupiter.api.Assertions.*;

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
import com.alpaca.service.impl.RoleServiceImpl;
import com.alpaca.service.impl.UserServiceImpl;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for {@link UserServiceImpl} */
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class AuthServiceImplIT {

  @Autowired private UserServiceImpl userService;
  @Autowired private RoleServiceImpl roleService;
  @Autowired private AuthServiceImpl service;

  @Autowired private JJwtManager jJwtManager;
  @Autowired private PasswordManager passwordManager;

  private String RAW_PASSWORD;
  private User firstEntity;
  private User secondEntity;
  private User thirdEntity;
  private Role role;
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
    user = UserProvider.alternativeEntity();
    user.setPassword(passwordManager.encodePassword(secondEntity.getPassword()));
    RAW_PASSWORD = UserProvider.alternativeEntity().getPassword();
    userDetails = new UserPrincipal(user, null);
    Profile profile = ProfileProvider.alternativeEntity();
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
  @Transactional
  void loginCaseOne() {
    roleService.save(
        new Role(role.getRoleName(), role.getRoleDescription(), Collections.emptySet()));
    service.register(firstEntity.getEmail(), firstEntity.getPassword());
    AuthResponseDTO response = service.login(firstEntity.getEmail(), firstEntity.getPassword());
    assertNotNull(response);
    assertNotNull(response.token());
    assertTrue(
        jJwtManager.isValidToken(
            jJwtManager.getAllClaims(jJwtManager.validateToken(response.token()))));
  }

  // --- register ---
  @Test
  @Transactional
  void registerCaseOne() {
    roleService.save(
        new Role(role.getRoleName(), role.getRoleDescription(), Collections.emptySet()));
    service.register(firstEntity.getEmail(), firstEntity.getPassword());
    assertThrows(
        BadRequestException.class,
        () -> service.register(firstEntity.getEmail(), firstEntity.getPassword()));
  }

  @Test
  @Transactional
  void registerCaseTwo() {
    roleService.save(
        new Role(role.getRoleName(), role.getRoleDescription(), Collections.emptySet()));
    AuthResponseDTO response =
        service.register(secondEntity.getEmail(), secondEntity.getPassword());
    assertNotNull(response);
    assertTrue(
        jJwtManager.isValidToken(
            jJwtManager.getAllClaims(jJwtManager.validateToken(response.token()))));
  }

  // --- loadUserByUsername ---
  @Test
  @Transactional
  void loadUserByUsernameCaseOne() {
    roleService.save(
        new Role(role.getRoleName(), role.getRoleDescription(), Collections.emptySet()));
    User user =
        userService.register(
            new User(secondEntity.getEmail(), secondEntity.getPassword(), Collections.emptySet()));
    UserPrincipal loaded = (UserPrincipal) service.loadUserByUsername(secondEntity.getEmail());
    assertNotNull(loaded);
    assertEquals(new UserPrincipal(user, null), loaded);
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
    assertThrows(
        BadRequestException.class,
        () -> service.validateUserDetails(RAW_PASSWORD + " ", userDetails));
  }

  @Test
  void validateUserDetailsCaseFive() {
    String rawPassword = thirdEntity.getPassword();
    thirdEntity.setPassword(passwordManager.encodePassword(rawPassword));
    assertThrows(
        UnauthorizedException.class,
        () -> service.validateUserDetails(rawPassword, new UserPrincipal(thirdEntity, null)));
  }

  @Test
  void validateUserDetailsCaseSix() {
    User user = UserProvider.createUser(false, true, true, true, true, true);
    String rawPassword = user.getPassword();
    user.setPassword(passwordManager.encodePassword(rawPassword));
    assertThrows(
        UnauthorizedException.class,
        () -> service.validateUserDetails(rawPassword, new UserPrincipal(user, null)));
  }

  @Test
  void validateUserDetailsCaseSeven() {
    User user = UserProvider.createUser(true, false, true, true, true, true);
    String rawPassword = user.getPassword();
    user.setPassword(passwordManager.encodePassword(rawPassword));
    assertThrows(
        UnauthorizedException.class,
        () -> service.validateUserDetails(rawPassword, new UserPrincipal(user, null)));
  }

  @Test
  void validateUserDetailsCaseEight() {
    User user = UserProvider.createUser(true, true, false, true, true, true);
    String rawPassword = user.getPassword();
    user.setPassword(passwordManager.encodePassword(rawPassword));
    assertThrows(
        UnauthorizedException.class,
        () -> service.validateUserDetails(rawPassword, new UserPrincipal(user, null)));
  }

  @Test
  void validateUserDetailsCaseNine() {
    User user = UserProvider.createUser(true, true, true, false, false, false);
    String rawPassword = user.getPassword();
    user.setPassword(passwordManager.encodePassword(rawPassword));
    assertThrows(
        UnauthorizedException.class,
        () -> service.validateUserDetails(rawPassword, new UserPrincipal(user, null)));
  }

  @Test
  void validateUserDetailsCaseTen() {
    UserDetails result = service.validateUserDetails(secondEntity.getPassword(), userDetails);
    assertNotNull(result);
    assertEquals(userDetails, result);
  }

  // --- authenticate ---
  @Test
  @Transactional
  void authenticateCaseOne() {
    User newUser =
        userService.register(new User(user.getEmail(), user.getPassword(), Collections.emptySet()));
    Authentication auth = service.authenticate(secondEntity.getEmail(), secondEntity.getPassword());
    assertNotNull(auth);
    assertNotNull(auth.getPrincipal());
    assertEquals(new UserPrincipal(newUser, null), (UserPrincipal) auth.getPrincipal());
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
  @Transactional
  void registerOrLoginOAuth2CaseFive() {
    roleService.save(
        new Role(role.getRoleName(), role.getRoleDescription(), Collections.emptySet()));
    UserPrincipal userPrincipal =
        service.registerOrLoginOAuth2(
            userInfoGoogleNotVerified.getEmail(),
            userInfoGoogleNotVerified.getFirstName(),
            userInfoGoogleNotVerified.getLastName(),
            userInfoGoogleNotVerified.getImageUrl(),
            userInfoGoogleNotVerified.getEmailVerified(),
            attributesNotVerified);
    User newUser = userService.findByEmail(userInfoGoogleNotVerified.getEmail());
    assertNotNull(userPrincipal);
    assertEquals(new UserPrincipal(newUser, null), userPrincipal);
  }

  @Test
  @Transactional
  void registerOrLoginOAuth2CaseSix() {
    User newUser =
        userService.register(
            new User(secondEntity.getEmail(), secondEntity.getPassword(), Collections.emptySet()));
    UserPrincipal userPrincipal =
        service.registerOrLoginOAuth2(
            userInfoGoogle.getEmail(),
            userInfoGoogle.getFirstName(),
            userInfoGoogle.getLastName(),
            userInfoGoogle.getImageUrl(),
            userInfoGoogle.getEmailVerified(),
            attributesVerified);
    assertNotNull(userPrincipal);
    assertEquals(new UserPrincipal(newUser, null), userPrincipal);
  }

  // --- registerProfile ---
  @Test
  void registerProfileCaseOne() {
    assertThrows(
        BadRequestException.class, () -> service.registerProfile(null, "first", "last", "image"));
  }

  @Test
  void registerProfileCaseTwo() {
    user.setId(null);
    assertThrows(
        BadRequestException.class, () -> service.registerProfile(user, "first", "last", "image"));
  }

  @Test
  void registerProfileCaseThree() {
    assertThrows(
        BadRequestException.class, () -> service.registerProfile(user, null, "last", "image"));
  }

  @Test
  void registerProfileCaseFour() {
    assertThrows(
        BadRequestException.class, () -> service.registerProfile(user, "first", null, "image"));
  }

  @Test
  void registerProfileCaseFive() {
    assertThrows(
        BadRequestException.class, () -> service.registerProfile(user, "first", "last", null));
  }

  @Test
  @Transactional
  void registerProfileCaseSix() {
    roleService.save(
        new Role(role.getRoleName(), role.getRoleDescription(), Collections.emptySet()));
    User newUser =
        service.registerProfile(
            user,
            userInfoGoogle.getFirstName(),
            userInfoGoogle.getLastName(),
            userInfoGoogle.getImageUrl());
    assertNotNull(newUser);
    assertNotNull(newUser.getProfile());
    assertEquals(userInfoGoogle.getFirstName(), newUser.getProfile().getFirstName());
    assertEquals(userInfoGoogle.getLastName(), newUser.getProfile().getLastName());
    assertEquals(userInfoGoogle.getImageUrl(), newUser.getProfile().getAvatarUrl());
  }

  // --- checkExistingUser ---
  @Test
  void checkExistingUserCaseOne() {
    assertThrows(
        UnauthorizedException.class,
        () -> service.checkExistingUser(thirdEntity, userInfoGoogle.getEmailVerified()));
  }

  @Test
  @Transactional
  void checkExistingUserCaseTwo() {
    User beforeUser =
        userService.register(new User(user.getEmail(), user.getPassword(), Collections.emptySet()));
    beforeUser.setGoogleConnected(false);
    beforeUser.setEmailVerified(true);
    User afterUser = userService.register(beforeUser);
    User newUser = service.checkExistingUser(afterUser, userInfoGoogle.getEmailVerified());
    assertEquals(beforeUser.getId(), newUser.getId());
    assertTrue(newUser.isGoogleConnected());
  }

  @Test
  @Transactional
  void checkExistingUserCaseThree() {
    User beforeUser =
        userService.register(new User(user.getEmail(), user.getPassword(), Collections.emptySet()));
    beforeUser.setGoogleConnected(true);
    beforeUser.setEmailVerified(false);
    User afterUser = userService.register(beforeUser);
    User newUser = service.checkExistingUser(afterUser, userInfoGoogle.getEmailVerified());
    assertEquals(beforeUser.getId(), newUser.getId());
    assertEquals(userInfoGoogle.getEmailVerified(), newUser.isEmailVerified());
  }

  @Test
  @Transactional
  void checkExistingUserCaseFour() {
    user.setGoogleConnected(true);
    user.setEmailVerified(false);
    User newUser = service.checkExistingUser(user, userInfoGoogleNotVerified.getEmailVerified());
    assertEquals(user.getId(), newUser.getId());
    assertEquals(userInfoGoogleNotVerified.getEmailVerified(), newUser.isEmailVerified());
  }

  @Test
  @Transactional
  void checkExistingUserCaseFive() {
    user.setGoogleConnected(true);
    user.setEmailVerified(true);
    User newUser = service.checkExistingUser(user, userInfoGoogle.getEmailVerified());
    assertEquals(user.getId(), newUser.getId());
    assertEquals(userInfoGoogle.getEmailVerified(), newUser.isEmailVerified());
  }
}
