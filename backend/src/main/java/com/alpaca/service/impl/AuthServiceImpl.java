package com.alpaca.service.impl;

import com.alpaca.dto.request.AuthRequestDTO;
import com.alpaca.dto.response.AuthResponseDTO;
import com.alpaca.entity.Profile;
import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.exception.OAuth2AuthenticationProcessingException;
import com.alpaca.exception.UnauthorizedException;
import com.alpaca.model.UserPrincipal;
import com.alpaca.security.manager.JJwtManager;
import com.alpaca.security.manager.PasswordManager;
import com.alpaca.security.oauth2.userinfo.OAuth2UserInfo;
import com.alpaca.security.oauth2.userinfo.OAuth2UserInfoFactory;
import com.alpaca.service.IAuthService;
import com.alpaca.service.IProfileService;
import com.alpaca.service.IRoleService;
import com.alpaca.service.IUserService;
import java.util.Map;
import java.util.UUID;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Implementation of the {@link IAuthService} interface that handles authentication, user
 * registration, and OAuth2 authentication processing.
 *
 * <p>This service integrates with Spring Security and provides methods for handling login,
 * registration, and setting the security context for authenticated users. It also extends {@link
 * DefaultOAuth2UserService} to support OAuth2 authentication.
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl extends DefaultOAuth2UserService implements IAuthService {

  private final IRoleService roleService;
  private final IUserService userService;
  private final IProfileService profileService;
  private final JJwtManager manager;
  private final PasswordManager passwordManager;

  /**
   * Sets the security context for an authenticated user.
   *
   * @param authentication the authentication object containing user details.
   * @return the authenticated user's principal.
   * @throws UnauthorizedException if the authentication object is null.
   */
  public Object setSecurityContextBefore(Authentication authentication) {
    if (authentication == null) {
      throw new UnauthorizedException("The account has been deactivated or blocked");
    }
    SecurityContext context = SecurityContextHolder.getContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    return authentication.getPrincipal();
  }

  /**
   * Authenticates a user and returns an authentication response containing a JWT token.
   *
   * @param requestDTO the authentication request containing email and password.
   * @return an {@link AuthResponseDTO} containing the generated JWT token.
   * @throws NotFoundException if the email is not registered.
   */
  @Override
  public AuthResponseDTO login(AuthRequestDTO requestDTO) {
    return new AuthResponseDTO(
        manager.createToken(
            (UserPrincipal)
                setSecurityContextBefore(
                    authenticate(requestDTO.getEmail(), requestDTO.getPassword()))));
  }

  /**
   * Registers a new user and logs them in.
   *
   * @param requestDTO the registration request containing email and password.
   * @return an {@link AuthResponseDTO} containing the generated JWT token for the new user.
   * @throws BadRequestException if the email is already registered.
   */
  @Override
  public AuthResponseDTO register(AuthRequestDTO requestDTO) {
    if (userService.existsByEmail(requestDTO.getEmail()))
      throw new BadRequestException("Email already registered");
    userService.register(
        new User(
            requestDTO.getEmail(),
            passwordManager.encodePassword(requestDTO.getPassword()),
            roleService.getUserRoles()));
    return login(requestDTO);
  }

  /**
   * Loads a user by their username (email).
   *
   * @param username the email of the user.
   * @return the {@link UserDetails} object representing the authenticated user.
   * @throws UsernameNotFoundException if the user is not found.
   */
  @Override
  public UserDetails loadUserByUsername(String username) {
    return new UserPrincipal(userService.findByEmail(username), null);
  }

  /**
   * Validates a user's credentials.
   *
   * @param userDetails the user details retrieved from the database.
   * @param password the password provided by the user.
   * @return the validated {@link UserDetails} object.
   * @throws BadRequestException if the user is not found or the password is incorrect.
   */
  public UserDetails validateUserDetails(UserDetails userDetails, String password) {
    if (userDetails == null) throw new BadRequestException("Invalid Username or Password");
    if (password == null
        || password.isBlank()
        || !passwordManager.matches(password, userDetails.getPassword()))
      throw new BadRequestException("Invalid Password");
    if (!(userDetails.isEnabled()
        && userDetails.isAccountNonLocked()
        && userDetails.isAccountNonExpired()
        && userDetails.isCredentialsNonExpired()))
      throw new UnauthorizedException("The account has been deactivated or blocked");
    return userDetails;
  }

  /**
   * Authenticates a user and returns an {@link Authentication} object.
   *
   * @param username the email of the user.
   * @param password the password of the user.
   * @return an {@link Authentication} object containing the authenticated user's details.
   */
  public Authentication authenticate(String username, String password) {
    return new UsernamePasswordAuthenticationToken(
        validateUserDetails(loadUserByUsername(username), password), null);
  }

  /**
   * Loads a User from an OAuth2 authentication request.
   *
   * @param userRequest the OAuth2 user request.
   * @return an {@link OAuth2User} representing the authenticated user.
   * @throws OAuth2AuthenticationException if authentication fails.
   */
  @Override
  @Generated
  public OAuth2User loadUser(OAuth2UserRequest userRequest) {
    try {
      return processOAuth2User(userRequest, super.loadUser(userRequest));
    } catch (ResponseStatusException e) {
      throw new InternalAuthenticationServiceException(e.getReason());
    } catch (Exception e) {
      throw new InternalAuthenticationServiceException("Error while authentication: ");
    }
  }

  /**
   * Processes an OAuth2 user by extracting their information and either registering a new profile
   * or retrieving an existing user.
   *
   * @param request the OAuth2 user request.
   * @param user the authenticated OAuth2 user.
   * @return an {@link OAuth2User} containing the authenticated user's details.
   * @throws OAuth2AuthenticationProcessingException if the email is not found from the provider.
   */
  @Generated
  public OAuth2User processOAuth2User(OAuth2UserRequest request, OAuth2User user) {
    OAuth2UserInfo userInfo =
        OAuth2UserInfoFactory.getOAuth2UserInfo(
            request.getClientRegistration().getRegistrationId(), user.getAttributes());
    if (userInfo.getEmail() == null || userInfo.getEmail().isBlank())
      throw new OAuth2AuthenticationProcessingException("Email not found from Oauth2 Provider");
    return registerOrLoginOAuth2(
        userInfo.getEmail(),
        userInfo.getFirstName(),
        userInfo.getLastName(),
        userInfo.getImageUrl(),
        userInfo.getEmailVerified(),
        user.getAttributes());
  }

  public UserPrincipal registerOrLoginOAuth2(
      String email,
      String firstName,
      String lastName,
      String imageURL,
      boolean emailVerified,
      Map<String, Object> attributes) {
    if (email == null
        || email.isBlank()
        || firstName == null
        || firstName.isBlank()
        || lastName == null
        || lastName.isBlank()
        || imageURL == null
        || imageURL.isBlank())
      throw new BadRequestException("The account does not have enough information");
    if (!userService.existsByEmail(email)) {
      return new UserPrincipal(
          registerProfile(
              userService.register(
                  new User(
                      email,
                      passwordManager.encodePassword(UUID.randomUUID().toString()),
                      true,
                      true,
                      true,
                      true,
                      emailVerified,
                      true,
                      roleService.getUserRoles())),
              firstName,
              lastName,
              imageURL),
          attributes);
    } else {
      return new UserPrincipal(
          checkExistingUser(userService.findByEmail(email), emailVerified), attributes);
    }
  }

  /**
   * Registers a new profile for a user.
   *
   * @param user the user entity to associate with the profile.
   * @param firstName the First Name of user information.
   * @param lastName the Last Name of user information.
   * @param imageURL the Image URL of user information.
   * @return the registered {@link User} entity.
   * @throws BadRequestException if the user, userInfo, userId are null.
   */
  public User registerProfile(User user, String firstName, String lastName, String imageURL) {
    if (user == null
        || user.getId() == null
        || firstName == null
        || lastName == null
        || imageURL == null)
      throw new BadRequestException("Invalid credentials of OAuth2 Provider");
    user.setProfile(profileService.save(new Profile(firstName, lastName, "", imageURL, user)));
    return user;
  }

  /**
   * Checks if an existing user has connected their account to Google. If so, updates their account
   * status and re-registers them.
   *
   * @param user The existing user entity.
   * @param emailVerified The existing user has verified email
   * @return the updated {@link User} entity.
   */
  public User checkExistingUser(User user, boolean emailVerified) {
    if (!user.isAllowUser())
      throw new UnauthorizedException("The account has been deactivated or blocked");
    if (!user.isGoogleConnected()) {
      user.setGoogleConnected(true);
      return userService.register(user);
    }
    if (user.isEmailVerified() != emailVerified) {
      user.setEmailVerified(emailVerified);
      return userService.register(user);
    }
    return user;
  }
}
